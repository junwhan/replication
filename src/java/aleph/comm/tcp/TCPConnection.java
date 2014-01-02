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
import aleph.Message;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import edu.vt.rt.hyflow.util.io.Logger;

/**
 * A TCPConnection encapsulates a stream socket to a remote PE.
 * @see aleph.comm.tcp.Connection
 * @see aleph.comm.tcp.AutoConnection
 *
 * @author Maurice Herlihy
 * @date   February 1999
 **/
public class TCPConnection extends Connection {
  private Socket  socket;        // I/O here
  private Address remoteAddress; // my partner
  private ObjectInputStream  objectInput  = null;
  private ObjectOutputStream objectOutput = null;

  private static final boolean DEBUG = false;

  /**
   * Constructor for incoming connection.
   * @param socket use this socket
   **/
  public TCPConnection (Socket socket) {
    try {
      if (DEBUG)
        Aleph.debug("creating incoming TCPConnection for " + socket);
      this.socket = socket;
      this.remoteAddress = new Address(socket.getInetAddress(), socket.getPort());
      this.objectInput   = new ObjectInputStream(socket.getInputStream());
      this.objectOutput  = new ObjectOutputStream(socket.getOutputStream());
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }

  /**
   * Constructor for outgoing connection.
   * @param destination partner's Address
   * @param localHost my address
   **/
  public TCPConnection (Address destination) {
    try {
      remoteAddress = destination;
      if (DEBUG)
        Aleph.debug("creating outgoing TCPConnection to " + destination);
      socket = new Socket(remoteAddress.inetAddress, remoteAddress.port);
      objectOutput  = new ObjectOutputStream(socket.getOutputStream());
      objectInput   = new ObjectInputStream(socket.getInputStream());
    } catch (Exception e) {
    	System.err.println(remoteAddress.inetAddress + " " + remoteAddress.port);
      Aleph.panic(e);
    }
  }

  /**
   * Close socket and let thread die naturally.
   **/
  public void close () {
    try {
      super.close();
      socket.close();
      if (DEBUG)
        Aleph.debug("TCPConnection: closing socket " + socket);
    } catch (IOException e) {
      return;
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }

  /**
   * Flush output stream.
   **/
  public void flush () {
    try {
      if (DEBUG)
        Aleph.debug("tcp.Connection flushing socket " + socket);
      objectOutput.flush();
    } catch (IOException e) {
      return;
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }

  /**
   * Ping output stream.
   **/
  public boolean ping()  {
    try {
      objectOutput.flush();
    } catch (IOException e) {
      return false;
    } catch (Exception e) {
      Aleph.panic("ping " + e);
    }
    return true;
  }

  /**
   * Send a message on this connection.  Synchronized for concurrent sends.
   * @param message what to send
   **/
  public synchronized void send (Message message) {
    try {
      message.stamp = String.valueOf(Math.random());
   	  Logger.debug("Send " + message.stamp+" ("+message.toString()+")");
      if (DEBUG)
        Aleph.debug("TCPConnection: sending " + message + " To: " + remoteAddress);
      messagesSent.inc();       // record message sent
      objectOutput.reset();     // don't preserve object sharing
      objectOutput.writeObject(message); // stuff it
      objectOutput.flush();     // get it out
    } catch (NotSerializableException e) {
      Aleph.panic(e);
    } catch (IOException e) {
      return;
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }

  /**
   * Pull in next message.  Blocking and unsynchronized.
   * @returns next message
   **/
  protected Message receive () throws IOException {
    try {
      if (DEBUG)
        Aleph.debug("TCPConnection: receive socket is " + socket);
      Message message = (Message) objectInput.readObject();
  	  Logger.debug("Received " + message.stamp + "("+message.toString()+")");
      if (DEBUG)
        Aleph.debug("TCPConnection: received " + message + " From: " + remoteAddress);
      return message;
    } catch (ClassNotFoundException e) {
      Aleph.panic(e);
      return null;              // not reached
    }
  }

}

