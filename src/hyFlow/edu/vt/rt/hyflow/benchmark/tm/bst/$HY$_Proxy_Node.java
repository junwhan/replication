package edu.vt.rt.hyflow.benchmark.tm.bst;

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

public class $HY$_Proxy_Node implements $HY$_INode{

	private static final long serialVersionUID = 1L;
	DirectoryManager locator;
	
    public $HY$_Proxy_Node() throws RemoteException{
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
		$HY$_INode stub = ($HY$_INode) UnicastRemoteObject.exportObject(this, 0);
		// Bind the remote object's stub in the registry
		registry.rebind(Node.class.getName(), stub);
		Logger.debug("RMI stub inited");
		locator = HyFlow.getLocator();
    }
    
	@Override
	public void setRightChild(Object id, ControlContext context, String rightId){
		Address caller = ((ControlContext)context).getLastExecuter();
		Network.linkDelay(true, caller);
		ControlContext.getNeighbors(context.getContextId()).add(caller);
		((Node)locator.open(context, id, "w")).setRightChild(rightId, context);
	}

	@Override
	public String getRightChild(Object id, ControlContext context){
		Address caller = ((ControlContext)context).getLastExecuter();
		Network.linkDelay(true, caller);
		ControlContext.getNeighbors(context.getContextId()).add(caller);
		return ((Node)locator.open(context, id, "r")).getRightChild(context);
	}

	@Override
	public void setLeftChild(Object id, ControlContext context, String leftId){
		Address caller = ((ControlContext)context).getLastExecuter();
		Network.linkDelay(true, caller);
		ControlContext.getNeighbors(context.getContextId()).add(caller);
		((Node)locator.open(context, id, "w")).setLeftChild(leftId, context);
	}

	@Override
	public String getLeftChild(Object id, ControlContext context){
		Address caller = ((ControlContext)context).getLastExecuter();
		Network.linkDelay(true, caller);
		ControlContext.getNeighbors(context.getContextId()).add(caller);
		return ((Node)locator.open(context, id, "r")).getLeftChild(context);
	}
	
	@Override
	public Integer getValue(Object id, ControlContext context){
		Address caller = ((ControlContext)context).getLastExecuter();
		Network.linkDelay(true, caller);
		ControlContext.getNeighbors(context.getContextId()).add(caller);
		return ((Node)locator.open(context, id, "r")).getValue(context); 
	}

}
