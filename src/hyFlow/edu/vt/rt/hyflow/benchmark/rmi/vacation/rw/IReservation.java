package edu.vt.rt.hyflow.benchmark.rmi.vacation.rw;

import java.rmi.RemoteException;

import edu.vt.rt.hyflow.benchmark.rmi.ILockabelRW;

public interface IReservation extends ILockabelRW{

	int getPrice() throws RemoteException;
	void setPrice(int price) throws RemoteException;

	void retrieItem() throws RemoteException;
	void release() throws RemoteException;
	boolean reserve() throws RemoteException;
	boolean isAvailable() throws RemoteException;
}
