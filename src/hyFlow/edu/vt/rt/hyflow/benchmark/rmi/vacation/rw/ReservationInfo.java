package edu.vt.rt.hyflow.benchmark.rmi.vacation.rw;

import java.rmi.RemoteException;

import edu.vt.rt.hyflow.benchmark.rmi.Lockable;
import edu.vt.rt.hyflow.benchmark.rmi.LockableRW;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.transaction.Remote;

public class ReservationInfo extends LockableRW
		implements IReservationInfo{

	private String id;
	private int price;
	private int type;
	private String resourceid;
	
	public ReservationInfo(String id, String resourceid, int price, int type) throws RemoteException {
		super(id);
		this.id = id;
		this.resourceid = resourceid;
		this.price = price;
		this.type = type;
	}
	
	public Object getId() {
		return id;
	}

	@Override
	public int getType() {
		return type;
	}

	@Override
	public int getPrice() {
		return price;
	}

	@Override
	public String getReservedResource() {
		return resourceid;
	}
}
