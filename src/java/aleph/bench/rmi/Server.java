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

import java.rmi.RemoteException;
import java.rmi.RMISecurityManager;
import java.rmi.Naming;
import java.rmi.server.UnicastRemoteObject;

/**
 * The <code>aleph.bench.rmi</code> package exists to test the latency of the
 * native <code>java.rmi</code> implementation, establishing an essential
 * baseline for evaluating Aleph communication packages.
 * <p>
 * This class implements the server.  It reflects messages back to the sender.
 * <p>
 * @see aleph.bench.rmi.Client
 * @author Maurice Herlihy
 * @date   November 1998
 **/
public class Server extends UnicastRemoteObject implements Test {
  private static Message reply = new Message(); // pre-allocated

  public Server() throws java.rmi.RemoteException {
    super();
  }
    
  public Message deliver (Message message) throws RemoteException {
    reply.payload = message.payload;
    return reply;
  }

  public static void main(String args[]) 
  {                             // Create and install the security manager
    System.setSecurityManager(new RMISecurityManager());
    try {
      Server obj = new Server();
      Naming.rebind(Constants.URL, obj);
    } catch (Exception e) {
      System.out.println("aleph.bench.rmi.Server.main: an exception occurred:");
      e.printStackTrace();
    }
  }
}
