package edu.vt.rt.hyflow.benchmark.rmi.bst;

import java.rmi.RemoteException;

import edu.vt.rt.hyflow.benchmark.rmi.ILockabel;

public interface INode extends ILockabel{
	
	public String getRightChild() throws RemoteException;
	public String getLeftChild() throws RemoteException;
	public Integer getValue() throws RemoteException;
	public void setRightChild(String nextId) throws RemoteException;
	public void setLeftChild(String nextId) throws RemoteException;
	
}
