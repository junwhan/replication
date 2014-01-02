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

import aleph.Aleph;
import aleph.AsynchMessage;
import aleph.Message;
import aleph.meter.Counter;
import aleph.thread.Scheduler;
import java.io.IOException;
import java.io.InterruptedIOException;

/**
 * A Connection is a stream socket.  A listener thread handles input, and a
 * <code>send</code> method handles output.
 *
 * @author Maurice Herlihy
 * @date   August 1998
 * @author Michael Rubin
 * @date   May 1998
 **/
public abstract class Connection {

  protected static Counter messagesSent  = new Counter("Messages sent");

  private static final boolean DEBUG = false; // Oy, va-voy!
  private volatile boolean quit = false; // The pipes are calling ...

  /**
   * Start thread to process incoming messages.
   * Must be called after calling the constructor.
   **/
  public void start () {
    (new MessageHandler()).start();
  }

  /**
   * Clean me up.
   **/
  public void close () {
    quit = true;
  }

  /**
   * Flush output stream.
   **/
  public abstract void flush() throws InterruptedIOException;

  /**
   * Ping output stream.
   **/
  public abstract boolean ping();

  /**
   * Blocking method that pulls in the next message.
   * @returns next message
   **/
  protected abstract Message receive () throws IOException;

  /**
   * Send'em a message.
   * @param message what to send
   * @exception java.io.IOException something's wrong
   **/
  public abstract void send (Message message) throws IOException;

  private class MessageHandler extends Thread {
    public void run() {
      try {
        while(! quit) {         // keep on keeping on
          Message message = Connection.this.receive();
          if (message != null) { // ignore spurious nulls
            if (DEBUG)
              Aleph.debug("Connection received " + message);
            if (message instanceof AsynchMessage)
              Scheduler.getScheduler().schedule(message); // maybe later
            else
              message.run();
          }
        }
      } catch(IOException e) {
        if (DEBUG)
          Aleph.debug("tcp.Connection closing: " + e);
        return;
      } catch(Exception e) {
        Aleph.panic(e);
      }
    }
  }


}

