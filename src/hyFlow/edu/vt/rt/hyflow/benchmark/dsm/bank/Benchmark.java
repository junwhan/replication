package edu.vt.rt.hyflow.benchmark.dsm.bank;

import java.util.Random;

import org.deuce.Atomic;

import aleph.dir.DirectoryManager;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;


public class Benchmark extends edu.vt.rt.hyflow.benchmark.Benchmark{

	final int amount = 10000;
	final int transfer = 10;
	private Random random = new Random(hashCode());

	@Override
	protected void createLocalObjects() {
		Integer id = Network.getInstance().getID();
		int nodes = Network.getInstance().nodesCount();
		for(int i=0; i<localObjectsCount; i++)
			if((i % nodes)== id)
				try {
					new BankAccount(id + "-" + i).deposit(amount);
				} catch (Throwable e) {
					e.printStackTrace();
				}
	}

	@Override
	protected int getOperandsCount() {	return 2; }

	@Override
	protected Object randomId() {
		int obj = random.nextInt(localObjectsCount);
		return (obj%Network.getInstance().nodesCount()) + "-" + obj ;
	}

	@Override
	protected void readOperation(Object... ids) {
		BankAccount.getTotalBalance(String.valueOf(ids[0]), String.valueOf(ids[1])); 
	}

	@Override
	protected void writeOperation(Object... ids) {
		BankAccount.transfer(String.valueOf(ids[0]), String.valueOf(ids[1]),transfer); 
	}

	@Override
	protected String getLabel() {
		return "Bank-DSM";
	}

	@Override
	@Atomic
	protected void checkSanity() {
		if(Network.getInstance().getID()!=0)
			 return;

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		long balance = 0;
		DirectoryManager locator = HyFlow.getLocator();
		int node = Network.getInstance().nodesCount();
		for(int i=0; i<localObjectsCount; i++){
			for(int j=0;j<node;j++){
				if(i%node==j){
					balance+=((BankAccount) locator.open(j + "-" + i,"r")).checkBalance();	
				}
			}
		}
		if(balance==localObjectsCount*amount)
			Logger.debug("Passed sanity check");
		else
			Logger.debug("Failed sanity check."+
					"\nbalance = "+ balance+
					"\nexpected = "+localObjectsCount*amount);
	}	
}

