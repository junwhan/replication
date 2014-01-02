package edu.vt.rt.hyflow.benchmark.rmi.vacation.rw;

import java.rmi.RemoteException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.vt.rt.hyflow.benchmark.rmi.Lockable;
import edu.vt.rt.hyflow.benchmark.rmi.LockableRW;

public class Reservation extends LockableRW
		implements IReservation{
	
	private String id;
	private int total;
	private int used;
	private int price;
	
	private static final int INITIAL = 10; 
	
	public Reservation(String id, int price) throws RemoteException {
		super(id);
		this.id = id;
		this.price = price;
		this.total = INITIAL;
		this.used = 0;
	}

	public Object getId() {
		return id;
	}

	@Override
	public boolean isAvailable() {
		return total-used>0;
	}
	
	@Override
	public boolean reserve() {
		if(used==total)
			return false;
		used++;
		return true;
	}

	@Override
	public void release() {
		used--;
	}

	@Override
	public int getPrice() {
		return price;
	}

	@Override
	public void setPrice(int price) {
		this.price = price;
	}

	@Override
	public void retrieItem() {
		total--;
	}

}
