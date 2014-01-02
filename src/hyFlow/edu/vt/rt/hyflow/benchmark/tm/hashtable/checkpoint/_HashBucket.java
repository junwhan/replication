package edu.vt.rt.hyflow.benchmark.tm.hashtable.checkpoint;

import java.util.ArrayList;
import java.util.List;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.ContextDelegator;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;

public class _HashBucket extends AbstractDistinguishable 
{
	private class Tuple 
	{
		public Object key, value;
		public Tuple(Object key, Object value)
		{
			this.key = key;
			this.value = value;
		}
	}
	
	private String id;
	private Long [] ts;
	public Long[] getTS(){
		return ts;
	}
	public void setTS(Long [] ts){
		// for DATS
		this.ts = ts;
	}
	private List<Tuple> content = new ArrayList<Tuple>();
	
	public _HashBucket(String id) {
		this.id = id;
		
		// Register
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
	
	public Object get(Object key)
	{
		for (Tuple t : content) {
			if (key.equals(t.key)) {
				return t.value;
			}
		}
		return null;
	}
	
	public boolean put(Object key, Object value) 
	{
		// TODO: what about an "immutable" version?
		for (Tuple t : content) {
			if (key.equals(t.key)) {
				t.value = value;
				return false;
			}
		}
		Tuple t = new Tuple(key, value);
		content.add(t);
		return true;
	}
	
	public boolean remove(Object key)
	{
		for (int i=0; i<content.size(); i++) {
			if (key.equals(content.get(i))) {
				content.remove(i);
				return true;
			}
		}
		return false;
	}
	
	public boolean contains(Object key)
	{
		for (Tuple t : content) {
			if (key.equals(t.key)) {
				return true;
			}
		}
		return false;
	}

}
