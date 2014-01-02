package edu.vt.rt.hyflow.benchmark.tm.bst2;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

import edu.vt.rt.hyflow.core.tm.control.ControlContext;

public interface $HY$_INode extends Remote, Serializable{
	public void setRightChild(Object id, ControlContext context, String rightId) throws RemoteException;
	public String getRightChild(Object id, ControlContext context) throws RemoteException;
	
	public void setLeftChild(Object id, ControlContext context, String rightId) throws RemoteException;
	public String getLeftChild(Object id, ControlContext context) throws RemoteException;
	
	public Integer getValue(Object id, ControlContext context) throws RemoteException;
}
