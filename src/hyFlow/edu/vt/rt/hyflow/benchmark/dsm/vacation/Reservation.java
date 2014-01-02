package edu.vt.rt.hyflow.benchmark.dsm.vacation;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.ContextDelegator;

import aleph.GlobalObject;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;

public class Reservation extends AbstractDistinguishable{
	
	private String id;
	private int total;
	private int used;
	private int price;
	
	private static final int INITIAL = 10; 
	
	public Reservation(String id, int price){
		this.id = id;
		this.price = price;
		this.total = INITIAL;
		this.used = 0;
		
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
	public boolean isAvailable() {
		return total-used>0;
	}
	
	public boolean reserve() {
		if(used==total)
			return false;
		used++;
		return true;
	}

	public void release() {
		used--;
	}

	public int getPrice() {
		return price;
	}

	public void setPrice(int price) {
		this.price = price;
	}

	public void retrieItem() {
		total--;
	}

}
