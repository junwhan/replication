/*
 * Aleph Toolkit
 *
 * Copyright 1997, Brown University, Providence, RI.
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
import aleph.PE;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Random;

/**
 * How to communicate with anyone.
 *
 * @author Maurice Herlihy
 * @date   May 1998
 **/

public class Address implements aleph.comm.Address {

  private static Random random = new Random();

  private String name;

  public Address(String name) {
    this.name = name;
  }

  /**
   * An address is a URL that looks like
   * //<code>rmi://hostname:port/PE.x.randomInt</code>
   **/
  public Address () {
    try {
      name = "rmi://" +
        InetAddress.getLocalHost().getHostAddress() + // long host name
        ":" +
        Aleph.getPort() +
        "/aleph/comm/rmi/" +
        System.getProperty("aleph.label",
                           "console@" +
                           InetAddress.getLocalHost().getHostAddress()) +
        "." + Math.abs(random.nextInt());
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }
  public String toString() {
    return name;
  }

  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof Address))
      return false;
    Address other = (Address) obj;
    return this.name.equals(other.name);
  }

  public int hashCode() {
    return name.hashCode();
  }

}
