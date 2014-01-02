package edu.vt.rt.hyflow.benchmark.rmi.dht;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Random;

import aleph.comm.tcp.Address;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;

public class Benchmark extends edu.vt.rt.hyflow.benchmark.rmi.Benchmark {

	private final int READ_RANGE = 4;
	private final int WRITE_RANGE = READ_RANGE/2;
	private Random random = new Random();

	public Benchmark() {
		super();
		if (System.getSecurityManager() == null)
			System.setSecurityManager(new RMISecurityManager());
		try {
			LocateRegistry.createRegistry(Network.getInstance().getPort());
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void createLocalObjects() {
		try {
			new DistributedHashTable();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected String getLabel() {
		return "DHT-RMI";
	}

	@Override
	protected int getOperandsCount() {
		return 1;
	}

	@Override
	protected Object randomId() {
		return random.nextInt(localObjectsCount);
	}

	private Integer findNode(Object key) {
		return key.hashCode() % Network.getInstance().nodesCount();
	}

	@Override
	protected void readOperation(Object... ids) {
		while (true) {
			int l=0, sum = 0;
			Integer key = (Integer) ids[0];
			IDistributedHashTable[] tables = new IDistributedHashTable[READ_RANGE];
			try {
				for (int i = 0; i < READ_RANGE; i++) {
					Integer nodeId = findNode(i + key);
					Address server1 = (Address) Network.getAddress(String
							.valueOf(nodeId));
					tables[i] = (IDistributedHashTable) LocateRegistry
							.getRegistry(server1.inetAddress.getHostAddress(),
									server1.port).lookup(
									DistributedHashTable.BINDING_KEY);
					tables[i].lock(key+i);
					l++;
				}
				for (int i = 0; i < READ_RANGE; i++) {
					Integer value = tables[i].get(key + i);
					if (value != null)
						sum += value;
				}
				edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
				for (int i = 0; i < READ_RANGE; i++) {
					tables[i].unlock(key+i);
				}
				break;
			} catch (AccessException e) {
				e.printStackTrace();
			} catch (RemoteException e) {
				e.printStackTrace();
			} 
				catch (InterruptedException e) {
				if (l > 0) {
					for (int j = 0; j < l; j++) {
						try {
							tables[j].unlock(key+j);
						} catch (RemoteException e1) {
							e1.printStackTrace();
						}
					}
				}
			} 
				catch (NotBoundException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void writeOperation(Object... ids) {
		while(true){
			int l=0;
			Integer key = (Integer) ids[0];
			IDistributedHashTable[] tables = new IDistributedHashTable[WRITE_RANGE];
			try {
				for(int i=0;i<WRITE_RANGE;i++){
					Integer nodeId = findNode(key+i);
					Address server1 = (Address) Network.getAddress(String.valueOf(nodeId));
					tables[i] = (IDistributedHashTable) LocateRegistry
					.getRegistry(server1.inetAddress.getHostAddress(),
							server1.port).lookup(
									DistributedHashTable.BINDING_KEY);
					tables[i].lock(key);
					l++;
				}
				
				for(int i=0;i<WRITE_RANGE;i++){
					Integer value = tables[i].get(key);
					value = (value == null) ? 1 : value + 1;
					tables[i].put(key, value);
				}
				edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
				for(int i=0;i<WRITE_RANGE;i++){
					tables[i].unlock(key);
				}
				
				break;
			} catch (AccessException e) {
				e.printStackTrace();
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (NotBoundException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				if (l > 0) {
					for (int j = 0; j < l; j++) {
						try {
							tables[j].unlock(key+j);
						} catch (RemoteException e1) {
							e1.printStackTrace();
						}
					}
				}
			} 
		}
	}

	@Override
	protected void checkSanity() {
		int sum = 0;
		IDistributedHashTable[] tables = new IDistributedHashTable[Network
				.getInstance().nodesCount()];
		try {
			for (int i = 0; i < Network.getInstance().nodesCount(); i++) {
				Integer nodeId = findNode(i);
				Address server1 = (Address) Network.getAddress(String
						.valueOf(nodeId));
				tables[i] = (IDistributedHashTable) LocateRegistry.getRegistry(
						server1.inetAddress.getHostAddress(), server1.port)
						.lookup(DistributedHashTable.BINDING_KEY);
			}
			
			for (int i = 0; i < Network.getInstance().nodesCount(); i++) {
				Integer value = tables[i].get(i);
				sum += value;
			}
			
		} catch (AccessException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
		if (sum == transactions * Network.getInstance().nodesCount())
			Logger.debug("Passed Sanity Check");
		else {
			Logger.debug("Failed Sanity Check" + "\nExpected = "
					+ transactions * Network.getInstance().nodesCount()
					+ "\nActual = " + sum);
		}

	}

}
