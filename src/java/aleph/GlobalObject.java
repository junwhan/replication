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

package aleph;

import aleph.dir.DirectoryManager;
import aleph.dir.ObjectsRegistery;
import aleph.dir.RegisterObject;
import edu.vt.rt.hyflow.util.io.Logger;


/**
 * Objects shared among PEs.
 *
 * @author  Maurice Herlihy
 * @date    March 1997
 **/

public class GlobalObject implements java.io.Serializable{

  private static transient final boolean DEBUG = false; // Lightning striking again and again and again ...
  static transient long uniqueId = 0;
  private static transient DirectoryManager dirManager = DirectoryManager.getManager();

  /**
   * Globally unique id
   **/
  private Object key;
  private PE   home;
  private Class<?> clazz;
  private int version;
  
  public Object getKey(){
	  return key;
  }

  public void incrementVersion(){
	  version++;
  }
  public int getVersion(){
	  return version;
  }

  private GlobalObject(Object object,  Object key, PE home, String hint){
	  	Logger.debug("Publish " + home + "-" + key);
	  	this.home = home;
	  	this.key = key;
	  	if(object!=null){
	  		clazz = object.getClass();
	  		dirManager.newObject(this, object, hint);
	  	}

	    ObjectsRegistery.regsiterObject(this);
	    if(object!=null)
	    	PE.thisPE().populate(new RegisterObject(this));	// REGISTER AT THE OTHER PEs
  }
  
  public void setHome(PE pe) {
	  home = pe;
  }
  
  /**
   * Create new global object.
   * @param object Global object's initial state.
   **/
  public GlobalObject (Object object) {
	  this(object, uniqueId++, PE.thisPE(), null);
  }

  public GlobalObject (Object object, Object key) {
	    this(object, key, PE.thisPE(), null);
  }
  
  /**
   * Create new global object.
   * @param object Global object's initial state.
   * @param hint <code>String</code> passed to transaction manager.
   **/
  public GlobalObject (Object object, String hint) {
	  this(object, uniqueId++, PE.thisPE(), hint);
  }
        
  /**
   * Applications should not use this Constructor.
   **/
  public GlobalObject() {}

  
  public Class<?> getObjectClass(){
	  return clazz;
  }

  /**
   * the PE at which the object was created
   * @return object's home PE
   **/
  public PE getHome() {
    return home;
  }

  /**
   * Always use equals to compare global objects.
   **/
  public boolean equals(Object anObject) {
    if ((anObject != null) && (anObject instanceof GlobalObject)) {
      GlobalObject other = (GlobalObject) anObject;
      boolean result = other.key.equals(this.key) && (home==null || other.home==null || (other.home.equals(this.home)));
      if(hashCode()==anObject.hashCode() && !result)
    	  Logger.debug("Comparing " + this + "/" + home + "/" + key + "  !=  " + other + "/" + other.home + "/" + other.key );
      return result;
    } else {
      return false;
    }
  }

  public int hashCode () {
    return key.hashCode();
  }

  public String toString () {
    return "GlobalObject" + "[" + home + "." + key + "]@" + version + "-" + clazz;
  }
}
