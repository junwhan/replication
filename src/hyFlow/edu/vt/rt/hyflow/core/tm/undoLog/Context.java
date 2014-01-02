package edu.vt.rt.hyflow.core.tm.undoLog;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.deuce.reflection.UnsafeHolder;
import org.deuce.transaction.TransactionException;

import aleph.dir.DirectoryManager;
import aleph.dir.cm.home.ClientSide;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.util.io.Logger;

public class Context  extends org.deuce.transaction.AbstractContext{

	private ReentrantLock lock = new ReentrantLock();
	
	private Map<AbstractLoggableObject, Integer> readset = new HashMap<AbstractLoggableObject, Integer>();
	
	class WriteEntry{
		AbstractLoggableObject reference;
		long field;
		Object oldValue;
		int oldVersion;
		WriteEntry(AbstractLoggableObject reference,  long field){
			this.reference = reference;
			this.field = field;
			this.oldValue = UnsafeHolder.getUnsafe().getObject(reference, field);
		}
	}
	private List<WriteEntry> writeset = new LinkedList<WriteEntry>();
	
	private Set<AbstractDistinguishable> acquiredObjects = new HashSet<AbstractDistinguishable>();
	
	private int retries;
	
	@Override
	public void init(int atomicBlockId) {
		try {
			lock.lock();
			super.init(atomicBlockId);
			Logger.debug(txnId + ":Init");
			writeset.clear();
			readset.clear();
			acquiredObjects.clear();
		} finally{
			lock.unlock();
		}
	}
	
	@Override
	public boolean commit() {
		try{
			status = STATUS.BUSY;
			lock.lock();
			// commit updated objects
			Logger.debug(txnId + ":TryCommit: releasing ownership");
			release();
			
			Logger.debug(txnId + ":TryCommit: done");
		} finally{
			status = STATUS.ABORTED;
			lock.unlock();
		}
		
		complete();
		return true;
	}

	@Override
	public boolean rollback() {
		if(status.equals(STATUS.BUSY)){
			try {
				throw new Exception();
			} catch (Exception e) {
				for (StackTraceElement trace : e.getStackTrace()) {
					if(trace.getClassName().equals(ClientSide.class.getName()))
						throw new TransactionException();
				}
			}
		}

		try{
			lock.lock();
			if(status.equals(STATUS.ABORTED))
				return true;
			status = STATUS.BUSY;
			aborts++;
			Logger.debug(txnId + ":Rollback... " + writeset.size());
			retries++;
			release();
		} finally{
			status = STATUS.ABORTED;
			lock.unlock();
		}
		return true;
	}
	
	private void release(){
		while(true)
			try {
				for(Iterator<WriteEntry> itr=writeset.iterator(); itr.hasNext(); ){
					AbstractLoggableObject reference = itr.next().reference;
					reference.release(this);
					if(reference instanceof AbstractDistinguishable)
						acquiredObjects.add((AbstractDistinguishable)reference);
				}
				for(Iterator<AbstractLoggableObject> itr=readset.keySet().iterator(); itr.hasNext(); ){
					AbstractLoggableObject reference = itr.next();
					if(reference instanceof AbstractDistinguishable)
						acquiredObjects.add((AbstractDistinguishable)reference);
				}
				if(acquiredObjects.isEmpty())
					return;
				
				Logger.debug("Backoff acquired objects ;" + acquiredObjects.size());
				DirectoryManager locator = HyFlow.getLocator();
				Logger.debug("Locator found");
				for(AbstractDistinguishable distinguishable:acquiredObjects){
					Logger.debug("Releasing " + distinguishable);
					locator.release(distinguishable);
					Logger.debug("Released " + distinguishable);
				}
				break;
			} catch (ConcurrentModificationException e) {
				Logger.debug("Conucrrent Exception !!");
			}
	}

	
	// ----------------------- Read Fields ------------------------ //
	@Override
	public void beforeReadAccess(Object obj, long field) {
		if(status.equals(STATUS.ABORTED))
			throw new TransactionException();
		if(obj instanceof AbstractLoggableObject){
			AbstractLoggableObject loggable = (AbstractLoggableObject)obj;
			if(!loggable.isFree(getContextId()))
				throw new TransactionException();	// contention	R/W
		}
	}
	
