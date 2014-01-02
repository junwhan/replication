package edu.vt.rt.hyflow.benchmark.rmi.dht;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;


public class DistributedHashTable extends UnicastRemoteObject
implements IDistributedHashTable{

	protected static final String BINDING_KEY = "dht_instance";

	private Hashtable<Object, Value> hashtable = new Hashtable<Object, Value>();
	
	protected DistributedHashTable() throws RemoteException {
		try {
			UnicastRemoteObject.unexportObject(this, true);
		} catch (Exception e) {
			Logger.debug("RMI unexporting");
		}
		IDistributedHashTable stub = (IDistributedHashTable) UnicastRemoteObject.exportObject(
				this, 0);

		// Bind the remote object's stub in the registry
		Registry registry = LocateRegistry.getRegistry(Network.getInstance().getPort());
		registry.rebind(BINDING_KEY, stub);
	}

	private static final long serialVersionUID = 1L;

	@Override
	public Integer get(Object key){
		Network.linkDelay(true, null);	//FIXME
		return hashtable.get(key).value;
	}

	@Override
	public void put(Object key, Integer value){
		Network.linkDelay(true, null);	//FIXME
		hashtable.put(key, new Value(value));
	}


	@Override
	public void lock(Object key) throws InterruptedException {
		Network.linkDelay(true, null);	//FIXME
		if(!hashtable.containsKey(key))
			put(key,0);
		long start = System.currentTimeMillis();
		boolean timedout = false;
		long timeoutPeriod = edu.vt.rt.hyflow.benchmark.Benchmark.timout();
		while(!hashtable.get(key).lock.compareAndSet(false, true)){
			if(System.currentTimeMillis() - start > timeoutPeriod){
				timedout = true;
				break;
			}
		}
		if(timedout)
			throw new InterruptedException();
	}

	@Override
	public void unlock(Object key){
		Network.linkDelay(true, null);	//FIXME
		hashtable.get(key).lock.set(false);
	}
	
	private class Value{
		protected Integer value;
		protected AtomicBoolean lock;
		
		protected Value(Integer val){
			value = val;
			lock = new AtomicBoolean();
		}
	}
}
