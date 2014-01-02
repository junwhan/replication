package edu.vt.rt.hyflow.core.tm.dtl;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.deuce.transaction.ContextDelegator;
import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.TransactionException;
import org.deuce.transform.Exclude;
import edu.vt.rt.hyflow.HyFlow;
import aleph.Message;
import aleph.comm.Address;
import aleph.comm.CommunicationManager;
import aleph.dir.DirectoryManager;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.dir.dtl2.TrackerDirectory;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;

import java.util.LinkedList;
@Exclude
public class LockTable {

	// Failure transaction 
	final private static org.deuce.transaction.TransactionException FAILURE_EXCEPTION = new TransactionException( "Faild on lock.");
//	final private static int LOCKS_SIZE = Integer.MAX_VALUE; // amount of locks - TODO add system property
//	final private static int MASK = 0xFFFFFFFF;
	final private static int REMOTE = 1 << 31;
//	final private static int LOCAL = ~REMOTE;
	final private static int LOCK = 1 << 30;
	final static int UNLOCK = ~LOCK;
//	
//	final static private int MODULE_8 = 7; //Used for %8
//	final static private int DIVIDE_8 = 3; //Used for /8

//	final private static AtomicIntegerArray locks =  new AtomicIntegerArray(LOCKS_SIZE); // array of 2^20 entries of 32-bit lock words
	final private static Map<Object, AtomicInteger> locks =  new ConcurrentHashMap<Object, AtomicInteger>(); // array of 2^20 entries of 32-bit lock words

	private static AtomicInteger getLock(Object lockIndex){
		AtomicInteger lockI = locks.get(lockIndex);
		if(lockI==null)
			synchronized (locks) {
				lockI = locks.get(lockIndex);
				if(lockI==null){
					lockI = new AtomicInteger(0);
					locks.put(lockIndex, lockI);
				}
			}
		
		return lockI;
	}
	
	public static void remoteLockResponse(Object lockIndex) throws TransactionException{
		AtomicInteger lock = getLock(lockIndex);
		int val = lock.get();
		if( (val & LOCK) != 0){  //is already locked?
			Logger.debug("Remote Lock Failed: Locked object");
			throw FAILURE_EXCEPTION; 
		}
		
		boolean isLocked = lock.compareAndSet(val, val | LOCK);
		
		if( !isLocked){
			Logger.debug("Remote Lock Failed: Concurrent Locked object");
			throw FAILURE_EXCEPTION;
		}		
		
		//Logger.debug("Remote Successful Locked object executed");
	}
	
