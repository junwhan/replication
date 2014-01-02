package edu.vt.rt.hyflow.benchmark.rmi.vacation.rw;

import java.rmi.RemoteException;

import edu.vt.rt.hyflow.benchmark.rmi.ILockabelRW;

public interface IReservationInfo extends ILockabelRW{

	int getType() throws RemoteException;
	int getPrice() throws RemoteException;
	String getReservedResource() throws RemoteException;

}
