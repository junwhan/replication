package edu.vt.rt.hyflow.benchmark.rmi.loan;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sun.org.apache.bcel.internal.generic.ILOAD;

import aleph.comm.tcp.Address;
import aleph.dir.cm.arrow.ArrowDirectory;
import edu.vt.rt.hyflow.benchmark.rmi.Lockable;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;

public class LoanAccount extends Lockable
implements ILoanAccount{

	private static final int BRANCHING = 2;
	
	private Long lockLastOwner;
	private int nestingLevel; 
	
    private int amount;
    private String id;
    
    public LoanAccount(String id) throws RemoteException{
    	super(id);
    }

	@Override
	public Integer checkBalance(){
		return amount;
	}

	public void deposit(int dollars){
		amount = amount + dollars;
	}

	public boolean withdraw(int dollars){
		amount = amount - dollars;
		return amount >= 0;
	}

	@Override
	public void lock(Long txnId) throws InterruptedException {
		Logger.debug("Try Lock [" + txnId + "]");
		long start = System.currentTimeMillis();
		boolean timedout = false;
		long timeoutPeriod = edu.vt.rt.hyflow.benchmark.Benchmark.timout();
		while(!lock.compareAndSet(false, true)){
			if(txnId.equals(lockLastOwner))
				break;
			if(System.currentTimeMillis() - start > timeoutPeriod){
				timedout = true;
				break;
			}
		}
		if(timedout){
			Logger.debug("TIMEOUT========================================");
			throw new InterruptedException();
		}
		else{
			if(!txnId.equals(lockLastOwner))
				Logger.debug("Lock" + id + " [" + txnId + "]");
			lockLastOwner = txnId;
			nestingLevel ++;
		}
	}

	@Override
	public void unlock(Long txnId){
		if(!txnId.equals(lockLastOwner))
			return;
		nestingLevel--;
		if(nestingLevel>0)
			return;
		Logger.debug("UnLock" + id + " [" + txnId + "]");
		lockLastOwner = null;
		lock.set(false);
	}
	
	private Object[] getAccounts(List<String> accountNums, int branching){
		ILoanAccount[] acc = new ILoanAccount[branching];
		Address[] server = new Address[branching];
		for(int i=0; i<branching && i<accountNums.size(); i++){
			String accNum = accountNums.get(i);
			server[i] = (Address) Network.getAddress(Benchmark.getServerId(accNum));
			try {
				acc[i]=((ILoanAccount)LocateRegistry.getRegistry(server[i].inetAddress.getHostAddress(), server[i].port).lookup(accNum));
			} catch (NotBoundException e) {
				e.printStackTrace();
			} catch (AccessException e) {
				e.printStackTrace();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		return new Object[] {acc, server};
	}
	
	@Override
	public void expandLock(Long txnId, List<String> accountNums, int branching) throws InterruptedException, RemoteException{
		boolean locked = false;
		try {
			lock(txnId);	// get lock for myself
			locked = true;
			Object[] data = getAccounts(accountNums, branching);
			ILoanAccount[] acc = (ILoanAccount[])data[0];
			Address[] server = (Address[])data[1];
			for(int i=0; i<acc.length; i++){
				if(acc[i]==null)
					break;
				accountNums.remove(0);
				Network.linkDelay(true, server[i]);
				acc[i].expandLock(txnId, accountNums, branching);
			}
		}catch (InterruptedException e) {
			if(locked)
				unlock(txnId);		
			throw e;
		}
	}
	
	@Override
	public void collapseLock(Long txnId, List<String> accountNums, int branching) throws RemoteException{
		unlock(txnId);	// unlock myself
		Object[] data = getAccounts(accountNums, branching);
		ILoanAccount[] acc = (ILoanAccount[])data[0];
		Address[] server = (Address[])data[1];
		for(int i=0; i<acc.length; i++){
			if(acc[i]==null)
				break;
			accountNums.remove(0);
			Network.linkDelay(true, server[i]);
			acc[i].collapseLock(txnId, accountNums, branching);
		}
	}

	
	@Override
	public void borrow(List<String> accountNums, int branching,	boolean initiator, int amount) throws RemoteException{
		Logger.debug("Processing");
		//processing
		Object[] data = getAccounts(accountNums, branching);
		ILoanAccount[] acc = (ILoanAccount[])data[0];
		Address[] server = (Address[])data[1];
		for(int i=0; i<acc.length; i++){
			if(acc[i]==null)
				break;
			boolean last = i!=acc.length-1;	// is the last one?
			int loan = last ? amount : (int)(Math.random()*amount);		// randomly have a loan amount from neighbor
			accountNums.remove(0);	// remove myself
			Network.linkDelay(true, server[i]);
			acc[i].borrow(accountNums, branching, false, loan);		// borrow from others
			deposit(loan);	// add the loaned amount to my money
			amount -= loan;
		}
		if(!initiator)
			withdraw(amount);	// provide the loan request
		
		Logger.debug("Processing done");		
	}
	public static void borrow(Long txnId, String id, List accountNums, int amount){
		while(true){
			Logger.debug("Retry");
			ILoanAccount account = null;
			Address server = null;
			try {
				server = (Address) Network.getAddress(Benchmark.getServerId(id));
				account = (ILoanAccount)LocateRegistry.getRegistry(server.inetAddress.getHostAddress(), server.port).lookup(id);
				//lock
				Network.linkDelay(true, server);
				account.expandLock(txnId, (List<String>)((LinkedList<String>)accountNums).clone(), BRANCHING);
				//processing
				edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
				Network.linkDelay(true, server);
				account.borrow((List<String>)((LinkedList<String>)accountNums).clone(), BRANCHING, true, amount);
				break;
			} catch (InterruptedException e) {
			} catch (AccessException e) {
				e.printStackTrace();
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (NotBoundException e) {
				e.printStackTrace();
			} finally{
				try {
					//unlock
					if(account != null){
						Network.linkDelay(true, server);
						account.collapseLock(txnId, (List<String>)((LinkedList<String>)accountNums).clone(), BRANCHING);
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public int sum(List<String> accountNums, int branching) throws RemoteException{
		Logger.debug("Processing");
		//processing
		int sum = checkBalance();
		Object[] data = getAccounts(accountNums, branching);
		ILoanAccount[] acc = (ILoanAccount[])data[0];
		Address[] server = (Address[])data[1];
		for(int i=0; i<acc.length; i++){
			if(acc[i]==null)
				break;
			accountNums.remove(0);	// remove myself
			Network.linkDelay(true, server[i]);
			sum += acc[i].sum(accountNums, branching);		// sum others
		}
		Logger.debug("Processing done");
		return sum;
	}
	public static void sum(Long txnId, String id, List accountNums){
		while(true){
			Logger.debug("Retry");
			ILoanAccount account = null;
			Address server = null;
			try {
				server = (Address) Network.getAddress(Benchmark.getServerId(id));
				account = (ILoanAccount)LocateRegistry.getRegistry(server.inetAddress.getHostAddress(), server.port).lookup(id);
				//lock
				Network.linkDelay(true, server);
				account.expandLock(txnId, (List<String>)((LinkedList<String>)accountNums).clone(), BRANCHING);
				//processing
				edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
				Network.linkDelay(true, server);
				account.sum((List<String>)((LinkedList<String>)accountNums).clone(), BRANCHING);
				break;
			} catch (InterruptedException e) {
			} catch (AccessException e) {
				e.printStackTrace();
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (NotBoundException e) {
				e.printStackTrace();
			} finally{
				try {
					//unlock
					if(account != null){
						Network.linkDelay(true, server);
						account.collapseLock(txnId, (List<String>)((LinkedList<String>)accountNums).clone(), BRANCHING);
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
	}
}