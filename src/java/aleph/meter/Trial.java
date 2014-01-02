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
 * Keep track of success/failure ratio.
 *
 * @author Maurice Herlihy
 * @date   August 1998
 **/

public class Trial extends Meter {

  long success;                 // how may successes?
  long failure;                 // how may failures?
  
  public Trial(String label) {
    super(label);               // make sure superclass knows
    success = failure = 0;
  }

  /**
   * Increment success count
   **/
  public void success() {
    success += 1;               // supposed to be atomic!
  }

  /**
   * Increment success count
   **/
  public void failure() {
    failure += 1;               // supposed to be atomic!
  }

  public String toString() {
    StringBuffer s = new StringBuffer();
    double total = (double) success + failure;
    if (total > 0.0) {
      double percent = (((double) success) * 100.0) / total;
      s.append(Long.toString(success));
      s.append(":");
      s.append(Long.toString(failure));
      s.append(" = ");
      s.append(Double.toString(percent));
      s.append("% success rate");
    } else {
      s.append("no trials");
    }
    return s.toString();
  }

}
