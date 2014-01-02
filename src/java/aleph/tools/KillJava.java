package aleph.tools;

import aleph.Config;
import aleph.Host;

public class KillJava {

  static final boolean DEBUG = false; // Geronimo!
  static final boolean PROMPT = false; // disable this if you get annoyed
    
  public static void main(String[] args) {
    try {
      if (args.length == 0)     // Use default hosts?
	args = Config.hosts;
      if (PROMPT) {
	System.out.println("Warning: This will kill ALL java processes on:");
	System.out.print("         ");
	for (int i = 0; i < args.length; i++)
          System.out.println(args[i]);
	  System.out.print( " ");
        }
	System.out.println();
	System.out.println();
	System.out.print("Do you wish to continue? (y/n) ");
	System.out.flush();
	while (true) {
	  int resp = System.in.read();
	  if (resp == 'n') {
	    System.out.println ("KillJava aborted.");
	    return;
	  }
	  else if (resp == 'y')
	    break;
	}
      
      Runtime runtime = Runtime.getRuntime();
      String pathRsh = System.getProperty("aleph.pathRsh", Config.pathRsh);
      String[] command = {pathRsh,
			  "HOSTNAME", // fill this in later
			  ";",
			  "/cs/bin/nkill",
			  "-all",
                          "java",
                          "&"};
      
      if (DEBUG) {
        System.out.println("KillJava execs");
        for (int i = 0; i < command.length; i++) {
          System.out.println("\t" + command[i]);
        }
      }

      for (int i = 0; i < args.length; i++) {
        command[1] = args[i];
        runtime.exec(command);
      }

    } catch (Exception e) {
      System.err.println("Could not kill java: " + e);
      System.exit(-1);
    }
  }
}
