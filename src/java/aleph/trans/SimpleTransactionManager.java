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

package aleph.trans;

import aleph.Aleph;
import aleph.AlephException;
import aleph.GlobalObject;
import aleph.PE;
import aleph.Transaction;
import aleph.dir.DirectoryManager;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Simple Transaction Manager.  Placeholder until we get a real transaction manager.
 * Currently supports transactions only within a single PE.
 *
 * @author Maurice Herlihy
 * @date   May 1997
 **/
public class SimpleTransactionManager extends TransactionManager {

  static final boolean DEBUG = false; // Top of the world, Ma!

  /* map transaction -> vector of global objects */
  private Hashtable t2object = new Hashtable();

  /* map transaction -> status */
  private Hashtable t2status = new Hashtable();

  static final DirectoryManager directory =
    DirectoryManager.getManager();
  
  /**
   * Try to commit a transaction.
   * @param transaction Transaction to commit.
   * @return <code>true</code> if and only if commit succeeds.
   * @exception AlephException if transaction aborts
   **/
  public void commit(Transaction t) throws AlephException{
    Status s = getStatus(t);
    if (s.status == Transaction.COMMITTED)
      Aleph.panic("Attempt to commit already committed transaction:" + this);
    Vector objects = getObjects(t);
    for (Enumeration e = objects.elements(); e.hasMoreElements();)
      ((GlobalObject) e.nextElement()).release();
  }

  /**
   * Open an object for access.
   * @param object      The object.
   * @param transaction Transaction on behalf of which operations occur.
   * @param mode        E.g., Read, Write, Copy, etc.
   **/
  public Object open(GlobalObject object, Transaction t, String mode) {
    Vector objects = getObjects(t);
    if (objects.indexOf(object) == -1) // locked already?
      objects.addElement(object); // mine, all mine
    return directory.open(object, mode);
  }

  /**
   * Open an object.
   * @param object      The object.
   * @param mode        E.g., Read, Write, Copy, etc.
   **/
  public Object open(GlobalObject object, String mode) {
    return directory.open (object, mode);
  }

  /**
   * Register newly-created global object.
   * @param key    Global object ID
   * @param object Initial state for global object.
   * @param hint   ignored
   **/
  public void newObject(GlobalObject key, Object object, String hint) {
    directory.newObject(key, object, hint);
  }

  /**
   * Called when object no longer needed.
   * @param object Formerly interesting object.
   **/
  public void release(GlobalObject key) {
    directory.release(key);
  }

  /**
   * Produce a human-readable snapshot of all global objects.  Very helpful in
   * a panic.
   **/
  public String toString() {
    return "TransactionManager";
  }

  /**
   * @return Label describing which communication manager we are.
   **/
  public String getLabel () {
    return "Simple";
  }

  /**
   * @return whether transaction is active, committed, or aborted.
   **/
  public int status(Transaction t) {
    return getStatus(t).status;
  }

  private Vector getObjects (Transaction t) {
    Vector objects = (Vector) t2object.get(t);
    if (objects == null) {
      objects = new Vector();
      t2object.put(t, objects);
    }
    return objects;
  }

  private Status getStatus (Transaction t) {
    Status status = (Status) t2status.get(t);
    if (status == null) {
      status = new Status();
      t2status.put(t, status);
    }
    return status;
  }
    
  private class Status {
    public int status;
    public Status() {
      status = Transaction.ACTIVE;
    }
  }

}
