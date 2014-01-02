package aleph.dir;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.Map;

import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.IDistinguishable;
import edu.vt.rt.hyflow.core.dir.dtl2.DTL2Directory;
import edu.vt.rt.hyflow.core.tm.dtl2.LockTable;
import edu.vt.rt.hyflow.util.io.Logger;

import aleph.GlobalObject;

public class ObjectsRegistery {

	private static Map<Object, GlobalObject> registery = new ConcurrentHashMap<Object, GlobalObject>();
	
	public static boolean regsiterObject(GlobalObject globalKey){
		Object key = globalKey.getKey();
		while(true){
			GlobalObject oldGlobalKey = registery.get(key);
			Logger.debug("ObjectsRegistery.registerObject: oldGlobalKey = "+oldGlobalKey);
			if(oldGlobalKey==null) {
				synchronized (registery) {
					if(registery.containsKey(key)) {
						continue;	// concurrent insert, retry
					}
					registery.put(key, globalKey);	// update the key
					break;
				}
			}
			else {
				synchronized (oldGlobalKey) {
					if(!registery.containsValue(oldGlobalKey)) {
						continue;	// the other thread retires
					}
					oldGlobalKey = registery.get(key);
					if(oldGlobalKey.getVersion()>=globalKey.getVersion()) {
						return false;	// discard old populated messages
					}
					registery.put(key, globalKey);	// update the key
					// Invalidate existing object, if any
					// TODO: what if not using DTL2
					final DTL2Directory dtl2mgr = (DTL2Directory)DirectoryManager.getManager();
					final Object val = dtl2mgr.getLocalObject(oldGlobalKey);
					if (val != null && val instanceof AbstractDistinguishable) {
						  ((IDistinguishable)val).invalidate();
						  LockTable.reset(val);
					}
					break;
				}
			}
		}
		return true;
	}
	
	public static void unregsiterObject(GlobalObject key){
		registery.remove(key.getKey());
	}

	public static void dump(){
		Logger.debug(Arrays.toString(((HashMap)registery).keySet().toArray()));
	}
	
	public static GlobalObject getKey(Object key){
		return registery.get(key);
	}
}
