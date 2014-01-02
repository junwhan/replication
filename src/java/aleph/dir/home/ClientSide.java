/*
 * Aleph Toolkit
 *
 * Copyright 1999, Brown University, Providence, RI.
 * 
 *                         All Rights Reserved
 * 
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose other than its incorporation into a
 * commercial product is hereby granted without fee, provided that the
 * above copyright notice appear in all copies and that both that
 * copyright notice and this permission notice appear in supporting
 * documentation, and that the name of Brown University not be used in
 * advertising or publicity pertaining to distribution of the software
 * without specific, written prior permission.
 * 
 * BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE,
 * INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR ANY
 * PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY BE LIABLE FOR
 * ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package aleph.dir.home;


import edu.vt.rt.hyflow.benchmark.Benchmark;
import edu.vt.rt.hyflow.util.io.Logger;
import aleph.Aleph;
import aleph.GlobalObject;
import aleph.PE;
/**
 * Home based directory manager<br>
 * Keeps track of locally-cached global objects.
 *
 * @author Mike Demmer
 * @author Maurice Herlihy
 * @date   May 1998
 **/
public class ClientSide implements Constants {

  private static final boolean DEBUG = false; // BANG-BANG Maxwell's silver hammer ...

  
  private GlobalObject key;
  private Object       object;

  // local synchronization state
  private boolean readValid  = false; // May I read this object?
  private boolean writeValid = false; // May I write this object?
  private boolean pending    = false; // Have I asked server for this object?
  private boolean writer     = false; // Does a local thread hold a write lock?
  private int     readers    = 0;     // How many local threads hold read locks?
  private boolean requested  = false; // Does Server want object back?
  
  private boolean timeout;

  static boolean warning = true; // Whine about unfamiliar access modes?

  /**
   * Constructor: Object state unknown.
   **/
  public ClientSide(GlobalObject key) {
    this.key = key;
  }

  /**
   * Constructor: Object state known.
   **/
  public ClientSide(GlobalObject key, Object object) {
    this.key    = key;
    this.object = object;
  }

  public synchronized Object open (String modeName) {
	  timeout = false;
		if (DEBUG)
			Aleph.debug(this + " open(" + modeName + ") called");
		try {
			int mode = parseMode(modeName);
			switch (mode) {
			case READ_MODE:
				while (writer || !readValid) { // Get shared access to object
					if (!readValid && !pending) { // Ask for object if absent.
						Logger.debug("Sending retrieve request for " + key );
						RetrieveRequest retrieve = new RetrieveRequest(key,
								mode);
						if (DEBUG)
							Aleph.debug(this + " sends " + retrieve + " to "
									+ key.getHome());
						retrieve.send(key.getHome());
						pending = true;
					}
					if(HomeDirectory.enableTimeout){
						long start = System.currentTimeMillis();
						long timeoutPeriod = Benchmark.timout();
						if(timeoutPeriod==0)
							throw new TimeoutException();
						try {
							wait(timeoutPeriod);
						} catch (InterruptedException e) {
						}
						timeout = (System.currentTimeMillis() - start > timeoutPeriod);
						if (timeout)
							throw new TimeoutException();
					}
					else
						try {
							wait();
						} catch (InterruptedException e) {
						}
				}
				readers++;
				break;
			case WRITE_MODE:
				// Get exclusive access.
				while (writer || readers > 0 || !writeValid) {
					// Ask for object if absent.
					if (!writeValid && !pending) {
						Logger.debug("Sending retrieve request for " + key);
						RetrieveRequest retrieve = new RetrieveRequest(key,
								mode);
						if (DEBUG)
							Aleph.debug(this + " sends " + retrieve + " to "
									+ key.getHome());
						retrieve.send(key.getHome());
						pending = true;
					}
					if(HomeDirectory.enableTimeout){
						long start = System.currentTimeMillis();
						long timeoutPeriod = Benchmark.timout();
						if(timeoutPeriod==0)
							throw new TimeoutException();
						try {
							wait(timeoutPeriod);
						} catch (InterruptedException e) {
						}
						timeout = (System.currentTimeMillis() - start > timeoutPeriod);
						if (timeout) 
							throw new TimeoutException();
					}else
						try {
							wait();
						} catch (InterruptedException e) {
						}
				}
				writer = true;
				break;
			default:
				Aleph.panic("ClientSide.open: unknown mode " + mode);
			}
			if (DEBUG)
				Aleph.debug(this + " open(" + modeName + ") returns "
						+ this.object);
		} catch (TimeoutException e) {
			throw e;
		} catch (Exception e) {
			Aleph.panic(e);
		}
		return this.object;
  }

