package edu.vt.rt.hyflow.benchmark.tm.vacation;

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

public class $HY$_Proxy_Customer implements $HY$_ICustomer{

	private static final long serialVersionUID = 1L;
	DirectoryManager locator;
	
    public $HY$_Proxy_Customer() throws RemoteException{
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
		$HY$_ICustomer stub = ($HY$_ICustomer) UnicastRemoteObject.exportObject(this, 0);
		// Bind the remote object's stub in the registry
		registry.rebind(Customer.class.getName(), stub);
		Logger.debug("RMI stub inited");
		locator = HyFlow.getLocator();
    }

	@Override
	public void addReservation(Object id, ControlContext context, ReservationInfo reservation) throws RemoteException {
		Address caller = ((ControlContext)context).getLastExecuter();
		Network.linkDelay(true, caller);
		ControlContext.getNeighbors(context.getContextId()).add(caller);
		((Customer)locator.open(context, id, "w")).addReservation(reservation, context);
	}

	@Override
	public List<String> getReservations(Object id, ControlContext context) throws RemoteException {
		Address caller = ((ControlContext)context).getLastExecuter();
		Network.linkDelay(true, caller);
		ControlContext.getNeighbors(context.getContextId()).add(caller);
		return ((Customer)locator.open(context, id, "r")).getReservations(context);
	}

}
