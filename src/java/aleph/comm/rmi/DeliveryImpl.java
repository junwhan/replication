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
package aleph.comm.rmi;

import aleph.Aleph;
import aleph.AsynchMessage;
import aleph.Message;
import aleph.thread.MessageQueue;
import aleph.thread.Scheduler;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.List;

/**
 * Handles incoming messages for the RMI communication manager.
 * @author Maurice Herlihy
 * @date   November 1998
 **/
public class DeliveryImpl
  extends UnicastRemoteObject
  implements Delivery {
  private static final boolean DEBUG = true; // a three-hour cruise ...

  private Scheduler    scheduler = Scheduler.getScheduler(); // pencil me in
  private MessageQueue queue     = new MessageQueue();

  /**
   * Sole constructor.
   **/
  public DeliveryImpl () throws java.rmi.RemoteException {
    super();
    Worker worker = new Worker();
    worker.setDaemon(true);
    worker.start();
  }

  /**
   * Remotely-accessible method to handle incoming message.
   **/
  public void deliver (Message message) throws RemoteException {
    if (DEBUG)
      Aleph.debug("delivering " + message);
    queue.enq(message);         // hand it to worker thread
  }

  /**
   * Tests whether target is still alive
   **/
  public void ping () throws RemoteException {}

  /**
   * Inner class that implements worker thread.
   **/
  private class Worker extends Thread {
   public void run () {
     while (true) {
       if (DEBUG)
         Aleph.debug("waiting for message ...");
       Message message = (Message) queue.deq();
       if (DEBUG)
         if (message instanceof AsynchMessage)
           Aleph.debug("dequeued asynchronous " + message);
         else
           Aleph.debug("dequeued normal " + message);
       if (message instanceof AsynchMessage)
         scheduler.schedule(message); // do it later
       else
         message.run();         // 
     }
   }
  }
}
