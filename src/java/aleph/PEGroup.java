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

import aleph.comm.Address;
import aleph.comm.CommunicationManager;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import edu.vt.rt.hyflow.util.io.Logger;

/**
 * A PE group is a collection of PEs that know each others' names.
 *
 * @author Maurice Herlihy
 * @date August 1999
 **/

public final class PEGroup {
  private static final boolean DEBUG = false;
  private static int unique = 0; // generate unique group id
  private static Map map = Collections.synchronizedMap(new HashMap());

  private List group;           // list of component PEs
  private String label;
  private volatile int pending; // unacked PE creations
  private Integer id;


  /**
   * Construct a PE Group, round-robin through hosts.
   * @param hosts iterates over hosts
   * @param label group-wide label
   * @param numPEs how many PEs in group?
   * @param args user-supplied args
   **/
  public PEGroup (String label) {
	 int numPEs = Integer.parseInt(Aleph.getProperty("aleph.numPEs", "1"));
    id = new Integer(unique++);
    map.put(id, this);          // add self to map
    this.label = label;
    if (DEBUG)
      Aleph.debug("new PEGroup[" + label + ", size " + group +"]");
    synchronized (this) {
      group = new ArrayList(numPEs); // list of component PEs
      group.addAll(Collections.nCopies(numPEs, null)); // initialize
      CommunicationManager cManager = CommunicationManager.getManager();
      // start up my PE
      pending = numPEs;

      if(pending==0)
		try {
			PE.startPE();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
	else
	      new Thread(){
	    	  public void run() {
			      try {
					PE.startPE();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
	      	}
	      }.start();
      
		// wait for acks
      while (pending > 0) { // wait for PEs to report
    	  Logger.fetal("waiting");
        try {wait();} catch (InterruptedException e) {}
      }
      
      Logger.fetal("reply");
      // send peer group to children
      PE.TellChild reply = new PE.TellChild(group);
      try {
        for (Iterator iter = group.iterator(); iter.hasNext();){
        	Logger.fetal("reply client");
        	reply.send((PE) iter.next());
        }
      } catch (Exception e) {
        Aleph.panic(e);
      }
    }
  }
  
  /**
   * Construct a PE Group, round-robin through hosts.
   * @param hosts iterates over hosts
   * @param label group-wide label
   * @param numPEs how many PEs in group?
   * @param args user-supplied args
   **/
  public PEGroup (Set hosts, String label, int numPEs, String[] args) {
    id = new Integer(unique++);
    map.put(id, this);          // add self to map
    this.label = label;
    if (DEBUG)
      Aleph.debug("new PEGroup[" + label + ", size " + group +"]");
    synchronized (this) {
      group = new ArrayList(numPEs); // list of component PEs
      group.addAll(Collections.nCopies(numPEs, null)); // initialize
      CommunicationManager cManager = CommunicationManager.getManager();
      Iterator iterator = hosts.iterator();
      // start up PEs
      pending = numPEs;
      for (int i = 0; i < numPEs; i++) {
        if (! iterator.hasNext())
          iterator = hosts.iterator();
        Host host = (Host) iterator.next();
        host.startPE(cManager.getAddress(),
                     cManager.getConsoleAddress(),
                     i,
                     numPEs,
                     id,
                     label + "." + i,
                     args,
                     Aleph.getProperties());
      }
      // wait for acks
      while (pending > 0) { // wait for PEs to report
        try {wait();} catch (InterruptedException e) {}
      }
      // send peer group to children
      PE.TellChild reply = new PE.TellChild(group);
      try {
        for (Iterator iter = group.iterator(); iter.hasNext();)
          reply.send((PE) iter.next());
      } catch (Exception e) {
        Aleph.panic(e);
      }
    }
  }

  public Iterator elements() {
    return group.iterator();
  }

  /**
   * @return how many PEs in this group
   **/
  public int size() {
    return group.size();
  }

  public String toString() {
    StringBuffer s = new StringBuffer(label);
    s.append("[");
    s.append(group);
    s.append("]");
    return s.toString();
  }

  public synchronized void stop () {
    PE.HaltMessage halt = new PE.HaltMessage(0);
    for (Iterator iter = group.iterator(); iter.hasNext();)
      try {
        halt.send((PE) iter.next());
      } catch (Exception e) {}
  }

  /**
   * Newly-created PE asks parent for group info.
   **/
  public static class AskParent extends aleph.Message{
    private PE      child;      // new child
    private Integer parent;     // parent id
    private int     index;      // index of child in group
    AskParent(PE child, Integer parent, int index) {
      this.child  = child;
      this.index  = index;
      this.parent = parent;
    }
    public void run () {
      try {
        PEGroup peGroup = (PEGroup) map.get(parent);
        synchronized (peGroup) {
          peGroup.group.set(index, child);
          peGroup.pending--;
          if (peGroup.pending == 0)
            peGroup.notifyAll();
        }
      } catch (Exception e) {
        Aleph.panic(e);
      }
    }
    public String toString () {
      return "PEGroup.AskParent[from: " + from +
	", "   + child + ", " + index + "]";
    }
  }

}
