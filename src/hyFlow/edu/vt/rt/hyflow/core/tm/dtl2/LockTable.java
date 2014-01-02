package edu.vt.rt.hyflow.core.tm.dtl2;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.TransactionException;
import org.deuce.transform.Exclude;

import aleph.GlobalObject;
import aleph.Message;
import aleph.comm.CommunicationManager;
import aleph.dir.DirectoryManager;
import aleph.dir.ObjectsRegistery;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.dir.dtl2.DTL2Directory;
import edu.vt.rt.hyflow.util.io.Logger;

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
		
		Logger.debug("Remote Successful Locked object executed");
	}
	
	static Map<Integer, AbstractContext> pendingLocks = new ConcurrentHashMap<Integer, AbstractContext>();  
	public static void remoteLockRequest(AbstractContext context, GlobalObject key){
		int hashCode = context.hashCode();
		pendingLocks.put(hashCode, context);
		synchronized (context) {
			try {
				Logger.debug("Request remote lock for " + key + " hashcode is " + hashCode + " for " + context);
				CommunicationManager.getManager().send(key.getHome(), new LockRequest(key, true, hashCode));
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
		AtomicInteger lock = getLock(lockIndex);
		AbstractDistinguishable obj = (AbstractDistinguishable) lockIndex;
		int val = lock.get();
		if( (val & REMOTE) != 0){  //is remote lock
			Logger.debug("Remotly Locked object");
			//if(!CheckCommute(obj.getId().toString())) return false;
		}

//		final int selfLockIndex = lockIndex>>>DIVIDE_8;
//		final byte selfLockByte = contextLocks[selfLockIndex];
//		final byte selfLockBit = (byte)(1 << (lockIndex & MODULE_8));
			
		if( (val & LOCK) != 0){  //is already locked?
			
			if(contextLocks.contains(lockIndex)) // check for self locking
				return true;
			
			//if(!CheckCommute(obj.getId().toString())){
			//	Logger.debug("Locally Locked object");
			//	throw FAILURE_EXCEPTION; 
			//}
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
	/*
	public static boolean CheckCommute(String id){
		HyFlow.requester req = HyFlow.commutativity.get(id);
		if(req != null) Logger.debug("Commute here :"+req.toString());
		return false;
	}
	public static Long [] UpdateTS(String id, Long [] ts){
		HyFlow.requester req = HyFlow.commutativity.get(id);
		Logger.debug("Commute "+req.toString());
		HyFlow.commutativity.remove(id);
		return ts;
		
	}
	*/
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
//		System.out.println("Check lock for " + lockIndex);
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
		
		if( checkLocalLock && (val & LOCK) != 0) {  //is already locked?
			if (contextLocks.contains(lockIndex)) {
				Logger.debug("Valid Locak object, self locked");
				return val;
			}
			Logger.debug("on read: Locally Locked object");
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

	public static void remoteUnlockRequest(GlobalObject key){
		if(key==null)
			return;
		try {
			Logger.debug("Remote Lock release for " + key + " sent to " + key.getHome());
			CommunicationManager.getManager().send(key.getHome(), new LockRequest(key, false, 0));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void reset(Object lockIndex){
		getLock(lockIndex).set(0);
	}
	
	public static boolean unLock( Object lockIndex, Set<Object> contextLocks){
		AtomicInteger lock = getLock(lockIndex);
		int val = lock.get();

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
		
		return true;
	}

	public static boolean setAndReleaseLock(Object lockIndex, int newClock, Set<Object> contextLocks){
		AtomicInteger lock = getLock(lockIndex);
		int val = lock.get();

		boolean remote = ( (val & REMOTE) != 0);  //is remote lock
		Logger.debug("set & release " + lockIndex + " from " + val + "/" + remote + " to " + newClock);
		
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

	private GlobalObject key;
	private boolean lock;
	private int senderClock;
	private int hashCode;
	LockRequest(GlobalObject key, boolean lock, int hashCode){
		this.key = key;
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
			AbstractDistinguishable object = ((DTL2Directory)DirectoryManager.getManager()).getLocalObject(key);
			if(object!=null){
				if(!lock){
					LockTable.unLock(object, null);
					Logger.debug("Remote Unlocked " + key);
					return;
				}	
				
				// Check for latest version to avoid bug
				GlobalObject globalObject = ObjectsRegistery.getKey(object.getId());
				if(globalObject == null) {
					Logger.debug("Deleted object.");
					success = false;
				} else if(globalObject.getVersion()!=key.getVersion()){
					Logger.debug("Old GlobalObject was used to lock object, retry later.");
					success = false;
				} else {
					LockTable.remoteLockResponse(object);
					Logger.debug("Remote Locked granted " + key);
					success = true;
				}	
			}
		} catch (TransactionException e) {
			Logger.debug("Remote Locked refused " + key);
		}
		try{
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
	GlobalObject key;
	LockResponse(GlobalObject key, boolean success, int hashCode){
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
