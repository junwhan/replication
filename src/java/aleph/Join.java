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

import aleph.Aleph;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Vector;

/** 
 * Join objects.<p>
 * A <code>Join</code> object is an optional argument to
 * <code>RemoteThread</code> or <code>RemoteFunction</code> calls.  A thread
 * that calls the Join object's <code>waitFor</code> method is blocked until
 * all remote threads started with that Join object have finished.  A
 * <code>Join</code> object also implements <code>java.util.Iterator</code>.
 * The Iterator methods can be used to collect the results of multiple
 * remote threads as they become available.
 *
 * @author Maurice Herlihy
 * @date   April 1998
 * @date   March 1998
 **/

public class Join implements Serializable, Iterator {
  private static final boolean DEBUG = false; // jettison fuel ..

  private int count;            // number of outstanding calls
  private Event event;          // used for communication
  private Vector values;	// non-null return values

  public Join() {
    values = new Vector();
    event =  new Event();
    event.setListener(new Listener() { // anonymous listener
      public void actionPerformed(Object object){
	synchronized (Join.this) {
	  count -= 1;           // decrement count
	  if (object != null)   // argument?
	    values.addElement(object);
	  Join.this.notifyAll();
	}
      }});
    if (DEBUG)
      Aleph.debug("Join creates " + event);
  }

  /**
   * @return whether any more return values are lurking here.
   **/
  public synchronized boolean hasNext() {
    while (true) {
      if (! values.isEmpty()) {
        return true;
      } else {
        if (count == 0)
          return false;
      }
      try { wait(); } catch (InterruptedException e) {}
    }
  }
   
  /**
   * @return next lurking return value.
   **/
  public synchronized Object next() throws NoSuchElementException {
    while (true) {
      if (values.isEmpty() && count == 0)
	throw new NoSuchElementException();
      if (! values.isEmpty()) {
	Object result = values.firstElement();
	values.removeElementAt(0); // next in line
	return result;
      }
      try { wait(); } catch (InterruptedException e) {};
    }
  }

  /**
   * Unsupported operation
   * @throws UnsupportedOperationException
   **/
  public void remove () {
    throw new UnsupportedOperationException();
  }

  /**
   * Inform everyone that I'm finished.
   * @param object optional argument
   **/
  public void signal(Object object) {
    event.signal(object);
  }

  /**
   * Inform everyone that I'm finished.
   **/
  public void signal() {
    event.signal();
  }

  public String toString() {
    return "Join" + event;
  }

  synchronized void increment() {
    count += 1;
    if (DEBUG)    
      Aleph.debug("Join " + this + " incremented");
  }

  /**
   * Wait for all threads started with this join object to finish.
   **/
  public synchronized void waitFor() {
    while (count > 0)
      try { wait(); } catch (InterruptedException e) {};
  }
}
