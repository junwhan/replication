/*
 * Aleph Toolkit
 *
 * Copyright 1998,1999 Brown University, Providence, RI.
 * 
 *                         All Rights Reserved
 * 
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose other than its incorporation into a commercial
 * product is hereby granted without fee, provided that the above copyright
 * notice appear in all copies and that both that copyright notice and this
 * permission notice appear in supporting documentation, and that the name of
 * Brown University not be used in advertising or publicity pertaining to
 * distribution of the software without specific, written prior permission.
 * 
 * BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE,
 * INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR ANY
 * PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY BE LIABLE FOR ANY
 * SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER
 * RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF
 * CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN
 * CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE
 */

package aleph.bench;

import aleph.comm.Address;
import aleph.AlephDebug;
import java.io.ObjectOutput;
import java.io.ObjectInput;
import java.io.IOException;
import java.io.Externalizable;

  public class PingMessage extends aleph.Message implements Externalizable {
    byte[] payload;
    static RoundTrip.PongMessage pong = new RoundTrip.PongMessage();
    public PingMessage () {
      payload = null;
    }

    public PingMessage(int size) {
      payload = new byte[size];
    }
    public void run() {
      pong.payload = payload;
      try {
		pong.send(from);
	} catch (IOException e) {
		e.printStackTrace();
	}
    }
    public void writeExternal(ObjectOutput out) throws IOException {
      AlephDebug.println("writeExternal called");
      out.writeObject(from);
    }
    public void readExternal(ObjectInput in)
      throws IOException, 
      java.lang.ClassNotFoundException {
      AlephDebug.println("readExternal called");
      from = (Address) in.readObject();
    }

}
