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

package aleph.trans;

import aleph.Aleph;
import aleph.AlephException;
import aleph.Config;
import aleph.GlobalObject;
import aleph.Transaction;

/**
 * Handles the underlying communication necessary to support transactions.
 * Extending classes must define a no-arg constructor.
 *
 * @see aleph.Transaction
 *
 * @author Maurice Herlihy
 * @date   August 1998
 **/

public abstract class TransactionManager {

  private static TransactionManager theManager;

  /**
   * @return the one and only TransactionManager
   **/
  public static TransactionManager getManager() {
    try {
      if (theManager == null) {
        String theClass = Aleph.getProperty("aleph.transactionManager"); // default choice
        theManager = (TransactionManager) Class.forName(theClass).newInstance();
      }
      return theManager;
    } catch (Exception e) {
      Aleph.panic(e);
    }
    return theManager;
  }

  /**
   * Change transaction managers in mid-stream.
   * Applications should probably not call this method.
   **/
  public static void setManager (String newManager) {
    try {
      theManager =              // install the new one
        (TransactionManager) Class.forName(newManager).newInstance();
      Aleph.setProperty("aleph.transactionManager", newManager);
    } catch  (ClassNotFoundException e) {
      Aleph.warning("Cannot find transaction manager " + newManager);
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
  public void newObject(GlobalObject key, Object object) {
    newObject(key, object, null);
  }

  /**
   * Register newly-created global object.
   * @param key    Global object ID
   * @param object Initial state for global object.
   * @param hint   <code>String</code> passed to transaction manager.
   **/
  public abstract void newObject(GlobalObject key, Object object, String hint);

  /**
   * Try to commit a transaction.
   * @param transaction Transaction to commit.
   * @exception AlephException if optimistic synchronization fails.
   **/
  public abstract void commit(Transaction transaction) throws AlephException;

  /**
   * Open a global object in desired mode.  All transaction managers should
   * support at least modes "w" (write) and "r" (read) Unrecognized modes
   * should be treated like "w".  Modes are case insensitive.
   * @param object The object to open.
   * @param mode   Mode in which to open object.
   **/
  public abstract Object open(GlobalObject object, String mode);

  /**
   * Open a global object within a transaction.
   * @param object The object to open.
   * @param transaction Transaction within which object is accessed.
   * @param mode   Mode in which to open object.
   **/
  public abstract Object open(GlobalObject object, Transaction transaction, String mode);

  /**
   * Called when object no longer needed.
   * @param object Formerly interesting object.
   **/
  public abstract void release(GlobalObject object);

  /**
   * @return whether transaction is active, committed, or aborted.
   **/
  public abstract int status(Transaction transaction);

  /**
   * @return Label describing which communication manager we are.
   **/
  public abstract String getLabel ();
  

}
