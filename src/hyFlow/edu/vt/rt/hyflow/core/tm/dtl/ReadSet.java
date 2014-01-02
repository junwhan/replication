package edu.vt.rt.hyflow.core.tm.dtl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.deuce.transaction.TransactionException;
import org.deuce.transform.Exclude;

import aleph.Message;
import aleph.comm.CommunicationManager;
import aleph.dir.DirectoryManager;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.dir.dtl2.TrackerDirectory;
import edu.vt.rt.hyflow.core.tm.dtl2.field.ReadObjectAccess;
import edu.vt.rt.hyflow.util.io.Logger;

/**
 * Represents the transaction read set.
 * And acts as a recycle pool of the {@link ReadObjectAccess}.
 *  
 * @author Guy Korland
 * @author Mohamed M. Saad
 * @since 0.7
 */
@Exclude
public class ReadSet{
	public class ValidationEntry {
		AbstractDistinguishable local_object;
		int remote_version;
		public ValidationEntry(AbstractDistinguishable obj) {
			local_object = obj;
		}
	}
	public class ValidationSet {
		Semaphore status_sem = new Semaphore(0);
		AtomicInteger pending_replies = new AtomicInteger(0);
		ReadSet calling_readset = null;
		ConcurrentHashMap<Object,ValidationEntry> object_results = new ConcurrentHashMap<Object,ValidationEntry>();
		ConcurrentLinkedQueue<Object> obj_reply_q = new ConcurrentLinkedQueue<Object>();
		boolean failed = false;
		int txclock;
		public ValidationSet(ReadSet rs, int txclock) {
			calling_readset = rs;
			this.txclock = txclock;
		}
	}
	
	private static final int DEFAULT_CAPACITY = 1024;
	private ReadObjectAccess[] readSet = new ReadObjectAccess[DEFAULT_CAPACITY];
	private int nextAvailable = 0;
	private ReadObjectAccess currentReadFieldAccess = null;
	
	public static ConcurrentHashMap<Integer, ValidationSet> pendingValidations = new ConcurrentHashMap<Integer, ValidationSet>();
	//int remoteValidateResult;
	
	//public static Map<Integer, ReadSet> pendingValidates = new ConcurrentHashMap<Integer, ReadSet>();
	public int getLength() {
		// talex: probably not always correct, but ehwell... 
		return nextAvailable;
	}
	
	public ReadSet(){
		fillArray(0);
	}
	
	public void clear(){
		nextAvailable = 0;
		// talex: let's enhance the clear op, we're running into trouble.
		// maybe we can only clear one, initially the first one, and on each access the next one
		for (int i=0; i<readSet.length; i++) {
			readSet[i].clear();
		}
	}

	private void fillArray( int offset){
		for( int i=offset ; i < readSet.length ; ++i){
			readSet[i] = new ReadObjectAccess();
		}
	}

	public ReadObjectAccess getNext(){
		if( nextAvailable >= readSet.length){
			int orignLength = readSet.length;
			ReadObjectAccess[] tmpReadSet = new ReadObjectAccess[ 2*orignLength];
			System.arraycopy(readSet, 0, tmpReadSet, 0, orignLength);
			readSet = tmpReadSet;
			fillArray( orignLength);
		}
		currentReadFieldAccess = readSet[ nextAvailable++];
		return currentReadFieldAccess;
	}
	
	public ReadObjectAccess getCurrent(){
		Logger.debug("get current " + currentReadFieldAccess);
		return currentReadFieldAccess;
	}
	
	// Parallel validation: start
	// TODO: by checking local objects first, I ruin the order. Does that matter?
	public void validation_start(int clock) {
		// Create results data structure
		Integer hash_code = hashCode();
		ValidationSet vset = new ValidationSet(this, clock);
		
		// Setup validation list
		for(ReadObjectAccess field: readSet){
			Object obj = field.getObject();
			
			// Check some conditions
			if(obj == null) {
				break;
			}
			
			if(! (obj instanceof AbstractDistinguishable)){
				Logger.debug("checkClock: obj "+obj+" not AbstractDistinguishable instance, skip");
				continue;
			}
			
			AbstractDistinguishable dist_obj = (AbstractDistinguishable) obj;
			if(dist_obj.isShared()){
				Logger.debug("Skip shared " + obj);
				continue;
			}

			// Handle local objects right here
			try {
				if (LockTable.checkLock( obj, clock, false, Context.LOCKS_MARKER.get() ) < 0 ) {
					// This is remote object, add to list
					vset.object_results.put(dist_obj.getId(), new ValidationEntry(dist_obj));
					Logger.debug("Add remote" + obj);
				}
			} catch (TransactionException e) {
				// Local object validation failed, no need to continue at this level
				Logger.debug("Local object validation failed, cancelling validation process.");
				return;
			}
		}
		
		pendingValidations.put(hash_code, vset);
		Logger.debug("Readlist " + vset.object_results.size() + " remote objects.");
		vset.pending_replies.set(vset.object_results.size());
		
		// Send validation request messages
		for (ValidationEntry entry : vset.object_results.values()) {
			AbstractDistinguishable obj = entry.local_object; 
			try {
				CommunicationManager.getManager().send(obj.getOwnerNode(), 
						new ValidateRequest(obj.getId(), hash_code));
			} catch (IOException e) {
				// Network problem?!?
				e.printStackTrace();
				entry.local_object.invalidate();
				// Request didn't get sent, makes sure we're not waiting for reply.
				vset.pending_replies.decrementAndGet();
			}
		}
	}
	
