package edu.vt.rt.hyflow.helper;

import java.io.IOException;
import java.util.concurrent.locks.*;
import java.util.concurrent.*;
import java.util.*;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.ContextDelegator;
import org.deuce.transaction.TransactionException;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import aleph.Message;
import aleph.PE;
import aleph.comm.Address;

import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;

public class TrackerLockMap {
	private static TrackerLockMap lockMap = null;
	
	public class LockRecord {
		String objname;
		String lockname;
		boolean readlock;
		boolean acquire;
		
		public LockRecord(String objname, String lockname, boolean readlock, boolean acquire) {
			this.objname = objname;
			this.lockname = lockname;
			this.readlock = readlock;
			this.acquire = acquire;
		}
		
		public boolean isAcquire() {
			return acquire;
		}
		
		@Override
		public boolean equals(Object other) {
			if (other == null) {
				return false;
			}
			if (!(other instanceof LockRecord)) {
				return false;
			}
			final LockRecord rec2 = (LockRecord) other;
			return rec2.objname.equals(objname) &&
					rec2.lockname.equals(lockname) &&
					rec2.readlock == readlock &&
					rec2.acquire == acquire;
		}
		
		@Override
		public int hashCode() {
			return (objname+"|"+lockname+readlock+acquire).hashCode();
		}
		
