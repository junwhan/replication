package edu.vt.rt.hyflow.benchmark.dsm.bst;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import aleph.dir.DirectoryManager;
import aleph.dir.home.HomeDirectory;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.util.network.Network;

public class BSTHandler {

	private static final String HEAD = "0-tree";
	private final DirectoryManager locator;
	
	public BSTHandler() {
		locator = HyFlow.getLocator();
		((HomeDirectory)locator).setTimeOut(false);
	}
	
	public void createTree(){
		new Node(HEAD, -1);	// create the head Node
	}
	
	public void add(Integer item) {
//		System.err.println("\nAdd " + item);
		Node pred = null, curr = null;
		Node head = (Node)locator.open(HEAD);
		try {
			pred = head;
			String nextId = pred.getRightChild();
			if(nextId==null){
				String newNodeId =  Network.getInstance().getID() + "-" + Math.random();	// generate random id
				new Node(newNodeId, item);
				pred.setRightChild(newNodeId);
				return;
			}
			curr = (Node)locator.open(nextId);
			try {
				while (true) {
					locator.release(pred);
					pred = curr;
					String currId;
					Integer currValue = curr.getValue();
					boolean right;
					if(item >= currValue){
						currId = pred.getRightChild();
						right = true;
					}
					else{ 
						currId = pred.getLeftChild();
						right = false;
					}
					if (currId == null){
						String newNodeId =  Network.getInstance().getID() + "-" + Math.random();	// generate random id
						new Node(newNodeId, item);
						if(right)
							pred.setRightChild(newNodeId);
						else
							pred.setLeftChild(newNodeId);
						return;
					}
					curr = (Node)locator.open(currId);
				}
			} finally {
				locator.release(curr);
			}
		} finally {
			edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
			locator.release(pred);
		}
	}

	public boolean delete(Integer item) {
		Node pred = null, curr = null;
		Node head = (Node)locator.open(HEAD);
		try {
			pred = head;
			boolean right = true;
			String nextId = pred.getRightChild();
			if(nextId==null)
				return false;
			curr = (Node)locator.open(nextId);
			try {
				while (true) {
					if(item.equals(curr.getValue())){
						String replacement;
						if(curr.getLeftChild()==null){
							replacement = curr.getRightChild();
//							System.err.println("replace with right child");
						}else if(curr.getRightChild()==null){
							replacement = curr.getLeftChild();
//							System.err.println("replace with left child");
						}else{	// get left most in right tree
//							System.err.println("replace with left most in right tree");
							
							String rcId = curr.getRightChild();
							Node rc = (Node)locator.open(rcId);
							Node replacementNode;
							if(rc.getLeftChild()==null){
								replacement = rcId;
								replacementNode = rc;
							}else{
								Node lf0 = rc;
								replacement = lf0.getLeftChild();
								Node lf1 = (Node)locator.open(replacement);
								while(lf1.getLeftChild()!=null){
									locator.release(lf0);
									lf0 = lf1;
									replacement = lf1.getLeftChild();
									lf1 = (Node)locator.open(replacement);
								}
								lf0.setLeftChild(lf1.getRightChild());	// disconnect replacement node from its parent
								locator.release(lf0);
								replacementNode = lf1;
								replacementNode.setRightChild(curr.getRightChild());
							}
							
							replacementNode.setLeftChild(curr.getLeftChild());
							locator.release(replacementNode);
						}
						
						if(right)
							pred.setRightChild(replacement);
						else
							pred.setLeftChild(replacement);
						locator.delete(curr);
						curr = null;
//						System.out.println("done");
						return true;
					}
					
					locator.release(pred);
					pred = curr;

					String currId;
					Integer predValue = pred.getValue();
					if(item > predValue){
						currId = pred.getRightChild();
						right = true;
					}else{
						currId = pred.getLeftChild();
						right = false;
					}
					
					if (currId == null){
//						System.err.println("Nothing to delete!");
						return false;
					}
					
					curr = (Node)locator.open(currId);
				}
			} finally {
				if(curr!=null)
					locator.release(curr);
			}
		} finally {
			edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
			locator.release(pred);
		}
	}

	public boolean find(Integer item) {
		Node pred = null, curr = null;
		Node head = (Node)locator.open(HEAD);
		try {
			pred = head;
			String nextId = pred.getRightChild();
			if(nextId==null)
				return false;
			curr = (Node)locator.open(nextId);
			try {
				while (true) {
					locator.release(pred);
					pred = curr;
					String currId;
					Integer currValue = curr.getValue();
					if(item > currValue)
						currId = pred.getRightChild();
					else if(item < currValue)
						currId = pred.getLeftChild();
					else{
//						System.err.println("Found!");
						return true;
					}
					
					if (currId == null){
//						System.err.println("Not Found!");
						return false;
					}
					curr = (Node)locator.open(currId);
				}
			} finally {
				locator.release(curr);
			}
		} finally {
			edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
			locator.release(pred);
		}
	}
	
	public int sum() throws AccessException, RemoteException, NotBoundException{
		return sum((Node)(Node)locator.open(HEAD)) + 1;
	}
	private int sum(Node node) throws RemoteException, NotBoundException{
		int sum = node.getValue();
		if(node.getLeftChild()!=null)
			sum += sum((Node)(Node)locator.open(node.getLeftChild()));
		if(node.getRightChild()!=null)
			sum += sum((Node)(Node)locator.open(node.getRightChild()));
		return sum;
	}
}
