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
import aleph.Message;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CommunicationManager extends aleph.comm.CommunicationManager {

  static final boolean DEBUG = false;   // La lutte finale ....

  DatagramSocket listenSocket = null;
  Address     myAddress = null;
  Address     consoleAddress = null;
  Address     parentAddress = null;
  /**
   * Per-connection state information.
   **/
  Map connections = new HashMap(); // unsynchronized

  private volatile boolean quit = false;
  /**
   * No-args constructor.
   **/
  public CommunicationManager() {
    super();

    try {
      listenSocket = new DatagramSocket();
      myAddress = new Address(listenSocket.getLocalPort());
      if (DEBUG)
        Aleph.debug("udp.CommunicationManager started " + myAddress);

      // Special connection for messages to myself
      Connection auto = new AutoConnection();
      auto.start();
      connections.put(myAddress, auto);
      (new GetPackets()).start(); // pull packets in
    } catch (Exception e) {
      Aleph.panic("CommunicationManager " + e);
    }
  }
  
  /**
   * @return my address
   **/
  public aleph.comm.Address getAddress() {
    return myAddress;
  }

  /**
   * @return who we are
   **/
  public String getLabel () {
    return "UDP";
  }

  /**
   * Sends a message
   * @param address destination
   * @param message what to send
   **/
  public void send (aleph.comm.Address _address, Message message) throws IOException {
    Address address = (Address) _address;
    message.from = myAddress;   // fill in source
    if (DEBUG)
      Aleph.debug("tcp.CommunicationManager sends " + message + " to " + address);
    getConnection(address).send(message);
  }

  /**
   * Shuts down this CommunicationManager
   **/
  public synchronized void close () {
    listenSocket.close();
      for (Iterator iter = connections.values().iterator();
           iter.hasNext();) {
        Connection connect = (Connection) iter.next();
        connect.close();        // close this socket
      }
    quit = true;
  }
  
  /**
   * Wait until all packets sent to this address are acknowledged.
   * @param destination where packets were sent
   * @param timout when to give up
   * @throws InterruptedIOException on timeout
   **/
  public void flush(aleph.comm.Address destination) throws InterruptedIOException {
    getConnection((Address) destination).flush();
  }

  /**
   * Detect if anyone is listening to this connection.
   **/
  public boolean ping(aleph.comm.Address destination) {
    return getConnection((Address) destination).ping();
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
   **/
  private synchronized Connection getConnection(Address destination)  {
    Connection connect = (Connection) connections.get(destination);
    if (connect == null) {
      // make sure connection is started before placing in map
      connect = new UDPConnection(destination);
      connect.start();
      connections.put(destination, connect);
    }
    return connect;
  }

  /**
   * Listen loop.
   **/
  private class GetPackets extends Thread {
    public void run () {
      if (DEBUG)
        Aleph.debug("getPackets started " + myAddress);
      while (! quit) {
        try {
          Packet packet = Packet.receive(listenSocket);
          getConnection(packet.getAddress()).deliver(packet);
        } catch (IOException e) {
          try {
            listenSocket = new DatagramSocket(myAddress.port);
          } catch (Exception x) {
            Aleph.panic("Rebinding socket throws " + x);
          }
        } catch (Exception e) {	
          Aleph.panic("GetPackets " + e);
        }
      }
      if (DEBUG)
        Aleph.debug("getPackets returns ");
    }
  }
}
