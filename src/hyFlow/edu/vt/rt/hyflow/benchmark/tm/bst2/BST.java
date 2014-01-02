package edu.vt.rt.hyflow.benchmark.tm.bst2;

import org.deuce.reflection.AddressUtil;
import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.Context;
import org.deuce.transaction.ContextDelegator;
import org.deuce.transaction.TransactionException;

import aleph.dir.DirectoryManager;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.benchmark.tm.skiplist.SkipList;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.tm.NestedContext;
import edu.vt.rt.hyflow.core.tm.NestingModel;
import edu.vt.rt.hyflow.core.tm.control.ControlContext;
import edu.vt.rt.hyflow.helper.Atomic;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;

public class BST<T extends Comparable<? super T>> extends AbstractDistinguishable {

    //public static Object __CLASS_BASE__;
    String baseId;
    String header;
    
    public static long baseId__ADDRESS__;
    public static long header__ADDRESS__;
    
    {
		try {
			baseId__ADDRESS__ = AddressUtil.getAddress(BST.class
					.getDeclaredField("baseId"));
			header__ADDRESS__ = AddressUtil.getAddress(BST.class
					.getDeclaredField("header"));
		} catch (Exception e) {
			e.printStackTrace(Logger.levelStream[Logger.DEBUG]);
		}
	}
    
    public BST(String baseId) 
    {
    	this.baseId = baseId;
    	header = baseId + "-0";
    	new Node(header, -1);
    	
    	AbstractContext context = ContextDelegator.getTopInstance();
		if(context == null)
			HyFlow.getLocator().register(this); // publish it now	
		else
			context.newObject(this);	// add it to context publish-set 
    }

