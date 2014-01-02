package edu.vt.rt.hyflow.benchmark.tm.skiplist;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.ContextDelegator;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;

public class _SkipNode<T extends Comparable<? super T>> extends AbstractDistinguishable {
	private String id;
	
	private String next[] = null;
	private T value = null;
	private int level;
	
	public _SkipNode(String id, int level, T value) {
		this.id = id;
		this.level = level;
		this.value = value;
		this.next = new String[level+1];
		
		AbstractContext context = ContextDelegator.getTopInstance();
		if (context == null)
			HyFlow.getLocator().register(this);
		else
			context.newObject(this); // add it to context publish-set
	}
	
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
	public String get_next(int level) {
		return next[level];
	}
	
	public void set_next(int level, String nxtval) {
		next[level] = nxtval;
	}
	
	public T get_value() {
		return value;
	}
	
	public void set_value(T newval) {
		value = newval;
	}
	
	public int get_level() {
		return level;
	}
	
	public void set_level(int newlevel) {
		level = newlevel;
	}	
}
