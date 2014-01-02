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
import aleph.dir.DirectoryManager;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Message client -> server returning object.
 * @see aleph.trans.RetrieveResponse
 * @see aleph.Message
 *
 * @author Maurice Herlihy
 * @date   March 1997
 **/
public class ReleaseResponse
  extends aleph.Message implements Constants, Externalizable {

  static HomeDirectory directory = (HomeDirectory) DirectoryManager.getManager();

  Object       object;
  GlobalObject key;
  PE           pe;

  static final boolean DEBUG = false; // Yee-HAW !!!
  /**
   * Constructor
   **/
  ReleaseResponse(GlobalObject key, Object object) {
    this.key    = key;
    this.object = object;
    this.pe     = PE.thisPE();
  }
  /**
   * No-arg constructor needed to make class Externalizable.  Do not use.
   * @see java.io.Externalizable
   **/
  public ReleaseResponse() {}

  /**
   * @see java.io.Externalizable
   **/
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeObject(object);
    out.writeObject(key);
    out.writeObject(pe);
  }

  /**
   * @see java.io.Externalizable
   **/
  public void readExternal(ObjectInput in)
    throws IOException, java.lang.ClassNotFoundException {
    super.readExternal(in);
    object = in.readObject();
    key = (GlobalObject) in.readObject();
    pe = (PE) in.readObject();
  }

  public String toString() {
    return "ReleaseResponse[from: " + from + ", " + key + ", " + object + "]";
  }

  public void run() {
    directory.releaseResponse(key, object, pe);
  }

}