	@Override
	public Object onReadAccess(Object obj, Object value, long field) {
		return value;
	}

	@Override
	public boolean onReadAccess(Object obj, boolean value, long field) {
		return value;
	}

	@Override
	public byte onReadAccess(Object obj, byte value, long field) {
		return value;
	}

	@Override
	public char onReadAccess(Object obj, char value, long field) {
		return value;
	}

	@Override
	public short onReadAccess(Object obj, short value, long field) {
		return value;
	}

	@Override
	public int onReadAccess(Object obj, int value, long field) {
		return value;
	}

	@Override
	public long onReadAccess(Object obj, long value, long field) {
		return value;
	}

	@Override
	public float onReadAccess(Object obj, float value, long field) {
		return value;
	}

	@Override
	public double onReadAccess(Object obj, double value, long field) {
		return value;
	}

	// ----------------------- Write Fields ------------------------ //
	
	public void beforeWriteAccess(Object obj, long field) {
		// TODO: OPEN OBJECT FOR WRITE
		
		if(status.equals(STATUS.ABORTED))
			throw new TransactionException();
		
		if(obj instanceof AbstractLoggableObject){
			AbstractLoggableObject loggable = (AbstractLoggableObject)obj;
			while(!loggable.own(this)){	// try to set me as the owner
				int res = HyFlow.getConflictManager().resolve(this, loggable.getOwner());
				if(res > 0)
					try {
						Thread.currentThread().sleep(res);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
			}
	//		System.out.println(System.nanoTime()+ " Adding " + obj + " to " + txnId);
			if(!status.equals(STATUS.ABORTED))
				writeset.add(new WriteEntry(loggable, field));
		}
	}
	
	@Override
	public void onWriteAccess(Object obj, Object value, long field) {
		beforeWriteAccess(obj, field);
		if(value instanceof AbstractDistinguishable)
			acquiredObjects.add((AbstractDistinguishable)value);
		UnsafeHolder.getUnsafe().putObject(obj, field, value);
	}

	@Override
	public void onWriteAccess(Object obj, boolean value, long field) {
		beforeWriteAccess(obj, field);
		UnsafeHolder.getUnsafe().putBoolean(obj, field, value);
	}

	@Override
	public void onWriteAccess(Object obj, byte value, long field) {
		beforeWriteAccess(obj, field);
		UnsafeHolder.getUnsafe().putByte(obj, field, value);
	}

	@Override
	public void onWriteAccess(Object obj, char value, long field) {
		beforeWriteAccess(obj, field);
		UnsafeHolder.getUnsafe().putChar(obj, field, value);
	}

	@Override
	public void onWriteAccess(Object obj, short value, long field) {
		beforeWriteAccess(obj, field);
		UnsafeHolder.getUnsafe().putShort(obj, field, value);
	}

	@Override
	public void onWriteAccess(Object obj, int value, long field) {
		beforeWriteAccess(obj, field);
		UnsafeHolder.getUnsafe().putInt(obj, field, value);
	}

	@Override
	public void onWriteAccess(Object obj, long value, long field) {
		beforeWriteAccess(obj, field);
		UnsafeHolder.getUnsafe().putLong(obj, field, value);
	}

	@Override
	public void onWriteAccess(Object obj, float value, long field) {
		beforeWriteAccess(obj, field);
		UnsafeHolder.getUnsafe().putFloat(obj, field, value);
	}

	@Override
	public void onWriteAccess(Object obj, double value, long field) {
		beforeWriteAccess(obj, field);
		UnsafeHolder.getUnsafe().putDouble(obj, field, value);
	}
}
