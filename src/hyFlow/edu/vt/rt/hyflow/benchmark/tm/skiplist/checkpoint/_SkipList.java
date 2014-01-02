package edu.vt.rt.hyflow.benchmark.tm.skiplist.checkpoint;

import org.deuce.Atomic;
import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.ContextDelegator;

import edu.vt.rt.hyflow.HyFlow;
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
	
	public void printAll() {
		printRecursive(header);
	}
	
	@Atomic
	private void printRecursive(String next) {
		_SkipNode<T> x = getNode(next);
		if (x.get_value() != null)
			Logger.debug("Value = " + x.get_value());
		if (x.get_next(0) != null)
			printRecursive(x.get_next(0));
	}
	
	@Atomic
	public boolean contains(T value) {
		return contains(value, get_level(), header);
	}
	private boolean contains(T value, int i, String x_id) {
		_SkipNode<T> x = getNode(x_id);
		if (x.get_next(i) == null)
		{
			if (i==0) {
				return x!=null && 
					x.get_value() != null && 
					x.get_value().equals(value);
			} else {
				return contains(value, i-1, x_id);
			}
		}
		String pfx_id = x.get_next(i);
		_SkipNode<T> pfx = getNode(pfx_id);
		if (pfx.get_value().compareTo(value) > 0)
		{
			if (i == 0) {
				return x!=null && 
					x.get_value() != null && 
					x.get_value().equals(value);
			} else {
				return contains(value, i-1, x_id);
			}
		}
		return contains(value, i, pfx_id);
	}
	
	public void insert(T value) {
		String[] updates = new String[MAX_LEVEL+1];
		insert_recurse(value, get_level(), header, updates);
	}
	
	@Atomic
	private void insert_recurse(T value, int i, String x_id, String[] updates) {
		_SkipNode<T> x = getNode(x_id);
		String pfx_id;
		_SkipNode<T> pfx;
		
		if (x.get_next(i) == null)
		{
			updates[i] = x_id;
			// Next i
			if (i > 0) {
				// check same node on a lower level
				insert_recurse(value, i-1, x_id, updates);
			} else {
				insert_finish(value, x.get_next(0), updates);
			}
			return;
		}
		pfx_id = x.get_next(i);
		pfx = getNode(pfx_id);
		if (pfx.get_value().compareTo(value) >= 0)
		{
			updates[i] = x_id;
			// Next i
			if (i > 0) {
				// check same node on a lower level
				insert_recurse(value, i-1, x_id, updates);
			} else {
				insert_finish(value, x.get_next(0), updates);
			}
			return;
		}
		
		// Check next node, on same level
		insert_recurse(value, i, pfx_id, updates);
	}
	
	private void insert_finish(T value, String x_id, String[] updates) 
	{
		_SkipNode<T> x = getNode(x_id);
		int level = get_level();
		
		if (x == null || !x.get_value().equals(value)) {        
			int lvl = randomLevel();
			
			// record header updates
			if (lvl > level) {
				for (int i = level + 1; i <= lvl; i++) {
			    	updates[i] = header;
			    }
			    level = lvl;
			    set_level(level);
			}
			
			// insert new node
			String new_id = id+"-node-"+Integer.toHexString((int)(Math.random()*Integer.MAX_VALUE)); 
			x = new _SkipNode<T>(new_id, lvl, value);
			for (int i = 0; i <= lvl; i++) {
				_SkipNode<T> upd = getNode(updates[i]);
			    x.set_next(i, upd.get_next(i));
			    upd.set_next(i, new_id);
			}
		}
	}
	

	public void delete(T value) {
		String[] updates = new String[MAX_LEVEL+1];
		delete_recurse(value, level, header, updates);
	}
	
	@Atomic
	private void delete_recurse(T value, int i, String x_id, String[] updates) {
		_SkipNode<T> x = getNode(x_id);
		String pfx_id;
		_SkipNode<T> pfx;
		
		if (x.get_next(i) == null)
		{
			updates[i] = x_id;
			// Next i
			if (i > 0) {
				// check same node on a lower level
				delete_recurse(value, i-1, x_id, updates);
			} else {
				delete_finish(value, x.get_next(0), updates);
			}
			return;
		}
		pfx_id = x.get_next(i);
		pfx = getNode(pfx_id);
		if (pfx.get_value().compareTo(value) >= 0)
		{
			updates[i] = x_id;
			// Next i
			if (i > 0) {
				// check same node on a lower level
				delete_recurse(value, i-1, x_id, updates);
			} else {
				delete_finish(value, x.get_next(0), updates);
			}
			return;
		}
		
		// Check next node, on same level
		delete_recurse(value, i, pfx_id, updates);
	}
	
	private void delete_finish(T value, String x_id, String[] updates) {
		_SkipNode<T> x = getNode(x_id);
		int level = get_level();
	    
		if (x.get_value().equals(value)) {
			// remove x from list
			for (int i = 0; i <= level; i++) {
				_SkipNode<T> upd = getNode(updates[i]);
				if (!upd.get_next(i).equals(x_id))
					break;
				upd.set_next(i, x.get_next(i));
			}
			
			// un-register node
			HyFlow.getLocator().delete(x);
			
			// decrease list level if needed
			_SkipNode<T> fetch_header = getNode(header);
			while (level > 0 && fetch_header.get_next(level) == null) {
				level--;
			}
			set_level(level);
		 }
	}
}
