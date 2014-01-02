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

import java.util.LinkedList;
import java.util.List;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.Context;
import org.deuce.transaction.TransactionException;
import org.deuce.transaction.AbstractContext.STATUS;

import aleph.Aleph;
import aleph.GlobalObject;
import aleph.PE;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.util.io.Logger;
/**
 * Home based directory manager<br>
 * Keeps track of locally-cached global objects.
 *
 * @author Mike Demmer
 * @author Maurice Herlihy
 * @date   May 1998
 **/
public class ClientSide implements Constants {

  private static final boolean DEBUG = Boolean.getBoolean("debug"); // BANG-BANG Maxwell's silver hammer ...

  private GlobalObject key;
  private Object       object;

  // local synchronization state
  private boolean readValid  = false; // May I read this object?
  private boolean writeValid = false; // May I write this object?
  private boolean pending    = false; // Have I asked server for this object?
  private AbstractContext writer= null; // Does a local thread hold a write lock?
  private List<AbstractContext> readers =new LinkedList<AbstractContext>();	// How many local threads hold read locks?
  private boolean requested  = false; // Does Server want object back?
  private List<AbstractContext> abortedPending =new LinkedList<AbstractContext>();
  private AbstractContext pendingContext;

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

  public synchronized Object open (AbstractContext context, String modeName) {
	  Logger.debug("TryOpen " + key + " for context " + context);
    if (DEBUG)
      Aleph.debug(this + " open(" + modeName + ") called");
    try {
      int mode = parseMode(modeName);
      switch (mode) {
	      case READ_MODE:
				while (writer!=null || ! readValid) { // Get shared access to object
				  if(writer!=null){
					  int res = HyFlow.getConflictManager().resolve(context, writer);
					  if(res==0)
						  writer = null;
					  else if(res > 0)
						  wait(res);
					  continue;
				  }
				  if (! readValid && ! pending) {	// Ask for object if absent.
				    RetrieveRequest retrieve = new RetrieveRequest(context, key, mode);
				    if (DEBUG)
				      Aleph.debug(this + " sends " + retrieve + " to " + key.getHome());
				    pendingContext = context;
				    retrieve.send(key.getHome());
				    pending = true;
				  }
				  try{ wait(); } catch (InterruptedException e) {};
				  if(abortedPending.remove(context)){
					  Logger.debug("Remotely aborting: " + context);
					  throw new TransactionException();
				  }
				}
				readers.add(context);
				break;
	      case WRITE_MODE:
			// Get exclusive access.
			while (writer!=null || !readers.isEmpty() || ! writeValid) {
			   if(writeValid){
				  if(writer!=null){
					  int res = HyFlow.getConflictManager().resolve(context, writer);
					  if(res==0)
						  writer = null;
					  else if(res > 0)
						  wait(res);
				  }
				  if(!readers.isEmpty()){
					  int res = HyFlow.getConflictManager().resolve(context, readers);
					  if(res==0)
						  readers.clear();
					  else if(res > 0)
						  wait(res);
				  }
				  continue;
			   }
			  // Ask for object if absent.
			  if (! writeValid && ! pending) {
			    RetrieveRequest retrieve = new RetrieveRequest(context, key, mode);
			    if (DEBUG)
			      Aleph.debug(this + " sends " + retrieve + " to " + key.getHome());
			    pendingContext = context;
			    retrieve.send(key.getHome());
			    pending = true;
			  }
			  try{ wait(); } catch (InterruptedException e) {};
			  if(abortedPending.remove(context)){
				  Logger.debug("Remotely aborting: " + context);
				  throw new TransactionException();
			  }
			}
			if(!context.status.equals(STATUS.ABORTED))
				writer = context;
			break;
	     default:
	    	 Aleph.panic("ClientSide.open: unknown mode " + mode);
      }
      if (DEBUG)
    	  Aleph.debug(this + " open(" + modeName + ") returns " + this.object);

    } catch(TransactionException e){
    	Logger.debug("Rollback Exp: " + context);
    	throw e;
    }
    catch (Exception e) {
      Aleph.panic(e);
    }
	Logger.debug("Open " + key + " for context " + context);
    return this.object;
  }

  public synchronized void release (Context context) {
	Logger.debug("TryRelease : <" + context + (object!=null ? "> " + ((AbstractDistinguishable)object).getId(): " " ));
    if (DEBUG)
      Aleph.debug(this + " release() called");

    if (writer!=null) {
      writer = null;
      notifyAll();
    }
    if (!readers.isEmpty()){
    	readers.remove(context);
      if (readers.isEmpty())
    	  notifyAll();
    }
    if (DEBUG)
      Aleph.debug(this + " release() returned");
    Logger.debug("Release :" + context);
  }


  /**
   * Server delivers requested object.<br>
   * Called by retrieveResponse message.
   * @param from Requesting PE
   * @see aleph.trans.RetrieveRes
   **/
  public synchronized void retrieveResponse (Object object, int mode) {
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
	    case ABORT_MODE:
	    	abortedPending.add(pendingContext);
	    	pendingContext = null;
	    	writeValid = readValid = false;
	    	break;
    }
    this.pending = false;
    this.object  = object;
    this.notifyAll();
    if (DEBUG)
      Aleph.debug(this + " RetrieveResponse(" +
			 object + ", " + printMode(mode) + ") returns");
  }


  /**
   * Server asks for object back.<br>
   * Called by releaseRequest message.
   * @param from Server PE
 * @param context 
   * @param mode Mode of interenst.
   * @see aleph.dir.cm.home.ReleaseRequest
   **/
  public synchronized void releaseRequest(PE from, AbstractContext context) {
    try {
      requested = true;
      if (DEBUG)
    	  Aleph.debug(this + " ReleaseRequest(" + from + ") called");
      
      
      boolean deny = false;
      // Handle remote/local contention
      while (!deny && (writer!=null || !readers.isEmpty())) {
	      try {
			  if(writer!=null){
				  Logger.debug("Resolve W/R:" + context + " " + writer);
				  int res = HyFlow.getConflictManager().resolve(context, writer);
				  Logger.debug("Resolved W/R:" + res);
				  if(res==0)
					  writer = null;
				  else if(res > 0)
					  wait(res);
			  }
			  if(!readers.isEmpty()){
				  Logger.debug("Resolve R/R:" + context + " " + readers);
				  int res = HyFlow.getConflictManager().resolve(context, readers);
				  Logger.debug("Resolved R/R:" + res);
				  if(res==0)
					  readers.clear();
				  else if(res > 0)
					  wait(res);
			  }
		  } catch (TransactionException e) {
				// abort remote transaction with this exception
			  	Logger.debug("Refuse Release: aborting " + context + "@" + from.getIndex());
			  	deny = true;
		  }
      }
      if (DEBUG)
          Aleph.debug(this + " ReleaseRequest(" + from + ") granted");
      requested = false;
      ReleaseResponse release = new ReleaseResponse(key, (writeValid && !deny ? object : null), deny);
      if (DEBUG)
    	  Aleph.debug(this + " sends " + release + " to " + from);
      release.send(from);
      if(!deny){
    	  writeValid = false;
    	  readValid  = false;
    	  object     = null;
      }
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
      (writer!=null ? "Writer|" : "NoWriter|") +
      "Readers(" + readers.size() + ")|" +
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
