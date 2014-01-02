/*
 * Aleph Toolkit
 *
 * Copyright 1998, Brown University, Providence, RI.
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
package aleph.bench.rmi;

import java.rmi.Naming;
import aleph.PE;

/**
 * The <code>aleph.bench.rmi</code> package exists to test the latency of the
 * native <code>java.rmi</code> implementation, establishing an essential
 * baseline for evaluating Aleph communication packages.
 * <p>
 * This class returns round-trip communication times using Java RMI.
 * <p>
 * In any kind of reasonable shell, a Makefile can do most of this work.
 * <il>
 * <li> Pick hosts P and Q.
 * <li> Compile <code>aleph.bench.rmi.*.java</code>.
 * <li> Start <code>rmiregistry</code> on P.
 * <li> Start <aleph.bench.rmi.Server</code> on P.
 * <li> Run <code>aleph.bench.rmi.Client P</code> on Q.
 * </il>
 * @author Maurice Herlihy
 * @date   November 1998
 **/

public class Client {

  private static volatile int howMany = 100;	// how many trips
  private static volatile long start; // start time
  private static volatile long stop; // stop time

  private static final int MAX_SIZE = 8;
  private static final int CHUNK    = 6000;

  static Test destination;

  public static void main (String[] args) {
    if (args.length == 0 || args.length > 1) {
      System.err.println("usage: aleph.bench.rmi.Client <host>");
      System.exit(1);
    }
    try {

      String destinationHandle = "rmi://" + args[0] + Constants.URL;
      destination = (Test) Naming.lookup(destinationHandle);
      
      System.out.println("Size\tmin\tmax\tavg");
      // compute statistics
      for (int i = 0; i < MAX_SIZE; i++) {
	long min = Integer.MAX_VALUE;
	long max = 0;
	long total = 0;
	System.out.print(i * CHUNK);
	System.out.print("\t");
        for (int j = 0; j < howMany; j++) {
	  Message message = new Message();
	  message.payload = new byte[i * CHUNK];
	  start = System.currentTimeMillis();
	  Message reply = destination.deliver(message);
	  stop = System.currentTimeMillis();
	  long duration = stop - start;
	  min = Math.min(min, duration);
	  max = Math.max(max, duration);
	  total += duration;
	  if (reply.payload.length != message.payload.length)
	    System.err.println("uh-oh: payload mismatch!");
	}
        StringBuffer s = new StringBuffer();
	s.append(Long.toString(min));
	s.append("\t");
	s.append(Long.toString(max));
	s.append("\t");
	s.append(Double.toString((double) total/ (double) howMany));
	System.out.println(s.toString());
	System.out.flush();
      }
    } catch (Exception e) {
      System.out.println("aleph.bench.rmi.Client: an exception occurred:");
      e.printStackTrace();
    }
  }
   
}



