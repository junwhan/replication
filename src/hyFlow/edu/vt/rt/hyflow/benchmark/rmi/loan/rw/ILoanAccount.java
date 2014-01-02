package edu.vt.rt.hyflow.benchmark.rmi.loan.rw;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ILoanAccount extends Remote, Serializable{
	public void borrow(List<String> accountNums, int branching, boolean initiator, int amount) throws RemoteException;
	public int sum(List<String> accountNums, int branching) throws RemoteException;
	public Integer checkBalance() throws RemoteException;
	
	public void expandLock(Long txnId, boolean write, List<String> accountNums, int branching) throws InterruptedException, RemoteException;
	public void collapseLock(Long txnId, boolean write, List<String> accountNums, int branching) throws RemoteException;
	public void rLock() throws RemoteException, InterruptedException;
	public void rUnlock() throws RemoteException;
	public void wLock(Long txnId) throws RemoteException, InterruptedException;
	public void wUnlock(Long txnId) throws RemoteException;
}
