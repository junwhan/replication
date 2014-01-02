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
package aleph.bench.rmi;

/**
 * The <code>aleph.bench.rmi</code> package exists to test the latency of the
 * native <code>java.rmi</code> implementation, establishing an essential
 * baseline for evaluating Aleph communication packages.
 * <p>
 * This class is a dummy message whose size can be varied at will.
 * <p>
 * @see aleph.bench.rmi.Driver
 * @author Maurice Herlihy
 * @date   November 1998
 **/
public interface Test extends java.rmi.Remote {
  Message deliver(Message message) throws java.rmi.RemoteException;
}

    
