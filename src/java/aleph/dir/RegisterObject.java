package aleph.dir;

import java.util.concurrent.atomic.AtomicInteger;

import edu.vt.rt.hyflow.util.io.Logger;
import aleph.GlobalObject;

/**
 * Newly-created object.
 **/
public class RegisterObject extends aleph.Message {
	GlobalObject key;

	public RegisterObject(GlobalObject key) {
		key.incrementVersion();
		this.key = key;
	}

	public void run() {
		Logger.debug("Registering" + key + "=>" + ObjectsRegistery.regsiterObject(key));
	}

	public String toString() {
		return "RegisterObject[from: " + from + ", " + key + "]";
	}
}
