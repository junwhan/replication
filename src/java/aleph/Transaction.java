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

package aleph;

import aleph.trans.TransactionManager;
import java.util.Enumeration;

/**
 * Names transactions.
 *
 * @author Maurice Herlihy
 * @date   August 1998
 **/

public class Transaction {

    // Transaction states.
  public final static int ACTIVE    = 0;
  public final static int COMMITTED = 1;
  public final static int ABORTED   = 2;

  private UniqueID id;		// it's about ME, ME, ME !!!
  private TransactionManager tManager = TransactionManager.getManager();
  private String hint;

  public Transaction() {
    this(null);
  }

  public Transaction(String hint) {
    this.id  = new UniqueID();
    this.hint = hint;
  }

  public int status() {
    return tManager.status(this);
  }

  /**
   * Try to commit transaction.
   * @exception AlephException if transaction aborts
   **/
  public void commit() throws AlephException{
    tManager.commit(this);
  }

  public int hashCode() {
    return id.hashCode();
  }

  public boolean equals(Object anObject) {
    if ((anObject != null) && (anObject instanceof Transaction)) {
      Transaction other = (Transaction) anObject;
      return (other.id.equals(this.id));
    } else {
      return false;
    }
  }
}
