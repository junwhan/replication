package edu.vt.rt.hyflow.benchmark.tm.vacation;

import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.transaction.Remote;

public class _Reservation extends AbstractDistinguishable{

	private String id;
	private int total;
	private int used;
	private int price;
	
	private static final int INITIAL = 10; 
	
	public _Reservation(String id, int price) {
		this.id = id;
		this.price = price;
		this.total = INITIAL;
		this.used = 0;
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
	@Remote
	public boolean isAvailable() {
		return total-used>0;
	}
	
	@Remote
	public boolean reserve() {
		if(used==total)
			return false;
		used++;
		return true;
	}

	@Remote
	public void release() {
		used--;
	}

	@Remote
	public int getPrice() {
		return price;
	}

	@Remote
	public void setPrice(int price) {
		this.price = price;
	}

	@Remote
	public void retrieItem() {
		total--;
	}

}
