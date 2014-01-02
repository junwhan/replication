package edu.vt.rt.hyflow.core.tm.dtl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.TransactionException;
import org.deuce.transform.Exclude;

import aleph.comm.Address;
import aleph.dir.DirectoryManager;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.dir.dtl2.TrackerDirectory;
import edu.vt.rt.hyflow.core.tm.ContextFactory;
import edu.vt.rt.hyflow.core.tm.NestedContext;
import edu.vt.rt.hyflow.core.tm.NestingModel;
import edu.vt.rt.hyflow.core.tm.dtl2.field.ReadObjectAccess;
import edu.vt.rt.hyflow.core.tm.dtl2.field.WriteObjectAccess;
import edu.vt.rt.hyflow.helper.Atomic;
import edu.vt.rt.hyflow.helper.StatsAggregator;
import edu.vt.rt.hyflow.helper.TrackerLockMap;
import edu.vt.rt.hyflow.helper.TrackerLockMap.LockRecord;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;

/**
 * DTL2 implementation
 *
 * @author Mohamed M. Saad
 * @since	1.0
 */
@Exclude
final public class Context extends NestedContext {
	
	
	final private ReadSet readSet = new ReadSet();
	final private WriteSet writeSet = new WriteSet();
	final private Map<Object,AbstractDistinguishable> lazyPublish = new HashMap<Object,AbstractDistinguishable>(); 
	final private List<AbstractDistinguishable> lazyDelete = new LinkedList<AbstractDistinguishable>();
	final private Map<Object,AbstractDistinguishable> openedObjects = new HashMap<Object,AbstractDistinguishable>();
	final private Set<TrackerLockMap.LockRecord> lockSet = new HashSet<TrackerLockMap.LockRecord>();

	public static int forwardings;

	// Used by the thread to mark locks it holds.
	// talex: need to explicitly make this variable into thread-local
	// final private HashSet<Object> locksMarker = new HashSet<Object>();
	final static public ContextThreadLocal LOCKS_MARKER = new ContextThreadLocal();

	@Exclude
	public static class ContextThreadLocal extends ThreadLocal<HashSet<Object>> {
		@Override
		protected synchronized HashSet<Object> initialValue() {
			try {
				return new HashSet<Object>();
			} catch (Exception e) {
				throw new TransactionException(e);
			}
		}
	}

	// Marked on beforeRead, used for the double lock check
	private int localClock;
	// private int lastReadLock;
	private int newClock;

	// talex: local vars turned fields
	public boolean pendingLock;
	private DirectoryManager locator;
	private AbstractDistinguishable[] sortedWriteSet;
	private LockRecord[] sortedAbstrLocks;
	
	// talex: open nesting fields
	private Atomic currentOpenNestingAction = null;
	final private List<Atomic> openNestingActions = new ArrayList<Atomic>();

	public Context() {
		this.localClock = LocalClock.get();
	}
	
	public void init(int atomicBlockId){
		if (isFlatFree()) {
			// stats
			if (parent == null) {
				StatsAggregator.get().startTxn();
			} else {
				StatsAggregator.get().startSubTxn();
			}
			
			Logger.debug("Init");
			super.init(atomicBlockId);
			LOCKS_MARKER.get().clear();
			this.readSet.clear();
			this.writeSet.clear();
			this.lazyPublish.clear();
			this.lazyDelete.clear();
			this.openedObjects.clear();
			this.lockSet.clear();
			this.localClock = LocalClock.get();
			// Open nesting
			openNestingActions.clear();
			Logger.debug("Init done");
		} else {
			Logger.debug("Skipping init, context already initialized.");
		}
	}

	@Override
	public void newObject(AbstractDistinguishable object) {
		lazyPublish.put(object.getId(), object);
	}
	
	@Override
	public void delete(AbstractDistinguishable deleted) {
		lazyDelete.add(deleted);
		writeSet.get(deleted);	// add to write set
	}
	
