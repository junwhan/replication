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
package aleph.thread;

import aleph.Aleph;
import aleph.Config;
import aleph.Message;
import aleph.PE;
import aleph.comm.Address;
import aleph.comm.CommunicationManager;
import aleph.dir.DirectoryManager;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.IllegalArgumentException;
import java.lang.Thread;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.ArrayList;

/**
 * This class assigns worker threads to low-priority jobs, and hunts down user
 * applications.
 *
 * @author  Maurice Herlihy
 * @date    September 1998
 **/

public class Scheduler {

  private static final boolean DEBUG = false; // mistakes were made ...

  private static Scheduler theScheduler = null;	// singleton

  private int workerID = 0;

  private Pool pool = new Pool();

  public static Scheduler getScheduler() {
    if (theScheduler == null)
      theScheduler = new Scheduler();
    return theScheduler;
  }

  /**
   * Constructor.
   **/
  private Scheduler () {
  }

  /**
   * Execute user's app.
   * @param args user's command.
   **/
  public void exec (String[] args){
    (new UserApp(args)).start();
  }

  /**
   * Schedules messages for execution.
   * @param message schedule this message
   **/
  public void schedule (Message message) {
    Worker worker = pool.remove();
    worker.schedule(message);
  }

  /**
   * Inner class representing user's app.
   **/
  private class UserApp extends Thread {
    String[] args;
    // constructor
    UserApp(String[] args) {
      this.args = args;
    }
    // do it
    public void run () {
      // Did user forget to specify a program?
      if (args.length == 0) {
	Aleph.error("No user program specified");
	Aleph.exit();
      }
      Class user_class = null;
      try {
	// Java convention: arguments start at position 0.
	String[] userArgs = new String[args.length-1];
	// Shift args left by one to eliminate program name.
	System.arraycopy(args, 1, userArgs, 0, userArgs.length);
	// Look for user's class.
	user_class = Class.forName(args[0]);// args[0] is name of user's program.
	// Look for user's main method.
	Class[]  arg_classes = {userArgs.getClass()};
	Object[] arg_objects = {userArgs};
	// call the method
	(user_class.getMethod("main", arg_classes)).invoke(null, arg_objects);

      } catch (NoClassDefFoundError e) {
	Aleph.error("Error: could not find class " + args[0]);
      } catch (ClassNotFoundException e) {
	Aleph.error("Error: could not find class " + args[0]);
      } catch (NoSuchMethodException e) { // no main program
	Aleph.error("In " + user_class +
                    ": void main(String[] argv) is not defined");
      } catch (IllegalAccessException e) { // malformed main program
	Aleph.error("In " + user_class +
                    ": main must be public and static");
      } catch (InvocationTargetException e) {
	Aleph.error(args[0] + ".main() throws exception " + e.getTargetException());
      } catch (IllegalArgumentException e) {
	Aleph.error("Could not make sense out of " + args[0]);
      } catch (Exception e) {
	Aleph.panic("Exception in Scheduler: " + e);
      } catch (Error e) {
	Aleph.panic("Error in Scheduler: " + e);
      } finally {		// quit when user returns
	Aleph.exit();
      }
    }
  }

  /**
   * Inner class that implements worker thread.
   **/
  private class Worker extends Thread {
    private Message job;
    public synchronized void run () {
      while (true) {
        while (job == null)     // wait for job
          try { wait(); } catch (InterruptedException e) {}
        job.run();              // do it
        job = null;             // forget it
        pool.put(this);         // rejoin pool
      }
    }
    public synchronized void schedule (Message message) {
      job = message;            // got a new job
      notifyAll();              // wake up run() method
    }
  }

  private class Pool {
    private LinkedList queue = new LinkedList();

    public synchronized void put (Worker worker) {
      queue.add(worker);
    }
    
    public synchronized Worker remove () {
      if (queue.isEmpty()) {    // make something up
        Worker worker = new Worker();
        worker.start();
        return worker;
      }
      return (Worker) queue.removeFirst();
    }
  }
}
