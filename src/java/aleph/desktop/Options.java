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

package aleph.desktop;

import aleph.Aleph;
import aleph.Config;
import java.util.Properties;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

/**
 * Keeps track of user-controlled options.
 *
 * @author Maurice Herlihy
 * @date August 1999
 **/

public class Options implements java.io.Serializable {

  /**
   * number of PEs desired (or 0 if one per host)
   **/
  public int numPEs;		// number of PEs requested
  /**
   * Set of all known hosts
   **/
  public Set hosts = new HashSet();
  /**
   * Hosts where server is running.  
   **/
  public Set liveHosts = new HashSet();
  /**
   * Subset of liveHosts selected by user.
   **/
  public Set selectedHosts = new HashSet();

  /**
   * log console I/O to a file?
   **/
  public boolean logging;

  /**
   * use compiler?
   **/
  public boolean compiler = true;

  /**
   * Constructor
   **/
  public Options() {
    // Set flags as requested by run-time switch or Config file.
    logging = (System.getProperty("aleph.logging") != null) || Config.logging;
    logging = (System.getProperty("aleph.noCompiler") != null) || Config.noCompiler;
    numPEs  = Aleph.getIntProperty("aleph.numPEs", Config.numPEs);
  }
  
  public String toString () {
    StringBuffer s = new StringBuffer("State[");
    s.append("numPEs: ");
    s.append(Integer.toString(numPEs));
    if (logging)
      s.append(", logging: on");
    s.append(", selected:" );
    for (Iterator iter = selectedHosts.iterator();
         iter.hasNext();) {
      s.append(" ");
      s.append(iter.next().toString());
    }
    s.append("]");
    return s.toString();
  }

}
