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

/**
 * Keep track of distribution over set of bins.
 *
 * @author Maurice Herlihy
 * @date   August 1998
 **/

public class Bins extends Meter {

  long[]   bins;
  String[] labels;
  
  public Bins(String label, String[] labels) {
    super(label);               // make sure superclass knows
    this.labels = labels;
    this.bins   = new long[labels.length];
  }

  /**
   * Increment success count
   **/
  public void inc(int i) {
    bins[i] += 1;               // supposed to be atomic!
  }

  public String toString() {
    StringBuffer s = new StringBuffer();
    double total = 0;
    for (int i = 0; i < bins.length; i++)
      total += (double) bins[i];
    if (total <= 0.0)
      return "no trials";
    for (int i = 0; i < bins.length; i++) {
      s.append(labels[i]);
      s.append(" ");
      s.append(Long.toString(bins[i]));
      s.append("(");    
      s.append(Double.toString(((double) bins[i] / total) * 100.0));
      s.append("%) ");
    }
    return s.toString();
  }

}