	public boolean add(final Integer value) {
		try {
			return new Atomic<Boolean>(true) {
				boolean added = false;
				@Override
				public Boolean atomically(AbstractDistinguishable self,
						Context transactionContext) {
					added = add(value, transactionContext);
					// O/N
					if (((NestedContext)transactionContext).getNestingModel() == NestingModel.OPEN
							&& !((NestedContext)transactionContext).isTopLevel()) {
						((NestedContext)transactionContext).onLockAction(
								baseId.toString(), value.toString(), false, true);
						m_hasOnAbort = true;
						m_hasOnCommit = true;
					}
					// end O/N
					return added;
				}
				@Override
				public void onCommit(Context transactionContext) {
					Logger.debug("BST::add() onCommit");
					((NestedContext)transactionContext).onLockAction(
							baseId.toString(), value.toString(), false, false);
				}
				@Override
				public void onAbort(Context transactionContext) {
					Logger.debug("BSTHandler::add() onAbort");
					((NestedContext)transactionContext).onLockAction(
							baseId.toString(), value.toString(), false, false);
					if (added) {
						delete(value, transactionContext);
					}
				}
			}.execute(null);
		} catch (TransactionException e){
			throw e;
		} catch (Throwable e) {
			e.printStackTrace();
			return false;
		}
	}
	public boolean add(Integer value, Context __transactionContext__){
		try{
			DirectoryManager locator = HyFlow.getLocator();
			String next = header;
			String prev = null;
			boolean right = true;
			do{	
				Node node = (Node)locator.open(next, /*"s"*/"r");
				Integer node_value = node.getValue(__transactionContext__);
				if(value > node_value ){
					prev = next;
					next = node.getRightChild(__transactionContext__);
					right = true;
				} else if (value != node_value) {
					prev = next;
					next = node.getLeftChild(__transactionContext__);
					right = false;
				} else {
					// Already in tree, don't insert
					return false;
				}
			}while(next!=null);
			
			Node prevNode = (Node)locator.open(prev);		//open previous node for write
			
			String newNodeId =  baseId + "-" + Math.random();	// generate random id
			new Node(newNodeId, value);	// create the node
			
			if(right)
				prevNode.setRightChild(newNodeId, __transactionContext__);
			else
				prevNode.setLeftChild(newNodeId, __transactionContext__);
			return true;
		} finally{
			edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
		}
	}

	
	public boolean delete(final Integer value) {
		try {
			return new Atomic<Boolean>(true) {
				boolean deleted = false;
				@Override
				public Boolean atomically(AbstractDistinguishable self,
						Context transactionContext) {
					// O/N
					if (((NestedContext)transactionContext).getNestingModel() == NestingModel.OPEN) {
						((NestedContext)transactionContext).onLockAction(
								baseId.toString(), value.toString(), false, true);
						m_hasOnAbort = true;
						m_hasOnCommit = true;
					}
					// end O/N
					deleted = delete(value, transactionContext);
					return deleted;
				}
				@Override
				public void onCommit(Context transactionContext) {
					Logger.debug("BSTHandler::delete() onCommit");
					((NestedContext)transactionContext).onLockAction(
							baseId.toString(), value.toString(), false, false);
				}
				@Override
				public void onAbort(Context transactionContext) {
					Logger.debug("BSTHandler::delete() onAbort");
					((NestedContext)transactionContext).onLockAction(
							baseId.toString(), value.toString(), false, false);
					if (deleted) {
						add(value, transactionContext);
					}
				}
			}.execute(null);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean delete(Integer value, Context __transactionContext__){
		try{
//			System.err.println("\ndelete " + value);
			DirectoryManager locator = HyFlow.getLocator();
			String next = header;
			String prev = null;
			boolean right = true;
			Node node;
			do{
				node = (Node)locator.open(next, /*"s"*/"r");
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
			//Node last = ((Node)locator.open(node.getId(), "r"));	// reopen last node for read
			//last.getValue(__transactionContext__);	// force add to the readset
			Logger.debug("Nothing to Delete....");
			return false;
		} finally{
			edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
		}
	}
	

	public boolean find(final Integer value) {
		try {
			return new Atomic<Boolean>(true) {
				@Override
				public Boolean atomically(AbstractDistinguishable self,
						Context transactionContext) {
					// O/N
					if (((NestedContext)transactionContext).getNestingModel() == NestingModel.OPEN) {
						((NestedContext)transactionContext).onLockAction(
								baseId.toString(), value.toString(), true, true);
						m_hasOnAbort = true;
						m_hasOnCommit = true;
					}
					// end O/N
					return find(value, transactionContext);
				}
				@Override
				public void onCommit(Context transactionContext) {
					Logger.debug("BSTHandler::contains() onCommit");
					((NestedContext)transactionContext).onLockAction(
							baseId.toString(), value.toString(), true, false);
				}
				@Override
				public void onAbort(Context transactionContext) {
					Logger.debug("BSTHandler::contains() onAbort");
					((NestedContext)transactionContext).onLockAction(
							baseId.toString(), value.toString(), true, false);
				}
			}.execute(null);
		} catch (TransactionException e) {
			throw e;
		}
		catch (Throwable e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean find(Integer value, Context __transactionContext__){
		DirectoryManager locator = HyFlow.getLocator();
		String prev = null;
		Node node = null;
		try{
			String next = header;
			do{	
				node = (Node)locator.open(next, /*"s"*/"r");
				if(value > node.getValue(__transactionContext__)){
					prev = next;
					next = node.getRightChild(__transactionContext__);
				}else if(value < node.getValue(__transactionContext__)){
					prev = next;
					next = node.getLeftChild(__transactionContext__);
				}else{
//					System.out.println("FOUND!");
					return true;
				}
			}while(next!=null);
//			System.out.println("NOT FOUND!");
			return false;
		} finally{
			if(prev!=null){
				Node lastPrev = ((Node)locator.open(prev, "r"));	// reopen last node for read
				lastPrev.getValue(__transactionContext__);	// force add to the readset
			}
			if(node!=null){
				Node last = ((Node)locator.open(node.getId(), "r"));	// reopen last node for read
				last.getValue(__transactionContext__);	// force add to the readset
			}
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
			return sum((Node)locator.open(header), __transactionContext__) + 1;
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

	@Override
	public Object getId() {
		return baseId;
	}
	private Long [] ts;
	@Override
	public Long[] getTS(){
		return ts;
	}
	@Override
	public void setTS(Long [] ts){
		// for DATS
		this.ts = ts;
	}

}
