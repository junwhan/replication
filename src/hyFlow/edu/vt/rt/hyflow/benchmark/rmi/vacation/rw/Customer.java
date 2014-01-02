package edu.vt.rt.hyflow.benchmark.rmi.vacation.rw;

import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;

import edu.vt.rt.hyflow.benchmark.rmi.LockableRW;

public class Customer extends LockableRW
	implements ICustomer{
	
	private String id;
	private List<String> reservations = new LinkedList<String>();
	
	public Customer(String id) throws RemoteException {
		super(id);
		this.id = id;
	}

	public Object getId() {
		return id;
	}

	@Override
	public void addReservation(String reservation) {
		reservations.add(reservation);
	}

	@Override
	public List<String> getReservations() {
		return reservations;
	}

}
