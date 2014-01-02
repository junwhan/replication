package edu.vt.rt.hyflow.benchmark.rmi.vacation;

import java.rmi.RemoteException;

import edu.vt.rt.hyflow.benchmark.rmi.ILockabel;

public interface IReservation extends ILockabel{

	int getPrice() throws RemoteException;
	void setPrice(int price) throws RemoteException;

	void retrieItem() throws RemoteException;
	void release() throws RemoteException;
	boolean reserve() throws RemoteException;
	boolean isAvailable() throws RemoteException;

}
