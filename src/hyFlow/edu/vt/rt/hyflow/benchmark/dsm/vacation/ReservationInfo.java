package edu.vt.rt.hyflow.benchmark.dsm.vacation;

import java.rmi.RemoteException;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.ContextDelegator;

import aleph.GlobalObject;

import edu.vt.rt.hyflow.benchmark.rmi.Lockable;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.transaction.Remote;

public class ReservationInfo  extends AbstractDistinguishable{

	private String id;
	private int price;
	private int type;
	private String resourceid;
	
	public ReservationInfo(String id, String resourceid, int price, int type){
		this.id = id;
		this.resourceid = resourceid;
		this.price = price;
		this.type = type;
		
		AbstractContext context = ContextDelegator.getInstance();
		if(context.getContextId()==null)
			new GlobalObject(this, ((AbstractDistinguishable)this).getId());	// publish it now	
		else
			context.newObject(this);	// add it to context publish-set 
	}
	
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
	public int getType() {
		return type;
	}

	public int getPrice() {
		return price;
	}

	public String getReservedResource() {
		return resourceid;
	}
}
