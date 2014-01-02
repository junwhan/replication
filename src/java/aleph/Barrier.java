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
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Vector;

/** 
 * Barrier objects
 *
 * @author Maurice Herlihy
 * @date   April 1998
 * @date   March 1998
 **/

public class Barrier implements Externalizable {

  private BarrierState state;   // number of outstanding calls
  private Event event;          // used for communication
  private static Hashtable table = new Hashtable(); // register barriers here

  /**
   * Construct a barrier, one thread per PE.
   **/
  public Barrier() {
    this(PE.numPEs());
  }

  /**
   * Construct a barrier for an arbitrary set of threads.
   * @param size Number of participating threads.
   **/
  public Barrier(int size) {
    event = new Event();
    state = new BarrierState(size);
    table.put(event, state);    // register me
  }

  public String toString() {
    return "Barrier" + event + "[" + state + "]";
  }

  /**
   * Wait for all threads to reach this barrier.
   **/
  public void waitFor() {
    event.resetListener(new Listener() {
      public void actionPerformed(Object object){
	synchronized (state) {
	  state.count -= 1;
	  state.notifyAll();
	}
      }});
    event.signal();
    synchronized (state) {
      while (state.count > 0) {
        try { state.wait(); } catch (InterruptedException e) {};
      }
    }
  }

  /**
   * @see java.io.Externalizable
   **/
  public void writeExternal (ObjectOutput out) throws IOException {
    out.writeObject(event);
    out.writeObject(state);
    }

  /**
   * @see java.io.Externalizable
   **/
  public void readExternal (ObjectInput in)
    throws IOException, java.lang.ClassNotFoundException {
    event = (Event) in.readObject();
    state = (BarrierState) in.readObject();
    if (table.containsKey(event))
      state = (BarrierState) table.get(event);
  }

  private class BarrierState implements Serializable {
    public int count;
    BarrierState (int c) {
      count = c;
    }
    public String toString() {
      return super.toString() + "[" + Integer.toString(count) + "]";
    }
  }

  /**
   * Simple debugging test.  Creates a RemoteThread at each PE to wait on a
   * barrier.  Must be called from Aleph!
   **/
  public static void main(String[] args) {
    Barrier barrier = new Barrier();
    TestThread thread = new TestThread(barrier);
    Join join = new Join();
    long start = System.currentTimeMillis();
    for (Iterator e = PE.allPEs(); e.hasNext(); )
      thread.start((PE) e.next(), join);
    join.waitFor();
    System.out.println("Elapsed time: " +
		       ((double) (System.currentTimeMillis() - start)) / 1000.0);
  }

  // Simple-minded Barrier testing thread.
  private static class TestThread extends aleph.RemoteThread {
    Barrier barrier;
    TestThread(Barrier barrier) {
      this.barrier = barrier;
    }
   public void run() {
     System.out.println(PE.thisPE() + " before barrier");
     System.out.flush();	// I/O buffering disguises real-time order
     barrier.waitFor();
     System.out.println(PE.thisPE() + " after barrier");
     System.out.flush();
   }
  }
}
