package edu.vt.rt.hyflow.benchmark.tm.list2;

import org.deuce.transaction.Context;
import org.deuce.transaction.ContextDelegator;
import org.deuce.transaction.TransactionException;

import aleph.dir.DirectoryManager;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.tm.control.ControlContext;
import edu.vt.rt.hyflow.helper.Atomic;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;

public class ListHandler {

    private static final String HEAD = "list";
	public static Object __CLASS_BASE__;
    
	public void createList() {
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
			DirectoryManager locator = HyFlow.getLocator();
			Node head = (Node)locator.open(HEAD);
			String oldNext = head.getNext(__transactionContext__);
			String newNodeId =  Network.getInstance().getID() + "-" + Math.random();	// generate random id
			Logger.debug(newNodeId);
			Node newNode = new Node(newNodeId, value);
			newNode.setNext(oldNext, __transactionContext__);
			head.setNext(newNodeId, __transactionContext__);
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
			DirectoryManager locator = HyFlow.getLocator();
			String next = HEAD;
			String prev = null;
			Node node;
			do{	// find the last node
				node = (Node)locator.open(next, "s");
				if(value.equals(node.getValue(__transactionContext__))){
					Node deletedNode = (Node)locator.open(next);	//reopen for write to be deleted
					Node prevNode = (Node)locator.open(prev);		//open previous node for write
					prevNode.setNext(deletedNode.getNext(__transactionContext__), __transactionContext__);
					locator.delete(deletedNode);	// added to the write set
					Logger.debug("<" + node.getId() + "> " + node.getValue(__transactionContext__) + "  DELETED....");
					return true;
				}
				prev = next;
				next = node.getNext(__transactionContext__);
				Logger.debug("<" + node.getId() + "> " + node.getValue(__transactionContext__));
			}while(next!=null);
			Node last = ((Node)locator.open(node.getId(), "r"));	// reopen last node for read
			last.getValue(__transactionContext__);	// force add to the readset
			Logger.debug("Nothing to Delete....");
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
			String prev = null;
			Node node;
			do{	// find the last node
				node = (Node)locator.open(next, "s");
				if(value.equals(node.getValue(__transactionContext__))){
					Node deletedNode = (Node)locator.open(next, "r");	//reopen for read to be found
					deletedNode.getValue(__transactionContext__);	// force add to the readset 
					Node prevNode = (Node)locator.open(prev, "r");		//open previous node for read
					prevNode.getValue(__transactionContext__);		// force add to the readset
					return true;
				}
				prev = next;
				next = node.getNext(__transactionContext__);
				Logger.debug("<" + node.getId() + "> " + node.getValue(__transactionContext__));
			}while(next!=null);
			Node last = ((Node)locator.open(node.getId(), "r"));	// reopen last node for read
			last.getValue(__transactionContext__);	// force add to the readset
			Logger.debug("Not Found!");
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
			String next = HEAD;
			int sum = 1;	// to avoid -1 value of head sentential node 
			do{	// find the last node
				Node node = (Node)locator.open(next, "r");
				next = node.getNext(__transactionContext__);
				sum += node.getValue(__transactionContext__);
			}while(next!=null);
			return sum;
		} finally{
			// commented to speed up sanity check
//			edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
		}
	}
}
