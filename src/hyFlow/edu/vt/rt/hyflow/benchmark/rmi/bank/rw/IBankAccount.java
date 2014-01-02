package edu.vt.rt.hyflow.benchmark.rmi.bank.rw;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IBankAccount extends Remote, Serializable{
	public void deposit(int dollars) throws RemoteException;
	public boolean withdraw(int dollars) throws RemoteException;
	public Integer checkBalance() throws RemoteException;
	
	public void wLock() throws RemoteException, InterruptedException;
	public void wUnlock() throws RemoteException;
	
	public void rLock() throws RemoteException, InterruptedException;
	public void rUnlock() throws RemoteException;
}
