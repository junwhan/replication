package edu.vt.rt.hyflow.benchmark.tm.deque;

import org.deuce.reflection.AddressUtil;
import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.Context;
import org.deuce.transaction.ContextDelegator;
import org.deuce.transaction.TransactionException;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.helper.Atomic;
import edu.vt.rt.hyflow.util.io.Logger;

public class Deque<T> extends AbstractDistinguishable
{
	private String id = null;
	private String front = null;
	private String back = null;
	private String free = null;
	
	public static long id__ADDRESS__;
	public static long front__ADDRESS__;
	public static long back__ADDRESS__;
	public static long free__ADDRESS__;
	
	{
		try {
			Logger.debug("Initializing Deque!");
			front__ADDRESS__ = AddressUtil.getAddress(Deque.class
					.getDeclaredField("front"));
			back__ADDRESS__ = AddressUtil.getAddress(Deque.class
					.getDeclaredField("back"));
			free__ADDRESS__ = AddressUtil.getAddress(Deque.class
					.getDeclaredField("free"));
			id__ADDRESS__ = AddressUtil.getAddress(Deque.class
					.getDeclaredField("id"));
		} catch (Exception e) {
			e.printStackTrace(Logger.levelStream[Logger.DEBUG]);
		}
	}
	
	public Deque(String id) {
		this.id = id;
		
		// Add one free node.
		final DequeNode<T> node = new DequeNode<T>(getId() + "-" + Benchmark.random.get().nextInt());
		this.free = (String)node.getId();
		
		// publish
		AbstractContext context = ContextDelegator.getTopInstance();
		if (context == null)
			HyFlow.getLocator().register(this);
		else
			context.newObject(this); // add it to context publish-set
	}
	