	public void forward(int senderClock) {
		forwardings++;
		Logger.debug("Forwarding time from <" + ((Context)root).localClock + "> to <" + senderClock + ">");

		Logger.debug("Validating readset against <" + ((Context)root).localClock + ">");
		final boolean result = validate( ((Context)root).localClock);
		// talex: important change - forward top level txn even on early validation failure
		// will need latest clock for subtxns
		((Context)root).localClock = senderClock;
		if (result) {
			Logger.debug("Forwarding successded.");
		} else {
			Logger.debug("Early Validation fail !!");
			// appropriate contexts already marked as aborted
			// talex: why would we keep executing? Let's abort
			throw new TransactionException();
		}
	}

	protected boolean checkCommit(boolean top) {
		//Logger.debug("Try Commit");
		//Logger.debug("CSTART "+ System.nanoTime());
		// Sanity check on write-set for "stolen" objects
		if (!this.checkContextTree()) {
			// throw new TransactionException();
			aborts++;
			return false;
		}

		if (top) 
		{
	        Logger.debug("Try Lock Write-Set");
			int lockedCounter = 0;//used to count how many fields where locked if unlock is needed
			int abstrLockCounter = 0;
			sortedWriteSet = this.writeSet.sortedItems();
			sortedAbstrLocks = lockSet.toArray(new LockRecord[0]);
			Arrays.sort(sortedAbstrLocks, new Comparator<LockRecord>() {
				@Override
				public int compare(LockRecord o1, LockRecord o2) {
					return o1.hashCode() - o2.hashCode();
				}
			});
			try
			{
				for(AbstractDistinguishable obj : sortedWriteSet){
					Object key = obj.getId();
					Address owner = obj.getOwnerNode();
					//Address owner = Network.getInstance().getCoordinator();
					Logger.debug("Validating: " + key + " / " + obj);
					
					if(!LockTable.lock(obj, LOCKS_MARKER.get())){
						//LockTable.remoteLockRequest(this, key, owner);
						LockTable.remoteLockRequest(this, obj, owner);
						if(!pendingLock){
							Logger.debug("Remote lock refused");
							throw new TransactionException();
						}
						Logger.debug("Remote lock granted");
					}
					++lockedCounter;
				}
				// Try lock abstract locks
				for (LockRecord lockrec: sortedAbstrLocks) {
					if (lockrec.isAcquire()) {
						Logger.debug("Acquiring abstract lock "+lockrec);
						if (lockrec.commit()) {
							abstrLockCounter++;
						} else {
							// On abstract lock failure, we must abort to root to avoid livelocks
							// TODO: maybe do it randomly on 50% of transactions? (or otherway)
							abortContextTree();
							throw new TransactionException();
						}
					}
				}
				// Validate read set
		        Logger.debug("Validate Read-Set");
		        readSet.validation_start(((Context)root).localClock);
		        if (! readSet.validation_result(true)) {
					// this is top-level readset, abort on validation vailure
					throw new TransactionException("Object at top-level txn invalid. Abort.");
				}
		        
		        // If validation succeeded, release abstract locks
				for (LockRecord lockrec: sortedAbstrLocks) {
					if (!lockrec.isAcquire()) {
						Logger.debug("Releasing abstract lock "+lockrec);
						lockrec.commit();
					}
				}
			}
			catch(TransactionException exception){
				Logger.debug("Invalid Read-Set");
				for (LockRecord lockrec: sortedAbstrLocks) {
					if (lockrec.isAcquire()) {
						if (abstrLockCounter-- == 0)
							break;
						Logger.debug("Releasing previously acquired abstract lock "+lockrec);
						lockrec.undo();
					}
				}
				for(AbstractDistinguishable obj : sortedWriteSet){
					if( lockedCounter-- == 0)
						break;
					Logger.debug("Releasing " + obj.getId());
					if(!LockTable.unLock(obj, LOCKS_MARKER.get())){
						//LockTable.remoteUnlockRequest(obj, Network.getInstance().getCoordinator());
						LockTable.remoteUnlockRequest(obj, obj.getOwnerNode());
					}
				}
				return false;
			}
			newClock = LocalClock.increment();
			return true;
		}
		else 
		{
			// No validate on closed nesting commit!
			/*if (validate(((Context)root).localClock)) {
				newClock = LocalClock.get();
				return true;
			} else {
				return false;
			}*/
			return true;
		}
}

protected void reallyCommit() {		
		
		Logger.debug("Commit values to objects");
		for( WriteObjectAccess writeField : this.writeSet)
			writeField.put(); // commit value to field
		
		//Logger.debug("Publish newly created objects");
		//DirectoryManager locator = HyFlow.getLocator();
		//for(AbstractDistinguishable object : lazyPublish.values())
		//	locator.register(object); // populate me as this object owner

		//Logger.debug("Unregister deleted objects");
		//for(AbstractDistinguishable object : lazyDelete){
		//	locator.unregister(object); // unregister this object
		//}

		releaseSets(true);
		
		
		if (parent == null) {
			StatsAggregator.get().commitTxn(sortedWriteSet.length, __get_readset_len());
		} else {
			StatsAggregator.get().commitSubTxn(sortedWriteSet.length, __get_readset_len());
		}
		
		// Executing children's open nested on-commit actions, first in first out order
		for (int i = 0; i<openNestingActions.size(); i++) {
			final Atomic action = openNestingActions.get(i);
			// TODO: need to run this as an open-nested transaction as well
			// Same improvements as onAbort, see below.
			if (action.hasOnCommit()) {
				try {
					new Atomic<Object>(NestingModel.INTERNAL_OPEN) {
						@Override
						public Object atomically(AbstractDistinguishable self,
								org.deuce.transaction.Context transactionContext) {
							// TODO: fix this uglyyy haaaack!
							((NestedContext)transactionContext).ignoreAborts = true;
							//end ugly haaaaack
							action.onCommit(transactionContext);
							return null;
						}
					}.execute(null);
				} catch (Throwable e) {
					// nada
				}
			}
		}
		
		// If we are an open-nested transaction, register with parent
		if (nestingModel == NestingModel.OPEN && currentOpenNestingAction != null && parent != null) {
			// skip registering if no commit/abort actions present
			if (currentOpenNestingAction.hasOnAbort() || currentOpenNestingAction.hasOnCommit()) {
				Context parent2 = (Context)parent;
				parent2.openNestingActions.add(currentOpenNestingAction);
			}
		} else if (parent==null) {
			StatsAggregator.get().endTxn();
		}
	}

