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


import aleph.Aleph;
import aleph.GlobalObject;
import aleph.PE;
import aleph.dir.ObjectsRegistery;
import aleph.dir.UnregisterObject;

import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.deuce.transaction.AbstractContext;

import edu.vt.rt.hyflow.core.AbstractDistinguishable;

/**
 * Home Based Directory implementation.
 *
 * @author Mike Demmer
 * @author Maurice Herlihy
 * @date May 1998
 */
public abstract class HomeDirectory extends aleph.dir.DirectoryManager {

  static final boolean DEBUG = false; // Top of the world, Ma!

  private static Map clientTable, serverTable;
  static boolean enableTimeout = true;

  public HomeDirectory () {
    clientTable = new HashMap(); // unsynchronized
    serverTable = Collections.synchronizedMap(new HashMap());
    Aleph.register("Home Directory", this);
  }
  
  public static void setTimeOut(boolean enabled){
	  enableTimeout = enabled;
  }

  @Override
  public void delete(AbstractDistinguishable deleted) {
	  // no need for context delegator in DSM model
	  GlobalObject key = ObjectsRegistery.getKey(deleted.getId());
	  unregister(key);
	  PE.thisPE().populate(new UnregisterObject(key));	// unregister this object from other nodes
  }
  
  @Override
	public void unregister(GlobalObject key) {
		super.unregister(key);
		clientTable.remove(key);
	}
  
  /**
   * Open an object.
   * @param object      The object.
   * @param mode        E.g., Read, Write, Copy, etc.
   **/
  public Object open (AbstractContext context, GlobalObject object, String mode) {
    return getClientSide(object).open(mode);
  }

  /**
   * Register newly-imported global object.
   * Has no effect if object already registered.
   * @param key    Global object
   **/
  public void importObject (GlobalObject key) {}

  /**
   * Register newly-created global object.
   * @param key    Global object ID
   * @param object Initial state for global object.
   * @param hint   currently ignored
   **/
  public void newObject (GlobalObject key, Object object, String hint) {
    if (DEBUG)
      Aleph.debug("HomeDirectory: registering " + key + " " + object);
    serverTable.put(key, new ServerSide(key, object));
  }

  @Override
	public void release(AbstractDistinguishable object) {
		release(null, object);
	}
  
  /**
   * Called when object no longer needed.
   * @param object Formerly interesting object.
   **/
  public void release (AbstractContext context, GlobalObject key) {
    if (DEBUG)
      Aleph.debug("HomeDirectory: releasing " + key);
    getClientSide(key).release();
  }

  /**
   * Called by RetrieveResponse message.
   * @param key global object's unique ID
   * @param object new value
   * @param mode in which object granted
   * @see aleph.dir.home.RetrieveResponse
   **/
  public void retrieveResponse (GlobalObject key, Object object, int mode) {
    getClientSide(key).retrieveResponse(object, mode);
  }

  /**
   * Called by RetrieveRequest message to acquire new object.
   * @param key global object's unique ID
   * @param object new value
   * @param mode in which object granted
   * @see aleph.dir.home.RetrieveRequest
   **/
  public void retrieveRequest (GlobalObject key, PE from, int mode) {
    getServerSide(key).retrieveRequest(from, mode);
  }

  /**
   * Called by ReleaseRequest message.
   * @param from request originated here
   * @param key object in question
   * @see aleph.dir.home.RetrieveRequest
   **/
  public void releaseRequest (PE from, GlobalObject key) {
    getClientSide(key).releaseRequest(from);
  }

  /**
   * Called by ReleaseResponse message.
   * @param key object in question
   * @param object actual data
   * @param from request originated here
   * @see aleph.dir.home.ReleaseResponse
   **/
  public void releaseResponse (GlobalObject key, Object object, PE from) {
    getServerSide(key).releaseResponse(object, from);
  }

  private static synchronized ClientSide getClientSide (GlobalObject key) {
    try {
      ClientSide clientSide = (ClientSide) clientTable.get(key);
      if (clientSide == null) {
        clientSide = new ClientSide(key);
        clientTable.put(key, clientSide);
      }
      return clientSide;
    } catch (Exception e) {
      Aleph.panic(e);
      return null;		// never reached
    }
  }
  private static ServerSide getServerSide (GlobalObject key) {
    try {
      ServerSide serverSide = (ServerSide) serverTable.get(key);
      if (serverSide == null) {
        Aleph.panic("HomeDirectoryManager: missing server side " + key);
      }
      return serverSide;
    } catch (Exception e) {
      Aleph.panic(e);
      return null;		// never reached
    }
  }

  /**
   * @return who we are
   **/
  public String getLabel () {
    return "Home";
  }

  /**
   * Produce a human-readable snapshot of all global objects.  Very helpful in
   * a panic.
   **/
  public String toString() {
    return "ServerSide " + serverTable + "\nClientSide " + clientTable;
  }

}

  
  
