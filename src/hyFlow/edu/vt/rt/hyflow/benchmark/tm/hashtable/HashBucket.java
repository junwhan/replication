package edu.vt.rt.hyflow.benchmark.tm.hashtable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.deuce.reflection.AddressUtil;
import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.Context;
import org.deuce.transaction.ContextDelegator;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.benchmark.tm.bank.$HY$_IBankAccount;
import edu.vt.rt.hyflow.core.tm.undoLog.AbstractLoggableObject;
import edu.vt.rt.hyflow.helper.AbstractLockMap;
import edu.vt.rt.hyflow.util.io.Logger;

public class HashBucket extends AbstractLoggableObject 
{
	private class Tuple implements Serializable
	{
		public Object key, value;
		public Tuple(Object key, Object value)
		{
			this.key = key;
			this.value = value;
		}
	}

	private ArrayList<Tuple> content = null;
	public AbstractLockMap locks = null;

	public static long content__ADDRESS__;
	public static long locks__ADDRESS__;

	private String id;
	public static long id__ADDRESS__;
	private $HY$_IBankAccount $HY$_proxy;
	public static long $HY$_proxy__ADDRESS__;
	private Object $HY$_id;
	public static long $HY$_id__ADDRESS__;

	{
		try {
			locks__ADDRESS__ = AddressUtil.getAddress(HashBucket.class
					.getDeclaredField("locks"));
			content__ADDRESS__ = AddressUtil.getAddress(HashBucket.class
					.getDeclaredField("content"));
			id__ADDRESS__ = AddressUtil.getAddress(HashBucket.class
					.getDeclaredField("id"));
			$HY$_proxy__ADDRESS__ = AddressUtil.getAddress(HashBucket.class
					.getDeclaredField("$HY$_proxy"));
			$HY$_id__ADDRESS__ = AddressUtil.getAddress(HashBucket.class
					.getDeclaredField("$HY$_id"));
		} catch (Exception e) {
			e.printStackTrace(Logger.levelStream[Logger.DEBUG]);
		}
	}
	
	public HashBucket(String id) {
		this.id = id;
		this.locks = new AbstractLockMap(id);
		this.content = new ArrayList<Tuple>();
		
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
	public Object get(Object key, Context __transactionContext__)
	{
		try {
			ContextDelegator.beforeReadAccess(this, content__ADDRESS__, __transactionContext__);
			final ArrayList<Tuple> oldContent = (ArrayList<Tuple>) ContextDelegator.onReadAccess(this, this.content, content__ADDRESS__, __transactionContext__);
			
			for (Tuple t : oldContent) {
				if (key.equals(t.key)) {
					return t.value;
				}
			}
			return null;
		} finally {
			if (Benchmark.inner_delay)
				edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
		}
	}
	
	public Object put(Object key, Object value, Context __transactionContext__) 
	{
		// TODO: what about an "immutable" version?
		// later: this looks immutable enough for me :)
		try {
			ContextDelegator.beforeReadAccess(this, content__ADDRESS__, __transactionContext__);
			final ArrayList<Tuple> oldContent = (ArrayList<Tuple>) ContextDelegator.onReadAccess(this, this.content, content__ADDRESS__, __transactionContext__);
			final ArrayList<Tuple> newContent = (ArrayList<Tuple>)oldContent.clone();
			
			for (int i=0; i<newContent.size(); i++) {
				if (key.equals(newContent.get(i).key)) {
					// go to extra lengths now to modify existing objects!
					newContent.set(i, new Tuple(key, value));
					// finalize write
					ContextDelegator.onWriteAccess(this, newContent, content__ADDRESS__, __transactionContext__);
					return oldContent.get(i).value;
				}
			}
			newContent.add(new Tuple(key, value));
			ContextDelegator.onWriteAccess(this, newContent, content__ADDRESS__, __transactionContext__);
			return null;
		} finally {
			if (Benchmark.inner_delay)
				edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
		}
	}
	
	public Object remove(Object key, Context __transactionContext__)
	{
		try {
			ContextDelegator.beforeReadAccess(this, content__ADDRESS__, __transactionContext__);
			final ArrayList<Tuple> oldContent = (ArrayList<Tuple>) ContextDelegator.onReadAccess(this, this.content, content__ADDRESS__, __transactionContext__);
			final ArrayList<Tuple> newContent = (ArrayList<Tuple>)oldContent.clone();
			
			for (int i=0; i<newContent.size(); i++) {
				if (key.equals(newContent.get(i).key)) {
					newContent.remove(i);
					ContextDelegator.onWriteAccess(this, newContent, content__ADDRESS__, __transactionContext__);
					return oldContent.get(i).value;
				}
			}
			return null;
		} finally {
			if (Benchmark.inner_delay)
				edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
		}
	}
	
	public boolean contains(Object key, Context __transactionContext__)
	{
		try {
			ContextDelegator.beforeReadAccess(this, content__ADDRESS__, __transactionContext__);
			final ArrayList<Tuple> oldContent = (ArrayList<Tuple>) ContextDelegator.onReadAccess(this, this.content, content__ADDRESS__, __transactionContext__);
			for (Tuple t : oldContent) {
				if (key.equals(t.key)) {
					return true;
				}
			}
			return false;
		} finally {
			if (Benchmark.inner_delay)
				edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
		}
	}
}
