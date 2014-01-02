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
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;

/**
 * How to communicate with anyone.
 *
 * @author Maurice Herlihy
 * @author Mike Demmer
 * @date   May 1998
 **/

public class Address implements aleph.comm.Address {

  private static InetAddress localHost;
  private static String myAddress = System.getProperty("myIP");
  public InetAddress inetAddress;
  public int         port;

  public Address(InetAddress inetAddress, int port) {
    this.inetAddress = inetAddress;
    this.port        = port;
  }
  
  public Address(int port) {
    this.inetAddress = localHost;
    this.port        = port;
  }

  public Address(Socket socket) {
    this.inetAddress = localHost;
    this.port        = socket.getLocalPort();
  }

  public Address(String s) {
    try {
      int i = s.indexOf("#");
      inetAddress = InetAddress.getByName(s.substring(0,i));
      port        = Integer.parseInt(s.substring(i+1));
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }
  
  public String toString() {
    if (inetAddress == null) {
      return "<null>" + "#" + port;
    } else {
      return inetAddress.getHostAddress() + "#" + port;
    }
  }

  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof Address))
      return false;
    Address other = (Address) obj;
    return (this.inetAddress.equals(other.inetAddress) && 
            this.port == other.port);
  }

  public int hashCode() {
    return inetAddress.hashCode();
  }

  public final static InetAddress getLocalHost() { return localHost; }
  
  static {
    try {
    	byte[] ip = new byte[4];
    	String[] strIP = myAddress.split("\\.");
    	for(int i=0; i<ip.length; i++)
    		ip[i] = (byte)Integer.parseInt(strIP[i]);
    	
      localHost = InetAddress.getByAddress(ip);
      //InetAddress.getLocalHost();
    } catch (Exception e) {
      Aleph.warning(e);
      System.exit(-9);
    }
  }

  public static void main(String[] args) {
    String s = args[0];
    int i = s.indexOf("#");
    System.out.println(s.substring(0,i));
    System.out.println(s.substring(i+1));
  }

}
