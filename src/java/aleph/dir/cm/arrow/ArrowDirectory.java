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

package aleph.dir.cm.arrow;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.TransactionException;
import org.deuce.transaction.AbstractContext.STATUS;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.util.io.Logger;

import aleph.Aleph;
import aleph.GlobalObject;
import aleph.PE;
import aleph.dir.DirectoryManager;

/**
 * Directory protocol based on path reversal on a fixed spanning tree.  See
 * M.P. Herlihy and M. Demmer.  The Arrow Directory Protocol.  in <i>12th
 * International Symposium on Distributed Computing</i>, September 1998, Andros
 * Island, Greece.<p>
 *
 * The PEs in a PGroup are organized as a binary tree based on index.  (This
 * protocol does not currently work across PEGroups!) The first time an object
 * is referenced, a <code>FindMessage</code> is sent to the object's home.
 *
 * @author Maurice Herlihy
 * @date   August 1998
 **/

public abstract  class ArrowDirectory extends DirectoryManager {

  private static ArrowDirectory theManager; // convenient access for messages

  // id -> object status
  private Hashtable arrow  = new Hashtable();

  private static final boolean DEBUG = Boolean.getBoolean("verbose");

  private static final long TIMEOUT = 300;
//  public static int timeout;

  private static final double RANDOM_TIMEOUT = 100;

  /**
   * Constructor
   **/
  public ArrowDirectory() {
    theManager = this;
    Aleph.register("Arrow Directory", this);
    if (DEBUG)
      Aleph.debug(this.toString());
  }

  /**
   * @see aleph.dir.DirectoryManager#newObject 
   * @param key    Global object
   * @param object Initial state for global object.
   * @param hint   <code>String</code> passed to directory
   **/
  public synchronized void newObject (GlobalObject key, Object object, String hint) {
    Status status = new Status(PE.thisPE());
    status.busy   = false;
    status.object = object;
    arrow.put(key, status);
  }

  /**
   * @see aleph.dir.DirectoryManager#importObject
   * @param key    Global object
   **/
  public void importObject (GlobalObject key) {}

