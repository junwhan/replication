package edu.vt.rt.hyflow.benchmark.rmi.dht;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IDistributedHashTable extends Remote, Serializable{

	public Integer get(Object key) throws RemoteException;
	public void put(Object key, Integer value) throws RemoteException;
	
	public void lock(Object key) throws RemoteException, InterruptedException;
	public void unlock(Object key) throws RemoteException;
	
}
