package edu.vt.rt.hyflow.benchmark.rmi.list;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.vt.rt.hyflow.benchmark.rmi.Lockable;
import edu.vt.rt.hyflow.benchmark.rmi.bank.IBankAccount;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.dir.IHyFlow;
import edu.vt.rt.hyflow.transaction.Remote;
import edu.vt.rt.hyflow.util.network.Network;

public class Node extends Lockable
		implements INode{
	
	private String id;
	private Integer value;
	private String nextId;
	
	public Node(String id, Integer value) throws RemoteException{
		super(id);
		this.id = id;
		this.value = value;
		enableTimeout = false;
	}
	
	public void setNext(String nextId){
		this.nextId = nextId;
	}

	public String getNext(){
		return nextId;
	}
	
	public Integer getValue(){
		return value;
	}
	
	public Object getId() {
		return id;
	}
}
