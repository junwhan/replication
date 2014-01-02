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
import aleph.Config;
import aleph.meter.Meter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

/**
 * The Aleph server must run at every host that participates in an Aleph
 * computation.  The server uses RMI to communicate with the console.
 **/ 
public class Server extends UnicastRemoteObject implements AlephServer {

  private PrintWriter log = null;
  private Runtime runtime = Runtime.getRuntime(); // get runtime environment
  private Vector childProcesses = new Vector();
  private int counter = 0;      // number of processes started
  private String longHostName;
  private String hostName;
  private static final boolean DEBUG = false; // Mayday, mayday!
  
  /**
   * name used in RMI registry
   **/
  private static final String NAME = "aleph/comm/Server";

  /**
   * Top-level code: creates and registers a Server.
   **/
  public static void main (String[] args) {
    try {
      Aleph.setProperty("aleph.label", name());
      Server server = new Server();
    } catch (Exception e) {
      System.err.println("Exception starting Server: " + e);
      e.printStackTrace();
    }
  }

  /**
   * The only constructor.
   **/
  public Server () throws java.rmi.RemoteException {
    super();
    try {
      Aleph.setDebugFile(name());
      log = new PrintWriter(new FileWriter(new File(Config.logDir, name() + ".log")));
      log.println(Config.banner);
      log.println("Server started at " + new Date());
      log.println("System properties:");
      System.getProperties().list(log);	// System properties
      log.println("Aleph properties:");
      Aleph.getProperties().list(log); // Aleph properties
      log.flush();
      Registry registry = null;
      try {
        registry = LocateRegistry.createRegistry(Config.PORT);
        log.println("Started rmiregistry at port " + Config.PORT);
      } catch (java.rmi.server.ExportException e) {
        registry = LocateRegistry.getRegistry(Config.PORT);
        log.println("Found running rmiregistry at port " + Config.PORT);
      }
      registry.rebind(NAME, this);
      log.println("Registered as\t" + NAME);
      log.flush();
    } catch (Exception e) {
      log.println(new Date() + "During Server intialization : " + e);
      log.close();              // cry like a rainstorm ...
      System.exit(-1);          // howl like the wind.
    }
  }

  /**
   * Die, server, die!
   **/
  public synchronized void shutdown (String who)
                             throws java.rmi.RemoteException {
    // kill all child processes
    for (Enumeration e = childProcesses.elements(); e.hasMoreElements();)
      ((Process) e.nextElement()).destroy();
    log.println(new Date() + "\tShutdown requested by " + who);
    Meter.report(log);
    log.close();
    System.exit(0);
  }

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
  public synchronized void startPE (Address  parent,
                                    Address  console,
                                    int      index,
                                    int      numPEs,
                                    Integer  id,
                                    String   label,
                                    String[] args,
                                    Properties properties)
                             throws java.rmi.RemoteException {
    try {
      // start the specified number of PEs for this server
      Vector line = new Vector(16);// accumulate command here
      line.addElement(Config.pathJava);

      /* pass on run-time environment from Client */
      for (Enumeration enm = properties.propertyNames(); enm.hasMoreElements();) {
        String key = (String) enm.nextElement();
        line.addElement("-D" + key + "=" + properties.getProperty(key));
      }

      /* console and parent addresses */
      line.addElement("-Daleph.console=" + console.toString());
      line.addElement("-Daleph.parent="  + parent.toString());
      line.addElement("-Daleph.group="   + id.toString());

      /* index and group size */
      line.addElement("-Daleph.index="          + String.valueOf(index));
      line.addElement("-Daleph.numPEs="         + String.valueOf(numPEs));

      line.addElement("-Daleph.label="          + label);
      // Don't need no compiler?
      if (properties.getProperty("aleph.noCompiler") != null)
        line.addElement("-Djava.compiler=NONE");
      line.addElement("aleph.PE");// our class name

      for (int i = 0; i < args.length; i++)// add user args
        line.addElement(args[i]);

      String[] command = new String[line.size()];
      for (int i = 0; i < line.size(); i++)
        command[i] = (String) line.elementAt(i);

      log.print(new Date() + "\texec");
      for (int i = 0; i < command.length; i++)
        log.print(" " + command[i]);
      log.println();
      log.flush();

      // run the command and place it in our list of processes we started
      childProcesses.addElement(runtime.exec(command)); // exec the process

      log.println ("Exec complete");
      log.flush();
    } catch (Exception e) {
      log.println(e);
      e.printStackTrace();
      log.close();
      System.exit(-1);
    }
  }

  /**
   * Return suggestive name for log and debugging files.
   **/
  private static String name () {
    String name = "server";
    try {
      name = "server@" +
        InetAddress.getLocalHost().getHostName() + // full host name
        "." + Config.PORT;	// server socket
    } catch (Exception e) {
      Aleph.panic(e);
    }
    return name;
  }

}

  
