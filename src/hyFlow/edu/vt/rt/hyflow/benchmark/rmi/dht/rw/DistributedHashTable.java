package edu.vt.rt.hyflow.benchmark.rmi.dht.rw;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Hashtable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.vt.rt.hyflow.benchmark.Benchmark;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;


public class DistributedHashTable extends UnicastRemoteObject
implements IDistributedHashTable{

	protected static final String BINDING_KEY = "dht_instance";
	private final int permits = Integer.getInteger("threads")*Integer.getInteger("nodes");

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
	public void wLock(Object key) throws InterruptedException {
		Network.linkDelay(true, null);	//FIXME
		if(!hashtable.containsKey(key))
			put(key,0);
		long start = System.currentTimeMillis();
		boolean timedout = false;
		long timeoutPeriod = edu.vt.rt.hyflow.benchmark.Benchmark.timout();
		while(!hashtable.get(key).wlock.compareAndSet(false, true)){
			if(System.currentTimeMillis() - start > timeoutPeriod){
				timedout = true;
				break;
			}
		}
		if(timedout)
			throw new InterruptedException();
	}

	@Override
	public void wUnlock(Object key){
		Network.linkDelay(true, null);	//FIXME
		hashtable.get(key).wlock.set(false);
	}

	@Override
	public void rLock(Object key) throws InterruptedException {
		Network.linkDelay(true, null);	//FIXME
		if(!hashtable.containsKey(key))
			put(key,0);
		long timeoutPeriod = edu.vt.rt.hyflow.benchmark.Benchmark.timout();
		if(!hashtable.get(key).rlock.tryAcquire(timeoutPeriod, TimeUnit.MILLISECONDS))
			throw new InterruptedException();

	}

	@Override
	public void rUnlock(Object key){
		Network.linkDelay(true, null);	//FIXME	
		hashtable.get(key).rlock.release();
	}

	private class Value{
		protected Integer value;
		protected AtomicBoolean wlock;
		protected Semaphore rlock;

		protected Value(Integer val){
			value = val;
			wlock = new AtomicBoolean();
			rlock = new Semaphore(permits);
		}
	}
}