  public synchronized void release () {
    if (DEBUG)
      Aleph.debug(this + " release() called");

    if (writer) {
      writer = false;
      notifyAll();
    }
    if (readers > 0)
      if (--readers == 0)
	notifyAll();
    if (DEBUG)
      Aleph.debug(this + " release() returned");
//    Logger.debug("Send release for server for " + key);
  }
  
  /**
   * Server delivers requested object.<br>
   * Called by retrieveResponse message.
   * @param from Requesting PE
   * @see aleph.trans.RetrieveResponse
   **/
  public synchronized void retrieveResponse (Object object, int mode) {
	  Logger.debug("Received " + key + " while " + timeout + " for " + this);

	  if (DEBUG)
		  Aleph.debug(this + " RetrieveResponse(" +
				  object + ", " + printMode(mode) + ") called");
	    switch (mode) {
	    case READ_MODE:
	      readValid = true;
	      break;
	    case WRITE_MODE:
	      writeValid = readValid = true;
	      break;
	    }
	
    this.object  = object;
    this.pending = false;
    this.notifyAll();
    if (DEBUG)
      Aleph.debug(this + " RetrieveResponse(" +
			 object + ", " + printMode(mode) + ") returns");
  }


  /**
   * Server asks for object back.<br>
   * Called by releaseRequest message.
   * @param from Server PE
   * @param mode Mode of interenst.
   * @see aleph.dir.home.ReleaseRequest
   **/
  public synchronized void releaseRequest(PE from) {
    try {
    	Logger.debug("Invalidating " + key);
      requested = true;
      if (DEBUG)
	Aleph.debug(this + " ReleaseRequest(" + from + ") called");
      while (writer || readers > 0) {
	try{ wait(); } catch (InterruptedException e) {};
      }
      requested = false;
      ReleaseResponse release = new ReleaseResponse(key, (writeValid ? object : null));
      if (DEBUG)
	Aleph.debug(this + " sends " + release + " to " + from);
      release.send(from);
      writeValid = false;
      readValid  = false;
      object     = null;
      notifyAll();
    } catch (Exception e) {
      Aleph.panic(e);
    }
    if (DEBUG)
      Aleph.debug(this + " ReleaseRequest(" + from + ") returns");
  }

  public String toString() {
    return super.toString() + "[" +
      (requested ? "requested|" : "") +
      (writeValid ? "writeValid|" : "") +
      (!writeValid && readValid ? "readValid|" : "") +
      (writer ? "Writer|" : "NoWriter|") +
      "Readers(" + readers + ")|" +
      (pending ? "Pending|" : "") +
      object + "]";
  }

  private static final int parseMode (String mode) {
    if (mode.equalsIgnoreCase("r"))
      return READ_MODE;
    if (mode.equalsIgnoreCase("w"))
      return WRITE_MODE;
    if (warning && Aleph.verbosity(aleph.Constants.BLOVIATING)) {
      Aleph.debug("WARNING: Unknown access mode `" + mode +
			 "' treated as WRITE (no further warnings)");
      warning = false;
    }
    // If I don't understand it, it must mean write.
    return WRITE_MODE;
  }

  private static final String printMode(int mode) {
    switch (mode) {
    case READ_MODE:  return "r";
    case WRITE_MODE: return "w";
    default: return "?";
    }
  }
  
}