	public boolean validation_result(boolean clear) {
		try {
			// Process replies as they arrive, stop at first invalid object
			ValidationSet vset = pendingValidations.get(hashCode());
			if (vset == null) {
				return false;
			}
			while (vset.pending_replies.get() > 0) {
				// wait for reply
				try {
					vset.status_sem.acquire();
				} catch (InterruptedException e) {
					// talex: would we ever get interrupted?
					e.printStackTrace();
				}
				// TODO: am I using too many atomic operations reducing performance?
				vset.pending_replies.decrementAndGet();
				Object obj_id = vset.obj_reply_q.remove();
				ValidationEntry entry = vset.object_results.get(obj_id);
				if (vset.txclock < (entry.remote_version & LockTable.UNLOCK)) {
					// Remove validation failure, invalidate object
					Logger.debug("ReadSet validation failed on object "+obj_id);
					entry.local_object.invalidate();
					// Cancel validation
					pendingValidations.remove(hashCode());
					return false;
				}
			}
			Logger.debug("ReadSet: Object validation succeeded.");
			return true;
		} finally {
			if(clear)
				for(ReadObjectAccess field: readSet)
					field.clear();
		}
	}
	
	public void validation_cancel() {
		pendingValidations.remove(hashCode());
	}
	
    /*public synchronized boolean checkClock(int clock, boolean clear) {
    	Logger.debug("READSET: Readset size:" + nextAvailable);
    	Integer hashCode = hashCode();
    	pendingValidates.put(hashCode, this);
    	try {
    		Logger.debug("Constructing Read list");
    		Map<Object, AbstractDistinguishable> set = new HashMap<Object, AbstractDistinguishable>();
    		for(ReadObjectAccess field: readSet){
    			Object obj = field.getObject();
    			if(obj!=null){
        			if(obj instanceof AbstractDistinguishable){
        				AbstractDistinguishable dist = (AbstractDistinguishable) obj;
        				if(dist.isShared()){
            				Logger.debug("Skip " + obj);
        					continue;
        				}
        				set.put(dist.getId(), dist);
        				Logger.debug("Add " + obj);
        			}
        			else
        				Logger.debug("checkClock: obj "+obj+" not AbstractDistinguishable instance, skip");
    			}
    			else
    				break;
    		}
    		Logger.debug("Readlist " + set.size());
    		
    		// urgent TODO: do this in parallel
	        for (AbstractDistinguishable obj: set.values()) {
	        	if(LockTable.checkLock( obj, clock, false, Context.LOCKS_MARKER.get())<0){
	        		AbstractDistinguishable object = (AbstractDistinguishable)obj;
	        		try {
	        			try {
							CommunicationManager.getManager().send(obj.getOwnerNode(), new ValidateRequest(obj.getId(), hashCode));
						} catch (IOException e) {
							e.printStackTrace();
							return false;
							//throw new TransactionException();
						}
						Logger.debug("READSET: Wait Remote " + obj + " Validation ...");
						wait();
						if(clock<(remoteValidateResult & LockTable.UNLOCK)){
							Logger.debug("READSET: Remote Validation failed, remote version:" + (remoteValidateResult & LockTable.UNLOCK));
							//talex: mark object as invalid
							obj.invalidate();
							return false;
							// Let's do things without exceptions when we can
							//throw new TransactionException();
						}
						Logger.debug("READSET: Remote Validation " + obj.getId() + " successeded.");
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
	        	}
	        }
	        return true;
		} finally{
			if(clear)
				for(ReadObjectAccess field: readSet)
					field.clear();
			pendingValidates.remove(hashCode);
		}
    }*/

	/**
	 * Merges this readset into the readset of the parent transaction (given).
	 */
	public void mergeInto(ReadSet other) {
		// Copy all references to the other ReadSet
		for (int i = 0; i < nextAvailable; i++) {
			if (readSet[i].getObject() == null)
				continue;
			ReadObjectAccess next = other.getNext();
			next.init(readSet[i].getObject());
		}
		// Clear current ReadSet
		clear();
	}
    
    public interface ReadSetListener{
    	void execute( ReadObjectAccess read);
    }
}

class ValidateRequest extends Message{

	private Object key;
	private Integer readsetHashcode;
	private int senderClock;
	ValidateRequest(Object key, Integer readsetHashcode){
		this.key = key;
		this.readsetHashcode = readsetHashcode;
		this.senderClock = LocalClock.get();
	}
	
	@Override
	public void run() {
		try {
			int localClock = LocalClock.get();
			if(senderClock>localClock)
				LocalClock.advance(senderClock);
			AbstractDistinguishable object = ((TrackerDirectory)DirectoryManager.getManager()).getLocalObject(key);
			int currentLockVersion = object == null ? Integer.MAX_VALUE : LockTable.getLockVersion(object);
			CommunicationManager.getManager().send(from, new ValidateResponse(key, currentLockVersion, readsetHashcode));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class ValidateResponse extends Message{

	private int lockVersion;
	private int senderClock;
	private int readsetHashcode;
	private Object key;
	ValidateResponse(Object key, int lockVersion, int readsetHashcode){
		this.readsetHashcode = readsetHashcode;
		this.lockVersion = lockVersion;
		this.key = key;
		this.senderClock = LocalClock.get();
	}
	
	@Override
	public void run() {
		if(senderClock>LocalClock.get())
			LocalClock.advance(senderClock);
		ReadSet.ValidationSet vset = ReadSet.pendingValidations.get(readsetHashcode);
		if (vset == null) {
			Logger.debug("Received response for cancelled validation; obj="+key);
			return;
		}
		// Setup return values
		ReadSet.ValidationEntry entry = vset.object_results.get(key);
		if (entry == null ) {
			Logger.debug("ValidateResponse ERROR: entry==null for key="+ key);
		} else {
			entry.remote_version = lockVersion;
			vset.obj_reply_q.add(key);
			// Signal thread to wake up
			vset.status_sem.release();
		}
	}
}
