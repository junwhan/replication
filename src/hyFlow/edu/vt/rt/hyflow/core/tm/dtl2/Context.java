package edu.vt.rt.hyflow.core.tm.dtl2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.TransactionException;
import org.deuce.transform.Exclude;

import aleph.GlobalObject;
import aleph.PE;
import aleph.dir.DirectoryManager;
import aleph.dir.ObjectsRegistery;
import aleph.dir.RegisterObject;
import aleph.dir.UnregisterObject;
import aleph.dir.cm.arrow.ArrowDirectory;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.dir.dtl2.DTL2Directory;
import edu.vt.rt.hyflow.core.tm.ContextFactory;
import edu.vt.rt.hyflow.core.tm.NestedContext;
import edu.vt.rt.hyflow.core.tm.NestingModel;
import edu.vt.rt.hyflow.core.tm.dtl2.field.ReadObjectAccess;
import edu.vt.rt.hyflow.core.tm.dtl2.field.WriteObjectAccess;
import edu.vt.rt.hyflow.helper.Atomic;
import edu.vt.rt.hyflow.util.io.Logger;

/**
 * DTL2 implementation
 * 
 * @author Mohamed M. Saad
 * @since 1.0
 */
@Exclude
final public class Context extends NestedContext {

	final private ReadSet readSet = new ReadSet();
	final private WriteSet writeSet = new WriteSet();
	final private List<AbstractDistinguishable> lazyPublish = new LinkedList<AbstractDistinguishable>(); 
	final private List<AbstractDistinguishable> lazyDelete = new LinkedList<AbstractDistinguishable>();
	final private Set<AbstractDistinguishable> openedObjects = new HashSet<AbstractDistinguishable>();

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
	
	// talex: open nesting fields
	private Atomic currentOpenNestingAction = null;
	final private List<Atomic> openNestingActions = new ArrayList<Atomic>();

	public Context() {
		this.localClock = LocalClock.get();
	}

	public void init(int atomicBlockId) {
		Logger.debug("Init");
		super.init(atomicBlockId);
		LOCKS_MARKER.get().clear();
		this.readSet.clear();
		this.writeSet.clear();
		this.lazyPublish.clear();
		this.lazyDelete.clear();
		this.openedObjects.clear();
		this.localClock = LocalClock.get();
		// Open nesting
		openNestingActions.clear();
		
		Logger.debug("Init done");
	}

	@Override
	public void newObject(AbstractDistinguishable object) {
		lazyPublish.add(object);
	}
	
	@Override
	public void delete(AbstractDistinguishable deleted) {
		lazyDelete.add(deleted);
		writeSet.get(deleted); // add to write set
	}

	public void forward(int senderClock) {
		forwardings++;
		Logger.debug("Forwarding time from <" + localClock + "> to <"
				+ senderClock + ">");
		try {
			validate(localClock);
			Logger.debug("Validating readset against <" + localClock + ">");
			localClock = senderClock;
			Logger.debug("Forwarding successded.");
		} catch (TransactionException e) {
			Logger.debug("Early Validation fail !!");
			status = STATUS.ABORTED;
		}
	}

	protected boolean checkCommit(boolean top) {
		Logger.debug("Try Commit");
		
		final boolean action = top || nestingModel == NestingModel.OPEN || nestingModel == NestingModel.INTERNAL_OPEN;

		/*if (writeSet.isEmpty()) { // if the writeSet is empty no need to lock a
									// thing.
			Logger.debug("Commit: easy way out (empty write set)");
			return true;
		}*/

		// Sanity check on write-set for "stolen" objects
		if (!this.checkContextTree()) {
			// throw new TransactionException();
			aborts++;
			return false;
		}
		
		if (action)
		{

			Logger.debug("Try Lock Write-Set");
			int lockedCounter = 0;// used to count how many fields where locked if
									// unlock is needed
			sortedWriteSet = this.writeSet.sortedItems();
			try {
				for (AbstractDistinguishable obj : sortedWriteSet) {
					Logger.debug("Validating: "
							+ ObjectsRegistery.getKey(obj.getId()));
	
					if (!LockTable.lock(obj, LOCKS_MARKER.get())) {
						GlobalObject globalObject = ObjectsRegistery
								.getKey(((AbstractDistinguishable) obj).getId());
						if (globalObject == null) // object was deleted
							throw new TransactionException();
						LockTable.remoteLockRequest(this, globalObject);
						if (!pendingLock) {
							Logger.debug("Remote lock refused");
							throw new TransactionException();
						}
						Logger.debug("Remote lock granted");
					}
					++lockedCounter;
				}
				Logger.debug("Validate Read-Set");
				
				readSet.checkClock(localClock, true);
			} catch (TransactionException exception) {
				Logger.debug("Invalid Read-Set");
				for (AbstractDistinguishable obj : sortedWriteSet) {
					if (lockedCounter-- == 0)
						break;
					Logger.debug("Releasing " + obj);
					if (!LockTable.unLock(obj, LOCKS_MARKER.get()))
						LockTable.remoteUnlockRequest(ObjectsRegistery
								.getKey(((AbstractDistinguishable) obj).getId()));
				}
				return false;
			}
			newClock = LocalClock.increment();
			return true;
		}
		else
		{
			try {
				validate(localClock);
				newClock = LocalClock.get();
				return true;
			}  catch (TransactionException exception) {
				return false;
			}
		}
	}

