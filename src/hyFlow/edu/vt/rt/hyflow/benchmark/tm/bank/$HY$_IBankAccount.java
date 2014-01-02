package edu.vt.rt.hyflow.benchmark.tm.bank;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.Context;

import edu.vt.rt.hyflow.core.tm.control.ControlContext;

public interface $HY$_IBankAccount extends Remote, Serializable{
	public boolean withdraw(Object id, ControlContext context, int dollars) throws RemoteException;
	public void deposit(Object id, ControlContext context, int dollars) throws RemoteException;
	public Integer checkBalance(Object id, ControlContext context) throws RemoteException;
}
