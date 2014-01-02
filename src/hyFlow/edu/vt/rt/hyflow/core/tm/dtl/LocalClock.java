package edu.vt.rt.hyflow.core.tm.dtl;

import java.util.concurrent.atomic.AtomicInteger;

import edu.vt.rt.hyflow.util.io.Logger;

/**
 * Local Clock for current node
 * 
 * @author Mohamed M. Saad
 */
public class LocalClock {
	final private static AtomicInteger clock = new AtomicInteger(1);

	public static int get() {
		return clock.get();
	}
	
	public static void advance(int n){
		Logger.debug("Advance clock to: <" + n + ">");
		clock.set(n);
	}

	public static int increment() {
		try {
			return clock.incrementAndGet();
		}finally{
			Logger.debug("Clock: <" + clock + ">");
		}
	}
	
}
