package edu.vt.rt.hyflow.benchmark.rmi.loan;

import java.rmi.RemoteException;
import java.util.List;

import edu.vt.rt.hyflow.benchmark.rmi.ILockabel;

public interface ILoanAccount extends ILockabel{
	public void borrow(List<String> accountNums, int branching, boolean initiator, int amount) throws RemoteException;
	public int sum(List<String> accountNums, int branching) throws RemoteException;
	public Integer checkBalance() throws RemoteException;
	
	public void expandLock(Long txnId, List<String> accountNums, int branching) throws InterruptedException, RemoteException;
	public void collapseLock(Long txnId, List<String> accountNums, int branching) throws RemoteException;
	public void lock(Long txnId) throws RemoteException, InterruptedException;
	public void unlock(Long txnId) throws RemoteException;
}
