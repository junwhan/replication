package edu.vt.rt.hyflow.benchmark.rmi;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ILockabel extends Remote, Serializable{
	public void lock() throws RemoteException, InterruptedException;
	public void unlock() throws RemoteException;
	
	public void destroy() throws RemoteException;
}
