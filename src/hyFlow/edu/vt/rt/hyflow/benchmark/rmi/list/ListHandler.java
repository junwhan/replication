package edu.vt.rt.hyflow.benchmark.rmi.list;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

import aleph.comm.tcp.Address;
import edu.vt.rt.hyflow.util.network.Network;

public class ListHandler {

	private static final String HEAD = "0-list";

	public void createList() throws RemoteException {
		new Node(HEAD, -1);	// create the head Node
	}
	
//	private INode locate(String id) throws AccessException, RemoteException, NotBoundException{
//		Address server = (Address) Network.getAddress(Benchmark.getServerId(id));
//		return (INode)LocateRegistry.getRegistry(server.inetAddress.getHostAddress(), server.port).lookup(id);
//	}
	
	public void add(Integer item) {
//		System.err.println("list.add("+item+");");
		try{
			String newNodeId =  Network.getInstance().getID() + "-" + Math.random();	// generate random id
			Node newNode = new Node(newNodeId, item);
			Address server = (Address) Network.getAddress(Benchmark.getServerId(HEAD));
			INode head = (INode)LocateRegistry.getRegistry(server.inetAddress.getHostAddress(), server.port).lookup(HEAD);
			Network.linkDelay(true, server);
			head.lock();
			Network.linkDelay(true, server);
			newNode.setNext(head.getNext());
			Network.linkDelay(true, server);
			head.setNext(newNodeId);
			edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
			Network.linkDelay(true, server);
			head.unlock();
		} catch (AccessException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public boolean delete(Integer item) {
//		System.err.println("list.delete("+item+");");
		INode pred = null, curr = null;
		Address predServer = null;
		try {
			predServer = (Address) Network.getAddress(Benchmark.getServerId(HEAD));
			INode head = (INode)LocateRegistry.getRegistry(predServer.inetAddress.getHostAddress(), predServer.port).lookup(HEAD);
			Network.linkDelay(true, predServer);
			head.lock();
			pred = head;
			Network.linkDelay(true, predServer);
			String nextId = pred.getNext();
			if(nextId==null)
				return false;
			Address currServer = (Address) Network.getAddress(Benchmark.getServerId(nextId));
			curr = (INode)LocateRegistry.getRegistry(currServer.inetAddress.getHostAddress(), currServer.port).lookup(nextId);
			Network.linkDelay(true, currServer);
			curr.lock();
			try {
				Network.linkDelay(true, currServer);
				while (!item.equals(curr.getValue())) {
					Network.linkDelay(true, predServer);
					pred.unlock();
					pred = curr;
					predServer = currServer;
					Network.linkDelay(true, currServer);
					nextId = curr.getNext();
					if(nextId==null)
						return false;
					currServer = (Address) Network.getAddress(Benchmark.getServerId(nextId));
					curr = (INode)LocateRegistry.getRegistry(currServer.inetAddress.getHostAddress(), currServer.port).lookup(nextId);
					Network.linkDelay(true, currServer);
					curr.lock();
					Network.linkDelay(true, currServer);
				}
				Network.linkDelay(true, predServer);
				pred.setNext(curr.getNext());
				Network.linkDelay(true, currServer);
				curr.destroy();
				curr = null;
				return true;
			} finally {
				if(curr!=null){
					Network.linkDelay(true, predServer);
					curr.unlock();
				}
			}
		} catch (AccessException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
			try {
				Network.linkDelay(true, predServer);
				pred.unlock();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}		
		return false;
	}

	public boolean find(Integer item) {
		INode pred = null, curr = null;
		Address predServer = null;
		try {
			predServer = (Address) Network.getAddress(Benchmark.getServerId(HEAD));
			INode head = (INode)LocateRegistry.getRegistry(predServer.inetAddress.getHostAddress(), predServer.port).lookup(HEAD);
			Network.linkDelay(true, predServer);
			head.lock();
			pred = head;
			Network.linkDelay(true, predServer);
			String nextId = pred.getNext();
			if(nextId==null){
//				System.out.println("NOT FOUND");
				return false;
			}
			Address currServer = (Address) Network.getAddress(Benchmark.getServerId(nextId));
			curr = (INode)LocateRegistry.getRegistry(currServer.inetAddress.getHostAddress(), currServer.port).lookup(nextId);
			Network.linkDelay(true, currServer);
			curr.lock();
			try {
				Network.linkDelay(true, currServer);
				while (!item.equals(curr.getValue())) {
					Network.linkDelay(true, predServer);
					pred.unlock();
					pred = curr;
					predServer = currServer;
					Network.linkDelay(true, currServer);
					nextId = curr.getNext();
					if(nextId==null){
//						System.out.println("NOT FOUND");
						return false;
					}
					currServer = (Address) Network.getAddress(Benchmark.getServerId(nextId));
					curr = (INode)LocateRegistry.getRegistry(currServer.inetAddress.getHostAddress(), currServer.port).lookup(nextId);
					Network.linkDelay(true, currServer);
					curr.lock();
					Network.linkDelay(true, currServer);
				}
//				System.out.println("FOUND");
				return true;
			} finally {
				Network.linkDelay(true, currServer);
				curr.unlock();
			}
		} catch (AccessException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
			try {
				Network.linkDelay(true, predServer);
				pred.unlock();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}		
		return false;
	}
	
	public int sum(){	// used in sanity checks only
		int sum = 0;
		INode pred = null, curr = null;
		try {
			Address server = (Address) Network.getAddress(Benchmark.getServerId(HEAD));
			INode head = (INode)LocateRegistry.getRegistry(server.inetAddress.getHostAddress(), server.port).lookup(HEAD);
			head.lock();
			pred = head;
			String nextId = pred.getNext();
			if(nextId==null){
				return 0;
			}
			server = (Address) Network.getAddress(Benchmark.getServerId(nextId));
			curr = (INode)LocateRegistry.getRegistry(server.inetAddress.getHostAddress(), server.port).lookup(nextId);
			curr.lock();
			try {
				while (true) {
					sum += curr.getValue();
					pred.unlock();
					pred = curr;
					nextId = curr.getNext();
					if(nextId==null)
						return sum;
					server = (Address) Network.getAddress(Benchmark.getServerId(nextId));
					curr = (INode)LocateRegistry.getRegistry(server.inetAddress.getHostAddress(), server.port).lookup(nextId);
					curr.lock();
				}
			} finally {
				curr.unlock();
			}
		} catch (AccessException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
			try {
				pred.unlock();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}		
		return sum;
	}
}
