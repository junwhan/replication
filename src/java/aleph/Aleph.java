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
package aleph;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import edu.vt.rt.hyflow.util.io.Logger;

import aleph.comm.CommunicationManager;
import aleph.comm.message.Error;
import aleph.comm.message.Inform;
import aleph.comm.message.Warning;

/** 
 * Provides basic utility methods.
 * <p>
 * Aleph defines the following system properties:
 * <dl>
 * <dt>aleph.version	<dd>aleph version number
 * <dt>aleph.verbosity  <dd>controls level of detail messages
 * <dt>aleph.java_g     <dd>if true, use java_g instead of java
 * <dt>aleph.profile	<dd>if true, profile everything
 * <dt>aleph.label	<dd>user-friendly name for this PE
 * <dt>aleph.numPEs	<dd>size of this PE's group
 * <dt>aleph.index	<dd>index of this PE in its group
 * </dl>
 * Other <code>aleph.*</code> properties may be defined by various packages
 * <p>
 * Any system property beginning with <code>aleph</code> is propagated to all
 * PEs when they are started up.
 *
 * @author  Maurice Herlihy
 * @date    September 98
 **/
public final class Aleph {

  static final boolean DEBUG = false; // A fine mess you've got us into ...

  private static int verbosity; // loquacity control

  private static boolean panicStarted = false; // no nested panics

  private static Properties properties;

  /**
   * port number for contacting servers
   **/
  private static int port;

  /**
   * Should errors create pop up boxes?
   **/
  private static boolean popup;

  /**
   * Send I/O goes through standard input and output instead of gui?
   **/
  private static boolean batch = false;

  /**
   * Ask for confirmation before doing anything rash?
   **/
  private static boolean confirm = true;

  /**
   * Write debugging messages here.
   **/
  static private PrintWriter debugLog;

  /**
   * Interesting objects to snapshot on panic.
   **/
  private static Map snap = new HashMap();

  /**
   * Prefix for debugging dump file.
   **/
  private static String debugFile;

  /**
   * Get the port number used to find servers.
   * @return Value of port.
   **/
  public static int getPort() {return port;}
  
  /**
   * Set the port number used to find servers.
   * @param v  Value to assign to port.
   **/
  public static void setPort(int  v) {port = v;}
  
  /**
   * Test loquacity level.
   * @param  level candidate verbosity level
   * @return whether actual vebosity level is at least that high
   **/
  public static boolean verbosity (int level) {
    return verbosity >= level;
  }

  /**
   * Adjust loquacity level.
   * Set by <code>aleph.Config.verbosity</code>.
   * Overridden by run-time flag <code>-Daleph.verbosity=</code><it>blah</it>.
   **/
  public static void setVerbosity (int level) {
    verbosity = level;
  }

  /**
   * Read loquacity level.
   * Applications should use <code>verbosity()</code> instead.
   **/
  public static int getVerbosity () {
    return verbosity;
  }

  /**
   * @return whether to use interactive gui
   **/
  public static boolean isBatch () {return batch;}

  /**
   * @return whether to ask for confirmation before shooting
   **/
  public static boolean getConfirm () {return confirm;}
  
