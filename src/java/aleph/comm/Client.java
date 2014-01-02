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

import aleph.Aleph;
import aleph.Config;
import aleph.PE;
import aleph.PEGroup;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.swing.*;

/**
 * A new Client is created at the start of each computation.  It creates the PE
 * group, relays outputs from PEs, and goes away when all output streams are closed.
 *
 * @author Maurice Herlihy
 * @date   January 1999
 **/

public class Client extends java.lang.Thread {

  private static final boolean DEBUG = false; // But that trick *never* works!

  private static Client theClient; // singleton

  private ActionListener listener; // notify here when done
  private int            openStreams; // how many PE streams open?
  private boolean        stopped;  // did user apply emergency brakes?

  private PrintWriter log;	// may be monitored for quality of service

  private OutputStream[] output; // array of output streams for PEs
  private int      numPEs;      // how many PEs?
  private String[] args;        // user's command
  private Set      selectedHosts; // where to create PEs

  private Hashtable pe2output  = new Hashtable(); // PE -> outputstream
  private int       outputIndex = 0; // how many output stream assigned?

  private PEGroup peGroup;	// my PEs

  // progress bar
  private ProgressMonitor progress;

  /**
   * Constructor
   * @param numPEs how many PEs to create
   * @param selectedHosts where to create them
   * @param args User's wish is our command
   * @param logging should we log everything?
   **/
  public Client (int numPEs, 
                 Set selectedHosts,
                 String[] args,
                 OutputStream[] output,
                 boolean logging) {
    this(null, numPEs, selectedHosts, args, output, logging);
  }

  /**
   * Constructor
   * @param listener if non-null, notify when done
   * @param numPEs how many PEs to create
   * @param selectedHosts where to create them
   * @param args User's wish is our command
   * @param logging should we log everything?
   **/
  public Client (ActionListener listener,
                 int numPEs, 
                 Set selectedHosts,
                 String[] args,
                 OutputStream[] output,
                 boolean logging) {
    theClient = this;         // appease getClient
    this.listener      = listener;
    this.numPEs        = numPEs;
    this.selectedHosts = selectedHosts;
    this.args          = args;
    this.output        = output;
    try {
      if (logging)
        log = new PrintWriter(new FileWriter(new File(Config.logDir,
                                                      Aleph.getProperty("aleph.label") +".log")));
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }
  
  /**
   * Used by messages to locate client.
   **/
  public static Client getClient () {
    if (theClient == null) {
      Throwable e = new Throwable("Unitialized Client");
      e.printStackTrace(System.err);
      System.exit(-1);
    }
    return theClient;
  }

  /**
   * This method does the real work.
   **/
  public void run () {
    try {
      int numHosts = selectedHosts.size();

      openStreams = 2 * numPEs; // Each PE has stdout and stderr open

      peGroup = new PEGroup(selectedHosts, "PE", numPEs, args);

      // Wait untill all PEs have finished
      synchronized (Client.this){
	while (!stopped && openStreams > 0) {
	  try {
	    Client.this.wait();
	  } catch (InterruptedException e)  {
            Aleph.panic(e);
          }
	}
      }

      if (log != null)
	log.close();
      
      if (DEBUG) {
	Aleph.debug("Client terminating...");
      }
      // notify console that we are done
      if (listener != null) {
        ActionEvent done = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Done");
        listener.actionPerformed(done);
      }
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }

  /**
   * Display a string from a particular PE.
   * @param pe source
   * @param string content
   **/
  public synchronized void stdout (PE pe, String string) {
    try {
      OutputStream stream = (OutputStream) pe2output.get(pe);
      if (stream == null) {
	stream = output[outputIndex++];
	pe2output.put(pe, stream);
      }
      stream.write(string.getBytes());
      if (log != null && string != null)
	log.println(string);
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }

  /**
   * Display a string from a particular PE.
   * @param pe source
   * @param string content
   **/
  public synchronized void stderr (PE pe, String string) {
    try {
      OutputStream stream = (OutputStream) pe2output.get(pe);
      if (stream == null) {
	stream = output[outputIndex++];
	pe2output.put(pe, stream);
      }
      stream.write(string.getBytes());
      if (log != null)
	log.println(string);
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }

  /**
   * Shutdown immediately
   **/
  public synchronized void exit () {
    stopped = true;
    try {
      if (peGroup != null)
        peGroup.stop();		// shut it down
    } catch (Exception e) {}    // everybody's a critic
    notifyAll();                // wake them up
  }

  /**
   * Input stream from PE shutting down.
   **/
  public synchronized void eof () {
    openStreams--;
    Aleph.debug("eof called: " + openStreams);
    System.out.flush();
    if (openStreams == 0)
      notifyAll();
  }

public void byeBye() {
	// TODO Auto-generated method stub
	
}
}

