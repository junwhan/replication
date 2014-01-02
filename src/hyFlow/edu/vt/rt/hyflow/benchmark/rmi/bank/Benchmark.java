package edu.vt.rt.hyflow.benchmark.rmi.bank;

import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Random;

import aleph.comm.tcp.Address;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;


public class Benchmark extends edu.vt.rt.hyflow.benchmark.rmi.Benchmark{

	private Random random = new Random();
	protected final int amount = 10000;
	final int transfer = 10;
	
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
		int nodes = Network.getInstance().nodesCount();
		for(int i=0; i<localObjectsCount; i++)
			if((i % nodes)== id)
				try {
					Logger.debug("Created:" + id + "-" + i);
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
	
	public static String getServerId(String accountNum){
		return accountNum.split("-")[0];
	}

	@Override
	protected void readOperation(Object... ids) {
		BankAccount.getTotalBalance(String.valueOf(ids[0]), String.valueOf(ids[1])); 
	}

	@Override
	protected void writeOperation(Object... ids) {
		BankAccount.transfer(String.valueOf(ids[0]), String.valueOf(ids[1]), amount); 
	}

	@Override
	protected String getLabel() {
		return "Bank-RMI";
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
						account.lock();
						balance+=account.checkBalance();
						account.unlock();
						
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
