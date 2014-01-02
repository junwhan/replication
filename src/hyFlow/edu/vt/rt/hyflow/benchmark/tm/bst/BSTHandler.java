package edu.vt.rt.hyflow.benchmark.tm.bst;

import org.deuce.transaction.Context;
import org.deuce.transaction.ContextDelegator;
import org.deuce.transaction.TransactionException;

import aleph.dir.DirectoryManager;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.tm.control.ControlContext;
import edu.vt.rt.hyflow.helper.Atomic;
import edu.vt.rt.hyflow.util.network.Network;

public class BSTHandler
{

    public static Object __CLASS_BASE__;
	private static final String HEAD = "tree";

	public void createTree() {
		new Node(HEAD, -1);	// create the head node
	}


	public void add(final Integer value) throws Throwable{
		new Atomic<Object>(true) {
			@Override
			public Object atomically(AbstractDistinguishable self,
					Context transactionContext) {
				add(value, transactionContext);
				return null;
			}
		}.execute(null);
	}
	public void add(Integer value, Context __transactionContext__){
		try{
//			System.err.println("\nadd " + value);
			DirectoryManager locator = HyFlow.getLocator();
			String next = HEAD;
			String prev = null;
			boolean right = true;
			do{	
				Node node = (Node)locator.open(next, "r");
				if(value >= node.getValue(__transactionContext__)){
					prev = next;
					next = node.getRightChild(__transactionContext__);
					right = true;
				}else{
					prev = next;
					next = node.getLeftChild(__transactionContext__);
					right = false;
				}
			}while(next!=null);
			
			Node prevNode = (Node)locator.open(prev);		//open previous node for write
			
			String newNodeId =  Network.getInstance().getID() + "-" + Math.random();	// generate random id
			new Node(newNodeId, value);	// create the node
			
			if(right)
				prevNode.setRightChild(newNodeId, __transactionContext__);
			else
				prevNode.setLeftChild(newNodeId, __transactionContext__);
		} finally{
			edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
		}
	}

	
	public boolean delete(final Integer value) throws Throwable{
		return new Atomic<Boolean>(true) {
			@Override
			public Boolean atomically(AbstractDistinguishable self,
					Context transactionContext) {
				return delete(value, transactionContext);
			}
		}.execute(null);
	}
	public boolean delete(Integer value, Context __transactionContext__){
		try{
//			System.err.println("\ndelete " + value);
			DirectoryManager locator = HyFlow.getLocator();
			String next = HEAD;
			String prev = null;
			boolean right = true;
			do{
				Node node = (Node)locator.open(next, "r");
				if(value > node.getValue(__transactionContext__)){
					prev = next;
					next = node.getRightChild(__transactionContext__);
					right = true;
				}else if(value < node.getValue(__transactionContext__)){
					prev = next;
					next = node.getLeftChild(__transactionContext__);
					right = false;
				}else{
					Node prevNode = (Node)locator.open(prev);		//open previous node for write
					Node deletedNode = (Node)locator.open(next);	//reopen for write to be deleted
					String replacement;
					if(deletedNode.getLeftChild(__transactionContext__)==null){
	//					System.err.println("replace with right child");
						replacement = deletedNode.getRightChild(__transactionContext__);
					}else if(deletedNode.getRightChild(__transactionContext__)==null){
	//					System.err.println("replace with left child");
						replacement = deletedNode.getLeftChild(__transactionContext__);
					}else{	// get left most in right tree
	//					System.err.println("replace with left most in right tree");
						String next2 = deletedNode.getRightChild(__transactionContext__);
						Node currNode2 = null;
						Node prevNode2 = null;
						do{
							prevNode2 = currNode2;
							replacement = next2;
							
							currNode2 = (Node)locator.open(next2, "r");
							next2 = currNode2.getLeftChild(__transactionContext__);
						}while(next2!=null);
						if(prevNode2!=null){	// disconnect replacement node from its parent
							Node prevNode2w = (Node)locator.open(prevNode2.getId());	//open previous node for write
							prevNode2w.setLeftChild(currNode2.getRightChild(__transactionContext__), __transactionContext__);
						}
						Node currNode2w = (Node)locator.open(replacement);	//replace
						currNode2w.setLeftChild(node.getLeftChild(__transactionContext__), __transactionContext__);
						if(!replacement.equals(node.getRightChild(__transactionContext__)))
							currNode2w.setRightChild(node.getRightChild(__transactionContext__), __transactionContext__);
					}
					if(right)
						prevNode.setRightChild(replacement, __transactionContext__);
					else
						prevNode.setLeftChild(replacement, __transactionContext__);
					locator.delete(deletedNode);
					return true;
				}
			}while(next!=null);
	//		System.err.println("Nothing to Delete....");
			return false;
		} finally{
			edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
		}
	}
	

	public boolean find(final Integer value) throws Throwable{
		return new Atomic<Boolean>(true) {
			@Override
			public Boolean atomically(AbstractDistinguishable self,
					Context transactionContext) {
				return find(value, transactionContext);
			}
		}.execute(null);
	}
	public boolean find(Integer value, Context __transactionContext__){
		try{
			DirectoryManager locator = HyFlow.getLocator();
			String next = HEAD;
			do{	
				Node node = (Node)locator.open(next, "r");
				if(value > node.getValue(__transactionContext__)){
					next = node.getRightChild(__transactionContext__);
				}else if(value < node.getValue(__transactionContext__)){
					next = node.getLeftChild(__transactionContext__);
				}else{
//					System.out.println("FOUND!");
					return true;
				}
			}while(next!=null);
//			System.out.println("NOT FOUND!");
			return false;
		} finally{
			edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
		}
	}
	
	public int sum() throws Throwable{
		return new Atomic<Integer>(true) {
			@Override
			public Integer atomically(AbstractDistinguishable self,
					Context transactionContext) {
				return sum(transactionContext);
			}
		}.execute(null);
	}
	public int sum(Context __transactionContext__){
		try{
			DirectoryManager locator = HyFlow.getLocator();
			return sum((Node)locator.open(HEAD), __transactionContext__) + 1;
		} finally{
			edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
		}
	}
	
	private int sum(Node node, Context __transactionContext__){
		int sum = node.getValue(__transactionContext__);
		DirectoryManager locator = HyFlow.getLocator();
		if(node.getLeftChild(__transactionContext__)!=null)
			sum += sum((Node)locator.open(node.getLeftChild(__transactionContext__)), __transactionContext__);
		if(node.getRightChild(__transactionContext__)!=null)
			sum += sum((Node)locator.open(node.getRightChild(__transactionContext__)), __transactionContext__);
		return sum;
	}

}
