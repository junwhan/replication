/*
 * Aleph Toolkit
 *
 * Copyright 1999 Brown University, Providence, RI.
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

import aleph.Aleph;
import aleph.comm.Address;
import aleph.comm.AlephServer;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.NoSuchElementException;

/**
 * A Host is a machine running the Aleph Server.  Multiple PEs coexist on a host.
 *
 * @author Maurice Herlihy
 * @date August 1998
 * @see aleph.CommunicationManager
 * @see java.io.serializable
 **/

public class Host implements Serializable {

  private static List hosts = new ArrayList(32);
  private static Host thisHost;

  private String label;         // human-readable
  private String name;          // for rmiregistry

  /**
   * Constructor.
   * @param hostName string name for host
   * @exception UnknownHostException if hostName can't be resolved
   **/
  public Host (String hostName) throws UnknownHostException {
    label = hostName + ":" + Config.PORT;
    name = "rmi://" +
      InetAddress.getByName(hostName).getHostAddress() + // long host name
      ":" +
      Config.PORT +
      "/aleph/comm/Server";
  }

  /**
   * start a new PE
   * @param parent Parent PE's address
   * @param console Console's address
   * @param index index of this PE in group
   * @param numPEs total number in group
   * @param id per/hunique
   * @param label suggestive label
   * @param args user's args
   * @param properties caller's environment
   **/
  public void startPE (Address parent,
                       Address console,
                       int index,
                       int numPEs,
                       Integer id,
                       String label,
                       String[] args,
                       Properties properties) {
    try {
    AlephServer server = getServer();
    if (server != null)
      server.startPE(parent, console, index, numPEs, id, label, args, properties);
    else
      Aleph.panic("No server running at " + name);
    } catch (java.rmi.RemoteException e) {
      Aleph.panic("While locating server: " + e);
    }
  }


  /**
   * @return whether a host seems to be running the server.
   **/
  public boolean ping () {
    try {
      Naming.lookup(name);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Shuts down a server.
   * @return whether shutdown is confirmed.
   * @exception SecurityException If caller is not allowed to kill this server.
   **/
  public boolean stop () throws SecurityException {
    try {			// croak on unexpected exceptions
      AlephServer server = getServer();
      if (server != null) {
        server.shutdown(InetAddress.getLocalHost().getHostAddress());
        return true;            // no problem
      } else {
        return false;           // something happened
      }
    } catch (java.rmi.UnmarshalException e) { // server shut down too fast
      return true;
    } catch (SecurityException e) {
      throw e;
    } catch (Exception e) {
      return false;
    }
  }

  public String toString () {
    return label;
  }
    
  public int hashCode () {
    return name.hashCode();
  }

  public boolean equals (Object anObject) {
    if ((anObject != null) && (anObject instanceof Host)) {
      Host other = (Host) anObject;
      return (other.name.equals(this.name));
    } else {
      return false;
    }
  }

  /**
   * @return the host on which we are running
   **/
  public static Host thisHost () {
    return thisHost;
  }

  /**
   * @return iterator over currently known hosts
   **/
  public static Iterator allHosts () {
    return hosts.iterator();
  }

  /**
   * @return number of currently known hosts
   **/
  public static int size () {
    return hosts.size();
  }

  /**
   * @return Iterate hosts in round-robin order.
   **/
  public static Iterator roundRobin () {
    return new Iterator() {	// Anonymous iterator
      Iterator e = hosts.iterator();
      public boolean hasNext() {
	return true;
      }
      public Object next() throws NoSuchElementException {
	if (! e.hasNext()) // got milk?
	  e = hosts.iterator();
	return e.next();
      }
      public void remove () {
        throw new UnsupportedOperationException();
      }
    };
  }

  private AlephServer getServer () throws java.rmi.RemoteException {
    try {
      return (AlephServer) Naming.lookup(name);
    } catch (Exception e) {
      if (Aleph.verbosity(Constants.LOQUACIOUS))
        Aleph.warning(e);
      return null;
    }
  }

  static {
    // transform Config.hosts String[] into set of Host
    for (int i = 0; i < Config.hosts.length; i++) {
      try {
        Host host = new Host(Config.hosts[i]);
        hosts.add(host);
      } catch (UnknownHostException e) {
        Aleph.error("Unknown host " + Config.hosts[i] + " in Config.hosts");
      }
    }
    /* compute my host */
    try {
      thisHost = new Host(InetAddress.getLocalHost().getHostAddress());
    } catch (UnknownHostException e) {
      Aleph.error(e.toString());
      System.exit(-1);		// too early for Aleph.panic
    }

    /* Does user want own host to be first? */
    try {
      if (Config.localHostFirst) {
        hosts.remove(thisHost); // remove if present
        hosts.add(0, thisHost); // make it first
      }
    } catch (Exception e) {
      Aleph.error(e);
      System.exit(-1);
    }
  }

}

