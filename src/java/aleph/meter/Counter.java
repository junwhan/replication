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
 * Keep track of the number of times some event has occurred.
 *
 * @author Maurice Herlihy
 * @date   August 1998
 **/

public class Counter extends Meter {

  long   count;                 // how may times has this event happened?
  
  public Counter(String label) {
    super(label);               // make sure superclass knows
    this.count = 0;             // things we ain't done yet
  }

  /**
   * Increment count for this event.
   **/
  public void inc() {
    count += 1;                 // supposed to be atomic!
  }

  /**
   * Add to this event.
   **/
  public void add(long i) {
    count += i;                 // supposed to be atomic!
  }
    
  public String toString() {
    return Long.toString(count);
  }

}