  /**
   * @see aleph.dir.DirectoryManager#open
   * @param object The object to open.
   * @param mode   Mode in which to open object.
   **/
  public synchronized Object open (AbstractContext context, GlobalObject key, String mode) {
	Logger.debug("TryOpen " + key + " for context " + context);
    Status status  = (Status) arrow.get(key);
    PE     thisPE  = PE.thisPE();
    int    myIndex = thisPE.getIndex();
    try {
      if (status == null)  {      // object is unknown
        status = new Status(thisPE);
        arrow.put(key, status);   // legalize it
        FindMessage find = new FindMessage(context, key);
        if (DEBUG) {
          Aleph.debug("open: home is " + key.getHome());
          Aleph.debug("open: find is " + find);
          Aleph.debug("open: route is " + route(key.getHome()));
        }
        find.send(route(key.getHome()));
      } else if (
    		  !status.direction.equals(thisPE) &&
//    		  (status.resendFind || 
    				  status.localPending.isEmpty() && ! status.busy
//    				  )
    	  ) { // known elsewhere
//    	status.resendFind = false;
        FindMessage find = new FindMessage(context, key);
        find.send(status.direction);
        if (DEBUG)
          Aleph.debug("open sending " + find + " to " + status.direction);
        status.direction = thisPE;  // flip arrow
      }
      
      /* wait for the object to appear */
      status.localPending.add(context);
      if (DEBUG)
        Aleph.debug("open waits, status = " + status);
      
      try {
	      while (status.object == null || status.busy) {
	    	  if(status.object != null ){
	    		  Logger.debug("Solving contnetion " + context + " " + status.owner + " / Busy:" + status.busy);
		    	  //Local Contention
		    	  int res = HyFlow.getConflictManager().resolve(context, status.owner);
				  if(res==0){
					  status.owner = null;
					  status.busy = false;
					  Logger.debug("Owner of " + key + " aborted " + status.owner);
				  }else if(res > 0)
					try {
						Logger.debug("Backoff for " + context);
						wait(res);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				  continue;
	    	  }
	    	  Logger.debug("Waiting for remote " + key + " context " + context);
	    	  
	//    	  if(status.object == null)
	//    		  Logger.debug("Context " + context + " waiting for finding object " + key);
	//    	  if(status.busy)
	//    		  Logger.debug("Context " + context + " waiting for busy object " + key);
	        try { 
	        	long start = System.currentTimeMillis();  
	            wait( TIMEOUT + (int)(Math.random()*RANDOM_TIMEOUT) );
	            if(context.status.equals(STATUS.ABORTED)){
	            	Logger.debug("Transaction meanwhile Aborted :" + context);
	            	while(status.object==null){
	            		Logger.debug("Wait Again for" + key);
	            		wait();	// wait again
	            	}
	            	release((AbstractDistinguishable)status.object);
	            	throw new TransactionException();
	            }
	            if ( ( System.currentTimeMillis() - start ) >= TIMEOUT ){
	            	context.rollback();	// release other objects
	            	Logger.debug("Transaction Timeout :" + context);
	            	while(status.object==null){
	            		Logger.debug("Wait Again for" + key);
	            		wait();	// wait again
	            	}
	            	release((AbstractDistinguishable)status.object);
	            	throw new TransactionException();
	            }
	        } catch (InterruptedException e) {}
	        
	        Logger.debug("Wakeup for remote " + key + " context " + context);
	      }
	      status.busy = true;
	      status.owner = context;
      } finally {
    	  status.localPending.remove(context);
      }
      if (DEBUG)
        Aleph.debug("open returns, status = " + status);
    } catch (IOException e) {
      Aleph.panic(e);
    }
    Logger.debug("Open " + key + " for context " + context);
    return status.object;
  }

  /**
   * @see aleph.dir.DirectoryManager#release
   * @param object Formerly interesting object.
   **/
  public synchronized void release (AbstractContext context, GlobalObject object){
	  Logger.debug("TryRelease :" + context);
    try {
      Status status = (Status) arrow.get(object);
      if (DEBUG)
        Aleph.debug("release called\t" + status);
      status.busy = false;
      status.owner = null;
      
      if(status.requester != null){
    	  List<AbstractContext> aborted = new LinkedList<AbstractContext>();
    	  for(Iterator<AbstractContext> itr=status.localPending.iterator(); itr.hasNext(); )
    		  try {
    			  AbstractContext local = itr.next();
    			  if(HyFlow.getConflictManager().resolve(status.pending, local) == 0){
    				  Logger.debug("Avoiding repeated TIMEOUT! " + local + " aborted");
    				  aborted.add(local);
    			  }
			  } catch (TransactionException e) {
			  }
		  if(!aborted.isEmpty())
			  status.localPending.removeAll(aborted);
      }
      
      if (!status.localPending.isEmpty())       // local suitors
        notifyAll();              // wake up and smell the coffee
      else
        if (status.requester != null) { // remote suitor
          if (status.object == null)
            Aleph.panic("null object" + status); // debugging
          (new GrantMessage(object, status.object)).send(status.requester);
          Logger.debug("Object " + object + " was sent to " + status.requester);
          status.object  = null;
          status.requester = null;
          status.pending = null;
        }
      if (DEBUG)
        Aleph.debug("release returns\t" + status);
    } catch (IOException e) {
      Aleph.panic(e);
    }
    Logger.debug("Release :" + context);
  }

  /**
   * @return who we are
   **/
  public String getLabel () {
    return "Arrow";
  }

  /* PEs are arranged into a binary tree by index */
  private static int parent (int i) {	// parent's index
    return (i - 1) >> 1;
  }
  private static int left (int i) {	// left child's index
    return (i << 1) + 1;
  }
  private static int right (int i) {	// right child's index
    return (i + 1) << 1;
  }
  private static int left (int i, int d) { // left grandchild's index
    if (d == 1)
      return left(i);
    else
      return left(left(i, d-1));
  }
  private static int right (int i, int d) { // right grandchild's index
    if (d == 1)
      return right(i);
    else
      return right(right(i, d-1));
  }

  private PE parentPE () {
    int myIndex = PE.thisPE().getIndex();
    if (DEBUG)
      Aleph.debug("parentPE: " + myIndex + " parent " + parent(myIndex));
    if (myIndex > 0)
      return PE.getPE(parent(myIndex));
    else
      return null;
  }

  private PE leftPE () {
    int myIndex = PE.thisPE().getIndex();
    if (DEBUG)
      Aleph.debug("leftPE: " + myIndex + " left " + left(myIndex));
    int i = left(myIndex);
    if (i < PE.numPEs())
      return PE.getPE(i);
    else
      return null;
  }

  private PE rightPE () {
    int myIndex = PE.thisPE().getIndex();
    int i = right(myIndex);
    if (DEBUG)
      Aleph.debug("rightPE: myIndex=" + myIndex + ", right=" + i);
    if (i < PE.numPEs())
      return PE.getPE(i);
    else
      return null;
  }

  private static int depth(int i) {	// depth of node in three
    int d = 0;
    int j = i + 1;
    while (j > 0) {
      j = (j >> 1);
      d++;
    }
    return d;
  }

  private static boolean isDescendant(int i0, int i1) {
    if (i0 == i1)
      return true;
    int d0 = depth(i0);
    int d1 = depth(i1);
    if (d0 >= d1)
      return false;
    int e = (d1 - d0);		// depth difference
    int lb = left(i0, e);	// left grandchild
    int rb = right(i0, e);	// right grandchild
    return (lb <= i1) && (i1 <= rb);
  }

  public String toString() {
    StringBuffer s = new StringBuffer("neighbors{");
    s.append(parentPE());
    s.append(" ");
    s.append(leftPE());
    s.append(" ");
    s.append(rightPE());
    s.append("}");
    return s.toString();
  }

  /**
   * Route message through tree for specific pe
   * @param pe destination pe
   * @return which way to send the message
   **/
  private PE route (PE pe) {
    int myIndex = PE.thisPE().getIndex();
    if (DEBUG) {
      Aleph.debug("route(" + pe + ") from " + PE.thisPE());
      Aleph.debug("left(" + myIndex + ") = " + left(myIndex));
      Aleph.debug("right(" + myIndex + ") = " + right(myIndex));
    }

    int i = pe.getIndex();
    if (DEBUG)
      Aleph.debug("route: i = " + i + " myIndex = " + myIndex);
    if (isDescendant(myIndex,i)) {
      if (isDescendant(left(myIndex), i)) {
	if (DEBUG)
	  Aleph.debug("route returns leftPE " + leftPE());
	return leftPE();
      } else {
	if (DEBUG)
	  Aleph.debug("route returns rightPE " + rightPE());
	return rightPE();
      }
    } else {
      if (DEBUG)
	Aleph.debug("route returns parentPE " + parentPE());
      return parentPE();
    }
  }

  /**
   * Inner class with object's location and synchronization information
   **/
  private static class Status {
    Object  object;		// object itself, or null
    PE      requester;		// next in line
    AbstractContext	pending;
    Set<AbstractContext> localPending = new HashSet<AbstractContext>();
    boolean busy;		// in use?
//    boolean resendFind;
//    int     count;              // how many local threads waiting?
    PE      direction;		// it went that-a-way
    AbstractContext owner;
    Status(PE direction) {
      this.direction = direction;
      this.object    = null;
      this.requester   = null;
      this.busy      = false;
    }
    public String toString() {
      return "Status[object " + object +
        ", direction " + direction +
        ", pending " + pending + "@" + requester +
//        ", resendFind " + resendFind +
        ", count " + localPending.size() +
        ", busy " + busy + "]";
    }
  }

  /* Private message classes */
  private static class FindMessage
    extends aleph.Message implements Externalizable{

    private static final boolean DEBUG = false;

    PE from;
    AbstractContext context;
    GlobalObject key;
    PE requestor;

    FindMessage(AbstractContext context, GlobalObject key) {
      PE pe = PE.thisPE();
      this.from = pe;
      this.context = context;
      this.key = key;
      requestor = pe;
    }
    public FindMessage() {};
    /**
     * @see java.io.Externalizable
     **/
    public void writeExternal(ObjectOutput out) throws IOException {
      super.writeExternal(out);
      out.writeObject(from);
      out.writeObject(context);
      out.writeObject(key);
      out.writeObject(requestor);
    }
    /**
     * @see java.io.Externalizable
     **/
    public void readExternal(ObjectInput in)
      throws IOException, java.lang.ClassNotFoundException {
      super.readExternal(in);
      from       = (PE) in.readObject();
      context	 = (AbstractContext) in.readObject();
      key        = (GlobalObject) in.readObject();
      requestor  = (PE) in.readObject();

    }

    public void run() {
      Logger.debug("Context " + context + "@" + requestor.getIndex() + " try finding here " + key);
      try{
          synchronized (theManager) {
            Status status = (Status) theManager.arrow.get(key);
            PE newDirection = from;
            if (ArrowDirectory.DEBUG)
              Aleph.debug(this + " called\t " + status);
            if (status == null) {
              /* Object is unknown.
               * Install status record for object.
               * Flip arrow toward incoming edge.
               * send toward object's id.home
               */
              status = new Status(newDirection); // install status
              theManager.arrow.put(key, status);
              this.from = PE.thisPE();
              Logger.debug("Install new status:" + status);
              PE pe = theManager.route(key.getHome());
              this.send(pe);
              Logger.debug("Sending first find to " + pe);
            } else {
              /* Object is known.
               * Flip arrow toward incoming edge.
               * Check whether object is local.
               */
              PE direction = status.direction;
              status.direction = newDirection; // flip arrow
              if (direction.equals(PE.thisPE())) { // object is local
                if (status.busy || status.localPending.size() > 0 || status.object == null) {
                  // object is real busy
                  status.requester = requestor; // so get in line
                  status.pending = context;
                  
                  Logger.debug("Resolving conflict with owner " + status.owner);
                  try {
                	  HyFlow.getConflictManager().resolve(context, status.owner);
                	  status.owner = null;
                	  status.busy = false;
                	  theManager.notifyAll();
                	  Logger.debug("Remote transaction wins: " + context);  
                  } catch (TransactionException e) {
                	  Logger.debug("Remote transaction aborted and backoff :" + context);  
                  }

//                  if(status.object!=null){
//	                  //Remote Contention
//	                  try {
//	                	  if(HyFlow.getConflictManager().resolve(context, status.owner)==0){
//	                		  GrantMessage grant = new GrantMessage(key, status.object);
//	                          grant.send(requestor);
//	                          Logger.debug("[1] Object " + key + " was sent to " + context + "@" + requestor);
//	                          status.resendFind = true;
//	                          status.object = null;
//	                          status.owner = null;
//	    					  status.busy = false;
//	    					  return;
//	                	  }
//					  } catch (TransactionException e) {
//						  // ignore aborting remote transaction and just wait
//					  }
//                  }
                  
                  if (ArrowDirectory.DEBUG)
                    Aleph.debug(requestor + " getting in line");
                } else {            // object is free, send it
                  if (ArrowDirectory.DEBUG)
                    Aleph.debug("granting to " + requestor);
                  if (status.object == null)
                    Aleph.panic("null object" + status); // debugging
                  GrantMessage grant = new GrantMessage(key, status.object);
                  grant.send(requestor);
                  Logger.debug("[2] Object " + key + " was sent to " + context + "@" + requestor);
                  status.object = null;
                }
              } else {          // object is somewhere else
                if (ArrowDirectory.DEBUG)
                  Aleph.debug("forwarding to " + direction);
                this.from = PE.thisPE();
                this.send(direction);
              }
            }
            if (ArrowDirectory.DEBUG)
              Aleph.debug(this + " returned\t " + status);
          }
        } catch (IOException e) {
          Aleph.panic(e);
        }
      }
	public String toString() {
      StringBuffer s = new StringBuffer("FindMessage[from ");
      s.append(from.toString());
      s.append(", key ");
      s.append(key.toString());
      s.append(", requestor ");
      s.append(requestor.toString());
      s.append("]");
      return s.toString();
    }
  }

  /**
   * This message conveys the object from one owner to the next.
   * It is sent directly, not through the tree.
   **/
  private static class GrantMessage
    extends aleph.Message implements Externalizable {

    private static final boolean DEBUG = false;

    GlobalObject key;
    Object       object;

    GrantMessage(GlobalObject key, Object object) {
      this.key = key;
      this.object = object;
    }
    public GrantMessage() {}
    public void run() {
      synchronized (theManager) {
		Status status = (Status) theManager.arrow.get(key);
		if (ArrowDirectory.DEBUG)
			Aleph.debug(this + " called\t" + status);
		Logger.debug("Received object " + key);
		status.object = object;
		status.busy = false;
		if (ArrowDirectory.DEBUG)
			Aleph.debug(this + " returns\t" + status);
		theManager.notifyAll();
      }
    }
    /**
     * @see java.io.Externalizable
     **/
    public void writeExternal(ObjectOutput out) throws IOException {
      super.writeExternal(out);
      out.writeObject(key);
      out.writeObject(object);
    }

    /**
     * @see java.io.Externalizable
     **/
    public void readExternal(ObjectInput in)
      throws IOException, java.lang.ClassNotFoundException {
      super.readExternal(in);
      key      = (GlobalObject) in.readObject();
      object   = in.readObject();
    }

    public String toString () {
      StringBuffer s = new StringBuffer("GrantMessage[key ");
      s.append(key.toString());
      s.append(", object ");
      s.append(object.toString());
      s.append("]");
      return s.toString();
    }
  }

  public static void main(String[] args) {
    int i0 = 0;
    int i1 = 1;
    try {
      if ( args.length > 0)
        i0 = Integer.parseInt(args[0]);
      if ( args.length > 1)
        i1 = Integer.parseInt(args[1]);
    } catch (NumberFormatException e) {
      System.err.println("usage: Matrix <#dimension> <#tests>");
      Aleph.exit(1);
    }
    System.out.println("depth " + i0 + " = " + depth(i0));
    System.out.println("depth " + i1 + " = " + depth(i1));
    System.out.println("descendant(" + i0 + ", " + i1 + ") = " + isDescendant(i0,i1));
  }

}

