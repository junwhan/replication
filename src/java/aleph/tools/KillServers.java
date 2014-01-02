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

package aleph.tools;

import aleph.Aleph;
import aleph.Config;
import aleph.Host;

/**
 * Shut down servers.<br>
 * <code>java aleph.tools.KillServers</code><br>
 * tries to kill all servers at hosts in <code>Config.hosts</code><br>
 * <code>java aleph.tools.KillServers</code><i>n</i><br>
 * tries to kill first <i>n</i> servers at hosts in <code>Config.hosts</code><br>
 * <code>java aleph.tools.KillServers</code><i>host1 ... hostn</i><br>
 * tries to kill servers at specified hosts<br>
 *
 * @author Maurice Herlihy
 * @date   March 1998
 **/
public class KillServers {

  public static void main(String[] args) {
    Aleph.setDebugFile("KillServers");
    int count = 0;
    String[] hosts = null;
    try { 
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
	Host host = new Host(hosts[i]);
	System.out.print("\t");
	System.out.print(host);
	if (host.stop())
	  System.out.println(" shutdown confirmed");
	else
	  System.out.println(" not responding");
      }
      System.exit(0);
    } catch (Exception e) {
      Aleph.warning("KillServers.main failed: " + e);
      System.exit(-1);
    }
  }
}