	private void releaseSets(boolean really) {
		if (locator == null) {
			locator = HyFlow.getLocator();
		}
		Logger.debug("Release Write-Set");
		if (sortedWriteSet == null) {
			Logger.fetal("NULL OBJECT, HOW!?!?");
		}
		for(AbstractDistinguishable obj: sortedWriteSet){
			// If this is a top-level or open commit, update lock version and bring obj here
			if (really) {
				if(LockTable.setAndReleaseLock( obj, newClock, LOCKS_MARKER.get())){	// is it remote
					//AbstractDistinguishable object = (AbstractDistinguishable)obj;
					Object key = obj.getId();
					//Logger.debug("I'm new owner of " + key);
					//locator.register(obj);	// register at the directory manager
					//Logger.debug("Registered as owner of " + key);
				}
			} else {
				// On merge-model commits, only unlock
				if(!LockTable.unLock(obj, LOCKS_MARKER.get()))
					//LockTable.remoteUnlockRequest(obj, Network.getInstance().getCoordinator());
					LockTable.remoteUnlockRequest(obj, obj.getOwnerNode());
			}
		}
		//Logger.debug("Commited ===================================");
		//Logger.debug("CEND "+ System.nanoTime());
		complete();
	}
	
	
	/**
	 * Smart validation, finds highest level still valid, aborts all rest.
	 * @author talex 
	 */
	private boolean validate(int localClock) 
	{
		boolean result = true;
		
		// List contexts for validation
		ArrayList<Context> ctx_arr = new ArrayList<Context>();
        for (Context c=this; c!=null; c=(Context)c.parent) {
        	ctx_arr.add(c);
        	// Do not pass a commit boundary
        	if (c.nestingModel == NestingModel.OPEN || c.nestingModel == NestingModel.INTERNAL_OPEN)
                break;
        }
        
        // start validating read-sets, root->leaf
        for (int i=ctx_arr.size()-1; i>=0; i--) {
        	Context c = ctx_arr.get(i);
        	c.readSet.validation_start(localClock);
        }
        
        // check validation results, root->leaf
        for (int i=ctx_arr.size()-1; i>=0; i--) {
        	Context c = ctx_arr.get(i);
        	if (result) {
        		// No transactions aborted yet, check for invalid objects
        		if (!c.readSet.validation_result(false)) {
        			// Invalid objects detected, abort txns starting here
        			//Logger.debug("Aborted here");
        			result = false;
        			c.status = STATUS.ABORTED;
        		}
        	} else {
        		// This level is bellow an aborted txn, abort it too
        		c.readSet.validation_cancel();
        		//Logger.debug("Aborted 2 here");
        		c.status = STATUS.ABORTED;
        	}
        }
        
        if (!result) {
        	Logger.debug("Validation fail in nested context, must abort up to invalid object.");
        }
        return result;
    }


/**
	 * Merge read/write and lazy sets into those as parent's.
	 */
	protected void mergeIntoParent() {
		// Some sanity checks
		assert (parent != null);
		assert (parent instanceof Context);

		Context parent2 = (Context) parent;

		// Merge read-sets
		readSet.mergeInto(parent2.readSet);
		// Merge write-sets
		sortedWriteSet = this.writeSet.sortedItems();
		writeSet.mergeInto(parent2.writeSet);
		// Merge lazily published objects
		parent2.lazyPublish.putAll(lazyPublish);
		// Merge lazily deleted objects
		parent2.lazyDelete.addAll(lazyDelete);
		// Merge openedObjects list
		parent2.openedObjects.putAll(openedObjects);
		// Merge open nested actions
		parent2.openNestingActions.addAll(openNestingActions);
		// Merge locks
		parent2.lockSet.addAll(lockSet);

		//talex: merge commits do not lock writesets
		//releaseSets(false);
	}