  /**
   * display warning message
   * @param message what to display
   **/
  public static void warning (String message) {
    try {
      CommunicationManager cManager = CommunicationManager.getManager();
      cManager.send(cManager.getConsoleAddress(),
                    new Warning(message));
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }

  /**
   * display warning message
   * @param e what to display
   **/
  public static void warning (Throwable throwable) {
    warning(throwable.toString());
  }

  /**
   * display error message
   * @param message what to display
   **/
  public static void error (String message) {
    try {
      CommunicationManager cManager = CommunicationManager.getManager();
      cManager.send(cManager.getConsoleAddress(),
                    new Error(message));
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }

  /**
   * display informative message
   * @param message what to display
   **/
  public static void inform (Throwable throwable) {
    inform(throwable.toString());
  }

  /**
   * display informative message
   * @param message what to display
   **/
  public static void inform (String message) {
    try {
      CommunicationManager cManager = CommunicationManager.getManager();
      cManager.send(cManager.getConsoleAddress(),
                    new Inform(message));
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }

  /**
   * display error message
   * @param e what to display
   **/
  public static void error (Throwable throwable) {
    error(throwable.toString());
  }

  /** 
   * Define an Aleph property.
   * @param key bind to this key
   * @param bind this value
   **/
  public static void setProperty (String key, String value) {
    properties.put(key, value);
  }

  /** 
   * Look up an Aleph property.
   * @param key return value bound to this key, or <code>null</code>
   **/
  public static String getProperty (String key) {
    return properties.getProperty(key);
  }
  /** 
   * Look up an Aleph property.
   * @param key return value bound to this key
   * @param value return this value if key is unbound
   **/
  public static String getProperty (String key, String value) {
    return properties.getProperty(key, value);
  }

  /**
   * Return integer value of Aleph property.
   * @param key property name
   * @param value default if property not found or not integer
   **/
  public static int getIntProperty (String key, int value) {
    try {
      String _value = properties.getProperty(key);
      if (_value != null)
        return Integer.parseInt(_value);
      else
        return value;
    } catch (Exception e) {
      return value;
    }
  }

  /**
   * Returns subset of system properties that match <code>aleph.*<code>.
   * @return aleph-related properties
   **/
  public static Properties getProperties () {
    return properties;
  }

  /**
   * Shut down the computation gracefully.
   * @param why the reason why
   **/
  public static void exit (String why) {
    error(why);
    exit(-1);
  }
  /**
   * Shut down the computation gracefully.
   **/
  public static void exit () {
    exit(0);
  }

  /**
   * Shut down the computation gracefully.
   * @param code status code
   **/
  public static void exit (int code) {
    try {
      if (code != 0 && batch)
	System.err.print("\007"); // feep!
      if (DEBUG)
        Aleph.debug("Aleph.exit called");
      PE.exit(code);
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }

  /**
   * Panic shutdown.<br>
   * Print exception's detail message, stack trace, and dump interesting objects.
   * @param e Exception that caused the problem.
   **/
  public static void panic (Throwable e) {
	  e.printStackTrace();
	  System.exit(1);
  }
//  
//  public static void panic (Throwable e) {
//    try {
//      if (!panicStarted) {
//	panicStarted = true;	// no nested panics!
//        StringBuffer s = new StringBuffer("Panic at "); // accumulate bad news
//        s.append(new Date());   // like when?
//        s.append("\n");
//	s.append(e.toString()); // like what?
//        s.append("\n");        
//        ByteArrayOutputStream dump = new ByteArrayOutputStream();
//	e.printStackTrace(new java.io.PrintStream(dump));
//        s.append(dump.toString());
//        s.append("\n");        
//        String uhoh = s.toString();
//	System.err.print("\007"); // feep!
//        warning(uhoh); // Je meurs pour la France!
//        // Now write the same damn thing to a panic file
//	PrintWriter panic
//          = new PrintWriter(new FileWriter(new File(Config.logDir,
//                                                     PE.thisPE().toString() + ".panic")));
//        panic.print(uhoh);      // diagnostic message
//        // dump interesting objects
//        for (Iterator iter = snap.keySet().iterator(); iter.hasNext();) {
//          Object key    = iter.next();
//          Object object = snap.get(key);
//          panic.println();
//          panic.println(key);
//          panic.print("\t");
//          panic.print(object);
//        }
//	panic.close();		// force to disk
//        PE.exit(-1);		// shut down everyone else
//      }
//    } catch (Exception x) {     // sauve qui peut
//      System.exit(-1);
//    }
//  }

  /**
   * Panic shutdown.  Prints message and stack trace.
   * @param message Famous last words.
   **/
  public static void panic (String message) {
    panic(new Throwable(message));
  }

  /**
   * Should warning and error messages be displayed in popup windows?
   **/
  public static void setPopup (boolean value) {
    popup = value;
  }

  /**
   * Should warning and error messages be displayed in popup windows?
   **/
  public static boolean getPopup () {
    return popup;
  }

  /**
   * Test an assertion, panic on failure.<br>
   * This is your brain on bugs.
   * @parm assertion test this!
   * @parm label informative note
   **/
  public static void assertValue (boolean assertion, String label) {
    if (! assertion) {          // uh-oh
      Throwable t = new Throwable("Assertion Failed: " + label);
      Aleph.panic(t);
    }
  }

  /**
   * Send a line of output to debugging display.
   * @parm msg text to display
   **/
  public static void debug (String msg) {
//    debug(msg, false);
	  Logger.debug(msg);
  }

  /**
   * Send a line of output to debugging stream.
   * @see aleph.Config
   * @parm msg text to display
   * @parm dump print stack trace?
   **/
  public static void debug (String msg, boolean trace) {
    try {
      if (debugLog == null) {        // don't open until user actually writes something
        if (debugFile == null)
          debugFile = Aleph.getProperty("aleph.label") + ".debug";
        debugLog = new PrintWriter(new FileWriter(new File(Config.logDir, debugFile)));
      }
      debugLog.println(msg);
      if (trace)
        (new Throwable()).printStackTrace(debugLog);
      debugLog.flush();
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }

  /** 
   * Main startup for Aleph. Checks the configuration and either
   * launches the gui or invokes batch mode.
   **/
  public static void main (String[] args) {
      String vers = System.getProperty("java.version");
      if (vers.compareTo("1.2") < 0) {
	  System.out.println("!!!WARNING: Aleph requires VM version 1.2 or higher!!!");
	  System.exit(0);
      }
    try {
      String alias = System.getProperty("aleph.communicationManager",
                                        Config.communicationManager);
      if (! Config.commRegistry.containsKey(alias)) {
        System.err.println("Unrecognized Communication Manager alias: " + alias);
        System.exit(1);
      }
      Aleph.setProperty("aleph.communicationManager",
			(String) Config.commRegistry.get(alias));

      alias = System.getProperty("aleph.directoryManager",
                                 Config.directoryManager);
      if (! Config.dirRegistry.containsKey(alias)) {
        System.err.println("Unrecognized Directory Manager alias: " + alias);
        System.exit(1);
      }
      Aleph.setProperty("aleph.directoryManager",
			(String) Config.dirRegistry.get(alias));
      
      alias = System.getProperty("aleph.eventManager",
                                 Config.eventManager);
      if (! Config.eventRegistry.containsKey(alias)) {
        System.err.println("Unrecognized Event Manager alias: " + alias);
        System.exit(1);
      }
      Aleph.setProperty("aleph.eventManager",
			(String) Config.eventRegistry.get(alias));

      alias = System.getProperty("aleph.transactionManager",
                                 Config.transactionManager);
      if (! Config.transRegistry.containsKey(alias)) {
        System.err.println("Unrecognized Transaction Manager alias: " + alias);
        System.exit(1);
      }
      Aleph.setProperty("aleph.transactionManager",
			(String) Config.transRegistry.get(alias));

      // If no args provided, use default command.
      if (args.length == 0) {
        StringTokenizer toke = new StringTokenizer(Config.defaultCommand);
        args = new String[toke.countTokens()];
        int i = 0;
        while (toke.hasMoreTokens())
          args[i++] = toke.nextToken();
      }

      // announce who we are for debugging and log files
      setProperty("aleph.label",
                  "console@" + InetAddress.getLocalHost().getHostName());

      // interactive or batch?
      if (System.getProperty("aleph.batch") != null)
        batch = true;
      else if (System.getProperty("aleph.noBatch") != null)
        batch = false;
      else
        batch = Config.batch;

      // Shoot first or ask questions?
      if (batch)                // don't confirm in batch mode
        confirm = false;
      else if (System.getProperty("aleph.confirm") != null)
        confirm = true;
      else if (System.getProperty("aleph.noConfirm") != null)
        confirm = false;
      else
        confirm = Config.confirm;

//      if (batch)
//        new Batch(args);
//      else
//        new Desktop(args);
    } catch (Exception e) {
      warning(e.toString());
      System.exit(-1);
    }
  }
 
  /**
   * Print out long banner listing managers, etc.
   **/
  public static String getLongBanner () {
    StringBuffer s = new StringBuffer(Config.banner);
    s.append("\nCommunication Manager:\t");
    s.append(Aleph.getProperty("aleph.communicationManager"));
    s.append("\nDirectory Manager:\t");
    s.append(Aleph.getProperty("aleph.directoryManager"));
    s.append("\nEvent Manager:\t\t");
    s.append(Aleph.getProperty("aleph.eventManager"));
    s.append("\nTransaction Manager:\t");
    s.append(Aleph.getProperty("aleph.transactionManager"));
    s.append("\n");
    return s.toString();
  }

  /**
   * reset name of debug file
   * @param title debug file is <title>.debug
   **/
  public static void setDebugFile (String title) {
    debugFile = title + ".debug";
  }

  /**
   * Register interesting object to be part of panic-induced dump.
   * @param label  suggestive tag
   * @param object dump me!
   **/
  public static void register (String label, Object object) {
    snap.put(label, object);
  }

  // Initialize Aleph properties here because server doesn't call Aleph.main().
  static {
    // Incorporate system properties starting with "aleph."
    properties = new Properties();
    for (Iterator iter = System.getProperties().keySet().iterator();
         iter.hasNext();) {
      String key = (String) iter.next();
      if (key.startsWith("aleph."))
        properties.put(key, System.getProperty(key));
    }
    properties.put("aleph.version", Config.version);

    // which port to use?
    port = Aleph.getIntProperty("aleph.port", Config.PORT);

    // Set verbosity level
    setVerbosity(getIntProperty("aleph.verbosity", Config.verbosity));
    // Set popup
    if (properties.get("aleph.popup") != null)
      popup = true;
    else if (properties.get("aleph.noPopup") != null)
      popup = false;
    else
      popup = Config.popup;


  }

}
