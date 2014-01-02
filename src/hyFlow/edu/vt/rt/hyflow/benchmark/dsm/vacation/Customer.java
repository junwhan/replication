package edu.vt.rt.hyflow.benchmark.dsm.vacation;

import java.util.LinkedList;
import java.util.List;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.ContextDelegator;

import aleph.GlobalObject;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;

public class Customer extends AbstractDistinguishable{

	private String id;
	private List<String> reservations = new LinkedList<String>();
	
	public Customer(String id){
		this.id = id;
		
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
	public void addReservation(String reservation) {
		reservations.add(reservation);
	}

	public List<String> getReservations() {
		return reservations;
	}

}
