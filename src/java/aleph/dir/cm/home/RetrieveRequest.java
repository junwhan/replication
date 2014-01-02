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

import org.deuce.transaction.AbstractContext;

/**
 * Message client -> server requesting object.
 * @see aleph.default.trans.RetrieveResponse
 * @see aleph.threads.Message
 *
 * @author Maurice Herlihy
 * @date   March 1997
 **/

public class RetrieveRequest
  extends aleph.AsynchMessage implements Constants, Externalizable {

  GlobalObject key;
  int          mode;		// Defined in aleph.trans.Constants.
  PE           pe;
  AbstractContext context;

  static final boolean DEBUG = false;

  /**
   * Constructor
   **/
  RetrieveRequest(AbstractContext context, GlobalObject key, int mode) {
	this.context = context;
    this.key  = key;
    this.mode = mode;
    this.pe   = PE.thisPE();
  }

  /**
   * No-arg constructor needed to make class Externalizable.  Do not use.
   * @see java.io.Externalizable
   **/
  public RetrieveRequest() {}

  public String toString() {
    return "RetrieveRequest[" + from +
      ", " + printMode(mode) +
      ", " + key +"]";
  }

  /**
   * @see java.io.Externalizable
   **/
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeObject(context);
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
    context = (AbstractContext) in.readObject();
    key  = (GlobalObject) in.readObject();
    mode = in.readInt();
    pe   = (PE) in.readObject();
  }

  private String printMode(int mode) {
    switch (mode) {
    case READ_MODE  : return "r";
    case WRITE_MODE : return "w";
    default :         return "?";
    }
  }

  public void run() {
    try {
      HomeDirectory directory = (HomeDirectory) DirectoryManager.getManager();
      directory.retrieveRequest(key, pe, context, mode);
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }

}
