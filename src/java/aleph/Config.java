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

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.net.URL;
import java.io.File;

/**
 * System and user-specific customization.
 * <p>
 * Some of the more useful run-time flags:
 *<p>
 *<il>
 *<li> <code>-Daleph.batch</code>
 *<li> <code>-Daleph.logging</code>
 *<li> <code>-Daleph.verbosity=</code><it>n</it>
 *<il>
 * @author Maurice Herlihy
 * @date   May 1999
 **/

public class Config {

  /**
   * Aleph version number.
   **/
  public static final String version = "1.2.0";

  /**
   * Unique port number for servers.
   * Override with <code>-Daleph.port=<it>n</it></code>
   **/
  public static final int PORT = 0; // must be between 1024 and 65535,

  /**
   * Array of default host names.
   **/
  public static String[] hosts; // initialized below

  /**
   * Make your current host first in the hosts array.  Useful if you don't
   * always use the same workstation.
   **/
  static boolean localHostFirst = false;

  /**
   * Controls how many messages are displayed.
   * Can be Constants.LACONIC, Constants.LOQUACIOUS, or Constants.BLOVIATING.
   **/
  public static final int verbosity = Constants.LACONIC;

  /**
   * If zero, Aleph initially creates one PE at each active host.  If <i>n</i>,
   * Aleph initially creates <i>n</i> PEs, in round-robin order at the hosts.
   * Overridden with <code>-Daleph.numPEs=<it>n</it></code>, or by the console.
   **/
  public static final int numPEs = 0; // One per host.

  /**
   * Maximum number of PEs.
   * Override with <code>-Daleph.maxPEs=<it>n</it></code>.
   **/
  public static final int maxPEs = 32;

  /**
   * Where to find java.
   **/
  public static String pathJava; // initialized below

  /**
   * Log the console?  Also set by <code>-Daleph.logging</code> flag.
   **/
  public static final boolean logging = false;

  /**
   * The Aleph server writes logs, debugging output, and instrumentation output
   * to this directory.  If the directory does not exist, it will try to create
   * it.  The directory name can be absolute or relative.
   **/
  public static File logDir;
  static {                      // static initializer for logDir
    File dir = new File("logs");
    if (! dir.exists()) {       // create the directory
      boolean ok = dir.mkdir();
      if (!ok)                // create failed, use current directory
        dir = new File("");
      System.err.println("Warning: using log directory " +
                         dir.getAbsolutePath());
    }
    logDir = dir;
  }
  
  /**
   * Run without a gui console?
   * Override with <code>-Daleph.batch</code> or <code>-Daleph.noBatch</code>.
   **/
  public static final boolean batch = false;

  /**
   * Where to find rsh (used by Unix tools.)
   * Override with <code>-Daleph.pathRsh=<it>p</it></code>.
   **/
  public static final String pathRsh = "/usr/bin/rsh"; // default

  /**
   * Run without a JIT compiler.  Applications will be much slower, but error
   * messages may be clearer.
   **/
  public static final boolean noCompiler = false;

  /**
   * Banner displayed on startup.
   **/
  public static final String banner = "Aleph Toolkit " + version + " (Fall 1999)";

