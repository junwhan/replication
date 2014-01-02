package edu.vt.rt.hyflow.benchmark.rmi;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ILockabelRW extends Remote, Serializable{
	public void wLock() throws RemoteException, InterruptedException;
	public void wUnlock() throws RemoteException;
	
	public void rLock() throws RemoteException, InterruptedException;
	public void rUnlock() throws RemoteException;
	
	public void destroy() throws RemoteException;
}
