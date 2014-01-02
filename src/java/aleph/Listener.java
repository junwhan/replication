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

package aleph;

/**
 * Implements the <i>Listener</i> pattern within Aleph.  A module that wants to
 * be informed of an event creates a <code>Listener</code> object and registers
 * it.  When the event occurs, the listener's <code>actionPerformed</code> method is called.

 * <code>actionPerformed</code> method is called.  An
 * <code>actionPerformed</code> method must <blink><b>never block</b></blink>
 * or your PE will surely wedge.
 *
 * @see aleph.Event
 * @see aleph.PE
 *
 * @author Maurice Herlihy
 * @date   July 1998
 **/

public interface Listener {
  /**
   * Invoked when an action occurs.
   * @param object arbitrary object.
   **/
  public void actionPerformed(Object object);
}
