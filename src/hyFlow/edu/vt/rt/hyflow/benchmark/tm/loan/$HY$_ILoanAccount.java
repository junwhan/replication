package edu.vt.rt.hyflow.benchmark.tm.loan;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import edu.vt.rt.hyflow.core.tm.control.ControlContext;

public interface $HY$_ILoanAccount extends Remote, Serializable{
	public void borrow(Object id, ControlContext context, List<String> accountNums, int branching, boolean initiator, int amount) throws RemoteException;
	public int sum(Object id, ControlContext instance, List<String> accountNums, int branching) throws RemoteException;
	public Integer checkBalance(Object id, ControlContext context) throws RemoteException;
}