	/**
	 * Check transaction and its ancestors for invalid objects in write-set.
	 */
	protected boolean checkContextTree() {
		// talex 29/09/2011: DTL/Tracker does not steal objects!
        return true;
		// TODO: when parent aborts, abort all children too
        /*
		boolean result = true;
		for (WriteObjectAccess writeField : writeSet) {
			final AbstractDistinguishable object = (AbstractDistinguishable) writeField.getObject();
			if (!object.isValid()) {
				Logger.debug("Transaction " + this
						+ " needs to abort due to stolen object "
						+ object.getId());
				// Oops, someone stole our object.
				result = false;
				break;
			}
		}
		// TODO: talex: i think we don't need this anymore, check before commit!!!
		if (result) {
			for (AbstractDistinguishable obj : openedObjects) {
				if (!obj.isValid()) {
					result = false;
					break;
				}
			}
		}
		if (parent != null) {
			final boolean parent_result = ((Context) parent).checkContextTree();
			if (result && !parent_result) {
				Logger.debug("Transaction " + this
						+ " needs to abort due to parent aborting.");
			}
			result = result && parent_result;
		}
		if (!result) {
			// Abort transaction
			status = STATUS.ABORTED;
		}
		return result;
		*/
	}
	
	public void doOnAbort() {
		// executing children's onAbort open nested actions, last in last out order
		for (int i=openNestingActions.size()-1; i>=0; i--) {
			final Atomic action = openNestingActions.get(i);
			// TODO: execute this as an open-nested transaction
			// Lots of possibilities here: lightweight transaction, parent-less (fake top-level), etc.
			if (action.hasOnAbort()) {
				try {
					new Atomic<Object>(NestingModel.INTERNAL_OPEN) {
						@Override
						public Object atomically(AbstractDistinguishable self,
								org.deuce.transaction.Context transactionContext) {
							// TODO: fix this uglyyy haaaack!
							((NestedContext)transactionContext).ignoreAborts = true;
							//end ugly haaaaack
							action.onAbort(transactionContext);
							return null;
						}
					}.execute(null);
				} catch (Throwable e) {
					// nada
				}
			}
		}
	}
	
