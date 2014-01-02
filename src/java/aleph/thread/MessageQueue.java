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

package aleph.thread;

import aleph.Aleph;
import aleph.Message;
import java.util.LinkedList;

/**
 * Synchronized FIFO message queue.
 * @see aleph.Message
 *
 * @author Maurice Herlihy
 * @date   October 1999
 **/

public class MessageQueue { 

  private static final boolean DEBUG = false; // Cannon to the right of them ...

  private LinkedList queue = new LinkedList();

  public synchronized void enq (Message message) {
    if (DEBUG)
      Aleph.debug("MessageQueue.enq(" + message + ")");
    queue.add(message);
    notifyAll();
  }

  public synchronized Message deq () {
    if (DEBUG)
      Aleph.debug("MessageQueue.deq ...");
    while (queue.isEmpty())
      try {wait();} catch (InterruptedException e) {}
    Message message = (Message) queue.removeLast();
    if (DEBUG)
      Aleph.debug("MessageQueue.deq() returns " + message);
    return message;
  }
}
