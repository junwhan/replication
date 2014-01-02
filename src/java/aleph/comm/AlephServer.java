/*
 * Aleph Toolkit
 *
 * Copyright 1999 Brown University, Providence, RI.
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
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Properties;

/**
 * The Aleph server must run at every host that participates in an Aleph
 * computation.  The server uses RMI to communicate with the console.
 * This is the interface file used by RMI.
 **/ 

public interface AlephServer extends java.rmi.Remote {
  /**
   * Start up new PE.
   * @param parent  parent's address
   * @param console console address
   * @param index   index in PE group
   * @param numPEs  size of PE group
   * @param id      per-host unique id for group
   * @param label   suggestive label for PE
   * @param args    arguments to app
   * @param properties properties inherited from local Aleph
   **/
  void startPE (Address parent,
                Address console,
                int index,
                int numPEs,
                Integer id,
                String label,
                String[] args,
                Properties properties) throws java.rmi.RemoteException;

  /**
   * Die, server, die!
   **/
  void shutdown (String why) throws java.rmi.RemoteException;

}

    
