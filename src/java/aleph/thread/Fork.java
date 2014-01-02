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

package aleph.thread;

import aleph.Aleph;
import aleph.Config;
import aleph.Join;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Ask a remote PE to start a thread.  All messages that create new long-lived
 * threads should be a subclass of this class.
 *
 * @see     aleph.thread.Call
 * @author  Maurice Herlihy
 * @date    May 1997
 * @see     aleph.Message
 * @see     aleph.RemoteThread
 **/
public class Fork extends aleph.AsynchMessage implements Externalizable {

  private static final boolean DEBUG = false; // Inshallah!

  protected Join join;
  protected Runnable program;

  /**
   * Consructor.
   **/
  public Fork (Join join, Runnable program) {
    this.join    = join;
    this.program = program;
  }

  /**
   * No-arg constructor needed to make the class Externalizable.
   **/
  public Fork () {};

  public void run () {
    if (DEBUG)
      Aleph.debug("Forking " + program);
    try {
      program.run();
      if (join != null)
        join.signal();
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }

  public String toString () {
    return "Fork[from: " + from + ", " + join + ", " + program + "]";
  }

  /**
   * @see java.io.Externalizable
   **/
  public void writeExternal (ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeObject(join);
    if (DEBUG)
      Aleph.debug("writing " + program);
    out.writeObject(program);
    if (DEBUG)
      Aleph.debug("done writing " + this);
  }
  /**
   * @see java.io.Externalizable
   **/
  public void readExternal (ObjectInput in)
    throws IOException, java.lang.ClassNotFoundException {
    super.readExternal(in);
    join = (Join) in.readObject();
    program = (Runnable) in.readObject();
  }

}
