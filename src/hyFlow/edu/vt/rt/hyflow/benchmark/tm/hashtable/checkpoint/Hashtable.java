package edu.vt.rt.hyflow.benchmark.tm.hashtable.checkpoint;

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
		
	public Object get(final Object key, Context transactionContext) {
		// Exception instead?
		if (key==null)
			return null;
		final HashBucket b = getBucket(key, false);
		return b.get(key, transactionContext);
	}
	
	public boolean put(final Object key, final Object value, Context transactionContext) {
		if (key==null)
			return false;
		
		final HashBucket b = getBucket(key, false);
	
		return b.put(key, value, transactionContext);
	}
	
	// TODO: make remove return the removed value, if any
	public Object remove(final Object key, Context transactionContext) {
		if (key==null)
			return false;
		final HashBucket b = getBucket(key, false);
		return b.remove(key, transactionContext);
	}
	
	public boolean contains(final Object key, Context transactionContext) {
		if (key==null)
			return false;
		final HashBucket b = getBucket(key, false);
		return b.contains(key, transactionContext);
	}

}

