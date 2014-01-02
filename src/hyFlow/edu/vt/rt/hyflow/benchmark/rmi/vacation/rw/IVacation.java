package edu.vt.rt.hyflow.benchmark.rmi.vacation.rw;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IVacation extends Remote, Serializable{

	public ICustomer createCustomer(String id) throws RemoteException;
	public void createReservation(String id, int price) throws RemoteException;

}
