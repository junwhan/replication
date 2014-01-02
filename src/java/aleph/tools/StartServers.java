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

/**

 * Unix-Specific package for starting up aleph Servers using rsh.<br>
 * <code>java aleph.tools.StartServers</code><br>
 * tries to start all servers at hosts in <code>Config.hosts</code><br>
 * <code>java aleph.tools.StartServers</code><i>n</i><br>
 * tries to start first <i>n</i> servers at hosts in <code>Config.hosts</code><br>
 * <code>java aleph.tools.StartServers</code><i>host1 ... hostn</i><br>
 * tries to start servers at specified hosts<br>
 * *
 * @see aleph.Config
 * @author Maurice herlihy
 * @date   May 1997
 **/

package aleph.tools;

import aleph.Aleph;
import aleph.Config;
import java.util.Enumeration;
import java.util.Vector;

public class StartServers {

  static final boolean DEBUG = false; // Geronimo!

  public static void main(String[] args) {
    Runtime runtime = Runtime.getRuntime();
    // Servers send acks to this socket:
    try { 
      String pathRsh = System.getProperty("aleph.pathRsh", Config.pathRsh);
      String pathJava = System.getProperty("aleph.pathJava", Config.pathJava);
      String[] command = {pathRsh,
			  "HOSTNAME", // fill this in later
			  ";",
			  "setenv",
			  "CLASSPATH", // duplicate current classpath
			  System.getProperty("java.class.path"),
			  ";",
			  "cd",	// duplicate current dirctory
			  System.getProperty("user.dir"),
			  "; exec",
			  pathJava,
			  "aleph.comm.Server",
			  "&"};	// return right away when process started.

      if (DEBUG) {
	System.out.println("StartServers execs");
	for (int i = 0; i < command.length; i++) {
	  System.out.println("\t" + command[i]);
	}
      }
      System.out.println("Starting servers at port " + Aleph.getPort());
      int count = 0;
      String[] hosts = null;
      switch (args.length) {
      case 0:                   // default hosts
        hosts = Config.hosts;
        count = hosts.length;
        break;
      case 1:                   // number specified?
        try {
          count = Math.min(Integer.parseInt(args[0]),
                           Config.hosts.length);
          hosts = Config.hosts;
          break;
        } catch (NumberFormatException e) {} // not a number, fall through
      default:
        hosts = args;
        count = args.length;
      }
      for (int i = 0; i < count; i++) {
	command[1] = hosts[i];
	runtime.exec(command);
      }
      Thread.sleep(5000);	// give them a few seconds
      PingServers.main(args);	// see if it worked
      System.exit(0);
    } catch (Exception e) {
      Aleph.warning("Could not start servers: " + e);
      System.exit(-1);
    }
  }
}
