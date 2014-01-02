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
package aleph.dir.cm.home;

import aleph.Aleph;
import aleph.GlobalObject;
import aleph.PE;
import aleph.dir.DirectoryManager;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Server grants object to client.
 * @see aleph.dir.cm.home.RetrieveRequest
 *
 * @author Maurice Herlihy
 * @date   March 1997
 **/

public class RetrieveResponse extends aleph.Message implements Constants {

  private Object       object;	// sender: object to be encoded
  private GlobalObject key;	// universal key for object
  private int          mode;	// defined in aleph.trans.Constants
  private PE           pe;	// defined in aleph.trans.Constants

  static final boolean DEBUG = false;

  /**
   * Constructor
   **/
  RetrieveResponse(GlobalObject key, Object object, int mode) {
    this.object = object;
    this.key    = key;
    this.mode   = mode;
    this.pe     = PE.thisPE();
  }

  /**
   * No-arg constructor needed to make class Externalizable.  Do not use.
   * @see java.io.Externalizable
   **/
  public RetrieveResponse() {}

  /**
   * @see java.io.Externalizable
   **/
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeObject(object);
    out.writeObject(key);
    out.writeInt(mode);
    out.writeObject(pe);
  }

  /**
   * @see java.io.Externalizable
   **/
  public void readExternal(ObjectInput in)
    throws IOException, java.lang.ClassNotFoundException {
    super.readExternal(in);
    object = in.readObject();
    key    = (GlobalObject) in.readObject();
    mode   = in.readInt();
    pe     = (PE) in.readObject();
  }

  private String printMode(int modes) {
    switch (mode) {
    case READ_MODE  : return "r";
    case WRITE_MODE : return "w";
    default :         return "?";
    }
  }

  public String toString() {
    return "RetrieveResponse[" + from +
      ", " + printMode(mode) +
      ", " + key +
      ", " + object +
      "]";
  }

  public void run() {
    if (DEBUG) {
      System.out.println(this + " delivered ");
      System.out.flush();
    }
    try {
      HomeDirectory dManager = (HomeDirectory) DirectoryManager.getManager();
      dManager.retrieveResponse(key, object, mode);
    } catch (Exception e) {
      Aleph.panic(e);
    }
  };

}
