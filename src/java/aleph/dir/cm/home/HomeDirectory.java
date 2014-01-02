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

package aleph.dir.cm.home;


import aleph.Aleph;
import aleph.GlobalObject;
import aleph.PE;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.Context;

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

  public HomeDirectory () {
    clientTable = new HashMap(); // unsynchronized
    serverTable = Collections.synchronizedMap(new HashMap());
    Aleph.register("Home Directory", this);
  }

  /**
   * Open an object.
   * @param object      The object.
   * @param mode        E.g., Read, Write, Copy, etc.
   **/
  public Object open (AbstractContext context, GlobalObject object, String mode) {
    return getClientSide(object).open(context, mode);
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

  /**
   * Called when object no longer needed.
   * @param object Formerly interesting object.
   **/
  public void release (AbstractContext context, GlobalObject key) {
    if (DEBUG)
      Aleph.debug("HomeDirectory: releasing " + key);
    getClientSide(key).release(context);
  }

  /**
   * Called by RetrieveResponse message.
   * @param key global object's unique ID
   * @param object new value
   * @param mode in which object granted
   * @see aleph.dir.cm.home.RetrieveResponse
   **/
  public void retrieveResponse (GlobalObject key, Object object, int mode) {
    getClientSide(key).retrieveResponse(object, mode);
  }

  /**
   * Called by RetrieveRequest message to acquire new object.
   * @param key global object's unique ID
   * @param object new value
   * @param mode in which object granted
   * @see aleph.dir.cm.home.RetrieveRequest
   **/
  public void retrieveRequest (GlobalObject key, PE from, AbstractContext context, int mode) {
    getServerSide(key).retrieveRequest(from, context, mode);
  }

  /**
   * Called by ReleaseRequest message.
   * @param from request originated here
 * @param context 
   * @param key object in question
   * @see aleph.dir.cm.home.RetrieveRequest
   **/
  public void releaseRequest (PE from, AbstractContext context, GlobalObject key) {
    getClientSide(key).releaseRequest(from, context);
  }

  /**
   * Called by ReleaseResponse message.
   * @param key object in question
   * @param object actual data
   * @param from request originated here
   * @see aleph.dir.cm.home.ReleaseResponse
   **/
  public void releaseResponse (GlobalObject key, Object object, PE from, boolean deny) {
    getServerSide(key).releaseResponse(object, from, deny);
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

  
  
