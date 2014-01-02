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

package aleph.meter;

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Vector;

/**

 * All metering-related classes should be subclasses of this class.  When a PE
 * or console shuts down, it optionally dumps all metering information to a file.
 *
 * @author Maurice Herlihy
 * @date   February 1998
 **/

public class Meter {
  private String label;

  // All Meter objects created at this PE.
  private static Vector stats = new Vector();

  /**
   * Generic constructor.  Records each object for later report.
   **/
  protected Meter (String label) {
    this.label = label;
    stats.addElement(this);
  }
    
  /**
   * Write all metering information to stream.
   **/
  public static void report (PrintWriter writer) {
    for (Enumeration enm = stats.elements(); enm.hasMoreElements(); ) {
      Meter meter = (Meter) enm.nextElement();
      writer.print(meter.label);
      writer.print(":\t");
      writer.println(meter.toString());
    }
  }

}
