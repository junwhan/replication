package edu.vt.rt.hyflow.core.dir.dtl2;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.ContextDelegator;
import org.deuce.transaction.TransactionException;
import org.deuce.transform.Exclude;
import edu.vt.rt.hyflow.HyFlow;
import aleph.GlobalObject;
import aleph.Message;
import aleph.PE;
import aleph.comm.Address;
import aleph.dir.DirectoryManager;
import aleph.dir.NotRegisteredKeyException;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.tm.NestedContext;
import edu.vt.rt.hyflow.core.tm.dtl.Context;
import edu.vt.rt.hyflow.core.tm.dtl.LocalClock;
import edu.vt.rt.hyflow.core.tm.dtl.LockTable;
import edu.vt.rt.hyflow.core.tm.dtl2.ReadSet;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;

@Exclude
public class TrackerDirectory extends DirectoryManager {

	public static TransactionException FAIL_RETRIEVE = new TransactionException();
	
	static Map<Object, Address> directory = new ConcurrentHashMap<Object, Address>();
	static Map<Object, AbstractDistinguishable> local = new ConcurrentHashMap<Object, AbstractDistinguishable>();

	static TrackerDirectory theManager;
	
	public TrackerDirectory(){
		theManager = this;
	}
	
	private AbstractDistinguishable getCached(AbstractContext context, Object key) {
		if (context instanceof Context) {
			Context ctx = (Context)context;
			return ctx.getCachedObject(key);
		}
		return null;
	}
	
	@Override
	public String getLabel() {
		return "Tracker";
	}

	@Override
	public void register(AbstractDistinguishable object) {
		Object key = object.getId();
		local.put(key, object);
		Logger.debug("Locally Register of " + key);
		//try {
			//Address tracker = getTracker(key);
			//Address tracker = Network.getInstance().getCoordinator();
			//Logger.debug("Register tracker of " + key + " at " + tracker);
			//new Register(key).send(tracker);	// register me as the owner
		//} catch (IOException e) {
		//	e.printStackTrace();
		//}
	}
	
	public void unregister(AbstractDistinguishable object){
		Object key = object.getId();
		//try {
			Logger.debug("Unregister "+key);
			//new Unregister(key).send(getTracker(key));	// unregister object
			// TODO: this wasn't removed from local object list/cache
			// Check if any bugs are introduced
			//local.remove(key);
			
		//} catch (IOException e) {
			//e.printStackTrace();
		//}
	}
	
	public Address getTracker(Object key){
		return Network.getAddress(String.valueOf(Math.abs(key.hashCode())%Network.getInstance().nodesCount()));
	}

