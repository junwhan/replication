package edu.vt.rt.hyflow.benchmark.tm.vacation;

import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.transaction.Remote;

public class _ReservationInfo extends AbstractDistinguishable{

	private String id;
	private int price;
	private int type;
	private String resourceid;
	
	public _ReservationInfo(String id, String resourceid, int price, int type) {
		this.id = id;
		this.resourceid = resourceid;
		this.price = price;
		this.type = type;
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
	public int getType() {
		return type;
	}

	@Remote
	public int getPrice() {
		return price;
	}

	@Remote
	public String getReservedResource() {
		return resourceid;
	}
}
