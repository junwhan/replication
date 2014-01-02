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
import aleph.Config;
import aleph.Constants;
import aleph.Message;

import java.io.IOException;
import java.io.InterruptedIOException;

import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CommunicationManager extends aleph.comm.CommunicationManager {

  static final boolean DEBUG = false;   // La lutte finale ....

  Address     myAddress = null;
  Address     consoleAddress = null;
  Address     parentAddress = null;
  Delivery    delivery = null;

  /**
   * Per-connection state information.  This is the only synchronized object.
   **/
  Map transmissions = new HashMap(); // unsynchronized

  /**
   * No-args constructor.
   **/
  public CommunicationManager () {
    super();
    try {
      theManager = this;
      myAddress = new Address();
      delivery = new DeliveryImpl();
      if (DEBUG)
        Aleph.debug("Registering as " + myAddress.toString());
      if (Aleph.verbosity(Constants.LOQUACIOUS))
        Aleph.inform("Registering as " + myAddress.toString());
      Registry registry = null;
      try {
        registry = LocateRegistry.createRegistry(Aleph.getPort());
        if (Aleph.verbosity(Constants.BLOVIATING))
          Aleph.inform("Started rmiregistry at port " + Aleph.getPort());
      } catch (java.rmi.server.ExportException e) {
        registry = LocateRegistry.getRegistry(Aleph.getPort());
        if (Aleph.verbosity(Constants.BLOVIATING))
          Aleph.inform("Found running rmiregistry at port " + Aleph.getPort());
      }
      Naming.rebind(myAddress.toString(), delivery);
    } catch (java.rmi.ConnectException e) {
      Aleph.exit("RMI Communication Manager failed: no RMI server running?\n" + e);
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }

  /**
   * @return my address
   **/
  public aleph.comm.Address getAddress () {
    return myAddress;
  }

  /**
   * @return who we are
   **/
  public String getLabel () {
    return "RMI";
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
      if (myAddress.equals(address)) // short-circuit?
        Aleph.debug("CommunicationManager short-circuiting " + message);
      else
        Aleph.debug("CommunicationManager sending " + message + " to " +
                    address);
    if (myAddress.equals(address)) { // short-circuit?
      delivery.deliver(message.copy()); // deliver copy, not original
    } else {
      getTransmission(address).send(message);
    }
  }

  /**
   * Shuts down this CommunicationManager
   **/
  public void close () {
    try {
      Naming.unbind(myAddress.toString());
    } catch (Exception e) {
    if (Aleph.verbosity(Constants.BLOVIATING))
      Aleph.warning(e);
    }
  }
  
  /**
   * Wait until all packets sent to this address are acknowledged.
   * @param destination where packets were sent
   * @param timout when to give up
   * @throws InterruptedIOException on timeout
   **/
  public void flush (aleph.comm.Address destination) throws InterruptedIOException {
  }

  /**
   * @return is anyone listening?
   **/
  public boolean ping (aleph.comm.Address destination) {
    try {
      return getTransmission((Address) destination).ping();
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
   * Lazily create a Transmission object
   **/
  private synchronized Transmission getTransmission(Address destination) throws IOException {
    Transmission transmit = (Transmission) transmissions.get(destination);
    if (transmit == null) {
      transmit = new Transmission(destination);
      transmissions.put(destination, transmit);
    }
    return transmit;
  }

  private class Transmission {
    public Address address;
    public Delivery delivery;
    public Transmission (Address address) throws IOException {
      try {
        delivery = (Delivery) Naming.lookup(address.toString());
      } catch (java.rmi.NotBoundException e) {
        throw new IOException(e.toString());
      } catch (Exception e) {
        Aleph.panic(e);
      }
    }
    public void send (Message message) throws IOException {
      try {
        delivery.deliver(message); // rmi
      } catch (java.rmi.MarshalException e) { // not serializable?
        Aleph.panic(e);
      } catch (java.rmi.RemoteException e) { // EOF or Shutdown messages...
        if (Aleph.verbosity(Constants.LOQUACIOUS))
          Aleph.warning(e);
        return;
      } catch (Exception e) {
        Aleph.panic(e);
      }
    }
    public boolean ping () {
      try {
        delivery.ping();
        return true;
      } catch (Exception e) {
        return false;
      }
    }
  }

}


