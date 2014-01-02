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
package aleph.comm.udp;

import aleph.Aleph;
import aleph.AsynchMessage;
import aleph.Message;
import aleph.meter.Counter;
import aleph.thread.Scheduler;
import java.io.IOException;

/**
 * A Connection is a two-way stream of messages between two PEs.  This class is
 * abstract because it has two implemenations: the standard one using a
 * reliable packet protocol, and a dummy one that short-circuits messages sent
 * from a PE to itself.
 *
 * Creating a Connection starts a thread that handles incoming messages.
 *
 * @author Maurice Herlihy
 * @date   January 1999
 **/
public abstract class Connection {

  /**
   * Instrumentation: count number of messages sent.
   **/
  protected static Counter messagesSent  = new Counter("Messages sent");

  private static final boolean DEBUG = false; // the Tao that can be debugged is not the true Tao 

  private volatile boolean quit = false; // time to shut down?
  private Scheduler scheduler = Scheduler.getScheduler();
  protected MessageHandler handler;

  /**
   * Starts thread that processes incoming messages.
   * Must be called after calling the constructor.
   **/
  public void start () {
    handler = new MessageHandler();
    handler.start(); // shepherd packets in
  }

  /**
   * Deliver new packet to connection.
   * @param packet new arrival.
   **/
  public void deliver(Packet packet) {};

  /**
   * Send'em a message.
   * @param message what to send
   * @exception java.io.IOException something's wrong
   **/
  public abstract void send (Message message) throws IOException;

  /**
   * Blocking method that pulls in the next message.
   * @returns next message
   **/
  protected abstract Message receive () throws IOException;

  /**
   * Clean up at the end.
   **/
  public void close () {
    quit = true;
  }

  /**
   * Push out queued packets.
   **/
  public void flush () {}

  /**
   * Is anyone listening?
   **/
  public boolean ping () {
    return true;
  }

  private class MessageHandler extends Thread {
    public void run() {
      try {
        while(! quit) {         // keep snarfing up messages
          Message message = Connection.this.receive();
          if (message != null) {	//  ignore spurious nulls in stream
            if (DEBUG)
              Aleph.debug("Connection received " + message);
            if (message instanceof AsynchMessage)
              scheduler.schedule(message); // place in work pool
            else
              message.run();	// run it immediately
          }
        }
      } catch(java.io.IOException e) {
        return;            
      } catch(Exception e) {
        Aleph.panic("Message Handler " + e);
      }
    }
  }

}
