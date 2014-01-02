package aleph.dir;

import edu.vt.rt.hyflow.util.io.Logger;
import aleph.GlobalObject;

/**
 * Newly-created object.
 **/
public class UnregisterObject extends aleph.Message{
	 GlobalObject key;
 public UnregisterObject (GlobalObject key) {
		 this.key = key;
	 }
  public void run () {
  	Logger.debug("Unregistering" + key);
  	ObjectsRegistery.unregsiterObject(key);
  }
  public String toString () {
    return "UnregisterObject[from: " + from + ", "  + key + "]";
  }
}