  /**
   * Banner displayed on startup.
   **/
  public static final String copyright = "Copyright (c) " +
    java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) +
    " Brown University";

  /**
   * What to display in <i>about</i> menu item.
   **/
  public static String[] about = {Config.banner,
                                  Config.copyright,
                                  "http://www.cs.brown.edu/~mph/aleph",
                                  "aleph@cs.brown.edu"
  };

  /**
   * Main aleph directory
   **/
  public static String alephDirectory; // initialized below

  /**
   * Relative path for splash image;
   **/
  public static String splashImage = "desktop/images/rabbit.gif";
  /**
   * Relative path for screen image;
   **/
  public static String screenImage = "desktop/images/europe.gif";

  /**
   * Background color for output windows.
   **/
  public static java.awt.Color background = new java.awt.Color(65, 105, 225); // RoyalBlue4

  /**
   * Foreground color for output windows.
   **/
  public static java.awt.Color foreground = java.awt.Color.white;

  /**
   * Confirm before doing anything serious:
   * Override with -Daleph.confirm or -Daleph.noConfirm
   **/
  public static final boolean confirm = true;

  /**
   * Default contents of command line.
   **/
  public static final String defaultCommand = "aleph.examples.Hello";

  /**
   * Map short names for communication managers to class names.
   * <br>
   **/
  public static Map commRegistry;
  /* add new communication managers here */
  static {
    commRegistry = new HashMap();
    commRegistry.put("RMI", "aleph.comm.rmi.CommunicationManager");
    commRegistry.put("UDP", "aleph.comm.udp.CommunicationManager");
    commRegistry.put("TCP", "aleph.comm.tcp.CommunicationManager");
  }
    
  /**
   * Default communication manager. Overridden by
   * <code>-Daleph.communicationManager=LABEL</code>.
   **/
  public static String communicationManager = "RMI";

  /**
   * Map short alias for directory managers to class names.
   * <br>
   **/
  public static Map dirRegistry;
  /* add new communication managers here */
  static {
    dirRegistry = new HashMap();
    dirRegistry.put("Home", "aleph.dir.home.HomeDirectory");
    dirRegistry.put("Arrow", "aleph.dir.arrow.ArrowDirectory");
    dirRegistry.put("Hybrid", "aleph.dir.hybrid.HybridDirectory");
  }
    
  /**
   * Default directory manager. Overridden by
   * <code>-Daleph.directoryManager=LABEL</code>.
   **/
  public static String directoryManager = "Home";

  /**
   * Map short names for event managers to class names.
   * <br>
   **/
  public static Map eventRegistry;
  /* add new event managers here */
  static {
    eventRegistry = new HashMap();
    eventRegistry.put("Simple", "aleph.event.SimpleEventManager");
  }

  /**
   * Default event manager. Overridden by
   * <code>-Daleph.eventManager=ALIAS</code>.
   **/
  public static String eventManager = "Simple";

  /**
   * Map short names for transaction managers to class names.
   * <br>
   **/
  public static Map transRegistry;
  /* add new transaction managers here */
  static {
    transRegistry = new HashMap();
    transRegistry.put("Simple", "aleph.trans.SimpleTransactionManager");
  }

  /**
   * Default transaction manager. Overridden by
   * <code>-Daleph.transactionManager=ALIAS</code>.
   **/
  public static String transactionManager = "Simple";

  /**
   * Should warning and error messages be displayed in popup windows?
   * Override with <code>-Daleph.popup</code> or <code>-Daleph.noPopup</code>.
   * Ignored in batch mode.
   **/
  public static boolean popup = true;

  // Personal customization
  static {
    String[] brownHosts = {"elmgrove", "botrytis", "markov", "barney",
                           "pebbles", "bruford", "reverb", "hamlet", "gromit"};
    String[] crlHosts = {"alpha", "beta", "gamma", "delta"};
    String[] laptopHosts   = {"127.0.0.1"};
    String[] ntHosts       = {"127.0.0.1"};
    String[] brandeisHosts = {"cyclops", "lachesis", "atropos"};

    String osName = System.getProperty("os.name");

    if (osName.equalsIgnoreCase("Solaris") ||
        osName.equalsIgnoreCase("SunOS")) {
      hosts = brownHosts;
      alephDirectory = "/home/mph/sandbox/aleph/";
      pathJava = "/pro/java/jdk1.2/bin/java";
    } else if (osName.equalsIgnoreCase("Digital UNIX")) { // DU inconsistent case
      hosts = crlHosts;
      pathJava = "/usr/bin/java";
      alephDirectory = "/u/herlihy/sandbox/aleph/";
    } else if (osName.equalsIgnoreCase("Windows 95")) {
      hosts = laptopHosts;
      pathJava = "c:/jdk1.2.2/bin/java";
      alephDirectory = "/c:/aleph/";
    } else if (osName.equalsIgnoreCase("Windows NT")) {
      hosts = ntHosts;
      alephDirectory = "/d:/aleph/";
      //      hosts = laptopHosts;
      pathJava = "d:/jdk1.2.2/bin/java";
    } else if (osName.equalsIgnoreCase("linux")) {
      hosts = brandeisHosts;
      pathJava = "/usr/local/java/bin/java";
      alephDirectory = "/usr/mph/sandbox/aleph/";
//    } else {
//      System.err.println("Unrecognized os " + osName);
//      System.exit(-1);
    }
  }

}
