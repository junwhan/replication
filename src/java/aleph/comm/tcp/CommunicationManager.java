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
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.vt.rt.hyflow.util.network.Network;

/**
 * TCP-based implementation of Communication Manager.
 *
 * @author Maurice Herlihy
 * @date   September 1999
 **/

public class CommunicationManager extends aleph.comm.CommunicationManager {

  final boolean DEBUG = false;   // Avanti popolo ...

  ServerSocket listenSocket = null;
  Address      myAddress = null;
  AcceptConnections acceptThread = new AcceptConnections();
  Address      consoleAddress = null;
  Address      parentAddress = null;
  /**
   * Per-connection state information.  Unsynchronized.
   **/
  Map connections = new HashMap();

  private volatile boolean quit = false;

  /**
   * No-args constructor.
   **/
  public CommunicationManager() {
    super();
    try {
      // Special connection for messages to myself
      listenSocket = new ServerSocket( Integer.parseInt(Aleph.getProperty("aleph.myPort", "0")));
      myAddress = new Address(listenSocket.getLocalPort());
      Connection auto = new AutoConnection();
      auto.start();             // start before making visible
      connections.put(myAddress, auto);
      if (DEBUG)
        Aleph.debug("autoconnection at " + myAddress);
    } catch (Exception e) {
      Aleph.panic(e);
    }
    acceptThread.start();
  }
  
  /**
   * @return my address
   **/
  public aleph.comm.Address getAddress() {
    return myAddress;
  }

  /**
   * Sends a message
   * @param address destination
   * @param message what to send
   **/
  public void send (aleph.comm.Address _address, Message message) {
	Network.linkDelay(true, _address);
	try {
      Address address = (Address) _address;
      message.from = myAddress;   // fill in source
      if (DEBUG)
        Aleph.debug("tcp.CommunicationManager sends " + message + " to " + address);
      getConnection(address).send(message);
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }

  /**
   * Shuts down this CommunicationManager
   **/
  public void close() {
    try {
      listenSocket.close();
      for (Iterator iter = connections.values().iterator();
           iter.hasNext();) {
        Connection connect = (Connection) iter.next();
        connect.close();		// close this socket
      }
      quit = true;
    } catch (IOException e) {
      Aleph.panic(e);
    }
  }
  
  /**
   * Make sure nothing left in the stream.
   **/
  public void flush (aleph.comm.Address address) {
    try {
      if (DEBUG)
        Aleph.debug("tcp.CommunicationManager flushing " + address);
      getConnection((Address) address).flush();
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }
  
  /**
   * Ask if connection is active.
   **/
  public boolean ping(aleph.comm.Address address) {
    try {
      if (DEBUG)
        Aleph.debug("tcp.CommunicationManager pinging " + address);
      return getConnection((Address) address).ping();
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * @return the Console's address
   * Computed from <code>aleph.console.address</code>and
   * <code>aleph.console.port</code> properties.
   **/
  public aleph.comm.Address getConsoleAddress () {
    if (consoleAddress == null) {
      String _console = System.getProperty("aleph.console");
      if (_console != null)	// defined by environment
	consoleAddress = new Address(_console);
      else
	consoleAddress = (Address) getAddress(); // we *are* the console
    }
    return consoleAddress;
  }

  /**
   * @return parent's address, or <code>null</code>
   **/
  public aleph.comm.Address getParentAddress () {
    if (parentAddress == null) {
      String _parent = System.getProperty("aleph.parent");
      if (_parent != null)	// defined by environment
	parentAddress = new Address(_parent);
    }
    return parentAddress;
  }

  /**
   * Lazily create a connection if it can't be found.
   * Synchronized to avoid race condition.
   **/
  private synchronized Connection getConnection (aleph.comm.Address _destination) {
    Address destination = (Address) _destination;
    Connection connect = (Connection) connections.get(destination);
    if (connect == null) {
      if (DEBUG)
        Aleph.debug("tcp.CommunicationManager: creating connection " + _destination);
      connect = new TCPConnection(destination);
      connections.put(destination, connect);
      connect.start();
    }
    return connect;
  }

  /**
   * Inner class defining listen loop.
   **/
  private class AcceptConnections extends Thread {
    public void run () {
      while (! quit) {
	try {
	  Socket socket = listenSocket.accept();
	  Connection connection = new TCPConnection(socket);
          if (DEBUG)
            Aleph.debug("new connection from " + socket);
	  connections.put(new Address(socket.getInetAddress(), socket.getPort()),
                          connection);
	  connection.start();	// start listening
	} catch (Exception e) {
        }
      }
    }

  }

}
