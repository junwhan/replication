/*
 * Aleph Toolkit
 *
 * Copyright 1997, Brown University, Providence, RI.
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

package aleph.dir;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.Context;
import org.deuce.transaction.ContextDelegator;
import java.util.LinkedList;
import aleph.Aleph;
import aleph.GlobalObject;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.HyFlow.requester;
import edu.vt.rt.hyflow.benchmark.tm.list.Node;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.util.io.Logger;

/**
 * Abstract Class that manages cached copies of GlobalObjects. Any class
 * extending this one must provide a no-arg constructor.
 *
 * @see aleph.GlobalObject
 *
 * @author Maurice Herlihy
 * @date   August 1998
 **/

public abstract class DirectoryManager {

  protected static DirectoryManager theManager = null; // singleton
  public static DirectoryManager getManager () {
    try {
      if (theManager == null) {
        String theClass = Aleph.getProperty("aleph.directoryManager");
        Logger.debug("Using DM: " + theClass);
        theManager =
          (DirectoryManager) Class.forName(theClass).newInstance();
      }
      return theManager;
    } catch (Exception e) {
      Aleph.panic(e);
    }
    return theManager;
  }
 
  /**
   * Change directory managers in mid-stream.
   * Applications should probably not call this method.
   **/
  public static void setManager (String newManager) {
    try {
      theManager =              // install the new one
        (DirectoryManager) Class.forName(newManager).newInstance();
      Aleph.setProperty("aleph.directoryManager", newManager);
    } catch  (ClassNotFoundException e) {
      System.err.println("Cannot find directory manager " + newManager);
      Aleph.exit(-1);
    } catch  (Exception e) {
      Aleph.panic(e);
    }
  }

  /**
   * Register newly-created global object.
   * @param key    Global object ID
   * @param object Initial state for global object.
   **/
  public void newObject (GlobalObject key, Object object) {
    newObject(key, object, null);
  }

  /**
   * Register newly-created global object.
   * @param key    Global object ID
   * @param object Initial state for global object.
   * @param hint   <code>String</code> passed to directory
   **/
  public abstract void newObject (GlobalObject key, Object object, String hint);

  /**
   * Open a global object in desired mode.  All transaction managers should
   * support at least modes "w" (write) and "r" (read) Unrecognized modes
   * should be treated like "w".  Modes are case insensitive.
   * @param key The object to open.
   * @param mode   Mode in which to open object.
   **/
  public Object open (Object key){
	  return this.open(key, "w");
  }
  public Object open (Object key, String mode){
	  return open (ContextDelegator.getTopInstance(), key, mode, new int [1]);
  }
  
  public Object open (Object key, String mode, int [] commute){
	  return open (ContextDelegator.getTopInstance(), key, mode, commute);
  }	
  
  public Object open (AbstractContext context, Object key, String mode){
	  GlobalObject globalKey = ObjectsRegistery.getKey(key);
	  if(globalKey==null){
		  Logger.debug("Key not found!! " + key);
//		  ObjectsRegistery.dump();
//		  Logger.info("[" + ObjectsRegistery.getKey(key) + "]");
		  throw new NotRegisteredKeyException(key);
	  }
	  Logger.debug("Key found " + key);
	  Object object = this.open(context, globalKey, mode, 0);
	  HyFlow.getConflictManager().open(context);
	  return object;
  }
  
  public Object open (AbstractContext context, Object key, String mode, int [] commute){
	  GlobalObject globalKey = ObjectsRegistery.getKey(key);
	  if(globalKey==null){
		  Logger.debug("Key not found!! " + key);
//		  ObjectsRegistery.dump();
//		  Logger.info("[" + ObjectsRegistery.getKey(key) + "]");
		  throw new NotRegisteredKeyException(key);
	  }

	  Object object = this.open(context, globalKey, mode, commute);
	  HyFlow.getConflictManager().open(context);
	  return object;
  }
  //protected abstract Object open (AbstractContext context, GlobalObject object, String mode);
  protected abstract Object open (AbstractContext context, GlobalObject object, String mode, int commute);
  /**
   * Called when object no longer needed.
   * @param object Formerly interesting object.
   **/
  public void release (AbstractDistinguishable object){}
  
  public void release (AbstractContext context, AbstractDistinguishable object){
	  if(object!=null)
		  this.release(ContextDelegator.getTopInstance(), ObjectsRegistery.getKey(object.getId()));
  }
  protected abstract void release (AbstractContext context, GlobalObject object);

  /**
   * @return who we are
   **/
  public abstract String getLabel ();

  public void delete(AbstractDistinguishable deleted) {
	  ContextDelegator.getTopInstance().delete(deleted);
  }
  
  public void unregister(GlobalObject key){
	  ObjectsRegistery.unregsiterObject(key);
  }


  public void unregister(AbstractDistinguishable object){
  }
  public void register(AbstractDistinguishable object) {
		new GlobalObject(object, object.getId());	// publish it now
  }


}
