package edu.vt.rt.hyflow.benchmark.rmi.bank.rw;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import aleph.comm.tcp.Address;

import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;

public class BankAccount extends UnicastRemoteObject
implements IBankAccount{

	private static final long serialVersionUID = 1L;

	private final int permits = Integer.getInteger("threads")*Integer.getInteger("nodes");
	private Semaphore rlock = new Semaphore(permits);
	private AtomicBoolean wlock = new AtomicBoolean();
    private int amount;
    private String id;

    public BankAccount(String id) throws RemoteException{
    	this.id = id;
          
		try {
			UnicastRemoteObject.unexportObject(this, true);
		} catch (Exception e) {
			Logger.sysout("RMI unexporting");
		}
		IBankAccount stub = (IBankAccount) UnicastRemoteObject.exportObject(this, 0);

		// Bind the remote object's stub in the registry
		Registry registry = LocateRegistry.getRegistry(Network.getInstance().getPort());
		registry.rebind(this.id, stub);
    }
    
	@Override
	public Integer checkBalance(){
		return amount;
	}

	@Override
	public void deposit(int dollars){
		amount = amount + dollars;
	}

	@Override
	public boolean withdraw(int dollars){
		amount = amount - dollars;
		return amount >= 0;
	}

	@Override
	public void rLock() throws InterruptedException {
		long timeoutPeriod = edu.vt.rt.hyflow.benchmark.Benchmark.timout();
		if(!rlock.tryAcquire(timeoutPeriod, TimeUnit.MILLISECONDS))
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
		
		while(!wlock.compareAndSet(false, true)){
			if(System.currentTimeMillis() - start > timeoutPeriod){
				timedout = true;
				break;
			}
		}
		if(timedout)
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
	
	public static long getTotalBalance(String subAccountNum1, String subAccountNum2) {
		IBankAccount subAccount1 = null;
		IBankAccount subAccount2 = null;
		Address server1 = null, server2 = null;
		try {
			while(true){
				boolean locked1 = false;
				try {
					server1 = (Address) Network.getAddress(Benchmark.getServerId(subAccountNum1));
					subAccount1 = (IBankAccount)LocateRegistry.getRegistry(server1.inetAddress.getHostAddress(), server1.port).lookup(subAccountNum1);
					Network.linkDelay(true, server1);
					subAccount1.rLock();
					locked1 = true;
					server2 = (Address) Network.getAddress(Benchmark.getServerId(subAccountNum2));
					subAccount2 = (IBankAccount)LocateRegistry.getRegistry(server2.inetAddress.getHostAddress(), server2.port).lookup(subAccountNum2);
					Network.linkDelay(true, server2);
					subAccount2.rLock();
					break;
				} catch (InterruptedException e) {
					if(locked1){
						Network.linkDelay(true, server1);
						subAccount1.rUnlock();
					}
				}
			}
			
			Logger.debug("In");
			long balance = 0;
			
			for(int i=0; i<Benchmark.calls; i++){
				Network.linkDelay(true, server1);
				subAccount1.checkBalance();
			}
			
			edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
			
			for(int i=0; i<Benchmark.calls; i++){
				Network.linkDelay(true, server2);
				balance += subAccount2.checkBalance();
			}
			Logger.debug("Out");
			
			return balance;
		} catch (AccessException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		} finally{
			if(subAccount1!=null)
				try {
					Network.linkDelay(true, server1);
					subAccount1.rUnlock();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			if(subAccount2!=null)
				try {
					Network.linkDelay(true, server2);
					subAccount2.rUnlock();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
		}
		return Long.MIN_VALUE;
	}
	
	public static boolean transfer(String subAccountNum1, String subAccountNum2, int amount) {
		IBankAccount account1 = null;
		IBankAccount account2 = null;
		Address server1 = null, server2 = null;
		try {
			while(true){
				boolean locked1 = false;
				try {
					server1 = (Address) Network.getAddress(Benchmark.getServerId(subAccountNum1));
					account1 = (IBankAccount)LocateRegistry.getRegistry(server1.inetAddress.getHostAddress(), server1.port).lookup(subAccountNum1);
					Network.linkDelay(true, server1);
					account1.wLock();
					locked1 = true;
					server2 = (Address) Network.getAddress(Benchmark.getServerId(subAccountNum2));
					account2 = (IBankAccount)LocateRegistry.getRegistry(server2.inetAddress.getHostAddress(), server2.port).lookup(subAccountNum2);
					Network.linkDelay(true, server2);
					account2.wLock();
					break;
				} catch (InterruptedException e) {
					if(locked1){
						Network.linkDelay(true, server1);
						account1.wUnlock();
					}
				}
			}
			
			Logger.debug("In");
			for(int i=0; i<Benchmark.calls; i++){
				Network.linkDelay(true, server1);
				account1.withdraw(amount);
			}
			
			edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
			
			for(int i=0; i<Benchmark.calls; i++){
				Network.linkDelay(true, server2);
				account2.deposit(amount);
			}
			Logger.debug("Out");
			
			return true;
		} catch (AccessException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		} finally{
			if(account1!=null)
				try {
					Network.linkDelay(true, server1);
					account1.wUnlock();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			if(account2!=null)
				try {
					Network.linkDelay(true, server2);
					account2.wUnlock();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
		}
		return false;
	}

}
