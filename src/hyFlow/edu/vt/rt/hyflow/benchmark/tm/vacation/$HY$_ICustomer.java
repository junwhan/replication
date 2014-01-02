package edu.vt.rt.hyflow.benchmark.tm.vacation;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import edu.vt.rt.hyflow.core.tm.control.ControlContext;

public interface $HY$_ICustomer extends Remote, Serializable{

	void addReservation(Object id, ControlContext context, ReservationInfo reservation) throws RemoteException;
	List<String> getReservations(Object id, ControlContext context) throws RemoteException;

}
