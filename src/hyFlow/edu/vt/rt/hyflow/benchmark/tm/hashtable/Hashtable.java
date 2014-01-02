package edu.vt.rt.hyflow.benchmark.tm.hashtable;

import org.deuce.reflection.AddressUtil;
import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.Context;
import org.deuce.transaction.ContextDelegator;
import org.deuce.transaction.TransactionException;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.benchmark.tm.bank.$HY$_IBankAccount;
import edu.vt.rt.hyflow.benchmark.tm.bank.BankAccount;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.dir.IHyFlow;
import edu.vt.rt.hyflow.core.tm.NestedContext;
import edu.vt.rt.hyflow.core.tm.NestingModel;
import edu.vt.rt.hyflow.core.tm.undoLog.AbstractLoggableObject;
import edu.vt.rt.hyflow.helper.AbstractLockMap;
import edu.vt.rt.hyflow.helper.Atomic;
import edu.vt.rt.hyflow.helper.TrackerLockMap;
import edu.vt.rt.hyflow.util.io.Logger;

public class Hashtable extends AbstractLoggableObject 
{
	//TODO: implement a way to copy from-to the same bucket!!! currently, it livelocks
	
	private int bucketCount = 0;
	private AbstractLockMap locks = null;

	public static long bucketCount__ADDRESS__;
	public static long locks__ADDRESS__;

	private String id;
	public static long id__ADDRESS__;
	private $HY$_IBankAccount $HY$_proxy;
	public static long $HY$_proxy__ADDRESS__;
	private Object $HY$_id;
	public static long $HY$_id__ADDRESS__;

	{
		try {
			locks__ADDRESS__ = AddressUtil.getAddress(Hashtable.class
					.getDeclaredField("locks"));
			bucketCount__ADDRESS__ = AddressUtil.getAddress(Hashtable.class
					.getDeclaredField("bucketCount"));
			id__ADDRESS__ = AddressUtil.getAddress(Hashtable.class
					.getDeclaredField("id"));
			$HY$_proxy__ADDRESS__ = AddressUtil.getAddress(Hashtable.class
					.getDeclaredField("$HY$_proxy"));
			$HY$_id__ADDRESS__ = AddressUtil.getAddress(Hashtable.class
					.getDeclaredField("$HY$_id"));
		} catch (Exception e) {
			e.printStackTrace(Logger.levelStream[Logger.DEBUG]);
		}
	}
	
