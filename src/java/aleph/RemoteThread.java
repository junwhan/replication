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
import aleph.comm.CommunicationManager;
import aleph.thread.Fork;
import java.io.Serializable;
import java.lang.Runnable;

/**
 * The base class for remote threads. <p>
 * Unlike java.lang.thread, one can create a single <code>RemoteThread</code> object,
 * and fork it multiple times at one or more PEs.
 * <p>
 * For example, a remote thread that prints a message could be written as follows.
 * <p><blockquote><pre>
 * class HelloThread extends RemoteThread {
 *   public void run() {
 *     System.out.println("Hello world!");
 *   }
 * }
 </pre></blockquote>
 * The following code would then create a remote thread, start it at all PEs,
 * and then wait until all instances finished.
 * <p><blockquote><pre>
 * for (Enumeration e = PE.allPEs(); e.hasMoreElements(); )
 *    fork.start((PE) e.nextElement(), join);
 * join.waitFor();
 * </pre></blockquote>
 *
 * @author Maurice Herlihy
 * @date   May 1997
 **/
abstract public class RemoteThread implements Runnable, Serializable {

  private static CommunicationManager cManager =
    CommunicationManager.getManager();

  /**
   * Start the thread at the indicated PE.
   * @param pe The PE where the thread should execute.
   **/
  public synchronized void start(PE pe) throws IllegalThreadStateException {
    start(pe, null);
  }
  /**
   * Start the thread at the indicated PE.
   * @param pe   The PE where the thread should execute.
   * @param join notify Join when thread finishes.
   **/
  public synchronized void start(PE pe, Join join) {
    if (join != null)
      join.increment();
    Fork fork = new Fork(join, this);
    try {
      cManager.send(pe, fork);
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }

}
