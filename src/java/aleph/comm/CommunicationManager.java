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

package aleph.comm;

import aleph.Aleph;
import aleph.Config;
import aleph.Message;
import aleph.PE;
import java.io.IOException;
import java.io.InterruptedIOException;

/**
 * The Communication Manager provides reliable, FIFO, point-to-point
 * communication.  It is an abstract class, meaning that the real work is done
 * by a subclass.
 *
 * @see aleph.Config#communicationManagers
 *
 * @author Maurice Herlihy
 * @date   August 1998
 **/

public abstract class CommunicationManager {

  /**
   * The current communication manager.
   **/
  protected static CommunicationManager theManager = null; // singleton

  /**
   * @return the current CommunicationManager
   **/
  public static CommunicationManager getManager () {
    try {
      if (theManager == null) {
        String theClass = Aleph.getProperty("aleph.communicationManager");
        theManager =
          (CommunicationManager) Class.forName(theClass).newInstance();
      }
      return theManager;
    } catch (Exception e) {
      Aleph.panic(e);
    }
    return theManager;
  }
  
  /**
   * Change communication managers in mid-stream.
   * Applications should probably not call this method.
   **/
  public static void setManager (String newManager) {
    try {
      if (theManager != null)   // retire the old manager
        theManager.close();
      theManager =              // install the new one
        (CommunicationManager) Class.forName(newManager).newInstance();
      Aleph.setProperty("aleph.communicationManager", newManager);
    } catch  (ClassNotFoundException e) {
      Aleph.warning("Cannot find communication manager " + newManager);
      Aleph.exit(-1);
    } catch  (Exception e) {
      Aleph.panic(e);
    }
  }

  /**
   * Sends a message
   * @param pe destination PE
   * @param message what to send
   **/
  public void send (PE pe, Message message) throws IOException {
    send(pe.getAddress(), message);
  }

  /* Abstract methods below this line */

  /**
   * @return my address
   **/
  public abstract Address getAddress();

  /**
   * The Console is the target for all I/O.
   * @return the Console's address
   **/
  public abstract Address getConsoleAddress ();

  /**
   * Every PE has a parent.
   * @return Parent's address
   **/
  public abstract Address getParentAddress ();

  /**
   * Sends a message
   * @param address destination Address
   * @param message what to send
   **/
  public abstract void send (Address address, Message message) throws IOException;

  /**
   * Shuts down this CommunicationManager
   **/
   public abstract void close();

  /**
   * Make sure all messages sent to address have been received
   * @param address connection to flush
   **/
   public abstract void flush(Address address) throws InterruptedIOException;

  /**
   * Is this connection still alive?
   * @param address connection to test
   **/
   public abstract boolean ping(Address address);

}
