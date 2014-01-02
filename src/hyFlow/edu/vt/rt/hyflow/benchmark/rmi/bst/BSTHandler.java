package edu.vt.rt.hyflow.benchmark.rmi.bst;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

import aleph.comm.tcp.Address;
import edu.vt.rt.hyflow.util.network.Network;

public class BSTHandler {

	private static final String HEAD = "0-tree";

	public void createTree() throws RemoteException {
		new Node(HEAD, -1);	// create the head Node
	}

	public void add(Integer item) {
//		System.err.println("\nAdd " + item);
		INode pred = null, curr = null;
		try{
			Address predServer = (Address) Network.getAddress(Benchmark.getServerId(HEAD));
			INode head = (INode)LocateRegistry.getRegistry(predServer.inetAddress.getHostAddress(), predServer.port).lookup(HEAD);
			Network.linkDelay(true, predServer);
			head.lock();
			try {
				pred = head;
				Network.linkDelay(true, predServer);
				String nextId = pred.getRightChild();
				if(nextId==null){
					String newNodeId =  Network.getInstance().getID() + "-" + Math.random();	// generate random id
					new Node(newNodeId, item);
					Network.linkDelay(true, predServer);
					pred.setRightChild(newNodeId);
					return;
				}
				Address currServer = (Address) Network.getAddress(Benchmark.getServerId(nextId));
				curr = (INode)LocateRegistry.getRegistry(currServer.inetAddress.getHostAddress(), currServer.port).lookup(nextId);
				Network.linkDelay(true, currServer);
				curr.lock();
				try {
					while (true) {
						Network.linkDelay(true, predServer);
						pred.unlock();
						predServer = currServer;
						pred = curr;
						String currId;
						Network.linkDelay(true, currServer);
						Integer currValue = curr.getValue();
						boolean right;
						if(item >= currValue){
							Network.linkDelay(true, predServer);
							currId = pred.getRightChild();
							right = true;
						}
						else{ 
							Network.linkDelay(true, predServer);
							currId = pred.getLeftChild();
							right = false;
						}
						if (currId == null){
							String newNodeId =  Network.getInstance().getID() + "-" + Math.random();	// generate random id
							new Node(newNodeId, item);
							if(right){
								Network.linkDelay(true, predServer);
								pred.setRightChild(newNodeId);
							}
							else{
								Network.linkDelay(true, predServer);
								pred.setLeftChild(newNodeId);
							}
							return;
						}
						currServer = (Address) Network.getAddress(Benchmark.getServerId(currId));
						curr = (INode)LocateRegistry.getRegistry(currServer.inetAddress.getHostAddress(), currServer.port).lookup(currId);
						Network.linkDelay(true, currServer);
						curr.lock();
					}
				} finally {
					Network.linkDelay(true, currServer);
					curr.unlock();
				}
			} finally {
				edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
				Network.linkDelay(true, predServer);
				pred.unlock();
			}
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
		INode pred = null, curr = null;
		try{
			Address predServer = (Address) Network.getAddress(Benchmark.getServerId(HEAD));
			INode head = (INode)LocateRegistry.getRegistry(predServer.inetAddress.getHostAddress(), predServer.port).lookup(HEAD);
			Network.linkDelay(true, predServer);
			head.lock();
			try {
				pred = head;
				boolean right = true;
				Network.linkDelay(true, predServer);
				String nextId = pred.getRightChild();
				if(nextId==null)
					return false;
				Address currServer = (Address) Network.getAddress(Benchmark.getServerId(nextId));
				curr = (INode)LocateRegistry.getRegistry(currServer.inetAddress.getHostAddress(), currServer.port).lookup(nextId);
				Network.linkDelay(true, currServer);
				curr.lock();
				try {
					while (true) {
						Network.linkDelay(true, currServer);
						if(item.equals(curr.getValue())){
							String replacement;
							Network.linkDelay(true, currServer);
							String leftChildId = curr.getLeftChild();
							String rightChildId; 
							if(leftChildId==null){
								Network.linkDelay(true, currServer);
								replacement = rightChildId = curr.getRightChild();
//								System.err.println("replace with right child");
							}else{
								Network.linkDelay(true, currServer);
								rightChildId = curr.getRightChild();
								if(rightChildId==null){
									replacement = leftChildId;
//									System.err.println("replace with left child");
								}else{	// get left most in right tree
//									System.err.println("replace with left most in right tree");
									String rcId = rightChildId;
									Address server = (Address) Network.getAddress(Benchmark.getServerId(rcId));
									INode rc = (INode)LocateRegistry.getRegistry(server.inetAddress.getHostAddress(), server.port).lookup(rcId);
									Network.linkDelay(true, server);
									rc.lock();
									Address replServer;
									INode replacementNode;
									Network.linkDelay(true, server);
									if(rc.getLeftChild()==null){
										replacement = rcId;
										replacementNode = rc;
										replServer = server;
									}else{
										INode lf0 = rc;
										Network.linkDelay(true, server);
										replacement = lf0.getLeftChild();
										Address lf1Server = (Address) Network.getAddress(Benchmark.getServerId(replacement));
										INode lf1 = (INode)LocateRegistry.getRegistry(lf1Server.inetAddress.getHostAddress(), lf1Server.port).lookup(replacement);
										Network.linkDelay(true, lf1Server);
										lf1.lock();
										Network.linkDelay(true, lf1Server);
										while(lf1.getLeftChild()!=null){
											Network.linkDelay(true, server);
											lf0.unlock();
											lf0 = lf1;
											server = lf1Server;
											Network.linkDelay(true, lf1Server);
											replacement = lf1.getLeftChild();
											lf1Server = (Address) Network.getAddress(Benchmark.getServerId(replacement));
											lf1 = (INode)LocateRegistry.getRegistry(lf1Server.inetAddress.getHostAddress(), lf1Server.port).lookup(replacement);
											Network.linkDelay(true, lf1Server);
											lf1.lock();									        
										}
										Network.linkDelay(true, server);
										Network.linkDelay(true, lf1Server);
										lf0.setLeftChild(lf1.getRightChild());	// disconnect replacement node from its parent
										Network.linkDelay(true, server);
										lf0.unlock();
										replacementNode = lf1;
										replServer = lf1Server;
										Network.linkDelay(true, currServer);
										Network.linkDelay(true, replServer);
										replacementNode.setRightChild(curr.getRightChild());
									}
									Network.linkDelay(true, currServer);
									Network.linkDelay(true, replServer);
									replacementNode.setLeftChild(curr.getLeftChild());
									Network.linkDelay(true, replServer);
									replacementNode.unlock();
								}
							}
							
							if(right){
								Network.linkDelay(true, predServer);
								pred.setRightChild(replacement);
							}
							else{
								Network.linkDelay(true, predServer);
								pred.setLeftChild(replacement);
							}
							
							Network.linkDelay(true, currServer);
							curr.destroy();
							curr = null;
//							System.out.println("done");
							return true;
						}
						
						Network.linkDelay(true, predServer);
						pred.unlock();
						pred = curr;

						String currId;
						Network.linkDelay(true, predServer);
						Integer predValue = pred.getValue();
						if(item > predValue){
							Network.linkDelay(true, predServer);
							currId = pred.getRightChild();
							right = true;
						}else{
							Network.linkDelay(true, predServer);
							currId = pred.getLeftChild();
							right = false;
						}
						
						if (currId == null){
//							System.err.println("Nothing to delete!");
							return false;
						}

						currServer = (Address) Network.getAddress(Benchmark.getServerId(currId));
						curr = (INode)LocateRegistry.getRegistry(currServer.inetAddress.getHostAddress(), currServer.port).lookup(currId);
						Network.linkDelay(true, currServer);
						curr.lock();
					}
				} finally {
					if(curr!=null){
						Network.linkDelay(true, currServer);
						curr.unlock();
					}
				}
			} finally {
				edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
				Network.linkDelay(true, predServer);
				pred.unlock();
			}
		} catch (AccessException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
//		System.err.println("[Ex] Nothing to delete!");
		return false;
	}

	public boolean find(Integer item) {
		INode pred = null, curr = null;
		try{
			Address predServer = (Address) Network.getAddress(Benchmark.getServerId(HEAD));
			INode head = (INode)LocateRegistry.getRegistry(predServer.inetAddress.getHostAddress(), predServer.port).lookup(HEAD);
			Network.linkDelay(true, predServer);
			head.lock();
			try {
				pred = head;
				Network.linkDelay(true, predServer);
				String nextId = pred.getRightChild();
				if(nextId==null)
					return false;
				Address currServer = (Address) Network.getAddress(Benchmark.getServerId(nextId));
				curr = (INode)LocateRegistry.getRegistry(currServer.inetAddress.getHostAddress(), currServer.port).lookup(nextId);
				Network.linkDelay(true, currServer);
				curr.lock();
				try {
					while (true) {
						String currId;
						Network.linkDelay(true, currServer);
						Integer currValue = curr.getValue();
						if(item > currValue){
							Network.linkDelay(true, currServer);
							currId = curr.getRightChild();
						}
						else if(item < currValue){
							Network.linkDelay(true, currServer);
							currId = curr.getLeftChild();
						}
						else{
//							System.err.println("Found!");
							return true;
						}
						
						if (currId == null){
//							System.err.println("Not Found!");
							return false;
						}
						Network.linkDelay(true, predServer);
						pred.unlock();
						pred = curr;
						predServer = currServer;
						Address server = (Address) Network.getAddress(Benchmark.getServerId(currId));
						curr = (INode)LocateRegistry.getRegistry(server.inetAddress.getHostAddress(), server.port).lookup(currId);
						Network.linkDelay(true, currServer);
						curr.lock();
					}
				} finally {
					Network.linkDelay(true, currServer);
					curr.unlock();
				}
			} finally {
				edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
				Network.linkDelay(true, predServer);
				pred.unlock();
			}
		} catch (AccessException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
//		System.err.println("[Ex] Not Found!");
		return false;
	}
	
	public int sum() throws AccessException, RemoteException, NotBoundException{
		Address server = (Address) Network.getAddress(Benchmark.getServerId(HEAD));
		return sum((INode)LocateRegistry.getRegistry(server.inetAddress.getHostAddress(), server.port).lookup(HEAD)) + 1;
	}
	private int sum(INode node) throws RemoteException, NotBoundException{
		int sum = node.getValue();
		String left = node.getLeftChild();
		if(left!=null){
			Address server = (Address) Network.getAddress(Benchmark.getServerId(left));
			sum += sum((INode)LocateRegistry.getRegistry(server.inetAddress.getHostAddress(), server.port).lookup(left));
		}
		String right = node.getRightChild();
		if(right!=null){
			Address server = (Address) Network.getAddress(Benchmark.getServerId(right));
			sum += sum((INode)LocateRegistry.getRegistry(server.inetAddress.getHostAddress(), server.port).lookup(right));
		}
		return sum;
	}
}
