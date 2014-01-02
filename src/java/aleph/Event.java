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

import aleph.Aleph;
import aleph.UniqueID;
import aleph.event.EventManager;
import java.io.Serializable;
import java.util.Hashtable;

/**

 * This class supports light-weight synchronization among PEs.  An
 * <code>Event</code> object provides a kind of atomic broadcast group.  Any
 * thread can <i>signal</i> the event object, and all threads aware of an event
 * are notified of all such signals in the same order.
 *
 * There are two kinds of signals: <i>regular</i>, and <i>flush</i>.  A flush
 * signal causes all records of earlier signals to be discarded.  When an event
 * object is first imported to a PE, the PE must register an
 * <code>Listener</code> object.  The PE is then notified (in order) of
 * the last flush signal, and all subsequent signals by calling the listener's
 * <code>actionPerformed</code> method with the argument (if any) to the signal.
 *
 * @see aleph.Listener
 * @see aleph.comm.EventManager
 *
 * @author Maurice Herlihy
 * @date   July 1998
 **/

public class Event implements Serializable {

  private UniqueID id;          // globally unique id
  protected String label;       // human-readable label

  private static EventManager manager = EventManager.getManager();

  /**
   * Constructor.
   **/
  public Event () {
    this(null);
  }

  /**
   * Constructor.
   **/
  public Event (String label) {
    this.label = label;
    id = new UniqueID();
    manager.newEvent(id);
  }

  /**
   * Send signal to each PE.  Delivers <code>null</code>.
   **/
  public void signal () {
    signal(null, false);
  }

  /**
   * Send signal to each PE.  Delivers <code>null</code>.
   * @param flush Flush earlier signals?
   **/
  public void signal (boolean flush) {
   signal(null, flush);
  }

  /**
   * Send signal to each PE.
   * @parm object Deliver this object to each waiting thread.
   **/
  public void signal (Object object) {
    signal(object, false);
  }

  /**
   * Send signal to each PE.
   * @parm object Deliver this object to each waiting thread.
   * @param flush Flush earlier signals?
   **/
  public void signal (Object object, boolean flush){
    manager.signal(id, object, flush);
  }

  /**
   * Register a listener for this event.  Panics if listener already set.
   **/
  public void setListener (Listener e) {
    manager.setListener(id, e);
  }

  /**
   * Reregister a listener for this event.
   **/
  public void resetListener (Listener e) {
    manager.resetListener(id, e);
  }

  /**
   * Remove the listener for this event, if any.
   **/
  public void removeListener () {
    manager.removeListener(id);
  }

  public String toString () {
    StringBuffer s = new StringBuffer("Event[");
    if (label != null) {
      s.append(label);
      s.append(", ");
    }
    s.append(id);
    s.append("]");
    return s.toString();
  }

  public boolean equals (Object obj) {
    if (obj == null || !(obj instanceof Event))
      return false;
    Event other = (Event) obj;
    return this.id.equals(other.id);
  }

  public int hashCode () {
    return id.hashCode();
  }

}
