/*
 * Aleph Toolkit
 *
 * Copyright 1998, Brown University, Providence, RI.
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

package aleph.comm.message;

import aleph.Aleph;
import aleph.AlephDebug;
import aleph.PE;
import aleph.comm.Client;
import aleph.comm.CommunicationManager;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Tell client that stream is closed.
 *   
 * @date   August 1999
 * @author Maurice Herlihy
 **/
public class ByeBye extends aleph.Message implements Externalizable {

  private static boolean DEBUG = false; // smite the sounding furrows ..

  public ByeBye() {}
  public void run() {
    if (DEBUG)
      AlephDebug.println("received EOF message", true);
    Client.getClient().byeBye();
  }

  public String toString() {
    return "ByeBye[from: " + from + "]";
  }

  /**
   * @see java.io.Externalizable
   **/
  public void writeExternal (ObjectOutput out) throws IOException {
    super.writeExternal(out);
  }

  /**
   * @see java.io.Externalizable
   **/
  public void readExternal (ObjectInput in)
    throws IOException, java.lang.ClassNotFoundException {
    super.readExternal(in);
  }

}