	protected void reallyCommit() {		
		Logger.debug("Commit values to objects");
		for (WriteObjectAccess writeField : this.writeSet)
			writeField.put(); // commit value to field

		Logger.debug("Publish newly created objects");
		for (AbstractDistinguishable object : lazyPublish)
			new GlobalObject(object, object.getId()); // populate me as this
														// object owner

		DirectoryManager locator = HyFlow.getLocator();
		Logger.debug("Unregister deleted objects");
		for (AbstractDistinguishable object : lazyDelete) {
			GlobalObject key = ObjectsRegistery.getKey(object.getId());
			locator.unregister(key); // unregister this object
			PE.thisPE().populate(new UnregisterObject(key)); // unregister this
																// object from
																// other nodes
		}

		releaseSets();
		
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
		}
	}

	private void releaseSets() {
		if (locator == null) {
			locator = HyFlow.getLocator();
		}
		Logger.debug("Release Write-Set");
		if (sortedWriteSet == null) {
			Logger.fetal("NULL OBJECT, HOW!?!?");
		}
		for (AbstractDistinguishable obj : sortedWriteSet) {
			if (LockTable.setAndReleaseLock(obj, newClock, LOCKS_MARKER.get())) { // is
																					// it
																					// remote
				AbstractDistinguishable object = (AbstractDistinguishable) obj;
				GlobalObject key = ObjectsRegistery.getKey(object.getId());
				if (key == null) {
					System.out.println("TTTTT:" + object.getId());
					continue;
				}
				Logger.debug("I'm new owner of " + key);
				key.setHome(PE.thisPE()); // me is the new owner
				ObjectsRegistery.regsiterObject(key); // register key local
				locator.newObject(key, object); // register at the directory
												// manager
				PE.thisPE().populate(new RegisterObject(key)); // populate me as
																// the new owner
				Logger.debug("Populate me owner of " + key);
				// FIXME: if more than one local transaction at this node then
				// we should release the object now not before changing its
				// location
			}
		}

		Logger.debug("Commited ===================================");
		complete();
	}
	
	private void validate(int localClock) 
	{
		try {
			for (Context c=this; c!=null; c=(Context)c.parent) {
				c.readSet.checkClock(localClock, false);
				// Do not pass a commit boundary
				if (c.nestingModel == NestingModel.OPEN || c.nestingModel == NestingModel.INTERNAL_OPEN)
					break;
			}
		} catch (TransactionException e) {
			Logger.debug("Validation fail in nested context, must abort up to commit boundary.");
			abortContextTree();
			throw e;
		}
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
		parent2.lazyPublish.addAll(lazyPublish);
		// Merge lazily deleted objects
		parent2.lazyDelete.addAll(lazyDelete);
		// Merge open nested actions
		parent2.openNestingActions.addAll(openNestingActions);

		//talex 09/29/2011: merge commits do not lock write-set!
		//releaseSets();
	}

	/**
	 * Check transaction and its ancestors for invalid objects in write-set.
	 */
	protected boolean checkContextTree() {
		// talex 29/09/2011: DTL/Tracker does not steal objects!
		return true;
		/*
		// TODO: when parent aborts, abort all children too
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

	public boolean rollback() {
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
		Logger.debug("Rollback !!!");
		if (nestingModel == NestingModel.INTERNAL_OPEN)
			Logger.debug(">>Internal Open Rollback!<<");
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
		if (obj instanceof AbstractDistinguishable) {
			Logger.debug("Try access " + ObjectsRegistery.getKey(((AbstractDistinguishable) obj).getId()));
		}

		ReadObjectAccess next = readSet.getNext();
		next.init(obj);

		if (obj instanceof AbstractDistinguishable) {
			if (!((AbstractDistinguishable) obj).isValid()) {
				this.checkContextTree();
			}
		}

		LockTable.checkLock(obj, localClock, true, LOCKS_MARKER.get());
	}
	
	@Override
	public void setCurrentOpenNestingAction(Atomic action) {
		currentOpenNestingAction = action;
	}

	public Object onReadAccess(Object obj, Object value, long field) {
		Logger.debug("On read access " + obj);
		WriteObjectAccess writeAccess = onReadAccess0(obj, field);
		if (writeAccess == null) {
			Logger.debug("On read access 2 " + writeAccess);
			return value;
		}
		Logger.debug("read new value " + obj);
		Object val = writeAccess.getValue(field);
		return val == null ? value : val;
	}

	public boolean onReadAccess(Object obj, boolean value, long field) {
		WriteObjectAccess writeAccess = onReadAccess0(obj, field);
		if (writeAccess == null)
			return value;

		Boolean val = (Boolean) writeAccess.getValue(field);
		return val == null ? value : val;
	}

	public byte onReadAccess(Object obj, byte value, long field) {
		WriteObjectAccess writeAccess = onReadAccess0(obj, field);
		if (writeAccess == null)
			return value;

		Byte val = (Byte) writeAccess.getValue(field);
		return val == null ? value : val;
	}

	public char onReadAccess(Object obj, char value, long field) {
		WriteObjectAccess writeAccess = onReadAccess0(obj, field);
		if (writeAccess == null)
			return value;

		Character val = (Character) writeAccess.getValue(field);
		return val == null ? value : val;
	}

	public short onReadAccess(Object obj, short value, long field) {
		WriteObjectAccess writeAccess = onReadAccess0(obj, field);
		if (writeAccess == null)
			return value;

		Short val = (Short) writeAccess.getValue(field);
		return val == null ? value : val;
	}

	public int onReadAccess(Object obj, int value, long field) {
		WriteObjectAccess writeAccess = onReadAccess0(obj, field);
		if (writeAccess == null)
			return value;

		Integer val = (Integer) writeAccess.getValue(field);
		return val == null ? value : val;
	}

	public long onReadAccess(Object obj, long value, long field) {
		WriteObjectAccess writeAccess = onReadAccess0(obj, field);
		if (writeAccess == null)
			return value;

		Long val = (Long) writeAccess.getValue(field);
		return val == null ? value : val;
	}

	public float onReadAccess(Object obj, float value, long field) {
		WriteObjectAccess writeAccess = onReadAccess0(obj, field);
		if (writeAccess == null)
			return value;

		Float val = (Float) writeAccess.getValue(field);
		return val == null ? value : val;
	}

	public double onReadAccess(Object obj, double value, long field) {
		WriteObjectAccess writeAccess = onReadAccess0(obj, field);
		if (writeAccess == null)
			return value;

		Double val = (Double) writeAccess.getValue(field);
		return val == null ? value : val;
	}

	public void onWriteAccess(Object obj, Object value, long field) {
		if (status.equals(STATUS.ABORTED))
			throw new TransactionException();

		WriteObjectAccess fieldAccess = writeSet.get(obj);
		fieldAccess.set(field, value);
	}

	public void onWriteAccess(Object obj, boolean value, long field) {
		this.onWriteAccess(obj, (Object) value, field);
	}

	public void onWriteAccess(Object obj, byte value, long field) {
		this.onWriteAccess(obj, (Object) value, field);
	}

	public void onWriteAccess(Object obj, char value, long field) {
		this.onWriteAccess(obj, (Object) value, field);
	}

	public void onWriteAccess(Object obj, short value, long field) {
		this.onWriteAccess(obj, (Object) value, field);
	}

	public void onWriteAccess(Object obj, int value, long field) {
		this.onWriteAccess(obj, (Object) value, field);
	}

	public void onWriteAccess(Object obj, long value, long field) {
		this.onWriteAccess(obj, (Object) value, field);
	}

	public void onWriteAccess(Object obj, float value, long field) {
		this.onWriteAccess(obj, (Object) value, field);
	}

	public void onWriteAccess(Object obj, double value, long field) {
		this.onWriteAccess(obj, (Object) value, field);
	}

	@Override
	public void onOpenObject(AbstractDistinguishable object) {
		openedObjects.add(object);
	}
}
