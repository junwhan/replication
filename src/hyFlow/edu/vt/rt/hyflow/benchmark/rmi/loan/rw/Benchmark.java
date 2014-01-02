package edu.vt.rt.hyflow.benchmark.rmi.loan.rw;

import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.deuce.Atomic;

import aleph.dir.DirectoryManager;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;


public class Benchmark extends edu.vt.rt.hyflow.benchmark.rmi.Benchmark{

	private Random random = new Random();
	final int amount = 10000;
	final int transfer = 10;
	final int nesting = Integer.getInteger("nesting");
	static long txnCounter;
	
	public Benchmark() {
		super();
		if (System.getSecurityManager() == null)
			System.setSecurityManager ( new RMISecurityManager() );
		try {
			LocateRegistry.createRegistry(Network.getInstance().getPort());
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void createLocalObjects() {
		Integer id = Network.getInstance().getID();
		txnCounter = id*1000000;
		int nodes = Network.getInstance().nodesCount();
		for(int i=0; i<localObjectsCount; i++)
			if((i % nodes)== id)
				try {
					Logger.debug("Created:" + id + "-" + i);
					new LoanAccount(id + "-" + i).deposit(amount);
				} catch (Throwable e) {
					e.printStackTrace();
				}
	}

	@Override
	protected int getOperandsCount() {	return nesting; }

	@Override
	protected Object randomId() {
		int obj = random.nextInt(localObjectsCount);
		return (obj%Network.getInstance().nodesCount()) + "-" + obj ;
	}
	
	public static String getServerId(String accountNum){
		return accountNum.split("-")[0];
	}

	@Override
	protected void readOperation(Object... ids) {
		List<String> accountNums = new LinkedList<String>(); 
		for(int i=1; i<ids.length; i++)
			accountNums.add((String)ids[i]);
		try {
			LoanAccount.sum(txnCounter++, (String)ids[0], accountNums);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void writeOperation(Object... ids) {
//		if(Network.getInstance().getID()!=0)
//			 return;
		
//		ids = new Object[]{
//				"0-0", "1-1", "2-2", "3-3", "4-4", "5-5",
//		};
	
		List<String> accountNums = new LinkedList<String>(); 
		for(int i=1; i<ids.length; i++)
			accountNums.add((String)ids[i]);
		try {
			LoanAccount.borrow(txnCounter++, (String)ids[0], accountNums, (int)(amount * Math.random()));
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	protected String getLabel() {
		return "Loan-RW";
	}

	@Atomic
	private long sumAssests(int node){
		long balance = 0;
		DirectoryManager locator = HyFlow.getLocator();
		for(int i=0; i<localObjectsCount; i++){
			for(int j=0;j<node;j++){
				if(i%node==j){
					try {
						balance+=((LoanAccount) locator.open(j + "-" + i,"r")).checkBalance();
					} catch (Throwable e) {
						e.printStackTrace();
					}	
				}
			}
		}
		return balance;
	}
	
	@Override
	protected void checkSanity() {
		if(Network.getInstance().getID()!=0)
			 return;
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		long balance = sumAssests(Network.getInstance().nodesCount());
		if(balance==localObjectsCount*amount)
			Logger.debug("Passed sanity check");
		else
			Logger.debug("Failed sanity check."+
					"\nbalance = "+ balance+
					"\nexpected = "+localObjectsCount*amount);
	}

}
