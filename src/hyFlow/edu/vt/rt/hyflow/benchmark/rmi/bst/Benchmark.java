package edu.vt.rt.hyflow.benchmark.rmi.bst;

import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

import edu.vt.rt.hyflow.util.network.Network;

public class Benchmark extends edu.vt.rt.hyflow.benchmark.rmi.Benchmark{

	BSTHandler bstHandler = new BSTHandler();

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
		if(Network.getInstance().getID()==0)
			try {
				bstHandler.createTree();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
	}

	@Override
	protected String getLabel() {
		return "BST-RMI";
	}

	@Override
	protected int getOperandsCount() {
		return 1;
	}

	@Override
	protected Object randomId() {
		return new Integer((int)(Math.random()*localObjectsCount));
	}

	@Override
	protected void readOperation(Object... ids) {
		try {
			bstHandler.find((Integer)ids[0]);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void writeOperation(Object... ids) {
		BSTHandler tree = bstHandler;
		if(Math.random()>0.5){
			System.out.println("[ADD]");
			try {
				tree.add((Integer)ids[0]);
				elementsSum += (Integer)ids[0];
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}else{
			System.out.println("[DEL]");
			try {
				if(tree.delete((Integer)ids[0]))
					elementsSum -= (Integer)ids[0];
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
//		try {
//			Thread.sleep(100);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
	}

	int elementsSum = 0;
	@Override
	protected void checkSanity() {
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		try {
			System.out.println("Sanity Check:" + ((Network.getInstance().getID()==0) ? bstHandler.sum() : "?") + "/" + elementsSum);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public static String getServerId(String id) {
		return id.split("-")[0];
	}
}
