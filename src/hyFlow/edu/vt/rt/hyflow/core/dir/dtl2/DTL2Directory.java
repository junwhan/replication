package edu.vt.rt.hyflow.core.dir.dtl2;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.ContextDelegator;
import org.deuce.transaction.TransactionException;
import org.deuce.transform.Exclude;

import aleph.GlobalObject;
import aleph.Message;
import aleph.PE;
import aleph.dir.DirectoryManager;
import aleph.dir.ObjectsRegistery;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.tm.dtl2.Context;
import edu.vt.rt.hyflow.core.tm.dtl2.LocalClock;
import edu.vt.rt.hyflow.core.tm.dtl2.LockTable;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;

@Exclude
public class DTL2Directory extends DirectoryManager {

	public static TransactionException FAIL_RETRIEVE = new TransactionException("Failed to retrieve object.");
	
	@Exclude
	static class ClientSide{
		public static Map<Integer, AbstractContext> pendingContexts = new ConcurrentHashMap<Integer, AbstractContext>();
		public static Map<Integer, Object[]> result = new ConcurrentHashMap<Integer, Object[]>();

		public Object open(AbstractContext context, GlobalObject key, String mode) {
			Object pendingObject = null;
			try {
				synchronized(context){
					String stamp = String.valueOf(Math.random());
					Logger.debug(stamp + "| Retrieve Request for " + key + " sent to " + key.getHome());
					int hash = context.hashCode();
					pendingContexts.put(hash, context);
					new RetrieveRequest(key, stamp, hash).send(key.getHome());
					Logger.debug("Wait response for " + key);
					try { context.wait(); } catch (InterruptedException e) { e.printStackTrace(); }
					Logger.debug("Notified response for " + key);
					Object[] data = result.get(hash);
					pendingObject = data[0];
					if(pendingObject==null){
						Logger.debug("Retrieve fail for " + key);
						throw FAIL_RETRIEVE;
					}
					int senderClock = (Integer)data[1];
					if(senderClock>=0)
						((Context)context).forward(senderClock);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return pendingObject;
		}
		
		public void objectReceived(int hashcode, Object object, int senderClock){
			if(object!=null)
				LockTable.setRemote(object);
			else
				Logger.debug("Refuse.. " + object);
			result.put(hashcode, new Object[]{
					object,
					senderClock
			});
			AbstractContext context = pendingContexts.remove(hashcode);
			if(context!=null)
				synchronized(context){
					Logger.debug("signal.");
					context.notifyAll();	
				}
			else
				System.err.println("How? " + Arrays.toString(pendingContexts.values().toArray()));
		}
	}
	
	private Map<Integer, ClientSide> clientSide = new ConcurrentHashMap<Integer, ClientSide>();
	
	static Map<GlobalObject, AbstractDistinguishable> local = new ConcurrentHashMap<GlobalObject, AbstractDistinguishable>();
	static DTL2Directory theManager;
	
	public DTL2Directory(){
		theManager = this;
	}
	
	@Override
	public String getLabel() {
		return "D-TL2";
	}

	@Override
	public void newObject(GlobalObject key, Object object, String hint) {
		Logger.debug("Put " + key + " [" + key.hashCode() + "] "+ object);
		int hash = object.hashCode();
//		LockTable.reset(hash);
		local.put(key, (AbstractDistinguishable)object);
		Logger.debug("Check " + key + " [" + key.hashCode() + "] "+ local.get(key) + " >>> " + hash + " " + LockTable.isAvailable(object, Context.LOCKS_MARKER.get()));
	}

	public synchronized ClientSide getClientSide(GlobalObject key){
		ClientSide client = clientSide.get(key.hashCode());
		if(client==null){
			client = new ClientSide();
			Logger.debug("Installing new client " + client + " for key " + key + " hashed " + key.hashCode());
			clientSide.put(key.hashCode(), client);
		}
		return client;
	}
	
	@Override
	protected Object open(AbstractContext context, GlobalObject key, String mode, int commute) {
		Logger.debug("Try get " + key);
		Object object = local.get(key);
		if(object!=null){
			Logger.debug("Got locally " + key);
			ContextDelegator.onOpenObject(object);
			return object;
		}
		Logger.debug("Try remote access " + key);
		object = getClientSide(key).open(context, key, mode);
		ContextDelegator.onOpenObject(object);
		return object;
	}

	public AbstractDistinguishable getLocalObject(GlobalObject key){
		return local.get(key);
	}
	
	@Override
	public void unregister(GlobalObject key) {
		super.unregister(key);
		local.remove(key);
	}
	
	@Override
	protected void release(AbstractContext context, GlobalObject key) {}

	public void retrieveResponse(GlobalObject key, int hashcode, Object object, int senderClock) {
		getClientSide(key).objectReceived(hashcode, object, senderClock);
	}
}

@Exclude
class RetrieveRequest extends Message{

	private GlobalObject key;
	private String mode;
	private int senderClock;
	private int contextHashcode;
	RetrieveRequest(GlobalObject key, String mode, int contextHashcode){
		this.key = key;
		this.mode = mode;
		this.senderClock = LocalClock.get();
		this.contextHashcode = contextHashcode;
	}
	
	@Override
	public void run() {
		Network.linkDelay(false, from);
		try {
			Logger.debug(mode + "| Retrieve request received from " + from + " for " + key + "[" + key.hashCode() + "]");
			key.setHome(PE.thisPE());	// adjust the PE error
			int localClock = LocalClock.get();
			if(senderClock>localClock)
				LocalClock.advance(senderClock);
			AbstractDistinguishable object = DTL2Directory.local.get(key);
			if(object==null)
				Logger.debug("Null local object!");
			else if(LockTable.isAvailable(object, Context.LOCKS_MARKER.get())){	// check if currently locked
				Logger.debug("Locked object");
				object = null;
			}else{
				GlobalObject globalObject = ObjectsRegistery.getKey(object.getId());
				if(globalObject==null){
					Logger.debug("Deleted object.");
					object = null;
				}else if(globalObject.getVersion()!=key.getVersion()){
					Logger.debug("Old GlobalObject was used to retreive object, retry later.");
					object = null;
				}
			}
			int lockVersion = object==null ? 0 : LockTable.getLockVersion(object);
			Logger.debug("Sending object " + object + "@" + lockVersion + " back");
			new RetrieveResponse(key, object, localClock, contextHashcode).send(from);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

@Exclude
class RetrieveResponse extends Message{

	private GlobalObject key;
	private Object object;
	private int senderClock;
	private int contextHashcode;
	RetrieveResponse(GlobalObject key, Object object, int currentClock, int contextHashcode){
		this.key = key;
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
		Logger.debug("Received object " + key + " " + object + " back");
		DTL2Directory.theManager.retrieveResponse(key, contextHashcode, object, senderClock);
	}
}