	@Override
	public Object getId() {
		return id;
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
	@Override
	public int hashCode() {
		return id.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof Deque))
			return false;
		Deque<T> obj2 = (Deque<T>)obj;
		return id.equals(obj2.id);
	}
	
	private String getFree(Context c) {
		ContextDelegator.beforeReadAccess(this, free__ADDRESS__, c);
		final String result = (String) ContextDelegator.onReadAccess(this, this.free, free__ADDRESS__, c);
		Logger.debug("Deque::getFree() = "+result);
		return result;
	}
	
	private void setFree(String newval, Context c) {
		Logger.debug("Deque::setFree( newval=" + newval +")");
		ContextDelegator.onWriteAccess(this, newval, free__ADDRESS__, c);
	}
	
	private String getFront(Context c) {
		ContextDelegator.beforeReadAccess(this, front__ADDRESS__, c);
		final String result = (String) ContextDelegator.onReadAccess(this, this.front, front__ADDRESS__, c);
		Logger.debug("Deque::getFront() = "+result);
		return result;
	}
	
	private void setFront(String newval, Context c) {
		Logger.debug("Deque::setFront(newval= "+newval+" )");
		ContextDelegator.onWriteAccess(this, newval, front__ADDRESS__, c);
	}
	
	private String getBack(Context c) {
		ContextDelegator.beforeReadAccess(this, back__ADDRESS__, c);
		final String result = (String) ContextDelegator.onReadAccess(this, this.back, back__ADDRESS__, c);
		return result;
	}
	
	private void setBack(String newval, Context c) {
		ContextDelegator.onWriteAccess(this, newval, back__ADDRESS__, c);
	}
	
	@SuppressWarnings("unchecked")
	private DequeNode<T> getNewNode(Context c) {
		Logger.debug("Deque::getNewNode()");
		String free = getFree(c);
		DequeNode<T> node = null;
		if (free != null) {
			node = (DequeNode<T>)HyFlow.getLocator().open(free);
			Logger.debug("Deque::getNewNode = old node "+node.getId());
			setFree(node.getNext(c), c);
		} else {
			node = new DequeNode<T>(getId() + "-" + Benchmark.random.get().nextInt());
			Logger.debug("Deque::getNewNode = new node "+node.getId());
		}
		return node;
	}
	
	private void returnToFree(DequeNode<T> n, Context c) {
		Logger.debug("Deque::returnToFree( node= " +n.getId()+ ")");
		String free = getFree(c);
		n.setNext(free, c);
		n.setPrev(null, c);
		setFree((String)n.getId(), c);
	}
	
	public void pushFront(final T item) {
		try {
			new Atomic<Object>(true) {
				@SuppressWarnings("unchecked")
				@Override
				public Object atomically(AbstractDistinguishable self, Context c) 
				{
					Logger.debug("Deque::pushFront()");
					final DequeNode<T> node = getNewNode(c);
					final String oldFront = getFront(c);
					node.setNext(oldFront, c);
					node.setPrev(null, c);
					node.setValue(item, c);
					if (oldFront != null) {
						final DequeNode<T> oldFrontNode = (DequeNode<T>) HyFlow.getLocator().open(oldFront);
						oldFrontNode.setPrev((String)node.getId(), c);
					} else {
						// the only node, update back too
						setBack((String)node.getId(), c);
					}
					setFront((String)node.getId(), c);
					Logger.debug("Deque::pushFront() done");
					return null;
				}
			}.execute(null);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	public void pushBack(final T item) {
		try {
			new Atomic<Object>(true) {
				@Override
				@SuppressWarnings("unchecked")
				public Object atomically(AbstractDistinguishable self, Context c) {
					final DequeNode<T> node = getNewNode(c);
					final String oldBack = getBack(c);
					node.setNext(null, c);
					node.setPrev(oldBack, c);
					node.setValue(item, c);
					if (oldBack != null) {
						// not the only node, update neighbor
						final DequeNode<T> oldBackNode = (DequeNode<T>) HyFlow.getLocator().open(oldBack);
						oldBackNode.setNext((String)node.getId(), c);
					} else {
						// the only node, update front too
						setFront((String)node.getId(), c);
					}
					setBack((String)node.getId(), c);
					return null;
				}
			}.execute(null);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	public T popFront() {
		try {
			return new Atomic<T>(true) {
				@SuppressWarnings("unchecked")
				@Override
				public T atomically(AbstractDistinguishable self, Context c) {
					Logger.debug("Deque::popFront()");
					final String oldFront = getFront(c);
					// empty deque, do nothing
					if (oldFront == null)
						return null;
					// we have at least a node
					final DequeNode<T> oldFrontNode = (DequeNode<T>) HyFlow.getLocator().open(oldFront);
					final T result = oldFrontNode.getValue(c);
					final String newFront = oldFrontNode.getNext(c);
					if (newFront != null) {
						// still nodes left
						final DequeNode<T> newFrontNode = (DequeNode<T>) HyFlow.getLocator().open(newFront);
						newFrontNode.setPrev(null, c);
					} else {
						// no more nodes left, update the back too!
						setBack(null, c);
					}
					setFront(newFront, c);
					returnToFree(oldFrontNode, c);
					Logger.debug("Deque::popFront() done, res="+result);
					return result;
				}
			}.execute(null);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public T popBack() {
		try {
			return new Atomic<T>(true) {
				@SuppressWarnings("unchecked")
				@Override
				public T atomically(AbstractDistinguishable self, Context c) {
					final String oldBack = getBack(c);
					// empty deque, do nothing
					if (oldBack == null)
						return null;
					// we have at least a node
					final DequeNode<T> oldBackNode = (DequeNode<T>) HyFlow.getLocator().open(oldBack);
					final String newBack = oldBackNode.getPrev(c);
					setBack(newBack, c);
					if (newBack != null) {
						// still nodes left
						final DequeNode<T> newBackNode = (DequeNode<T>) HyFlow.getLocator().open(newBack);
						newBackNode.setNext(null, c);
					} else {
						// no more nodes left, update front too
						setFront(null, c);
					}
					final T result = oldBackNode.getValue(c);
					returnToFree(oldBackNode, c);
					return result;
				}
			}.execute(null);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public T peekFront() {
		try {
			return new Atomic<T>(true) {
				@SuppressWarnings("unchecked")
				@Override
				public T atomically(AbstractDistinguishable self, Context c) {
					final String oldFront = getFront(c);
					// empty deque, do nothing
					if (oldFront == null)
						return null;
					// we have at least a node
					final DequeNode<T> oldFrontNode = (DequeNode<T>) HyFlow.getLocator().open(oldFront);
					return oldFrontNode.getValue(c);
				}
			}.execute(null);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public T peekBack() {
		try {
			return new Atomic<T>(true) {
				@Override
				public T atomically(AbstractDistinguishable self, Context c) {
					final String oldBack = getBack(c);
					// empty deque, do nothing
					if (oldBack == null)
						return null;
					// we have at least a node
					final DequeNode<T> oldBackNode = (DequeNode<T>) HyFlow.getLocator().open(oldBack);
					return oldBackNode.getValue(c);
				}
			}.execute(null);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}
}
