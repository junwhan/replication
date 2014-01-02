package edu.vt.rt.hyflow.benchmark.rmi.vacation;

import java.rmi.RemoteException;
import java.util.List;

import edu.vt.rt.hyflow.benchmark.rmi.ILockabel;

public interface ICustomer extends ILockabel{

	void addReservation(String reservation) throws RemoteException;
	List<String> getReservations() throws RemoteException;

}
