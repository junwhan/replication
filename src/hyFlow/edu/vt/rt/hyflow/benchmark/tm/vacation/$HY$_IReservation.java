package edu.vt.rt.hyflow.benchmark.tm.vacation;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

import edu.vt.rt.hyflow.core.tm.control.ControlContext;

public interface $HY$_IReservation extends Remote, Serializable{

	boolean isAvaliable(Object id, ControlContext context) throws RemoteException;
	boolean reserve(Object id, ControlContext context) throws RemoteException;
	void release(Object id, ControlContext context) throws RemoteException;
	int getPrice(Object id, ControlContext context) throws RemoteException;
	void setPrice(Object id, ControlContext context, int price) throws RemoteException;
	void retrieItem(Object id, ControlContext context) throws RemoteException;

}
