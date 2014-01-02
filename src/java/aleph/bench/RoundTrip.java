/*
 * Aleph Toolkit
 *
 * Copyright 1998,1999 Brown University, Providence, RI.
 * 
 *                         All Rights Reserved
 * 
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose other than its incorporation into a commercial
 * product is hereby granted without fee, provided that the above copyright
 * notice appear in all copies and that both that copyright notice and this
 * permission notice appear in supporting documentation, and that the name of
 * Brown University not be used in advertising or publicity pertaining to
 * distribution of the software without specific, written prior permission.
 * 
 * BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE,
 * INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR ANY
 * PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY BE LIABLE FOR ANY
 * SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER
 * RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF
 * CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN
 * CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE
 */

package aleph.bench;

import aleph.Aleph;
import aleph.PE;
import aleph.comm.Address;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;

/**
 * The <code>aleph.bench</code> package exists to establish comparable
 * benchmarks for the Aleph toolkit on various platforms.
 * <p>
 * This class tests round-trip latency for <code>aleph.Message</code>.
 * @author Maurice Herlihy
 * @date   November 1998
 **/
public class RoundTrip {

  private static volatile int howMany = 100;	// how many trips
  private static volatile boolean ok; // handshake bit
  private static volatile long start; // start time
  private static volatile long stop; // stop time
  private static Integer lock = new Integer(0);	// dummy lock

  private static final int MAX_SIZE = 8;
  private static final int CHUNK    = 6000;

  public static void main(String[] args) {
    try {
      if ( args.length > 0)
	howMany = Integer.parseInt(args[0]);
    } catch (NumberFormatException e) {
      System.err.println("usage: RoundTrip <#iterations>");
      Aleph.exit(1);
    }

    // If I'm the first PE, skip one.
    Iterator e = PE.roundRobin();
    PE pe = (PE) e.next(); // first PE
    if (pe.equals(PE.thisPE()))	// it's me
      pe = (PE) e.next(); // use the next PE
    System.out.println("RoundTrip: " + PE.thisPE().getAddress() +
                       " and " + pe.getAddress() +
                       " " + howMany + " rounds.");

    System.out.println("Size\tmin\tmax\tavg");
    try {
      for (int i = 0; i < MAX_SIZE; i++) {
        long min = Integer.MAX_VALUE;
        long max = 0;
        long total = 0;
        PingMessage ping = new PingMessage(i * CHUNK);
        System.out.print(i * CHUNK);
        System.out.print("\t");
        synchronized (lock) {
          for (int j = 0; j < howMany; j++) {
            ok = false;             // reset handshake bit
            start = System.currentTimeMillis();
            ping.send(pe);          // send 'em a message
            while (!ok)             // wait for reply
              try {lock.wait();} catch (InterruptedException x) {};
            long duration = stop - start;
            min = Math.min(min, duration);
            max = Math.max(max, duration);
            total += duration;
          }
        }
        System.out.print(Long.toString(min));
        System.out.print("\t");
        System.out.print(Long.toString(max));
        System.out.print("\t");
        System.out.println(Double.toString((double) total/ (double) howMany));
        System.out.flush();
      }
    } catch (IOException x) {
      Aleph.panic(x);
    }
  }

  static class PingMessage extends aleph.Message implements Externalizable {
    byte[] payload;
    static RoundTrip.PongMessage pong = new RoundTrip.PongMessage();
    public PingMessage () {
      payload = null;
    }

    public PingMessage(int size) {
      payload = new byte[size];
    }
    public void run() {
      try {
        pong.payload = payload;
        pong.send(from);
      } catch (IOException e) {
        Aleph.panic(e);
      }
    }
    public void writeExternal(ObjectOutput out) throws IOException {
      out.writeObject(from);
      out.writeObject(payload);
    }
    public void readExternal(ObjectInput in)
      throws IOException, java.lang.ClassNotFoundException {
      from = (Address) in.readObject();
      payload = (byte[]) in.readObject();
    }
  }

  public static class PongMessage extends aleph.Message implements Externalizable {
    byte[] payload;
    public void run() {
      stop = System.currentTimeMillis();
      ok = true;
      synchronized (lock) {
        lock.notifyAll();
      }
    }
    public void writeExternal(ObjectOutput out) throws IOException {
      out.writeObject(from);
      out.writeObject(payload);
    }
    public void readExternal(ObjectInput in)
      throws IOException, java.lang.ClassNotFoundException {
      from = (Address) in.readObject();
      payload = (byte[]) in.readObject();
    }
  }
}
