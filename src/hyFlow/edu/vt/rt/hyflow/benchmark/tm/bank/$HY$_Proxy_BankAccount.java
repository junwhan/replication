package edu.vt.rt.hyflow.benchmark.tm.bank;

import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import aleph.comm.Address;
import aleph.dir.DirectoryManager;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.dir.control.ControlFlowDirectory;
import edu.vt.rt.hyflow.core.tm.control.ControlContext;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;

public class $HY$_Proxy_BankAccount extends UnicastRemoteObject
implements $HY$_IBankAccount{

	private static final long serialVersionUID = 1L;
	DirectoryManager locator;
	
    public $HY$_Proxy_BankAccount() throws RemoteException{
		Logger.debug("Creating PROXY");
		((ControlFlowDirectory)HyFlow.getLocator()).addProxy(this);
		// Install Secrutiy Manager
    	if (System.getSecurityManager() == null)
			System.setSecurityManager ( new RMISecurityManager() );
    	// Create objects registery
    	int port = Network.getInstance().getPort()+1000;
    	Registry registry = null;
		try {
			Logger.debug("Reg: " + port);
			registry = LocateRegistry.createRegistry(port);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
    	
		// Remove old registered object
		try {
			UnicastRemoteObject.unexportObject(this, true);
		} catch (Exception e) {
			Logger.error("RMI unexporting");
		}
		$HY$_IBankAccount stub = ($HY$_IBankAccount) UnicastRemoteObject.exportObject(this, 0);
		// Bind the remote object's stub in the registry
		registry.rebind(BankAccount.class.getName(), stub);
		Logger.debug("RMI stub inited");
		locator = HyFlow.getLocator();
    }


	@Override
	public Integer checkBalance(Object id, ControlContext context){
		Address caller = ((ControlContext)context).getLastExecuter();
		Network.linkDelay(true, caller);
		ControlContext.getNeighbors(context.getContextId()).add(caller);
		//return (int) ((BankAccount)locator.open(context, id, "r")).checkBalance();
		return (int) BankAccount.checkBalance((String) id);
	}


	@Override
	public boolean withdraw(Object id, ControlContext context, int dollars){
		Address caller = ((ControlContext)context).getLastExecuter();
		Network.linkDelay(true, caller);
		ControlContext.getNeighbors(context.getContextId()).add(caller);
		//return ((BankAccount)locator.open(context, id, "w")).withdraw(dollars);
		return BankAccount.withdraw((String) id, dollars);
	}


	@Override
	public void deposit(Object id, ControlContext context, int dollars){
		Address caller = ((ControlContext)context).getLastExecuter();
		Network.linkDelay(true, caller);
		ControlContext.getNeighbors(context.getContextId()).add(caller);
		//((BankAccount)locator.open(context, id, "w")).deposit(dollars);
		BankAccount.deposit((String) id, dollars);
	}


}