	public boolean rollback() {
		Logger.debug("Rollback !!!");
		if (nestingModel == NestingModel.INTERNAL_OPEN)
			Logger.debug(">>Internal Open Rollback!<<");
		if (parent == null)
			StatsAggregator.get().abortTxn();
		else
			StatsAggregator.get().abortSubTxn();
		doOnAbort();
		aborts++;
		checkParent();
		return true;
	}

	private WriteObjectAccess onReadAccess0(Object obj, long field) {
		if (status.equals(STATUS.ABORTED)) {
			Logger.debug("Context.onReadAccess0: Aborted transaction!!");
			throw new TransactionException();
		}

		// Check if it is already included in the write set (of self or
		// ancestor)
		Context ctx = this;
		WriteObjectAccess res = null;
		while (ctx != null && res == null) {
			res = ctx.writeSet.contains(readSet.getCurrent());
			ctx = (Context) ctx.parent;
		}
		return res;
	}

	public void beforeReadAccess(Object obj, long field) {
		if(obj instanceof AbstractDistinguishable)
			Logger.debug("Try access " + ((AbstractDistinguishable)obj).getId());
		ReadObjectAccess next = readSet.getNext();
		next.init(obj);

		if (obj instanceof AbstractDistinguishable) {
			if (!((AbstractDistinguishable) obj).isValid()) {
				this.checkContextTree();
			}
		}

		LockTable.checkLock(obj, ((Context)root).localClock, true, LOCKS_MARKER.get());
	}

	@Override
	public void setCurrentOpenNestingAction(Atomic action) {
		currentOpenNestingAction = action;
	}
	
	public Object onReadAccess( Object obj, Object value, long field){
		Logger.debug("On read access " + obj);
		WriteObjectAccess writeAccess = onReadAccess0(obj, field);
		if( writeAccess == null){
			Logger.debug("On read access 2 " + writeAccess);
			return value;
		}
		Logger.debug("read new value " + obj);
		Object val = writeAccess.getValue(field);
		return val==null ? value : val;
	}
		
	public boolean onReadAccess(Object obj, boolean value, long field) {
		WriteObjectAccess writeAccess = onReadAccess0(obj, field);
		if( writeAccess == null)
			return value;
		
		Boolean val = (Boolean)writeAccess.getValue(field);
		return val==null ? value : val;    
	}
	
	public byte onReadAccess(Object obj, byte value, long field) {
		WriteObjectAccess writeAccess = onReadAccess0(obj, field);
		if( writeAccess == null)
			return value;
		
		Byte val = (Byte)writeAccess.getValue(field);
		return val==null ? value : val;
	}
	
	public char onReadAccess(Object obj, char value, long field) {
		WriteObjectAccess writeAccess = onReadAccess0(obj, field);
		if( writeAccess == null)
			return value;
		
		Character val = (Character)writeAccess.getValue(field);
		return val==null ? value : val;
	}
	
	public short onReadAccess(Object obj, short value, long field) {
		WriteObjectAccess writeAccess = onReadAccess0(obj, field);
		if( writeAccess == null)
			return value;
		
		Short val = (Short)writeAccess.getValue(field);    
		return val==null ? value : val;
	}
	
