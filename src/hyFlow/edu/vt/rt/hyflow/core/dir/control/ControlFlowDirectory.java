package edu.vt.rt.hyflow.core.dir.control;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.deuce.transaction.AbstractContext;

import aleph.GlobalObject;
import aleph.comm.tcp.Address;
import aleph.dir.DirectoryManager;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.IDistinguishable;
import edu.vt.rt.hyflow.core.dir.IHyFlow;
import edu.vt.rt.hyflow.core.tm.control.ControlContext;
import edu.vt.rt.hyflow.core.tm.undoLog.AbstractLoggableObject;
import edu.vt.rt.hyflow.util.io.Logger;

public class ControlFlowDirectory extends DirectoryManager{

	Map<GlobalObject, Object> local = new HashMap<GlobalObject, Object>();
	List<Object> proxy = new LinkedList<Object>();
	
	@Override
	public String getLabel() {
		return "ControlFlow";
	}

	@Override
	public void newObject(GlobalObject key, Object object, String hint) {
		local.put(key, object);		
//		Logger.info("Object regsitered locally " + key);
	}
	
	public void addProxy(Object p){
		proxy.add(p);
	}

	@Override
	public Object open(AbstractContext context, GlobalObject key, String mode, int commute) {
		Object object = local.get(key);
		if(object!=null)
			return object;

		Logger.debug(context + ": Open Remotly " + key);
		Class<?> clazz = key.getObjectClass();
		try {
			object = clazz.newInstance();
			Address address = (Address)key.getHome().getAddress();
			((ControlContext)context).addNeighbor(address);
			((IHyFlow)object).setRemote(key.getKey(), address.inetAddress.getHostAddress(), address.port + 1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return object;
	}

	@Override
	public void release(AbstractContext context, AbstractDistinguishable object) {
//		super.release(object);
		if(object instanceof AbstractLoggableObject)
			((AbstractLoggableObject)object).release(context);
	}
	
	@Override
	public void release(AbstractContext context, GlobalObject object) {
		
	}

}
