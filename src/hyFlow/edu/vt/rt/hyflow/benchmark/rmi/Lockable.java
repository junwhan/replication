package edu.vt.rt.hyflow.benchmark.rmi;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;

public class Lockable extends UnicastRemoteObject
		implements ILockabel{

	protected AtomicBoolean lock = new AtomicBoolean();
	protected boolean enableTimeout = true; 
	private boolean destroied = false;
	private String id;
	
	public static int timouts;
	
    protected Lockable(String id) throws RemoteException {
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
    	
    	final Lockable me = this;
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
	public void lock() throws RemoteException, InterruptedException {
		long start = System.currentTimeMillis();
		boolean timedout = false;
		long timeoutPeriod = edu.vt.rt.hyflow.benchmark.Benchmark.timout();
		while(!destroied && !lock.compareAndSet(false, true)){
			if(enableTimeout && System.currentTimeMillis() - start > timeoutPeriod){
				timedout = true;
				timouts++;
				break;
			}
		}
		if(destroied || timedout)
			throw new InterruptedException();
	}

	@Override
	public void unlock() throws RemoteException {
		lock.set(false);
	}
}
