package edu.vt.rt.hyflow.benchmark.tm.skiplist.checkpoint;

import org.deuce.reflection.AddressUtil;
import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.Context;
import org.deuce.transaction.ContextDelegator;
import org.deuce.transaction.TransactionException;

import aleph.dir.DirectoryManager;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.benchmark.tm.bank.$HY$_IBankAccount;
import edu.vt.rt.hyflow.benchmark.tm.hashtable.Benchmark;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.helper.Atomic;

public class SkipList<T extends Comparable<? super T>> extends AbstractDistinguishable {

	public static final int MAX_LEVEL = 6;
	public static final double PROBABILITY = 0.5;
	String id;
	String header;
	Integer level;
	
	public static long header__ADDRESS__;
	public static long level__ADDRESS__;
	public static long id__ADDRESS__;
	private $HY$_IBankAccount $HY$_proxy;
	public static long $HY$_proxy__ADDRESS__;
	private Object $HY$_id;
	public static long $HY$_id__ADDRESS__;
	
	{
		try {
			header__ADDRESS__ = AddressUtil.getAddress(SkipList.class
					.getDeclaredField("header"));
			level__ADDRESS__ = AddressUtil.getAddress(SkipList.class
					.getDeclaredField("level"));
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
		this.level = 0;
		
		// Create header node
		this.header = id+"-header";
		new SkipNode<T>(this.header, MAX_LEVEL, null);
		
		AbstractContext context = ContextDelegator.getTopInstance();
		if (context == null)
			HyFlow.getLocator().register(this);
		else
			context.newObject(this); // add it to context publish-set
	}
	
	@SuppressWarnings("unchecked")
	public SkipNode<T> getNode(String id) {
		if (id == null)
			return null;
		else
			return (SkipNode<T>)HyFlow.getLocator().open(id);
	}
	
	private int get_level(Context __transactionContext__) {
		ContextDelegator.beforeReadAccess(this, level__ADDRESS__, __transactionContext__);
		final Integer old_value = (Integer) ContextDelegator.onReadAccess(this, this.level, level__ADDRESS__, __transactionContext__);
		return old_value;
	}
	
	private void set_level(Integer newval, Context __transactionContext__) {
		ContextDelegator.onWriteAccess(this, newval, level__ADDRESS__, __transactionContext__);
	}
	
	public void printAll() {
		printRecursive(header);
	}
	
	private void printRecursive(final String next) {
		try {
			new Atomic<Object>(true) {
				@Override
				public Object atomically(AbstractDistinguishable self,
						Context transactionContext) {
					SkipNode<T> x = getNode(next);
					if (x.get_value(transactionContext) != null)
						Logger.debug("Value = " + x.get_value(transactionContext));
					if (x.get_next(0, transactionContext) != null)
						printRecursive(x.get_next(0, transactionContext));
					return null;
				}
			}.execute(this);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean contains(T value) {
		return contains(value, -1, header);
	}
	
	private boolean contains(final T value, final int ii, final String x_id) {
		try {
			return new Atomic<Boolean>(true) {
				@Override
				public Boolean atomically(AbstractDistinguishable self,
						Context transactionContext) {
					int i = ii<0 ? get_level(transactionContext): ii;
					SkipNode<T> x = getNode(x_id);
					if (x.get_next(i, transactionContext) == null)
					{
						if (i==0) {
							return x!=null && 
								x.get_value(transactionContext) != null && 
								x.get_value(transactionContext).equals(value);
						} else {
							return contains(value, i-1, x_id);
						}
					}
					String pfx_id = x.get_next(i, transactionContext);
					SkipNode<T> pfx = getNode(pfx_id);
					if (pfx.get_value(transactionContext).compareTo(value) > 0)
					{
						if (i == 0) {
							return x!=null && 
								x.get_value(transactionContext) != null && 
								x.get_value(transactionContext).equals(value);
						} else {
							return contains(value, i-1, x_id);
						}
					}
					return contains(value, i, pfx_id);
				}
			}.execute(this);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public void insert(T value) {
		String[] updates = new String[MAX_LEVEL+1];
		insert_recurse(value, -1, header, updates);
	}
	
	// TODO: take care, updates is an object which can change.
	private void insert_recurse(final T value, final int ii, final String x_id, final String[] updates) {
		try {
			new Atomic<Object>(true) {
				@Override
				public Object atomically(AbstractDistinguishable self,
						Context transactionContext) {
					SkipNode<T> x = getNode(x_id);
					String pfx_id;
					SkipNode<T> pfx;
					int i = ii<0 ? get_level(transactionContext): ii;
					
					if (x.get_next(i, transactionContext) == null)
					{
						updates[i] = x_id;
						// Next i
						if (i > 0) {
							// check same node on a lower level
							insert_recurse(value, i-1, x_id, updates);
						} else {
							insert_finish(value, x.get_next(0, transactionContext), updates);
						}
						return null;
					}
					pfx_id = x.get_next(i, transactionContext);
					pfx = getNode(pfx_id);
					if (pfx.get_value(transactionContext).compareTo(value) >= 0)
					{
						updates[i] = x_id;
						// Next i
						if (i > 0) {
							// check same node on a lower level
							insert_recurse(value, i-1, x_id, updates);
						} else {
							insert_finish(value, x.get_next(0, transactionContext), updates);
						}
						return null;
					}
					
					// Check next node, on same level
					insert_recurse(value, i, pfx_id, updates);
					return null;
				}
			}.execute(this);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	private void insert_finish(final T value, final String x_id, final String[] updates) 
	{
		try {
			new Atomic<Object>(true) {
				@Override
				public Object atomically(AbstractDistinguishable self,
						Context transactionContext) {
					SkipNode<T> x = getNode(x_id);
					int level = get_level(transactionContext);
					
					if (x == null || !value.equals(x.get_value(transactionContext))) {        
						int lvl = randomLevel();
						
						// record header updates
						if (lvl > level) {
							for (int i = level + 1; i <= lvl; i++) {
						    	updates[i] = header;
						    }
						    level = lvl;
						    set_level(level, transactionContext);
						}
						
						// insert new node
						String new_id = id+"-node-"+Integer.toHexString((int)(Math.random()*Integer.MAX_VALUE)); 
						x = new SkipNode<T>(new_id, lvl, value);
						for (int i = 0; i <= lvl; i++) {
							SkipNode<T> upd = getNode(updates[i]);
						    x.set_next(i, upd.get_next(i, transactionContext), transactionContext);
						    upd.set_next(i, new_id, transactionContext);
						}
					}
					return null;
				}
			}.execute(this);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
		
	public void delete(T value) {
		String[] updates = new String[MAX_LEVEL+1];
		delete_recurse(value, -1, header, updates);
	}
	
	private void delete_recurse(final T value, final int ii, final String x_id, final String[] updates) {
		try {
			new Atomic<Object>(true) {
				@Override
				public Object atomically(AbstractDistinguishable self,
						Context transactionContext) {
					SkipNode<T> x = getNode(x_id);
					String pfx_id;
					SkipNode<T> pfx;
					int i = ii<0 ? get_level(transactionContext): ii;
					
					if (x.get_next(i, transactionContext) == null)
					{
						updates[i] = x_id;
						// Next i
						if (i > 0) {
							// check same node on a lower level
							delete_recurse(value, i-1, x_id, updates);
						} else {
							delete_finish(value, x.get_next(0, transactionContext), updates);
						}
						return null;
					}
					pfx_id = x.get_next(i, transactionContext);
					pfx = getNode(pfx_id);
					if (pfx.get_value(transactionContext).compareTo(value) >= 0)
					{
						updates[i] = x_id;
						// Next i
						if (i > 0) {
							// check same node on a lower level
							delete_recurse(value, i-1, x_id, updates);
						} else {
							delete_finish(value, x.get_next(0, transactionContext), updates);
						}
						return null;
					}
					
					// Check next node, on same level
					delete_recurse(value, i, pfx_id, updates);
					return null;
				}
			}.execute(this);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	private void delete_finish(final T value, final String x_id, final String[] updates) {
		try {
			new Atomic<Object>(true) {
				@Override
				public Object atomically(AbstractDistinguishable self,
						Context transactionContext) {
					SkipNode<T> x = getNode(x_id);
					int level = get_level(transactionContext);
				    
					if (x!=null && value.equals(x.get_value(transactionContext))) {
						// remove x from list
						for (int i = 0; i <= level; i++) {
							SkipNode<T> upd = getNode(updates[i]);
							if (!x_id.equals(upd.get_next(i, transactionContext)))
								break;
							upd.set_next(i, x.get_next(i, transactionContext), transactionContext);
						}
						
						// un-register node
						HyFlow.getLocator().delete(x);
						
						// decrease list level if needed
						SkipNode<T> fetch_header = getNode(header);
						while (level > 0 && fetch_header.get_next(level, transactionContext) == null) {
							level--;
						}
						set_level(level, transactionContext);
					 }
					return null;
				}
			}.execute(this);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}