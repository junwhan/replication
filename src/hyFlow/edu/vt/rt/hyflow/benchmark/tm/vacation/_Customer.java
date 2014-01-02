package edu.vt.rt.hyflow.benchmark.tm.vacation;

import java.util.LinkedList;
import java.util.List;

import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.transaction.Remote;

public class _Customer extends AbstractDistinguishable{

	private String id;
	private List<String> reservations = new LinkedList<String>();
	
	public _Customer(String id) {
		this.id = id;
	}

	@Override
	public Object getId() {
		return id;
	}
	private Long [] ts;
	public Long[] getTS(){
		return ts;
	}
	public void setTS(Long [] ts){
		// for DATS
		this.ts = ts;
	}
	@Remote
	public void addReservation(_ReservationInfo reservation) {
		reservations.add((String)reservation.getId());
	}

	@Remote
	public List<String> getReservations() {
		return reservations;
	}

}
