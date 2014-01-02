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
import aleph.Listener;
import aleph.PE;
import aleph.UniqueID;
import aleph.comm.CommunicationManager;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
/**
 * The default Event Manager.
 *
 * @see aleph.Event
 * @see aleph.event.EventManager
 *
 * @author Maurice Herlihy
 * @date   March 1998
 **/

public class SimpleEventManager extends EventManager {

  static final boolean DEBUG = false; // Cannon to the right of them ...

  // Server-side hash tables.
  // UniqueID -> vector of PEs to notify
  Hashtable notifyTable;
  // UniqueID -> history (vector of past signals)
  Hashtable historyTable;
  // The ubiquitous connection manager
  static CommunicationManager cManager = CommunicationManager.getManager();

  // Client-side hash tables
  // id -> Eventlistener
  Hashtable listenerTable;

  /**
   * Only constructor.
   **/
  SimpleEventManager() {
    notifyTable   = new Hashtable();
    historyTable  = new Hashtable();
    listenerTable = new Hashtable();
  }

  /**
   * Create a new event.
   **/
  public synchronized void newEvent(UniqueID id) {
    if (notifyTable.containsKey(id)) // sanity check
      Aleph.panic("Event " + id + " registered twice");
    historyTable.put(id, new Vector()); // no history so far
    notifyTable.put(id, new Vector()); // no one cares
  }

  /**
   * Register a listener for this event.  Panics if listener already set.
   **/
  public void setListener(UniqueID id, Listener e) {
    try {
      if (listenerTable.containsKey(id))
	Aleph.panic("Event " + id + " listener already set.");
      listenerTable.put(id, e);
      if (DEBUG)
        Aleph.debug("sending register message to " + id.getHome());
      cManager.send(id.getHome(), new RegisterMessage(id));
    } catch (Exception ex) {
      Aleph.panic(ex);
    }
  }

  /**
   * Reregister a listener for this event.
   **/
  public void resetListener(UniqueID id, Listener e) {
    try {
      if (listenerTable.containsKey(id)) { // no need to reregister
        listenerTable.put(id, e);
      } else {
        listenerTable.put(id, e);
        if (DEBUG)
          Aleph.debug("sending register message to " + id.getHome());
        cManager.send(id.getHome(), new RegisterMessage(id));
      }
    } catch (Exception ex) {
      Aleph.panic(ex);
    }
  }

  /**
   * Remove the listener for this event, if any.
   **/
  public void removeListener(UniqueID id) {
    try {
      listenerTable.remove(id);
    } catch (Exception ex) {
      Aleph.panic(ex);
    }
  }

