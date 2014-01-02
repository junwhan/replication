package edu.vt.rt.hyflow.benchmark.tm.skiplist;

import org.deuce.Atomic;
import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.ContextDelegator;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.benchmark.tm.skiplist.checkpoint.SkipNode;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.util.io.Logger;

// Inspired from: http://code.activestate.com/recipes/576930/
// And from: http://en.literateprograms.org/Skip_list_%28Java%29

public class _SkipList<T extends Comparable<? super T>> extends AbstractDistinguishable {

	public static final int MAX_LEVEL = 6;
	public static final double PROBABILITY = 0.5;
	String id;
	String header;
	Integer level;
	
	@Override
	public Object getId() {
		return id;
	}
	private Long [] ts;
	@Override
	public Long[] getTS(){
		return ts;
	}
	@Override
	public void setTS(Long [] ts){
		// for DATS
		this.ts = ts;
	}
	public static int randomLevel() {
	    int level = (int)(Math.log(1.0-Math.random()) / Math.log(1.0-PROBABILITY));
	    return Math.min(level, MAX_LEVEL);
	}
	
	public _SkipList(String id) {
		this.id = id;
		this.level = 0;
		
		// Create header node
		this.header = id+"-header";
		new _SkipNode<T>(this.header, MAX_LEVEL, null);
		
		AbstractContext context = ContextDelegator.getTopInstance();
		if (context == null)
			HyFlow.getLocator().register(this);
		else
			context.newObject(this); // add it to context publish-set
	}
	
	@SuppressWarnings("unchecked")
	public _SkipNode<T> getNode(String id) {
		if (id == null)
			return null;
		else
			return (_SkipNode<T>)HyFlow.getLocator().open(id);
	}
	
	private int get_level() {
		return level;
	}
	
	private void set_level(Integer newval) {
		level = newval;
	}
	
	
	
	@Atomic
	// Start with next="header"
	public void printRecursive(String next) {
		_SkipNode<T> x = getNode(next);
		if (x.get_value() != null)
			Logger.debug("Value = " + x.get_value());
		if (x.get_next(0) != null)
			printRecursive(x.get_next(0));
	}
	
	@Atomic
	public void printIterative() {
	    _SkipNode<T> x = getNode(header);
	    String next_id = x.get_next(0);
	    while (next_id != null) {
	    	x = getNode(next_id);
	        Logger.debug("Value = " + x.get_value());
	        next_id = x.get_next(0);
	    }
	}
	
	@Atomic
	public boolean contains(T value) {
		_SkipNode<T> pfx, x = getNode(header);
		int level = get_level();
		// in decreasing order of levels
		for (int i=level; i>=0; i--) {
			// keep going until end or passed
			while (true) {
				if (x.get_next(i) == null)
					break;
				pfx = getNode(x.get_next(i));
				if (pfx.get_value().compareTo(value) > 0)
					break;
				x = pfx;
			}
		}
		// result
		return x!=null && 
			x.get_value() != null && 
			x.get_value().equals(value);
	}
	
	@Atomic
	@SuppressWarnings("unchecked")
	public void insert(T value)
	{
		_SkipNode<T> fetch_header = getNode(header);
		_SkipNode<T> fetch_x, x = fetch_header;
		_SkipNode<T>[] update = new _SkipNode[MAX_LEVEL + 1];
		int level = get_level();
		
		// find and record updates
		for (int i = level; i >= 0; i--) {
			while (true) {
				if (x.get_next(i) == null)
					break;
				fetch_x = getNode(x.get_next(i));
				if (fetch_x.get_value().compareTo(value) >= 0)
					break;
				x = fetch_x;
			}
			update[i] = x; 
		}
		x = getNode(x.get_next(0));
	
		
		if (x == null || !x.get_value().equals(value)) {        
			int lvl = randomLevel();
			
			// record header updates
			if (lvl > level) {
				for (int i = level + 1; i <= lvl; i++) {
			    	update[i] = fetch_header;
			    }
			    level = lvl;
			    set_level(level);
			}
			
			// insert new node
			String new_id = id+"-node-"+Integer.toHexString((int)(Math.random()*Integer.MAX_VALUE)); 
			x = new _SkipNode<T>(new_id, lvl, value);
			for (int i = 0; i <= lvl; i++) {
			    x.set_next(i, update[i].get_next(i));
			    update[i].set_next(i, new_id);
			}
		}
	}
	
	@Atomic
	@SuppressWarnings("unchecked")
	public void delete(T value)
	{
	    _SkipNode<T> fetch_header = getNode(header);	
	    _SkipNode<T> fetch_x, x = fetch_header;
	    _SkipNode<T>[] update = new _SkipNode[MAX_LEVEL + 1];
	    int level = get_level();
	    
	    // find and record updates (Same as for insert)
		for (int i = level; i >= 0; i--) {
			while (true) {
				if (x.get_next(i) == null)
					break;
				fetch_x = getNode(x.get_next(i));
				if (fetch_x.get_value().compareTo(value) >= 0)
					break;
				x = fetch_x;
			}
			update[i] = x; 
		}
		x = getNode(x.get_next(0));
	    
		if (x.get_value().equals(value)) {
			// remove x from list
			String x_id = (String)x.getId();
			for (int i = 0; i <= level; i++) {
				if (!update[i].get_next(i).equals(x_id))
					break;
				update[i].set_next(i, x.get_next(i));
			}
			
			// un-register node
			HyFlow.getLocator().delete(x);
			
			// decrease list level if needed
			while (level > 0 && fetch_header.get_next(level) == null) {
				level--;
			}
			set_level(level);
		 }
	}
}