	public Hashtable(String id, int buckets) {
		this.id = id;
		this.locks = new AbstractLockMap(id);
		this.bucketCount = buckets;
		
		for (int i=0; i<buckets; i++) {
			new HashBucket(id+"-b"+i);
		}

		AbstractContext context = ContextDelegator.getTopInstance();
		if (context == null)
			HyFlow.getLocator().register(this);
		else
			context.newObject(this); 
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
	private HashBucket getBucket(Object key, boolean readonly) {
		final int bucket = key.hashCode() % bucketCount;
		final String bucketid = id+"-b"+bucket;
		return (HashBucket)HyFlow.getLocator().open(bucketid, readonly ? "r" : "w");
	}
		
	public Object get(final Object key) {
		// Exception instead?
		if (key==null)
			return null;
		try {
			return new Atomic<Object>(true) {
				@Override
				public Object atomically(AbstractDistinguishable self,
						Context transactionContext) {
					final HashBucket b = getBucket(key, false);
					// O/N
					if (((NestedContext)transactionContext).getNestingModel() == NestingModel.OPEN) {
						// TODO: early/late lock acquisition (upon calling lock or upon commit)
						// Mixed? i.e. Check lock upon call, acquire on commit
						// If early, must release locks on abort
						// What about livelocks?!? 
						((NestedContext)transactionContext).onLockAction(
								b.getId().toString(), key.toString(), true, true);
						m_hasOnAbort = true;
						m_hasOnCommit = true;
					}
					// end O/N
					return b.get(key, transactionContext);
				}
				@Override
				public void onAbort(Context __transactionContext__) {
					// TODO: automatic lock release on abort/commit, override only for special cases
					final HashBucket b = getBucket(key, false);
					((NestedContext)__transactionContext__).onLockAction(
							b.getId().toString(), key.toString(), true, false);
				}
				@Override
				public void onCommit(Context __transactionContext__) {
					final HashBucket b = getBucket(key, false);
					((NestedContext)__transactionContext__).onLockAction(
							b.getId().toString(), key.toString(), true, false);
				}
			}.execute(this);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	public boolean put(final Object key, final Object value) {
		if (key==null)
			return false;
		try {
			return new Atomic<Boolean>(true) {
				Object oldValue = null;
				@Override
				public Boolean atomically(AbstractDistinguishable self,
						Context transactionContext) {
					final HashBucket b = getBucket(key, false);
					// O/N
					if (((NestedContext)transactionContext).getNestingModel() == NestingModel.OPEN) {
						((NestedContext)transactionContext).onLockAction(
								b.getId().toString(), key.toString(), false, true);
						m_hasOnAbort = true;
						m_hasOnCommit = true;
					}
					// end O/N
					oldValue = b.put(key, value, transactionContext);
					return oldValue != null;
				}
				@Override
				public void onAbort(Context __transactionContext__) {
					final HashBucket b = getBucket(key, false);
					if (oldValue != null)
						b.put(key, oldValue, __transactionContext__);
					else
						b.remove(key, __transactionContext__);
					((NestedContext)__transactionContext__).onLockAction(
							b.getId().toString(), key.toString(), false, false);
				}
				@Override
				public void onCommit(Context __transactionContext__) {
					final HashBucket b = getBucket(key, false);
					((NestedContext)__transactionContext__).onLockAction(
							b.getId().toString(), key.toString(), false, false);
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
	
	// TODO: make remove return the removed value, if any
	public Object remove(final Object key) {
		if (key==null)
			return false;
		try {
			return new Atomic<Object>(true) {
				Object oldValue = null;
				@Override
				public Object atomically(AbstractDistinguishable self,
						Context transactionContext) {
					final HashBucket b = getBucket(key, false);
					// O/N
					if (((NestedContext)transactionContext).getNestingModel() == NestingModel.OPEN) {
						((NestedContext)transactionContext).onLockAction(
								b.getId().toString(), key.toString(), false, true);
						m_hasOnAbort = true;
						m_hasOnCommit = true;
					}
					// end O/N
					oldValue = b.remove(key, transactionContext);
					return oldValue;
				}
				@Override
				public void onAbort(Context __transactionContext__) {
					final HashBucket b = getBucket(key, false);
					if (oldValue != null)
						b.put(key, oldValue, __transactionContext__);
					((NestedContext)__transactionContext__).onLockAction(
							b.getId().toString(), key.toString(), false, false);
				}
				@Override
				public void onCommit(Context __transactionContext__) {
					final HashBucket b = getBucket(key, false);
					((NestedContext)__transactionContext__).onLockAction(
							b.getId().toString(), key.toString(), false, false);
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
	
	public boolean contains(final Object key) {
		if (key==null)
			return false;
		try {
			return new Atomic<Boolean>(true) {
				@Override
				public Boolean atomically(AbstractDistinguishable self,
						Context transactionContext) {
					final HashBucket b = getBucket(key, false);
					// O/N
					if (((NestedContext)transactionContext).getNestingModel() == NestingModel.OPEN) {
						((NestedContext)transactionContext).onLockAction(
								b.getId().toString(), key.toString(), true, true);
						m_hasOnAbort = true;
						m_hasOnCommit = true;
					}
					// end O/N
					return b.contains(key, transactionContext);
				}
				@Override
				public void onAbort(Context __transactionContext__) {
					final HashBucket b = getBucket(key, false);
					((NestedContext)__transactionContext__).onLockAction(
							b.getId().toString(), key.toString(), true, false);
				}
				@Override
				public void onCommit(Context __transactionContext__) {
					final HashBucket b = getBucket(key, false);
					((NestedContext)__transactionContext__).onLockAction(
							b.getId().toString(), key.toString(), true, false);
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

}

