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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import edu.vt.rt.hyflow.util.io.Logger;

import aleph.comm.Address;
import aleph.comm.CommunicationManager;
import aleph.thread.Scheduler;

/**
 * A PE object names a Processing Element.  A PE can be
 *<il>
 *<li> a <i>PE group</i> member, or a <i>solo</i> PE
 *<li> a <i>child</i> of another PE, or <i>autonomous</i>
 *</il>
 * When a PE group is shut down, it shuts down all members.
 * When a PE exits or is shut down, it shuts down its children.
 *
 * @author Maurice Herlihy
 * @date August 1998
 **/

public class PE implements java.io.Serializable {

  private static PE      thisPE;

  // Used for parent/child handshake
  private static Integer lock = new Integer(0);// static synchronization
  private static int     counter; // how many responses so far?

  // Each PE keeps this information
  private int     numPEs = 0;   // size of my group (0 => no group)
  private Address parent;       // parent's address
  private int     index;        // my index in the group
  private String  label;        // my nickname
  private Address address;      // where I live
 

  private transient Set  children = new HashSet();
  private transient List group; // put responses here

  private final static boolean DEBUG = false;

  private PE (Address address) {
    try {
      this.address = address;
      this.label = Aleph.getProperty("aleph.label",
                                     "console@" +
                                     InetAddress.getLocalHost().getHostName());
      this.index = Integer.parseInt(Aleph.getProperty("aleph.index", "0"));
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }

  /**
   * @return this PE's address
   * @see aleph.comm.Address;
   **/
  public Address getAddress () {
    return address;
  }
 
  /**
   * @return whether a PE seems to be running.
   **/
  public boolean ping () {
    try {
      return CommunicationManager.getManager().ping(address);
    } catch (Exception e) {
      Aleph.panic(e);
    }
    return false;		// not reached
  }

  /**
   * Always compare PEs with <code>equals</code>, never <code>==</code>.
   **/
  public boolean equals (Object anObject) {
    if ((anObject != null) && (anObject instanceof PE)) {
      PE other = (PE) anObject;
      return (other.address.equals(this.address));
    } else {
      return false;
    }
  }

  public int hashCode (){
    return address.hashCode();
  }

  public String toString () {
    return label;
  }

  /**
   * Identity politics.
   * @return our PE
   **/
  public static PE thisPE () {
    if (thisPE == null) {
      CommunicationManager cManager = CommunicationManager.getManager();
      thisPE = new PE(cManager.getAddress());
    }
    return thisPE;
  }
  
  public void populate(final Message message){
	  populate(message, true);
  }
  public void populate(final Message message, boolean asynchronous){
	  Logger.debug("Populate " + message);
	  for(Iterator<PE> itr=allPEs(); itr.hasNext(); ){
			final PE pe = itr.next();
			Logger.debug("Sent to " + pe);
			if(!pe.equals(PE.thisPE()))
				if(asynchronous)
					new Thread(){
						@Override
						public void run() {
							try {
								CommunicationManager.getManager().send(pe, message);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}.start();
				else
					try {
						CommunicationManager.getManager().send(pe, message);
					} catch (IOException e) {
						e.printStackTrace();
					}
		}
  }

  /**
   * @return number of PEs in our group.
   **/
  public static int numPEs () {
    if (thisPE.group == null)
      return 1;
    else
      return thisPE.group.size();
  }

  /**
   * @return Iterates over all PEs in this PE group.
   **/
  public static Iterator allPEs () {
    return thisPE.group.iterator();
  }

  /**
   * @return Iterate over my group's PEs in round-robin order.
   **/
  public static Iterator roundRobin () {
    return new Iterator () {	// Anonymous iterator
      Iterator iterator = thisPE.group.iterator();
      public boolean hasNext () {
	return true;
      }
      public Object next () throws NoSuchElementException {
        if (! iterator.hasNext())
          iterator = thisPE.group.iterator();
	return iterator.next();
      }
      public void remove () {
        throw new UnsupportedOperationException();
      }
    };
  }

  
  public static void startPE() throws IOException{
	  // Concoct user-readable name
      CommunicationManager cManager = CommunicationManager.getManager();
      Aleph.setDebugFile(thisPE().label);

      // Initialize static fields
      thisPE = new PE(cManager.getAddress());
      thisPE.parent = cManager.getParentAddress();
      thisPE.numPEs = Integer.parseInt(Aleph.getProperty("aleph.numPEs", "1"));
      thisPE.index  = Integer.parseInt(Aleph.getProperty("aleph.index",  "0"));

      // Fill in peers, using handshake with parent 
      synchronized (lock) {	// only the paranoid survive
        thisPE.group = null;
        Integer id = new Integer(Aleph.getIntProperty("aleph.group", 0));
        cManager.send(thisPE.parent, new PEGroup.AskParent(thisPE, id, thisPE.index));
        while (thisPE.group == null) {
          try {lock.wait();} catch (InterruptedException e) {}
        }
        
        Logger.fetal("PE started...");
      }
  }
  /**
   * Top-level program called by Server.  Initializes local PE state from Aleph
   * properties.  Undertakes handshake with parent to discover salient properties.
   **/
  public static void main (String[] args) {
    try {
    	
    	startPE();
    
      // Rebind I/O streams.
      if (Aleph.verbosity(Constants.LOQUACIOUS)){	// Learn to love deprecation warnings!
	System.setOut(new VerbosePrintStream(new aleph.comm.OutputStream(),
					     "[" + PE.thisPE()+" out] "));
	System.setErr(new VerbosePrintStream(new aleph.comm.OutputStream(),
					     "[" + PE.thisPE()+" err] "));
      } else {
        System.setOut(new PrintStream(new aleph.comm.OutputStream()));
        System.setErr(new PrintStream(new aleph.comm.OutputStream()));
      }
      // run user's app
      if (thisPE.index == 0) {
        Scheduler scheduler = Scheduler.getScheduler();
        scheduler.exec(args);
      }
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }

  /**
   * Shuts down a PE and its children.  Leaves other group members unharmed.
   * @return whether shutdown is confirmed.
   * @exception SecurityException If caller is not allowed to kill this server.
   **/
  public boolean stop () throws SecurityException {
    return stop(0);
  }

  /**
   * Shuts down a PE and its children.  Leaves other group members unharmed.
   * @param code termination code
   * @return whether shutdown is confirmed.
   * @exception SecurityException If caller is not allowed to kill this server.
   **/
  public boolean stop (int code) throws SecurityException {
    try {
      CommunicationManager cManager = CommunicationManager.getManager();
      cManager.send(address, new HaltMessage(code));
      cManager.flush(address);
      return true;
    } catch (IOException e) { // timed out
      return false;
    } catch (Exception e) {
      Aleph.panic(e);
    }
    return false;		// not reached
  }

  /**
   * Shut down me, my children, and my group.
   * @param code exit code (0 is normal, etc).
   **/
  public static void exit (int code) {
    try {
      // shut down other peers
      for (Iterator iter = thisPE.group.iterator(); iter.hasNext();) {
        PE pe = (PE) iter.next();
        if (! pe.equals(thisPE))
          pe.stop(code);
      }
    } catch (Exception e) {}
    halt(code);
  }

  /**
   * Shut down me and my children, but not other group members.
   * @param code exit code (0 is normal, etc).
   **/
  public static void halt (int code) {
    // stop children
    for (Iterator iter = thisPE.children.iterator(); iter.hasNext();) {
      PE pe = (PE) iter.next();
      pe.stop(code);
    }
    // shut down gracefully
    closeConsoleStreams();
    CommunicationManager.getManager().close();           // close sockets
    System.exit(code);
  }

  /**
   * Shuts down peers, children, and parent.
   **/
  public static void panic () {
    panic(null);
  }

  /**
   * Shuts down peers, children, and parent.
   **/
  public static void panic (PE sender) {
    try {
      // stop children
      for (Iterator iter = thisPE.children.iterator(); iter.hasNext();) {
	PE pe = (PE) iter.next();
        if (! pe.equals(sender))
          pe.stop(-1);
      }
      // inform parent
      System.out.close();         // flush remaining I/O
      System.err.close();
      CommunicationManager.getManager().close();           // close sockets
    } catch (Exception e) {}
    System.exit(-1);
  }

  /**
   * @return my index within group
   **/
  public int getIndex () {
    return index;
  }

  /**
   * @return i-th PE, wrapping around if necessary
   **/
  public static PE getPE (int i) {
    return (PE) thisPE.group.get(i % thisPE.group.size());
  }

  /**
   * Inner class that implements verbose output stream.
   * Causes deprecation warning.
   **/
  static class VerbosePrintStream extends PrintStream {
    String label;
    VerbosePrintStream(OutputStream output, String label) {
      super(output, true);
      this.label = label;
    }
    public synchronized void println (String line) {
      super.print(label);
      super.println(line);
    }
  }

  /**
   * Tell new PE about group.
   **/
  public static class TellChild extends aleph.Message{
    private List peers;
    TellChild (List peers) {
      this.peers = peers;
    }
    public void run () {
      synchronized (lock) {
	thisPE.group = peers;
	lock.notifyAll();
      }
    }
    public String toString () {
      StringBuffer s = new StringBuffer("PE.TellChild[from: ");
      s.append(from);
      s.append(", PEs: ");
      s.append(peers.toString());
      s.append("]");
      return s.toString();
    }
  }

  /**
   * Ask PE to shut down
   **/
  public static class HaltMessage
    extends aleph.Message implements java.io.Externalizable {
    int code;
    public HaltMessage (int code) {
      this.code = code;
    }
    public HaltMessage () {}
    public void run () {
      PE.halt(code);
    }
    public String toString () {
      return "PE.HaltMessage[from: " + from + "]";
    }
    /**
     * @see java.io.Externalizable
     **/
    public void writeExternal (ObjectOutput out) throws IOException {
      super.writeExternal(out);
      out.writeInt(code);
    }
    /**
     * @see java.io.Externalizable
     **/
    public void readExternal (ObjectInput in)
      throws IOException, java.lang.ClassNotFoundException {
      super.readExternal(in);
      code = in.readInt();
    }
  }

  /**
   * Close input and error streams to console.  Can be called when PE shuts
   * down, or if the PE wants to disassociate itself from the console.
   **/
  public static void closeConsoleStreams () {
    if (DEBUG)
      Aleph.debug("closing console streams");
    System.out.close();         // flush remaining I/O
    System.err.close();
  }

}