	static Map<Integer, AbstractContext> pendingLocks = new ConcurrentHashMap<Integer, AbstractContext>();  
	public static void remoteLockRequest(AbstractContext context, AbstractDistinguishable obj, Address owner){
		int hashCode = context.hashCode();
		pendingLocks.put(hashCode, context);
		owner = Network.getInstance().getCoordinator();
		synchronized (context) {
			try {
				Logger.debug("Request remote lock for " + obj.getId() + " hashcode is " + hashCode + " for " + context);
				CommunicationManager.getManager().send(owner, new LockRequest(obj, true, hashCode));
				context.wait();
				Logger.debug("Request remote lock come back...");
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		pendingLocks.remove(hashCode);
	}
	
	public static boolean lock(Object lockIndex, Set<Object> contextLocks) throws TransactionException{
		//final AtomicInteger clock = new AtomicInteger(1);
		//Logger.debug("Trying to lock object "+lockIndex);
		AtomicInteger lock = getLock(lockIndex);
		//AbstractDistinguishable obj = (AbstractDistinguishable) lockIndex;
		//Logger.debug("Check commute here "+val+" "+REMOTE);
		int val = lock.get();
		if( (val & REMOTE) != 0){  //is remote lock
			Logger.debug("Remotly Locked object");
			//if(!Checkcommute(obj.getId().toString())) 
			return false;
		}
			
		if( (val & LOCK) != 0){  //is already locked?
			
			if(contextLocks.contains(lockIndex)) // check for self locking
				return false;
			Logger.debug("Locally Locked object");
			throw FAILURE_EXCEPTION; 
		}
		
		boolean isLocked = lock.compareAndSet(val, val | LOCK);
		
		if( !isLocked){
			Logger.debug("Being Locked object");
			throw FAILURE_EXCEPTION;
		}		
		
		contextLocks.add(lockIndex); //mark in self locks
		Logger.debug("Successful Locked object");
		return true;
	}
	
	public static boolean isAvailable(Object lockIndex, Set<Object> contextLocks) {
		AtomicInteger lock = getLock(lockIndex);
		int val = lock.get();
		boolean remote = (val & REMOTE)!=0;
		boolean locked = (val & LOCK)!=0;
		boolean selflock = contextLocks.contains(lockIndex);
		Logger.debug("Available " + lockIndex + " " + val + " <" + remote + "," + locked + "," + selflock +">");
		return remote || (locked && (!selflock));
	}
	
	public static int checkLock(Object lockIndex, int clock, boolean checkLocalLock, Set<Object> contextLocks) {
		Logger.debug("Check lock for " + lockIndex);
		AtomicInteger lock = getLock(lockIndex);
		int val = lock.get();
		
		if((val & REMOTE)!=0){
			Logger.debug("Remotly Locked object");
			return -1;
		}

		if( clock < (val & UNLOCK)){ // check the clock without lock, TODO check if this is the best way
			Logger.debug("Outdated version: " + clock + " current is "+ (val & UNLOCK));
			throw FAILURE_EXCEPTION;
		}
		
		if( checkLocalLock && (val & LOCK) != 0){  //is already locked?
			if (contextLocks.contains(lockIndex)) {
				Logger.debug("Valid Local object, self locked");
				return val;
			}
			if (lockIndex instanceof AbstractDistinguishable) {
				Logger.debug("on read: Locally Locked object | "+((AbstractDistinguishable)lockIndex).getId());
			} else {
				Logger.debug("on read: Locally Locked object");
			}
			throw FAILURE_EXCEPTION; 
		}
		
		Logger.debug("Valid Local object");
		return val;
	}

//	private static void checkLock(int lockIndex, int clock, int expected) {
//		int lock = checkLock( lockIndex, clock);
//		if( lock != expected || (lock & LOCK) != 0)
//			throw FAILURE_EXCEPTION;
//	}

	public static void remoteUnlockRequest(AbstractDistinguishable obj, Address owner){
		if(obj==null)
			return;
		try {
			Logger.debug("Remote Lock release for " + obj.getId() + " sent to " + owner);
			
			CommunicationManager.getManager().send(owner, new LockRequest(obj, false, 0));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void reset( Object lockIndex){
		getLock(lockIndex).set(0);
	}
	
	public static boolean unLock( Object lockIndex, Set<Object> contextLocks){
		AtomicInteger lock = getLock(lockIndex);
		int val = lock.get();
		//AbstractDistinguishable obj = (AbstractDistinguishable) lockIndex;
		//Long [] mytime = obj.getTS();
		
		if( (val & REMOTE) != 0){  //is remote lock
			Logger.debug("Can't unlock remote object");
			return false;
		}
		
		int unlockedValue = val & UNLOCK;
		if(!lock.compareAndSet(val, unlockedValue)){
			Logger.debug("Fail due to concurrent unlock!!");
			return false;
		}
		
		if(contextLocks!=null)
			clearSelfLock(lockIndex, contextLocks);
		
		Logger.debug("Unlock local object successfully.");
		//obj.setTS(UpdateTS(obj.getId().toString(), mytime));
		return true;
	}
	
	public static boolean setAndReleaseLock(Object lockIndex, int newClock, Set<Object> contextLocks){
		AtomicInteger lock = getLock(lockIndex);
		int val = lock.get();

		boolean remote = ( (val & REMOTE) != 0);  //is remote lock
		Logger.debug("set & release " + lockIndex + " from " + val + "/" + remote + " to " + newClock);
		if (lockIndex instanceof AbstractDistinguishable) {
			AbstractDistinguishable obj = (AbstractDistinguishable) lockIndex;
			Logger.debug("Released local object "+obj.getId());
		}
		
		lock.set(newClock);		// now it will be local
		clearSelfLock( lockIndex, contextLocks);
		
		return remote;
	}
	
//	private static void setLockVersion( int lockIndex, int newClock){
//		Logger.debug("changing version of " + lockIndex + " to " + newClock + "[" + (newClock | REMOTE) + "]");
//		getLock(lockIndex).set(newClock | REMOTE);
//	}

	public static void setRemote(Object lockIndex){
		Logger.debug("changing version of " + lockIndex + " to remote.");
		getLock(lockIndex).set(REMOTE);
	}

//	private static void setRemote(int hash, boolean remote){
//		int lockIndex = hash & MASK;
//		int version = locks.get(lockIndex);
//		locks.set(lockIndex, (remote ? version | REMOTE : version & LOCAL));
//	}
	
	private static void dump(){
//		for(int i=0; i<locks.length(); i++) {
//			int entry = locks.get(i);
//			if(entry!=0){
//				StringBuffer buffer = new StringBuffer(i + ":");
//				int mask = 1 << 31;
//				for(int b=1; b<=32; b++){
//					buffer.append((entry & mask)==0 ? "0" : "1");
//					entry = entry << 1;
//				}
//				System.out.println(buffer);
//			}
//		}
	}
	
	public static int getLockVersion(Object lockIndex){
		return getLock(lockIndex).get();
	}
	
	/**
	* Clears lock marker from self locking array
	*/
	private static void clearSelfLock(Object lockIndex, Set<Object> contextLocks){
		// clear marker TODO might clear all bits
		contextLocks.remove(lockIndex);
	}
}


class LockRequest extends Message{

	private Object key;
	private AbstractDistinguishable obj;
	private boolean lock;
	private int senderClock;
	private int hashCode;
	LockRequest(AbstractDistinguishable obj, boolean lock, int hashCode){
		this.key = obj.getId();
		this.obj = obj;
		this.lock = lock;
		this.senderClock = LocalClock.get();
		this.hashCode = hashCode;
	}
	
	@Override
	public void run() {
		Logger.debug((lock? "Lock" : "Unlock" ) + " request received from " + from + " for " + key);
		boolean success = false;
		try {
			int localClock = LocalClock.get();
			if(senderClock>localClock)
				LocalClock.advance(senderClock);
			AbstractDistinguishable object = ((TrackerDirectory)DirectoryManager.getManager()).getLocalObject(key);
			if(obj != null) 
				DirectoryManager.getManager().register(obj); 
			
			if(object!=null){
				if(!lock){
					LockTable.unLock(object, null);
					//DirectoryManager.getManager().register(obj); 
					//Logger.debug("Remote Unlocked " + key +" "+from);
					return;
				}
				
				// TODO: will eliminating the "old key" check be detrimental?!?
				LockTable.remoteLockResponse(object);
				//Logger.debug("Remote Locked granted " + key);
				success = true;
			}
			
				
		} catch (TransactionException e) {
			Logger.debug("Remote Locked refused " + key);
		}
		try{
			//if(success == true) UpdateTS(key, hashCode);
			//else if(Checkcommute(key)) success = true;
			CommunicationManager.getManager().send(from, new LockResponse(key, success, hashCode));
		} catch (IOException e) {
			e.printStackTrace();
		}	

	}
}

class LockResponse extends Message{

	private boolean success;
	private int senderClock;
	private int hashCode;
	Object key;
	LockResponse(Object key, boolean success, int hashCode){
		this.key = key;
		this.hashCode = hashCode;
		this.success = success;
		this.senderClock = LocalClock.get();
	}
	
	@Override
	public void run() {
		Logger.debug(key + " Retured Lock " + (success ? "granted" : "refused"));
		if(senderClock>LocalClock.get())
			LocalClock.advance(senderClock);
		if(hashCode==0){
			Logger.debug("No call backs");
			return;
		}
		Logger.debug("Hashcode is " + hashCode);
		AbstractContext context = LockTable.pendingLocks.get(hashCode);
		if(context==null){
			System.out.println("Null context: " + hashCode);
			return;
		}
		Logger.debug(context + " will notified");
		synchronized (context) {
			((Context)context).pendingLock = success;
			context.notifyAll();
			Logger.debug(context + " notified");
		}
	}
}