  /**
   * Signal event to interested PEs.
   * @param id event's id
   * @param object what to deliver
   * @param flush discard earlier signals?
   **/
  public void signal(UniqueID id, Object object, boolean flush) {
    try {
      if (DEBUG)
        Aleph.debug("sending signal message to " + id.getHome());
      cManager.send(id.getHome(), new SignalMessage(id, object, flush));
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }

  /**
   * Message: client PE -> home PE.  Signal this event.
   **/
  public static class SignalMessage
    extends aleph.Message implements Externalizable {

    private Object   object;
    private UniqueID id;
    private boolean  flush;

    public SignalMessage(UniqueID id, Object object, boolean flush) {
      this.object = object;
      this.id     = id;
      this.flush  = flush;
    }
    public SignalMessage() {}
    public void run() {
      SimpleEventManager manager = (SimpleEventManager) getManager();
      manager.signalHandler(id, object, flush);
    }
    /**
     * @see java.io.Externalizable
     **/
    public void writeExternal(ObjectOutput out) throws IOException {
      super.writeExternal(out);
      out.writeObject(object);
      out.writeObject(id);
      out.writeBoolean(flush);
    }
    /**
     * @see java.io.Externalizable
     **/
    public void readExternal(ObjectInput in)
      throws IOException, java.lang.ClassNotFoundException {
      super.readExternal(in);
      object = in.readObject();
      id     = (UniqueID) in.readObject();
      flush  = in.readBoolean();
    }

    public String toString() {
      return "SignalMessage[from: " + from +
	",object:" + object +
	", id: " + id +
	(flush? ", flush]" : "]");
    }
  }
  /**
   * Called by SignalMessage.
   **/
  void signalHandler(UniqueID id, Object object, boolean flush) {
    try {
      Vector notify = (Vector) notifyTable.get(id);
      Vector signals = new Vector();
      signals.addElement(object);
      NotifyMessage wakeup = new NotifyMessage(id, signals);
      for (Enumeration enm = notify.elements(); enm.hasMoreElements();) {
        if (DEBUG)
          Aleph.debug("sending notify message to " + id.getHome());
	cManager.send((PE) enm.nextElement(), wakeup);
      }
      Vector history = (Vector) historyTable.get(id);
      if (flush)		// add new history to table
	historyTable.put(id, signals);
      else                    // append latest object to history
	history.addElement(object);
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }
      

  /**
   * Message: home PE -> client PE.  Event was signalled.
   **/
  public static class NotifyMessage
    extends aleph.Message implements Externalizable {
    private UniqueID id;
    private Vector   history;

    public NotifyMessage(UniqueID id, Vector history) {
      this.id = id;
      this.history = history;
    }
    public NotifyMessage() {}
    public void run() {
      SimpleEventManager manager = (SimpleEventManager) getManager();
      manager.notifyHandler(id, history);
    }

   /**
     * @see java.io.Externalizable
     **/
    public void writeExternal(ObjectOutput out) throws IOException {
      super.writeExternal(out);
      out.writeObject(id);
      out.writeObject(history);
    }
    /**
     * @see java.io.Externalizable
     **/
    public void readExternal(ObjectInput in)
      throws IOException, java.lang.ClassNotFoundException {
      super.readExternal(in);
      id      = (UniqueID) in.readObject();
      history = (Vector) in.readObject();
    }
    public String toString() {
      StringBuffer s = new StringBuffer("NotifyMessage[id: ");
      s.append(id.toString());
      s.append(", history: ");
      if (history == null)
        s.append("no");
      else
        s.append(Integer.toString(history.size()));
      s.append(" entries]");
      return s.toString();
    }
    
  }
  /**
   * Called by notifyMessage.
   **/
  void notifyHandler(UniqueID id, Vector history) {
    Listener listener = (Listener) listenerTable.get(id);
    for (Enumeration enm = history.elements(); enm.hasMoreElements();)
      listener.actionPerformed(enm.nextElement());
  }
     
  /**
   * Message: client PE -> home PE.  I'm interested in this event.
   **/
  public static class RegisterMessage
    extends aleph.Message implements Externalizable {
    private UniqueID id;
    private PE pe;

    public RegisterMessage(UniqueID id) {
      this.id = id;
      this.pe = PE.thisPE();
    }
    public RegisterMessage() {}
    public void run() {
      ((SimpleEventManager) getManager()).registerHandler(id, pe);
    }
  /**
   * @see java.io.Externalizable
   **/
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeObject(id);
    out.writeObject(pe);
  }
  /**
   * @see java.io.Externalizable
   **/
  public void readExternal(ObjectInput in)
    throws IOException, java.lang.ClassNotFoundException {
    super.readExternal(in);
    id = (UniqueID) in.readObject();
    pe = (PE) in.readObject();
  }
    public String toString() {
      return "RegisterMessage[id:" + id + "]";
    }
  }
  /**
   * Called by RegisterMessage
   **/
  void registerHandler(UniqueID id, PE pe) {
    try {
      Vector addresses = (Vector) notifyTable.get(id);
      addresses.addElement(pe);
      // Did client miss anything?
      Vector history = (Vector) historyTable.get(id);
      if (history != null && history.size() > 0) {
        if (DEBUG)
          Aleph.debug("sending notify message to " + id.getHome());
	cManager.send(pe, new NotifyMessage(id, history));
      }
    } catch (Exception e) {Aleph.panic(e);}
  }

  /**
   * @return which event manager are we?
   **/
  public String getLabel() {
    return "Simple";
  }
}
