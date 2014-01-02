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
import aleph.comm.message.EOF;
import aleph.comm.message.Stdout;
import java.io.IOException;

public class OutputStream extends java.io.OutputStream {

  private static CommunicationManager cManager = 
    CommunicationManager.getManager();
  private static Address address = cManager.getConsoleAddress();

  private StringBuffer buffer = null; // accumulate text here
  private boolean      open   = true; // status

  private static final boolean DEBUG = false;
    
  public OutputStream () {
    super();
  }
    
  public synchronized void write (int b) throws IOException {
    if (DEBUG)
      Aleph.debug("OutputStream.write(byte)");
    if (buffer == null){
      buffer = new StringBuffer();
    }
    buffer.append(b);
  }
    
  public synchronized void write (byte[] b, int off, int len) throws IOException {
    if (DEBUG)
      Aleph.debug("OutputStream.write("
                  + new String(b, off, len) + ")");
    if (buffer == null){
      buffer = new StringBuffer();
    }
    buffer.append(new String(b, off, len));
  }

  public synchronized void flush () throws IOException {
    if (open) {
      if (DEBUG)
	Aleph.debug("OutputStream.flush(" +
			   ((buffer == null) ? "null" : buffer.toString()) +
			   ")");
      try {
	if (buffer != null)	// don't bother without data
	  cManager.send(address,
			new Stdout(buffer.toString()));
	cManager.flush(address);
	buffer = null;
      } catch (Exception e) {}
    }
  }
  public synchronized void close () throws IOException {
    if (open) {
      cManager.send(address, new EOF());
      flush();
      open = false;
    }
  } 
}
