package edu.vt.rt.hyflow.benchmark.tm.experiment;

import java.util.ArrayList;
import java.util.List;

import org.deuce.reflection.AddressUtil;
import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.ContextDelegator;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.tm.NestedContext;
import edu.vt.rt.hyflow.util.io.Logger;

public class ExpObject extends AbstractDistinguishable 
{
	private String id;
	private Integer counter = 0;
	
	public static long id__ADDRESS__;
	public static long counter__ADDRESS__;
	
	{
		try {
			counter__ADDRESS__ = AddressUtil.getAddress(ExpObject.class
					.getDeclaredField("counter"));
			id__ADDRESS__ = AddressUtil.getAddress(ExpObject.class
					.getDeclaredField("id"));
		} catch (Exception e) {
			e.printStackTrace(Logger.levelStream[Logger.DEBUG]);
		}
	}
	
	public ExpObject(String id) {
		this.id = id;
		this.counter = 0;
		
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
	public int get(NestedContext tx) {
		ContextDelegator.beforeReadAccess(this, counter__ADDRESS__, tx);
		final int value = (Integer) ContextDelegator.onReadAccess(this, this.counter, counter__ADDRESS__, tx);
		return value;
	}
	
	public int inc(NestedContext tx) {
		ContextDelegator.beforeReadAccess(this, counter__ADDRESS__, tx);
		final int value = (Integer) ContextDelegator.onReadAccess(this, this.counter, counter__ADDRESS__, tx);
		ContextDelegator.onWriteAccess(this, value+1, counter__ADDRESS__, tx);
		return value+1;
	}
	
	public int dec(NestedContext tx) {
		ContextDelegator.beforeReadAccess(this, counter__ADDRESS__, tx);
		final int value = (Integer) ContextDelegator.onReadAccess(this, this.counter, counter__ADDRESS__, tx);
		ContextDelegator.onWriteAccess(this, value-1, counter__ADDRESS__, tx);
		return value-1;
	}
}
