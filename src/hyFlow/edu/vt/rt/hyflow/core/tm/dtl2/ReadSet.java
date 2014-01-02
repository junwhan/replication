package edu.vt.rt.hyflow.core.tm.dtl2;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.deuce.transaction.TransactionException;
import org.deuce.transform.Exclude;

import aleph.GlobalObject;
import aleph.Message;
import aleph.comm.CommunicationManager;
import aleph.dir.DirectoryManager;
import aleph.dir.ObjectsRegistery;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.dir.dtl2.DTL2Directory;
import edu.vt.rt.hyflow.core.tm.dtl2.field.ReadObjectAccess;
import edu.vt.rt.hyflow.core.tm.dtl2.field.WriteObjectAccess;
import edu.vt.rt.hyflow.util.io.Logger;

/**
 * Represents the transaction read set. And acts as a recycle pool of the
 * {@link ReadObjectAccess}.
 * 
 * @author Guy Korland
 * @author Mohamed M. Saad
 * @since 0.7
 */
@Exclude
public class ReadSet {

	private static final int DEFAULT_CAPACITY = 1024;
	private ReadObjectAccess[] readSet = new ReadObjectAccess[DEFAULT_CAPACITY];
	private int nextAvailable = 0;
	private ReadObjectAccess currentReadFieldAccess = null;

	int remoteValidateResult;

	public static Map<Integer, ReadSet> pendingValidates = new ConcurrentHashMap<Integer, ReadSet>();

	public ReadSet() {
		fillArray(0);
	}

	public void clear() {
		nextAvailable = 0;
	}

	private void fillArray(int offset) {
		for (int i = offset; i < readSet.length; ++i) {
			readSet[i] = new ReadObjectAccess();
		}
	}

	public ReadObjectAccess getNext() {
		if (nextAvailable >= readSet.length) {
			int orignLength = readSet.length;
			ReadObjectAccess[] tmpReadSet = new ReadObjectAccess[2 * orignLength];
			System.arraycopy(readSet, 0, tmpReadSet, 0, orignLength);
			readSet = tmpReadSet;
			fillArray(orignLength);
		}
		currentReadFieldAccess = readSet[nextAvailable++];
		return currentReadFieldAccess;
	}

	public ReadObjectAccess getCurrent() {
		Logger.debug("get current " + currentReadFieldAccess);
		return currentReadFieldAccess;
	}

	public synchronized void checkClock(int clock, boolean clear) {
		Logger.debug("READSET: Readset size:" + nextAvailable);
		Integer hashCode = hashCode();
		pendingValidates.put(hashCode, this);
		try {
			//System.out.println("Constructing Read list");
			Map<Object, AbstractDistinguishable> set = new HashMap<Object, AbstractDistinguishable>();
			for (ReadObjectAccess field : readSet) {
				Object obj = field.getObject();
				if (obj != null) {
					if (obj instanceof AbstractDistinguishable) {
						AbstractDistinguishable dist = (AbstractDistinguishable) obj;
						set.put(dist.getId(), dist);
					}
					//System.out.println("Add " + obj);
				} else
					break;
			}
			//System.out.println("Readlist " + set.size());

			for (AbstractDistinguishable obj : set.values()) {
				if (LockTable.checkLock(obj, clock, false, Context.LOCKS_MARKER.get()) < 0) {
					AbstractDistinguishable object = (AbstractDistinguishable) obj;
					try {
						GlobalObject key = ObjectsRegistery.getKey(object.getId());
						if (key == null) // deleted object
							throw new TransactionException();
						Logger.debug("READSET: Remote Validation for " + key);
						try {
							CommunicationManager.getManager().send(
									key.getHome(),
									new ValidateRequest(key, hashCode));
						} catch (IOException e) {
							e.printStackTrace();
							throw new TransactionException();
						}
						Logger.debug("READSET: Wait Remote " + key
								+ " Validation ...");
						wait();
						if (clock < (remoteValidateResult & LockTable.UNLOCK)) {
							Logger
									.debug("READSET: Remote Validation failed, remote version:"
											+ (remoteValidateResult & LockTable.UNLOCK));
							throw new TransactionException();
						}
						Logger.debug("READSET: Remote Validation " + key
								+ " succeeded. remoteLock="+(remoteValidateResult & LockTable.UNLOCK)
								+ "arg<clock>="+clock);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		} finally {
			if (clear)
				for (ReadObjectAccess field : readSet)
					field.clear();
			pendingValidates.remove(hashCode);
		}
	}// woot

	/**
	 * Merges this readset into the readset of the parent transaction (given).
	 */
	public void mergeInto(ReadSet other) {
		// TODO: Is this behavior correct if there are still validations
		// pending?
		// Can there even exist pending validations at this stage anyway?
		if (pendingValidates.size() > 0)
			throw new TransactionException();
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

	public interface ReadSetListener {
		void execute(ReadObjectAccess read);
	}
}

class ValidateRequest extends Message {

	private GlobalObject key;
	private Integer readsetHashcode;
	private int senderClock;

	ValidateRequest(GlobalObject key, Integer readsetHashcode) {
		this.key = key;
		this.readsetHashcode = readsetHashcode;
		this.senderClock = LocalClock.get();
	}

	@Override
	public void run() {
		try {
			int localClock = LocalClock.get();
			if (senderClock > localClock)
				LocalClock.advance(senderClock);
			AbstractDistinguishable object = ((DTL2Directory) DirectoryManager
					.getManager()).getLocalObject(key);
			int currentLockVersion = object == null ? Integer.MAX_VALUE
					: LockTable.getLockVersion(object);
			Logger.debug("ValidateRequest::run(): key="+key+", lockVersion="+currentLockVersion+", localClock="+localClock);
			CommunicationManager.getManager().send(from,
					new ValidateResponse(currentLockVersion, readsetHashcode));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class ValidateResponse extends Message {

	private int lockVersion;
	private int senderClock;
	private int readsetHashcode;

	ValidateResponse(int lockVersion, int readsetHashcode) {
		this.readsetHashcode = readsetHashcode;
		this.lockVersion = lockVersion;
		this.senderClock = LocalClock.get();
	}

	@Override
	public void run() {
		if (senderClock > LocalClock.get())
			LocalClock.advance(senderClock);
		ReadSet readSet = ReadSet.pendingValidates.get(readsetHashcode);
		if (readSet == null) {
			System.out.println("Null context: " + readSet);
			return;
		}
		synchronized (readSet) {
			readSet.remoteValidateResult = lockVersion;
			readSet.notifyAll();
		}
	}
}
