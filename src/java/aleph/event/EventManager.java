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

package aleph.event;

import aleph.Aleph;
import aleph.Config;
import aleph.Listener;
import aleph.UniqueID;

/**
 * Handles the underlying communication necessary to support distributed
 * events.  Customizable.  Extending classes must define a no-arg constructor.
 *
 * @see aleph.Event
 *
 * @author Maurice Herlihy
 * @date   August 1998
 **/

public abstract class EventManager {

  private static EventManager theManager = null; // singleton

  public static EventManager getManager() {
    try {
      if (theManager == null) {
        String theClass = Aleph.getProperty("aleph.eventManager"); // default choice
        theManager = (EventManager) Class.forName(theClass).newInstance();
      }
    return theManager;
    } catch (Exception e) {
      Aleph.panic(e);
    }
    return theManager;
  }

  /**
   * Change event managers in mid-stream.
   * Applications should probably not call this method.
   **/
  public static void setManager (String newManager) {
    try {
      theManager =              // install the new one
        (EventManager) Class.forName(newManager).newInstance();
      Aleph.setProperty("aleph.eventManager", newManager);
    } catch  (ClassNotFoundException e) {
      Aleph.error("Cannot find event manager " + newManager);
      Aleph.exit(-1);
    } catch  (Exception e) {
      Aleph.panic(e);
    }
  }

  /**
   * Create a new event.
   **/
  public abstract void newEvent(UniqueID id);

  /**
   * Register a listener for this event.  Panics if listener already set.
   **/
  public abstract void setListener(UniqueID id, Listener e);

  /**
   * Reregister a listener for this event.
   **/
  public abstract void resetListener(UniqueID id, Listener e);

  /**
   * Remove the listener for this event, if any.
   **/
  public abstract void removeListener(UniqueID id);

  /**
   * Signal event to interested PEs.
   * @param id event's id
   **/
  public void signal(UniqueID id) {
    signal(id, null, false);
  }

  /**
   * Signal event to interested PEs.
   * @param id event's id
   * @param object what to deliver
   **/
  public void signal(UniqueID id, Object object) {
    signal(id, object, false);
  }

  /**
   * Signal event to interested PEs. Delivers <code>null</code>.
   * @param id event's id
   * @param flush discard earlier signals?
   **/
  public void signal(UniqueID id, boolean flush) {
    signal(id, null, flush);
  }

  /**
   * Signal event to interested PEs.
   * @param id event's id
   * @param object what to deliver
   * @param flush discard earlier signals?
   **/
  public abstract void signal(UniqueID id, Object object, boolean flush);

  /**
   * @return which event manager are we?
   **/
  public abstract String getLabel();

}
