package edu.vt.rt.hyflow.benchmark.rmi.vacation;

import java.rmi.RemoteException;

import edu.vt.rt.hyflow.benchmark.rmi.ILockabel;

public interface IReservationInfo extends ILockabel{

	int getType() throws RemoteException;
	int getPrice() throws RemoteException;
	String getReservedResource() throws RemoteException;

}
