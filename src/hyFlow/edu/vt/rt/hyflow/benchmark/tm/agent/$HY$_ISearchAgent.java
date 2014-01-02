package edu.vt.rt.hyflow.benchmark.tm.agent;

import java.io.Serializable;
import java.net.URI;
import java.rmi.Remote;
import java.rmi.RemoteException;

import edu.vt.rt.hyflow.core.tm.control.ControlContext;

public interface $HY$_ISearchAgent extends Remote, Serializable{
	public URI find(Object id, ControlContext context, String keyword) throws RemoteException;
	public Object transfer(Object id, ControlContext context, URI name) throws RemoteException;
}
