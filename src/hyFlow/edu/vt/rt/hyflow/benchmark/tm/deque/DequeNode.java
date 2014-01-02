package edu.vt.rt.hyflow.benchmark.tm.deque;

import org.deuce.reflection.AddressUtil;
import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.Context;
import org.deuce.transaction.ContextDelegator;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.util.io.Logger;

public class DequeNode<T> extends AbstractDistinguishable
{
	private String id = null;
	private T value = null;
	private String prev = null;
	private String next = null;
	
	public static long id__ADDRESS__;
	public static long value__ADDRESS__;
	public static long prev__ADDRESS__;
	public static long next__ADDRESS__;
	
	{
		try {
			Logger.debug("Initializing DequeNode!");
			next__ADDRESS__ = AddressUtil.getAddress(DequeNode.class
					.getDeclaredField("next"));
			value__ADDRESS__ = AddressUtil.getAddress(DequeNode.class
					.getDeclaredField("value"));
			prev__ADDRESS__ = AddressUtil.getAddress(DequeNode.class
					.getDeclaredField("prev"));
			id__ADDRESS__ = AddressUtil.getAddress(DequeNode.class
					.getDeclaredField("id"));
		} catch (Exception e) {
			e.printStackTrace(Logger.levelStream[Logger.DEBUG]);
		}
	}
	
	public DequeNode(String id, T value, String prev, String next) {
		this.id = id;
		this.value = value;
		this.prev = prev;
		this.next = next;
		
		AbstractContext context = ContextDelegator.getTopInstance();
		if (context == null)
			HyFlow.getLocator().register(this);
		else
			context.newObject(this); // add it to context publish-set
	}
	
	public DequeNode(String id) {
		this(id, null, null, null);
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
	@Override
	public int hashCode() {
		return id.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof DequeNode))
			return false;
		DequeNode<T> obj2 = (DequeNode<T>)obj;
		return id.equals(obj2.id);
	}
	
	@SuppressWarnings("unchecked")
	public T getValue(Context c) {
		ContextDelegator.beforeReadAccess(this, value__ADDRESS__, c);
		final T result = (T) ContextDelegator.onReadAccess(this, this.value, value__ADDRESS__, c);
		return result;
	}
	
	public void setValue(T newval, Context c) {
		ContextDelegator.onWriteAccess(this, newval, value__ADDRESS__, c);
	}
	
	public String getNext(Context c) {
		ContextDelegator.beforeReadAccess(this, next__ADDRESS__, c);
		final String result = (String) ContextDelegator.onReadAccess(this, this.next, next__ADDRESS__, c);
		Logger.debug("DequeNode::getNext(this="+this.id+")="+result);
		return result;
	}
	
	public void setNext(String newval, Context c) {
		Logger.debug("DequeNode::setNext(this= "+this.id+" , newval= "+newval+" )");
		ContextDelegator.onWriteAccess(this, newval, next__ADDRESS__, c);
	}
	
	public String getPrev(Context c) {
		ContextDelegator.beforeReadAccess(this, prev__ADDRESS__, c);
		final String result = (String) ContextDelegator.onReadAccess(this, this.prev, prev__ADDRESS__, c);
		Logger.debug("DequeNode::getPrev(this="+this.id+")="+result);
		return result;
	}
	
	public void setPrev(String newval, Context c) {
		Logger.debug("DequeNode::setPrev(this= "+this.id+" , newval= "+newval+" )");
		ContextDelegator.onWriteAccess(this, newval, prev__ADDRESS__, c);
	}
	
}