	public static Map<Long, Object> repliesQueue = new ConcurrentHashMap<Long, Object>();
	public static Map<Long, AbstractContext> pendingContexts = new ConcurrentHashMap<Long, AbstractContext>();
	@Override
	public Object open(AbstractContext context, Object key, String mode, int [] commute) {
		// What if object is already in local cache?
		// Idea: if already local, validate object
		Logger.debug("TrackerDirectory.open(key=" + key + ", mode="+mode+")");
		
		// do we have a cached copy?
		// TODO: check for cached copy before or after contacting tracker?
		// if after, provides a sanity check on whether the object is valid or not
		// TODO: tracker should store and forward object version, can help with caching.
		// TODO: tracker could store cached copies, and notify them when object is updated.
		
		
		AbstractDistinguishable object = getCached(context, key);
		if (object != null) {
			// cached copy exists
			if (object.isValid()) {
				Logger.debug("TrackerDirectory.open: Valid copy found in cache for key="+key);
				object.setShared(mode.equals("s"));
				return object;
			} else {
				// Invalid object, drop from cache and continue retrieving
				Logger.debug("TrackerDirectory.open: Invalid copy found in cache for key="+key+", HOW is txn still alive?");
			}
		}
		
		Object pendingObject = null;
		try {
			synchronized(context){
				long hash = context.hashCode()+System.currentTimeMillis();
				pendingContexts.put(hash, context);
				
				// get tracker
				/*
				Address tracker = getTracker(key);
				new WhoOwner(key, hash).send(tracker);
				Logger.debug("Wait for owner of " + key + " at " + tracker);
				try {
					context.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				*/
				//Logger.debug("Received owner response " + key);
				//Address owner = (Address)repliesQueue.get(hash);
				Address owner = Network.getInstance().getCoordinator();
				//if(owner==null){
				//	Logger.debug("Retrieve fail for no owner " + key);
				//	new Exception().printStackTrace(Logger.levelStream[Logger.DEBUG]);
				//	throw new NotRegisteredKeyException(key);
				//}
				if(owner.equals(PE.thisPE().getAddress())){
					Logger.debug("Try get locally" + key);
					object = local.get(key);
					object.setShared(mode.equals("s"));
					if(object!=null){
						Logger.debug("Got locally " + key);
						onOpenObject(context, object);
						return object;
					} else {
						Logger.debug("TrackerDirectory.open: failed to get object locally (key="+key+")");
					}
				}
				
				// contact owner
				new Retrieve(key, hash, commute).send(owner);
				Logger.debug("Wait for object " + key);
				try {
					context.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Logger.debug("Received object response " + key);
				Object[] data = (Object[])repliesQueue.get(hash);
				pendingObject = data[0];
				if(pendingObject==null){
					Logger.debug("Retrieve fail for no object " + key);
					throw FAIL_RETRIEVE;
				}
				AbstractDistinguishable distinguishable = ((AbstractDistinguishable)pendingObject);
				distinguishable.setOwnerNode(owner);
				distinguishable.setShared(mode.equals("s"));
				onOpenObject(context, distinguishable);
				
				int senderClock = (Integer)data[1];
				if(senderClock>=0)
					((Context)context).forward(senderClock);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NotRegisteredKeyException e) {
			throw e;
		} catch (Exception e) {
			throw FAIL_RETRIEVE;
		}
		
		return pendingObject;
	
	}

	private void onOpenObject(AbstractContext context, AbstractDistinguishable object) {
		if (context instanceof NestedContext) {
			((NestedContext)context).onOpenObject(object);
		}
	}

	public AbstractDistinguishable getLocalObject(Object key){
		return local.get(key);
	}
	
	
	
	
	
	@Override
	public void newObject(GlobalObject key, Object object, String hint) {
		// not used
	}
	@Override
	public void unregister(GlobalObject key) {
		// not used
	}
	@Override
	protected void release(AbstractContext context, GlobalObject key) {
		// not used
	}
	@Override
	protected Object open(AbstractContext context, GlobalObject object, String mode, int commute) {
		return null;	// not used
	}
}


class Retrieve extends Message{
	Object key;
	int [] commute;
	private int senderClock;
	private long contextHashcode;
	Retrieve(Object key, long contextHashcode, int [] commute){
		this.key = key;
		this.senderClock = LocalClock.get();
		this.contextHashcode = contextHashcode;
		this.commute = commute;
	}
	
	@Override
	public void run() {
		Network.linkDelay(false, from);
		try {
			int localClock = LocalClock.get();
			if(senderClock>localClock)
				LocalClock.advance(senderClock);
			AbstractDistinguishable object = TrackerDirectory.local.get(key);
			if(object==null)
				Logger.debug("Null local object! "+key+" "+from);
			else if(LockTable.isAvailable(object, Context.LOCKS_MARKER.get())){	// check if currently locked
				Logger.debug("Locked object");
				object = null;
			}
			int lockVersion = object==null ? 0 : LockTable.getLockVersion(object);
			Logger.debug("Sending object " + object + "@" + lockVersion + " back");

			  //Long id = ContextDelegator.getTopInstance().getContextId();
			  //HyFlow.requester req = HyFlow.commutativity.get(key.toString());
			  //if(req == null) req = new HyFlow.requester(); 
			  //else req.requesterInit(contextHashcode, commute);
			  //Logger.debug("Open commute here " + key);
			  //HyFlow.commutativity.put(key.toString(), req);

			new RetrieveRes(object, localClock, contextHashcode).send(from);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class RetrieveRes extends Message{
	private Object object;
	private int senderClock;
	private long contextHashcode;
	RetrieveRes(Object object, int currentClock, long contextHashcode){
		this.object = object;
		this.senderClock = currentClock;
		this.contextHashcode = contextHashcode;
		Network.callCostDelay();
	}
	
	@Override
	public void run() {
		Network.linkDelay(false, from);
		int localClock = LocalClock.get();
		if(senderClock>localClock)
			LocalClock.advance(senderClock);
		else 
			senderClock=-1;
		if(object!=null)
			LockTable.setRemote(object);
		else
			Logger.debug("Refuse.. " + object);
		TrackerDirectory.repliesQueue.put(contextHashcode, new Object[]{
				object,
				senderClock
		});
		AbstractContext context = TrackerDirectory.pendingContexts.remove(contextHashcode);
		if(context!=null)
			synchronized(context){
				Logger.debug("signal.");
				context.notifyAll();	
			}
		else
			System.err.println("How? " + Arrays.toString(TrackerDirectory.pendingContexts.values().toArray()));
	}
}

class WhoOwner extends Message{
	Object key;
	long hash;
	public WhoOwner(Object key, long hash) {
		this.key = key;
		this.hash = hash;
	}
	
	@Override
	public void run() {
		Network.linkDelay(false, from);
		Logger.debug("TrackerDirectory.WhoOwner(key="+key+", from="+from+")");
		try {
			
			for(int i=0; i<5; i++){
				Address owner = TrackerDirectory.directory.get(key);
				if(owner!=null){
					new Owner(owner, hash).send(from);
					return;
				}
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			};
			new Owner(TrackerDirectory.directory.get(key), hash).send(from);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class Owner extends Message{
	Address owner;
	long contextHashcode;
	public Owner(Address key, long hash) {
		this.owner = key;
		this.contextHashcode = hash;
	}
	
	@Override
	public void run() {
		Network.linkDelay(false, from);
		Logger.debug("TrackerDirectory.Owner(from="+from+", owner="+owner+")");
		if(owner!=null)
			TrackerDirectory.repliesQueue.put(contextHashcode, owner);
		else
			Logger.debug("Null owner");
		AbstractContext context = TrackerDirectory.pendingContexts.get(contextHashcode);
		if(context!=null)
			synchronized(context){
				Logger.debug("signal.");
				context.notifyAll();	
			}
		else
			System.err.println("How? " + Arrays.toString(TrackerDirectory.pendingContexts.values().toArray()));
	}
}

class Register extends Message{
	Object key;
	public Register(Object key) {
		this.key = key;
	}
	
	@Override
	public void run() {
		Network.linkDelay(false, from);
		Logger.debug("TrackerDirectory.Register(key="+key+", from="+from+")");
		TrackerDirectory.directory.put(key, from);
	}
}

class Unregister extends Message{
	Object key;
	public Unregister(Object key) {
		this.key = key;
	}
	
	@Override
	public void run() {
		Network.linkDelay(false, from);
		Logger.debug("TrackerDirectory.Unregister(key="+key+", from="+from+")");
		TrackerDirectory.directory.remove(key);
	}
}
