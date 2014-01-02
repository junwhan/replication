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

package aleph.desktop;

import aleph.Aleph;
import aleph.Config;
import aleph.Host;
import aleph.comm.Client;
import aleph.comm.CommunicationManager;
import aleph.dir.DirectoryManager;
import aleph.event.EventManager;
import aleph.trans.TransactionManager;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Calls Aleph without an AWT console.
 *
 * @author Maurice Herlihy
 * @date   April 1998
 **/

public class Batch {

  private Client client;
  private Options options;

  /**
   * Create batch job running on only one PE.
   * @param host where to run
   * @param args user's arguments
   **/
  public Batch (String[] args) {
    this(null, args);
  }

  /**
   * Create batch job running on only one PE.
   * @param host where to run
   * @param args user's arguments
   **/
  public Batch (String host, String[] args) {
    Aleph.setDebugFile("alephBatch");
    System.out.print(Aleph.getLongBanner());
    System.out.flush();
    options = new Options();    // initialize to default values
    Set liveHosts = findLiveHosts(host); // round up the usual hosts
    if (liveHosts.size() == 0) {
      System.out.println("No hosts found");
      System.exit(1);
    }
    // Tell user who's listening.
    System.out.print("Hosts found at:\n\t");
    for (Iterator iter = liveHosts.iterator(); iter.hasNext(); ) {
      System.out.print(iter.next().toString());
      System.out.print(" ");
    }
    System.out.println();

    // Display command
    for (int i = 0; i < args.length; i++) {
      System.out.print(args[i]);
      System.out.print(" ");
    }
    System.out.println();

    // If default numPEs is zero, make one for each live host.
    int numPEs = (options.numPEs == 0) ? liveHosts.size() : options.numPEs;

    // Initialize per-PE output streams
    OutputStream[] output = new OutputStream[numPEs];
    for (int i = 0; i < output.length; i++)
      output[i] = System.out;

    client = new Client(numPEs,
                        liveHosts,
                        args,
                        output,
                        options.logging);
    client.run();
    System.exit(0);
  }

  /**
   * Start batch job directly from shell.  If first two args are "-h host", run
   * only at specified host.  Otherwise, run everywhere.
   **/
  static public void main (String[] args) {
    if (args.length > 1 && args[0].equals("-h")) {
      String[] newArgs = new String[args.length -2];
      System.arraycopy(args, 2, newArgs, 0, args.length-2);
      Batch console = new Batch(args[1], newArgs);
    } else {
      new Batch(args);
    }
  }

  /**
   * Locate live hosts.
   * @param host if non-null, check if this host is live. Otherwise check all hosts.
   * @return Set of live hosts
   **/
  private Set findLiveHosts(String host) {
    Set liveHosts = new HashSet();
    try {
      if (host == null) {
        for (Iterator iter = Host.allHosts(); iter.hasNext(); ) {
          Host nextHost = (Host) iter.next();
          if (nextHost.ping())
            liveHosts.add(nextHost);
        }
      } else {
        Host nextHost = new Host(host);
        if (nextHost.ping())
          liveHosts.add(nextHost);
      }
    } catch (UnknownHostException e) {
      System.err.println(e);
      System.exit(-1);
    }
    return liveHosts;
  }
}
