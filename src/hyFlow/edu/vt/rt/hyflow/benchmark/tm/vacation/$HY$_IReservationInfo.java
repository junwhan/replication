package edu.vt.rt.hyflow.benchmark.tm.vacation;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

import edu.vt.rt.hyflow.core.tm.control.ControlContext;

public interface $HY$_IReservationInfo extends Remote, Serializable{

	int getPrice(Object id, ControlContext context) throws RemoteException;
	int getType(Object id, ControlContext context) throws RemoteException;
	String getReservedResource(Object id, ControlContext context) throws RemoteException;

}
