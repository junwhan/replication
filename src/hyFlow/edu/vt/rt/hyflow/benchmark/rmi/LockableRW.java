package edu.vt.rt.hyflow.benchmark.rmi;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;

public class LockableRW extends UnicastRemoteObject
		implements ILockabelRW{

	private final int permits = Integer.getInteger("threads")*Integer.getInteger("nodes");
	private Semaphore rlock = new Semaphore(permits);
	private AtomicBoolean wlock = new AtomicBoolean();
	
	private boolean destroied = false;
	private String id;
	
	public static int timouts;
	
    protected LockableRW(String id) throws RemoteException {
		super();
		this.id = id;
		try {
			UnicastRemoteObject.unexportObject(this, true);
		} catch (Exception e) {
			Logger.error("RMI unexporting");
		}
		Remote stub = UnicastRemoteObject.exportObject(this, 0);

		// Bind the remote object's stub in the registry
		Registry registry = LocateRegistry.getRegistry(Network.getInstance().getPort());
		registry.rebind(id, stub);
	}
    
    @Override
    public void destroy() throws RemoteException {
    	destroied = true;
    	
    	final LockableRW me = this;
    	new Thread(){
    		public void run() {
//    			System.err.println("Destroy         >--@@@@   B O O M   @@@@--<");
    	    	// Wait for quiescent time
    	    	try {
    				Thread.sleep(edu.vt.rt.hyflow.benchmark.Benchmark.timout()*2);
    			} catch (InterruptedException e1) {
    				e1.printStackTrace();
    			}
    	    	
    			// Unbind the remote object's stub in the registry
				try {
					Registry registry = LocateRegistry.getRegistry(Network.getInstance().getPort());
					registry.unbind(id);
				} catch (RemoteException e1) {
					e1.printStackTrace();
				} catch (NotBoundException e) {
					e.printStackTrace();
				}
    	    	
    			try {
    				UnicastRemoteObject.unexportObject(me, true);
    			} catch (Exception e) {
    				Logger.error("RMI unexporting");
    			}	
    		}
    	}.start();
    }

	@Override
	public void rLock() throws InterruptedException {
		long timeoutPeriod = edu.vt.rt.hyflow.benchmark.Benchmark.timout();
		if(destroied || !rlock.tryAcquire(timeoutPeriod, TimeUnit.MILLISECONDS))
			throw new InterruptedException();
	}

	@Override
	public void rUnlock(){
		rlock.release();
	}
	
	@Override
	public void wLock() throws InterruptedException {
		long start = System.currentTimeMillis();
		boolean timedout = false;
		long timeoutPeriod = edu.vt.rt.hyflow.benchmark.Benchmark.timout();
		
		while(!destroied && !wlock.compareAndSet(false, true)){
			if(System.currentTimeMillis() - start > timeoutPeriod){
				timedout = true;
				break;
			}
		}
		if(destroied || timedout)
			throw new InterruptedException();
		
		for(int i=0; i<permits; i++){
			long timeoutPeriodRemains = timeoutPeriod - (System.currentTimeMillis() - start);
			if(!rlock.tryAcquire(timeoutPeriodRemains, TimeUnit.MILLISECONDS)){
				if(i>0){
					rlock.release(i);
				}
				wlock.set(false);
				throw new InterruptedException();
			}
		}
	}

	@Override
	public void wUnlock(){
		rlock.release(permits);
		wlock.set(false);
	}
	
}
