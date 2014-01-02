package edu.vt.rt.hyflow.benchmark.tm.loan;

import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

import aleph.comm.Address;
import aleph.dir.DirectoryManager;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.dir.control.ControlFlowDirectory;
import edu.vt.rt.hyflow.core.tm.control.ControlContext;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;

public class $HY$_Proxy_LoanAccount extends UnicastRemoteObject
implements $HY$_ILoanAccount{

	private static final long serialVersionUID = 1L;
	DirectoryManager locator;
	
    public $HY$_Proxy_LoanAccount() throws RemoteException{
		Logger.debug("Creating PROXY");
		((ControlFlowDirectory)HyFlow.getLocator()).addProxy(this);
		// Install Secrutiy Manager
    	if (System.getSecurityManager() == null)
			System.setSecurityManager ( new RMISecurityManager() );
    	// Create objects registery
    	int port = Network.getInstance().getPort()+1000;
    	Registry registry = null;
		try {
//			Logger.info("Reg: " + port);
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
		$HY$_ILoanAccount stub = ($HY$_ILoanAccount) UnicastRemoteObject.exportObject(this, 0);
		// Bind the remote object's stub in the registry
		registry.rebind(LoanAccount.class.getName(), stub);
//		Logger.info("RMI stub inited");
		locator = HyFlow.getLocator();
    }

	@Override
	public void borrow(Object id, ControlContext context,
			List<String> accountNums, int branching, boolean initiator,
			int amount){
		Address caller = ((ControlContext)context).getLastExecuter();
		Network.linkDelay(true, caller);
		ControlContext.getNeighbors(context.getContextId()).add(caller);
		((LoanAccount)locator.open(context, id, "w")).borrow(accountNums, branching, initiator, amount, context);		
	}

	@Override
	public int sum(Object id, ControlContext context,
			List<String> accountNums, int branching){
		Address caller = ((ControlContext)context).getLastExecuter();
		Network.linkDelay(true, caller);
		ControlContext.getNeighbors(context.getContextId()).add(caller);
		return ((LoanAccount)locator.open(context, id, "r")).sum(accountNums, branching, context);
	}

	@Override
	public Integer checkBalance(Object id, ControlContext context){
		Address caller = ((ControlContext)context).getLastExecuter();
		Network.linkDelay(true, caller);
		ControlContext.getNeighbors(context.getContextId()).add(caller);
		return ((LoanAccount)locator.open(context, id, "r")).checkBalance(context);
	}
}