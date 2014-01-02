package edu.vt.rt.hyflow.benchmark.tm.skiplist;

import org.deuce.reflection.AddressUtil;
import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.Context;
import org.deuce.transaction.ContextDelegator;
import org.deuce.transaction.TransactionException;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.benchmark.tm.bank.$HY$_IBankAccount;
import edu.vt.rt.hyflow.benchmark.tm.hashtable.Benchmark;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.tm.NestedContext;
import edu.vt.rt.hyflow.core.tm.NestingModel;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.helper.Atomic;

public class SkipList<T extends Comparable<? super T>> extends AbstractDistinguishable {

	public static final int MAX_LEVEL = 6;
	public static final double PROBABILITY = 0.5;
	String id;
	String header;
	//Integer level;
	
	public static long header__ADDRESS__;
	//public static long level__ADDRESS__;
	public static long id__ADDRESS__;
	private $HY$_IBankAccount $HY$_proxy;
	public static long $HY$_proxy__ADDRESS__;
	private Object $HY$_id;
	public static long $HY$_id__ADDRESS__;
	
	{
		try {
			header__ADDRESS__ = AddressUtil.getAddress(SkipList.class
					.getDeclaredField("header"));
			//level__ADDRESS__ = AddressUtil.getAddress(SkipList.class
			//		.getDeclaredField("level"));
			id__ADDRESS__ = AddressUtil.getAddress(SkipList.class
					.getDeclaredField("id"));
			$HY$_proxy__ADDRESS__ = AddressUtil.getAddress(SkipList.class
					.getDeclaredField("$HY$_proxy"));
			$HY$_id__ADDRESS__ = AddressUtil.getAddress(SkipList.class
					.getDeclaredField("$HY$_id"));
		} catch (Exception e) {
			e.printStackTrace(Logger.levelStream[Logger.DEBUG]);
		}
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
	public static int randomLevel() {
	    int level = (int)(Math.log(1.0-Math.random()) / Math.log(1.0-PROBABILITY));
	    return Math.min(level, MAX_LEVEL);
	}
	
	public SkipList(String id) {
		this.id = id;
		//this.level = 0;
		
		// Create header node
		this.header = id+"-header";
		new SkipNode<T>(this.header, 0, null, MAX_LEVEL+1);
		
		AbstractContext context = ContextDelegator.getTopInstance();
		if (context == null)
			HyFlow.getLocator().register(this);
		else
			context.newObject(this); // add it to context publish-set
	}
	
	@SuppressWarnings("unchecked")
	public SkipNode<T> getNode(String id) {
		Logger.debug("SkipNode.getNode("+id+")");
		if (id == null)
			return null;
		else
			return (SkipNode<T>)HyFlow.getLocator().open(id);
	}
	
	/*private int get_level(Context __transactionContext__) {
		ContextDelegator.beforeReadAccess(this, level__ADDRESS__, __transactionContext__);
		final Integer old_value = (Integer) ContextDelegator.onReadAccess(this, this.level, level__ADDRESS__, __transactionContext__);
		Logger.debug("SkipList.get_level() = "+old_value);
		return old_value;
	}*/
	
	/*private void set_level(Integer newval, Context __transactionContext__) {
		Logger.debug("SkipList.set_level(newval="+newval+")");
		ContextDelegator.onWriteAccess(this, newval, level__ADDRESS__, __transactionContext__);
	}*/
	
	/*
	@Atomic
	// Start with next="header"
	public void printRecursive(String next) {
		SkipNode<T> x = getNode(next);
		if (x.get_value() != null)
			Logger.debug("Value = " + x.get_value());
		if (x.get_next(0) != null)
			printRecursive(x.get_next(0));
	}
	*/
	
	public void printAll() {
		try {
			new Atomic<Object>(true) {
				@Override
				public Object atomically(AbstractDistinguishable self, Context transactionContext) {
					try
					{
						Logger.debug("SkipList::printAll() begin");
						SkipNode<T> x = getNode(header);
					    String next_id = x.get_next(0, transactionContext);
					    while (next_id != null) {
					    	x = getNode(next_id);
					        Logger.debug("Value = " + x.get_value(transactionContext));
					        x.print_debug_info(transactionContext);
					        next_id = x.get_next(0, transactionContext);
					    }
					    Logger.debug("SkipList::printAll() end");
						return null;
					} finally {
						if (Benchmark.inner_delay)
							edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
					}
				}
			}.execute(this);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private boolean contains(T value, Context transactionContext) {
		try
		{
			Logger.debug("SkipList::contains() begin");
			SkipNode<T> pfx, x = getNode(header);
			int level = x.get_level(transactionContext);
			// in decreasing order of levels
			for (int i=level; i>=0; i--) {
				// keep going until end or passed
				while (true) {
					if (x.get_next(i, transactionContext) == null)
						break;
					pfx = getNode(x.get_next(i, transactionContext));
					if (pfx.get_value(transactionContext).compareTo(value) > 0) {
						break;
					}
					x = pfx;
				}
				
			}
			// result
			Logger.debug("SkipList::contains() end");
			return x!=null && 
				x.get_value(transactionContext) != null && 
				x.get_value(transactionContext).equals(value);
		} finally {
			if (Benchmark.inner_delay)
				edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
		}
	}
	
	public boolean contains(final T value) {
		try {
			return new Atomic<Boolean>(true) {
				@Override
				public Boolean atomically(AbstractDistinguishable self,
						Context transactionContext) {
					// O/N
					if (((NestedContext)transactionContext).getNestingModel() == NestingModel.OPEN) {
						((NestedContext)transactionContext).onLockAction(
								id.toString(), value.toString(), true, true);
						m_hasOnAbort = true;
						m_hasOnCommit = true;
					}
					// end O/N
					return contains(value, transactionContext);
				}
				@Override
				public void onCommit(Context transactionContext) {
					Logger.debug("SkipList::contains() onCommit");
					((NestedContext)transactionContext).onLockAction(
							id.toString(), value.toString(), true, false);
				}
				@Override
				public void onAbort(Context transactionContext) {
					Logger.debug("SkipList::contains() onAbort");
					((NestedContext)transactionContext).onLockAction(
							id.toString(), value.toString(), true, false);
				}
			}.execute(this);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	
	@SuppressWarnings("unchecked")
	private boolean insert(T value, Context transactionContext) {
		try
		{
			Logger.debug("SkipList::insert() begin");
			SkipNode<T> fetch_header = getNode(header);
			SkipNode<T> fetch_x, x = fetch_header;
			SkipNode<T>[] update = new SkipNode[MAX_LEVEL + 1];
			Integer level = fetch_header.get_level(transactionContext);
			
			// find and record updates
			for (int i = level; i >= 0; i--) {
				while (true) {
					if (x.get_next(i, transactionContext) == null)
						break;
					fetch_x = getNode(x.get_next(i, transactionContext));
					if (fetch_x.get_value(transactionContext).compareTo(value) >= 0)
						break;
					x = fetch_x;
				}
				update[i] = x; 
			}
			x = getNode(x.get_next(0, transactionContext));
		
			
			if (x == null || !x.get_value(transactionContext).equals(value)) {        
				int lvl = randomLevel();
				
				// record header updates
				if (lvl > level) {
					for (int i = level + 1; i <= lvl; i++) {
				    	update[i] = fetch_header;
				    	//talex: hack: can solve problem by clearing header pointers
				    	//on higher levels here
				    }
				    level = lvl;
				    fetch_header.set_level(level, transactionContext);
				}
				
				// insert new node
				String new_id = id+"-node-"+Integer.toHexString((int)(Math.random()*Integer.MAX_VALUE)); 
				x = new SkipNode<T>(new_id, lvl, value);
				for (int i = 0; i <= lvl; i++) {
				    x.set_next(i, update[i].get_next(i, transactionContext), transactionContext);
				    update[i].set_next(i, new_id, transactionContext);
				}
				Logger.debug("SkipList::insert() end");
				return true;
			}
			else {
				Logger.debug("SkipList::insert() end");
				return false;
			}
		} finally {
			if (Benchmark.inner_delay)
				edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
		}
	}
	
	public void insert(final T value)
	{
		try {
			new Atomic<Object>(true) {
				boolean inserted = false;
				@Override
				public Object atomically(AbstractDistinguishable self,
						Context transactionContext) {
					if (((NestedContext)transactionContext).getNestingModel() == NestingModel.OPEN 
							&& !((NestedContext)transactionContext).isTopLevel()) {
						((NestedContext)transactionContext).onLockAction(
								id, value.toString(), false, true);
						m_hasOnAbort = true;
						m_hasOnCommit = true;
					}
					inserted = insert(value, transactionContext);
					return null;
				}
				@SuppressWarnings("unchecked")
				@Override 
				public void onAbort(Context __transactionContext__) {
					Logger.debug("SkipList::insert() onAbort begin");
					if (inserted) {
						SkipList<T> sl2 = (SkipList<T>)HyFlow.getLocator().open(id); 
						sl2.delete(value, __transactionContext__);
					}
					((NestedContext)__transactionContext__).onLockAction(
							id, value.toString(), false, false);
					Logger.debug("SkipList::insert() onAbort end");
				}
				@Override
				public void onCommit(Context __transactionContext__) {
					Logger.debug("SkipList::insert() onCommit");
					((NestedContext)__transactionContext__).onLockAction(
							id, value.toString(), false, false);
				}
			}.execute(this);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	@SuppressWarnings("unchecked")
	private boolean delete(T value, Context transactionContext) {
		try 
		{
			Logger.debug("SkipList::delete() begin");
			SkipNode<T> fetch_header = getNode(header);
		    SkipNode<T> fetch_x, x = fetch_header;
		    SkipNode<T>[] update = new SkipNode[MAX_LEVEL + 1];
		    int level = fetch_header.get_level(transactionContext);
		    
		    // find and record updates (Same as for insert)
			for (int i = level; i >= 0; i--) {
				while (true) {
					if (x.get_next(i, transactionContext) == null)
						break;
					fetch_x = getNode(x.get_next(i, transactionContext));
					if (fetch_x.get_value(transactionContext).compareTo(value) >= 0)
						break;
					x = fetch_x;
				}
				update[i] = x;
			}
			x = getNode(x.get_next(0, transactionContext));
		    
			if (x != null && x.get_value(transactionContext).equals(value)) {
				Logger.debug("SkipList.delete: Found node to delete: "+x.getId()+", val="+x.get_value(transactionContext));
				
				// remove x from list
				String x_id = (String)x.getId();
				for (int i = 0; i <= level; i++) {
					if (!x_id.equals(update[i].get_next(i, transactionContext)))
						break;
					update[i].set_next(i, x.get_next(i, transactionContext), transactionContext);
				}
				
				// un-register node
				HyFlow.getLocator().delete(x);
				
				// decrease list level if needed
				int new_level = level;
				while (new_level > 0 && fetch_header.get_next(new_level, transactionContext) == null) {
					new_level--;
				}
				if (level != new_level)
					fetch_header.set_level(new_level, transactionContext);
				Logger.debug("SkipList::delete() end");
				return true;
			 }
			 else {
				 Logger.debug("SkipList::delete() end");
				 return false;
			 }
		} finally {
			if (Benchmark.inner_delay)
				edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
		}
	}
	
	public void delete(final T value)
	{
		try {
			new Atomic<Object>(true) {
				boolean deleted = false;
				@Override
				public Object atomically(AbstractDistinguishable self,
						Context transactionContext) {
					if (((NestedContext)transactionContext).getNestingModel() == NestingModel.OPEN) {
						((NestedContext)transactionContext).onLockAction(
								id, value.toString(), false, true);
						m_hasOnAbort = true;
						m_hasOnCommit = true;
					}
					deleted = delete(value, transactionContext);
					return null;
				}
				@SuppressWarnings("unchecked")
				@Override 
				public void onAbort(Context __transactionContext__) {
					Logger.debug("SkipList::delete() onAbort begin");
					if (deleted) {
						SkipList<T> sl2 = (SkipList<T>)HyFlow.getLocator().open(id);
						sl2.insert(value, __transactionContext__);
					}
					((NestedContext)__transactionContext__).onLockAction(
							id, value.toString(), false, false);
					Logger.debug("SkipList::delete() onAbort end");
				}
				@Override
				public void onCommit(Context __transactionContext__) {
					Logger.debug("SkipList::delete() onCommit");
					((NestedContext)__transactionContext__).onLockAction(
							id, value.toString(), false, false);
				}
			}.execute(this);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}