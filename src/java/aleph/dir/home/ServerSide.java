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
package aleph.dir.home;

import aleph.Aleph;
import aleph.GlobalObject;
import aleph.PE;

/**
 * Keeps track of each global object's location and status.
 *
 * @author  Maurice Herlihy
 * @date    July 1997
 **/

public class ServerSide implements Constants {

  private static final boolean DEBUG = false; // Shuffle off to Buffalo ...

  private GlobalObject key;

  // Synchronization state
  private int    pending    = 0;// how may release requests in progress?
  private PE     writer     = null; // Unique writer.
  private PE[]   readers    = null; // Plurality of readers.
  private int    numReaders = 0;    // How many readers?
  private Object object     = null; // The object itself.

  /**
   * Register new global object.
   **/
  public ServerSide(GlobalObject key, Object object) {
    this.key     = key;
    this.readers = new PE[PE.numPEs()]; // max # readers
    this.object  = object;
    Aleph.register("ServerSide " + PE.thisPE(), this);
  }
  
  public ServerSide(GlobalObject key) {
    this.key     = key;
    this.readers = new PE[PE.numPEs()]; // max # readers
    Aleph.register("ServerSide " + PE.thisPE(), this);
  }

  /**
   * Client requests access.<br>
   * Called by RetrieveRequest message.
   * @param from Requesting PE
   * @see aleph.dir.home.RetrieveRequest
   **/
  public synchronized void retrieveRequest(PE from, int mode) {
    if (DEBUG)
      Aleph.debug(this + " retrieveRequest(" + from +
                  ", " + printMode[mode] + ") called");
    try {
      switch (mode) {
      case READ_MODE:
        while (writer != null || pending > 0) {
          if (pending == 0) {	// Did someone ask for object back?
            ReleaseRequest request = new ReleaseRequest(key);
            if (DEBUG)
              Aleph.debug(this + " sending " + request + " to " + writer);
            request.send(writer);
            pending = 1;		// free after 1 release
          } // Otherwise be patient.
          try{ wait(); } catch (InterruptedException e) {};
        }
        readers[numReaders++] = from; // join the readers
        break;
      case WRITE_MODE:
        while (writer != null || numReaders > 0 || pending > 0) {
          if (pending == 0) {	// Did someone ask for object back?
            ReleaseRequest release = new ReleaseRequest(key);
            if (writer != null) {	// nuke writer,
              if (DEBUG)
                Aleph.debug(this + " sending " + release + " to " + writer);
              release.send(writer);
              pending = 1;	// free after one release
            } else {		// or nuke readers
              for (int i = 0; i < numReaders; i++) {
                if (DEBUG)
                  Aleph.debug(this + " sending " + release + " to " + readers[i]);
                release.send(readers[i]); // invalidate
              }
              pending = numReaders;
            }
          }// else // Otherwise, be patient.
	  try{ wait(); } catch (InterruptedException e) {};
        }
        writer = from;
        break;
      default:
        Aleph.panic("Unknown access mode " +
                    mode + " in retrieveRequest");
      }
      RetrieveResponse retrieveResponse = new RetrieveResponse(key, object, mode);
      retrieveResponse.send(from);
      if (DEBUG)
        Aleph.debug(this + " retrieveRequest(" +
                    from + ", " + printMode[mode] + ") returns\n\tsending " +
                    retrieveResponse + " to " + from);
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }

  /**
   * Client releases lock on object.<br>
   * Called by ReleaseResponse message.
   * @param key Unique ID of object released
   * @param object new object value
   * @param object new object value
   * @see java.dir.home.ReleaseResponse
   **/
  public synchronized void releaseResponse(Object object, PE from) {
    if (DEBUG)
      Aleph.debug(this + " releaseResponse(" + object +
                  ", " + from + ") called");
    if (object != null)		// new object version?
      this.object = object;
    pending--;			// One less pending release.
//    Aleph.assert(pending >= 0, "pending is " + pending);
    if (pending == 0 ) {	// Are we there yet?
      writer = null;		// One if by land,
      numReaders = 0;		// and two if by sea ...
      notifyAll();		// The British are coming!
    }
    if (DEBUG)
      Aleph.debug(this + " releaseResponse(" + object +
                  ", " + from + ") returns");
  }

  public String toString() {
    return "ServerSide[Writer:" + writer +
      "|Readers:" + numReaders +
      "|Pending:" +  pending + "]";
  }

}
