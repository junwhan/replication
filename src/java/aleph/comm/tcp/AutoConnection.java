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

package aleph.comm.tcp;

import aleph.Message;
import aleph.thread.MessageQueue;
import java.io.InterruptedIOException;

/**
 * An AutoConnection is a connection from a PE to itself.
 *
 * @author Maurice Herlihy
 * @date   February 1999
 **/
public class AutoConnection extends Connection {

  private static final boolean DEBUG = false; // Oy, va-voy!

  private MessageQueue queue;	// messages to myself
  /**
   * Constructor for incoming connection.
   * @param socket use this socket
   **/
  public AutoConnection () {
    super();
    queue = new MessageQueue();
  }

  /**
   * Close socket and let thread die naturally.
   **/
  public void close () {}

  /**
   * Flush output stream.
   **/
  public void flush () throws InterruptedIOException {}
 
  /**
   * Ping output stream.
   **/
  public boolean ping ()  {
    return true;
  }

  /**
   * Send'em a message.
   * @param message what to send
   * @exception java.io.IOException something's wrong
   **/
  public void send (Message message) throws java.io.IOException {
    // To avoid bugs caused by sharing between messages and objects, we make
    // sure to deliver a serialized/deserialized copy of the message, not the
    // message itself.
    messagesSent.inc();
    queue.enq(message.copy());
  }

  /**
   * Blocking method that pulls in the next message.
   * @returns next message
   **/
  protected Message receive () {
    return queue.deq();
  }
}