		public boolean commit() {
			try {
				AbstractContext context = ContextDelegator.getTopInstance();
				long hash = context.hashCode()+System.currentTimeMillis();
				pendingContexts.put(hash, context);;
				synchronized(context) {
					Address remote = getTracker(objname);
					if (remote == null) {
						ConcurrentHashMap<Object, ReadWriteLock> map = TrackerLockMap.getMapForObjKey(objname);
						ReadWriteLock rwlock = TrackerLockMap.getLockByName(map, lockname);
						Lock lock = rwlock.writeLock();
						if (readlock) {
							lock = rwlock.readLock();
						}
						if (acquire) {
							final boolean result = lock.tryLock();
							Logger.debug("LockRecord.commit(): acquire("+objname+"/"+lockname+"/"+(readlock?"R":"W")+") = "+result);
							return result;
						} else {
							lock.unlock();
							Logger.debug("LockRecord.commit(): release("+objname+"/"+lockname+"/"+(readlock?"R":"W")+") ");
							return true;
						}
					} else {
						Logger.debug("LockRecord.commit(): sending remote request: "+(acquire?"acquire":"release")+"("+objname+"/"+lockname+"/"+(readlock?"R":"W")+") to "+remote );
						new AbstractLockRequest(objname, lockname, readlock, acquire, hash).send(remote);
						if (!acquire) {
							return true;
						}
						try {
							context.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						return pendingResults.get(hash);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}
		
		public boolean undo() {
			try {
				AbstractContext context = ContextDelegator.getTopInstance();
				long hash = context.hashCode()+System.currentTimeMillis();
				pendingContexts.put(hash, context);;
				synchronized(context) {
					Address remote = getTracker(objname);
					if (remote == null) {
						ConcurrentHashMap<Object, ReadWriteLock> map = TrackerLockMap.getMapForObjKey(objname);
						ReadWriteLock rwlock = TrackerLockMap.getLockByName(map, lockname);
						Lock lock = rwlock.writeLock();
						if (readlock) {
							lock = rwlock.readLock();
						}
						// negate to perform undo
						if (!acquire) {
							boolean result = lock.tryLock();
							return result;
						} else {
							lock.unlock();
							return true;
						}
					} else {
						//negate to undo
						new AbstractLockRequest(objname, lockname, readlock, !acquire, hash).send(remote);
						if (acquire) {
							return true;
						}
						try {
							context.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						return pendingResults.get(hash);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}
	}
	
	// TODO: ensure locks are acquired in order
	// TODO: implement check anyway, to avoid doomed states?
	public static LockRecord getRecord(String objname, String lockname, boolean readlock, boolean acquire) {
		return getInst().new LockRecord(objname, lockname, readlock, acquire);
	}
	
	static Map<Object,ConcurrentHashMap<Object,ReadWriteLock>> locks = 
		new ConcurrentHashMap<Object, ConcurrentHashMap<Object, ReadWriteLock>>();
	public static Map<Long, AbstractContext> pendingContexts = 
		new ConcurrentHashMap<Long, AbstractContext>();
	public static Map<Long, Boolean> pendingResults = 
		new ConcurrentHashMap<Long, Boolean>();
	
	public TrackerLockMap() {
	}
	
	public static TrackerLockMap getInst() {
		if (lockMap == null) {
			synchronized(TrackerLockMap.class) {
				if (lockMap == null) {
					lockMap = new TrackerLockMap();
				}
			}
		}
		return lockMap;
	}
	
	public static ConcurrentHashMap<Object, ReadWriteLock> getMapForObjKey(Object key) {
		ConcurrentHashMap<Object, ReadWriteLock> map = locks.get(key);
		if (map == null) {
			synchronized(locks) {
				map = locks.get(key);
				if (map == null) {
					map = new ConcurrentHashMap<Object, ReadWriteLock>();
					locks.put(key, map);
				}
			}
		}
		return map;
	}
	
	public static ReadWriteLock getLockByName(ConcurrentHashMap<Object, ReadWriteLock> map, Object name) {
		ReadWriteLock lock = map.get(name);
		if (lock == null) {
			synchronized (map) {
				lock = map.get(name);
				if (lock == null) {
					lock = getInst().new AbstractReadWriteLock();
					map.put(name, lock);
				}
			}
		}
		return lock;
	}
	
	public static ReadWriteLock get(String objName, String lockName) {
		return getLockByName(getMapForObjKey(objName), lockName);
		
	}
	
	public static Address getTracker(Object key) {
		Address res = Network.getAddress(String.valueOf(Math.abs(key.hashCode())%Network.getInstance().nodesCount()));
		if (res.equals(PE.thisPE().getAddress())) {
			return null;
		}
		return res;
	}
	
	public boolean lock_ex(AbstractDistinguishable object, Object lockname, boolean readlock, boolean acquire) {
		try {
			AbstractContext context = ContextDelegator.getTopInstance();
			long hash = context.hashCode()+System.currentTimeMillis();
			pendingContexts.put(hash, context);;
			synchronized(context) {
				Address remote = getTracker(object.getId());
				if (remote == null) {
					ConcurrentHashMap<Object, ReadWriteLock> map = TrackerLockMap.getMapForObjKey(object.getId());
					ReadWriteLock rwlock = TrackerLockMap.getLockByName(map, lockname);
					Lock lock = rwlock.writeLock();
					if (readlock) {
						lock = rwlock.readLock();
					}
					if (acquire) {
						boolean result = lock.tryLock();
						return result;
					} else {
						lock.unlock();
						return true;
					}
				} else {
					new AbstractLockRequest(object.getId(), lockname, readlock, acquire, hash).send(remote);
					if (!acquire) {
						return true;
					}
					try {
						context.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					return pendingResults.get(hash);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private class AbstractReadWriteLock implements ReadWriteLock
	{
		public class AbstractReadLock implements Lock 
		{
			private AbstractReadWriteLock parent = null;
			private int val = 0;
			
			public AbstractReadLock(AbstractReadWriteLock parent) {
				this.parent = parent;
			}
			
			@Override
			public void lock() {
				if (!tryLock())
					throw new TransactionException("Couldn't acquire abstract lock (R).");
			}

			@Override
			public void lockInterruptibly() throws InterruptedException {
				throw new NotImplementedException();
			}

			@Override
			public Condition newCondition() {
				throw new NotImplementedException();
			}

			@Override
			public boolean tryLock() {
				// This is a read lock
				synchronized(parent) {
					if (parent.wl.isLocked()) {
						return false;
					} else {
						val++;
						return true;
					}
				}
			}

			@Override
			public boolean tryLock(long time, TimeUnit unit)
					throws InterruptedException {
				throw new NotImplementedException();
			}

			@Override
			public void unlock() {
				synchronized (parent) {
					if (val <= 0) {
						Logger.debug("AbstractReadWriteLock ERROR: ai <= 0");
					} else {
						val--;
					}
				}
			}
			
			public boolean isLocked() {
				synchronized (parent) {
					return val > 0;
				}
			}
		}
		
		public class AbstractWriteLock implements Lock 
		{
			private AbstractReadWriteLock parent = null;
			boolean val = false;
			
			public AbstractWriteLock(AbstractReadWriteLock parent) {
				this.parent = parent;
			}

			@Override
			public void lock() {
				if (!tryLock())
					throw new TransactionException("Unable to acquire abstract lock (W)");
			}

			@Override
			public void lockInterruptibly() throws InterruptedException {
				throw new NotImplementedException();
			}

			@Override
			public Condition newCondition() {
				throw new NotImplementedException();
			}

			@Override
			public boolean tryLock() {
				synchronized(parent) {
					if (parent.rl.isLocked()) {
						return false;
					} else if (val == true) {
						return false;
					} else {
						val = true;
						return true;
					}
				}
			}

			@Override
			public boolean tryLock(long time, TimeUnit unit)
					throws InterruptedException {
				throw new NotImplementedException();
			}

			@Override
			public void unlock() {
				synchronized(parent) {
					val = false;
				}
			}
			
			public boolean isLocked() {
				synchronized (parent) {
					return val;
				}
			}
		}
		
		AbstractReadLock rl = new AbstractReadLock(this);
		AbstractWriteLock wl = new AbstractWriteLock(this);
		
		@Override
		public Lock readLock() {
			return rl;
		}
		
		@Override
		public Lock writeLock() {
			return wl;
		}
	}
}

class AbstractLockRequest extends Message {
	Object key;
	Object lockname;
	Boolean readlock;
	Boolean acquire;
	Long hash;
	
	public AbstractLockRequest(Object key, Object lockname, boolean readlock, boolean acquire, long hash) {
		this.key = key;
		this.lockname = lockname;
		this.readlock = readlock;
		this.acquire = acquire;
		this.hash = hash;
	}

	@Override
	public void run() {
		Network.linkDelay(false, from);
		if (TrackerLockMap.getTracker(key) != null) {
			Logger.debug("AbstractLockRequest ERROR: received req for object that I don't own");
			reply(false);
			return;
		}
		ConcurrentHashMap<Object, ReadWriteLock> map = TrackerLockMap.getMapForObjKey(key);
		ReadWriteLock rwlock = TrackerLockMap.getLockByName(map, lockname);
		Lock lock = rwlock.writeLock();
		if (readlock) {
			lock = rwlock.readLock();
		}
		if (acquire) {
			final boolean result = lock.tryLock();
			Logger.debug("AbstractLockRequest: acquire("+key+"/"+lockname+"/"+(readlock?"R":"W")+") = "+result );
			reply(result);
		} else {
			lock.unlock();
			Logger.debug("AbstractLockRequest: release("+key+"/"+lockname+"/"+(readlock?"R":"W")+")" );
		}
		
	}
	
	private void reply(boolean granted) {
		try {
			new AbstractLockResponse(hash, granted).send(from);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class AbstractLockResponse extends Message {
	Long hash;
	Boolean result;
	
	public AbstractLockResponse(long hash, boolean result) {
		this.hash = hash;
		this.result = result;
	}

	@Override
	public void run() {
		TrackerLockMap.pendingResults.put(hash, result);
		Logger.debug("AbstractLockResponse: result="+result);
		AbstractContext context = TrackerLockMap.pendingContexts.get(hash);
		if(context != null) {
			synchronized(context){
				Logger.debug("signal.");
				context.notifyAll();	
			}
		}
	}
}
