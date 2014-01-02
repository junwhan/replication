package edu.vt.rt.hyflow.benchmark.rmi.bank.rw;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

import aleph.comm.tcp.Address;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;


public class Benchmark extends edu.vt.rt.hyflow.benchmark.rmi.bank.Benchmark{

	@Override
	protected void createLocalObjects() {
		Integer id = Network.getInstance().getID();
		int nodes = Network.getInstance().nodesCount();
		for(int i=0; i<localObjectsCount; i++)
			if((i % nodes)== id)
				try {
					Logger.debug("Created:" + id + "-" + i);
					new BankAccount(id + "-" + i).deposit(10000);
				} catch (Throwable e) {
					e.printStackTrace();
				}
	}

	@Override
	protected void readOperation(Object... ids) {
		BankAccount.getTotalBalance(String.valueOf(ids[0]), String.valueOf(ids[1])); 
	}

	@Override
	protected void writeOperation(Object... ids) {
		BankAccount.transfer(String.valueOf(ids[0]), String.valueOf(ids[1]),10); 
	}

	@Override
	protected String getLabel() {
		return "Bank-RW";
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
		
		long balance = 0;
		IBankAccount account = null;
		int node = Network.getInstance().nodesCount();
		for(int i=0; i<localObjectsCount; i++){
			for(int j=0;j<node;j++){
				if(i%node==j){
					try {
						Address server1 = (Address) Network.getAddress(Benchmark.getServerId(j+"-"+i));
						account = (IBankAccount)LocateRegistry.getRegistry(server1.inetAddress.getHostAddress(), server1.port).lookup(j+"-"+i);
						account.rLock();
						balance+=account.checkBalance();
						account.rUnlock();
					} catch (RemoteException e) {
						e.printStackTrace();
					} catch (NotBoundException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}	
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
