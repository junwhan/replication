package edu.vt.rt.hyflow.benchmark.tm.bank;

import java.util.Random;

import org.deuce.Atomic;

import aleph.dir.DirectoryManager;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;
import aleph.comm.Address;

public class Benchmark extends edu.vt.rt.hyflow.benchmark.tm.Benchmark{

	final int amount = 10000;
	final int transfer = 10;
	private Random random = new Random(hashCode());

	@Override
	protected Class[] getSharedClasses() {
		return new Class[] { BankAccount.class };
	}
	
	@Override
	protected void createLocalObjects() {
		Integer id = Network.getInstance().getID();
		int nodes = Network.getInstance().nodesCount();
		//Address add = Network.getInstance().getCoordinator();
		//Logger.debug("Distributing " + localObjectsCount + " objects over " + nodes + " nodes," + add + "Coordinator");
		for(int i=0; i<localObjectsCount; i++){
			//Logger.debug("Try creating object " + i);
			//if((i % nodes)== id){
			if(id == 0){
				Logger.debug("Created locally object " + i);
				try {
					new BankAccount(id + "-" + i).deposit(amount);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	protected int getOperandsCount() {	return 2; }

	@Override
	protected Object randomId() {
		int obj = random.nextInt(localObjectsCount);
		//return (obj%Network.getInstance().nodesCount()) + "-" + obj ;
		return ("0-" + obj) ;
	}

	@Override
	protected void readOperation(Object... ids) {
		try {
			BankAccount.totalBalance(ids);
		} catch (Throwable e) {
			e.printStackTrace();
		} 
	}

	@Override
	protected void writeOperation(Object... ids) {
		try {
			BankAccount.transfer(ids);
		} catch (Throwable e) {
			e.printStackTrace();
		} 
	}

	@Override
	protected String getLabel() {
		return "Bank-TM";
	}


	@Override
	@Atomic
	protected void checkSanity() {
		if(Network.getInstance().getID()!=0)
			 return;

		try {
			// TODO: WTF?!?
			Thread.sleep(120000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		long balance = 0;
		//DirectoryManager locator = HyFlow.getLocator();
		int node = Network.getInstance().nodesCount();
		for(int i=0; i<localObjectsCount; i++){
			for(int j=0;j<node;j++){
				if(i%node==j){
					try {
						//BankAccount account = ((BankAccount) locator.open(j + "-" + i,"r"));
						balance+=BankAccount.checkBalance(j + "-" + i);
					} catch (Throwable e) {
						e.printStackTrace();
					}	
				}
			}
		}
		if(balance==localObjectsCount*amount)
			System.err.println("Passed sanity check");
		else
			System.err.println("Failed sanity check."+
					"\nbalance = "+ balance+
					"\nexpected = "+localObjectsCount*amount);
	}	
}