	public int onReadAccess(Object obj, int value, long field) {
		WriteObjectAccess writeAccess = onReadAccess0(obj, field);
		if( writeAccess == null)
			return value;
		
		Integer val = (Integer)writeAccess.getValue(field);
		return val==null ? value : val;
	}
	
	public long onReadAccess(Object obj, long value, long field) {
		WriteObjectAccess writeAccess = onReadAccess0(obj, field);
		if( writeAccess == null)
			return value;
		
		Long val = (Long)writeAccess.getValue(field);
		return val==null ? value : val;
	}
	
	public float onReadAccess(Object obj, float value, long field) {
		WriteObjectAccess writeAccess = onReadAccess0(obj, field);
		if( writeAccess == null)
			return value;
		
		Float val = (Float)writeAccess.getValue(field);
		return val==null ? value : val;
	}
	
	public double onReadAccess(Object obj, double value, long field) {
		WriteObjectAccess writeAccess = onReadAccess0(obj, field);
		if( writeAccess == null)
			return value;
		
		Double val = (Double)writeAccess.getValue(field);
		return val==null ? value : val;
	}
	
	public void onWriteAccess( Object obj, Object value, long field){
		if(status.equals(STATUS.ABORTED))
			throw new TransactionException();

		WriteObjectAccess fieldAccess = writeSet.get(obj);
		fieldAccess.set(field, value);
	}
	
	public void onWriteAccess(Object obj, boolean value, long field) {
		this.onWriteAccess(obj, (Object)value, field);
	}
	
	public void onWriteAccess(Object obj, byte value, long field) {
		this.onWriteAccess(obj, (Object)value, field);
	}
	
	public void onWriteAccess(Object obj, char value, long field) {
		this.onWriteAccess(obj, (Object)value, field);
	}
	
	public void onWriteAccess(Object obj, short value, long field) {
		this.onWriteAccess(obj, (Object)value, field);	
	}
	
	public void onWriteAccess(Object obj, int value, long field) {
		this.onWriteAccess(obj, (Object)value, field);		
	}
	
	public void onWriteAccess(Object obj, long value, long field) {
		this.onWriteAccess(obj, (Object)value, field);		
	}

	public void onWriteAccess(Object obj, float value, long field) {
		this.onWriteAccess(obj, (Object)value, field);		
	}
	
	public void onWriteAccess(Object obj, double value, long field) {
		this.onWriteAccess(obj, (Object)value, field);		
	}

	@Override
	public void onOpenObject(AbstractDistinguishable object) {
		openedObjects.put(object.getId(), object);
	}

	public Map<Object, AbstractDistinguishable> getObjectCache() {
		return openedObjects;
	}
	
	@Override
	public void onLockAction(String objname, String lockname, boolean readlock, boolean acquire)
	{
		// Save lock, and acquire/release it on commit
		// TODO: consider a special kind of sub-transaction, the internal open nested actions
		// TODO: Check lock to avoid doomed transactions here!
		
		// TODO: temporarily assume all objects commute
		//lockSet.add(TrackerLockMap.getRecord(objname, lockname, false, acquire));
		lockSet.add(TrackerLockMap.getRecord(objname, lockname, readlock, acquire));
	}

	public AbstractDistinguishable getCachedObject(Object key) {
		Context ctx = this;
		while (ctx != null) {
			AbstractDistinguishable result = ctx.openedObjects.get(key);
			if (result!=null) {
				return result;
			}
			result = ctx.lazyPublish.get(key);
			if (result!=null) {
				return result;
			}
			ctx = (Context)ctx.parent;
		}
		return null;
	}
	
	public int __get_readset_len() {
		return readSet.getLength();
	}
	
	private void debug() {
		Logger.debug("DEBUG: sortedWriteSet = "+sortedWriteSet);
		if (sortedWriteSet!= null) {
			Logger.debug("DEBUG: len="+sortedWriteSet.length);
			for (AbstractDistinguishable obj : sortedWriteSet) {
				Logger.debug("DEBUG: contains" + obj + " / "+obj.getId());
			}
		}
	}
}
