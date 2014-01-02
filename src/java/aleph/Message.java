/*
 * Aleph Toolkit
 *
 * Copyright 1999, Brown University, Providence, RI.
 * 
 *                         All Rights Reserved
 * 
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose other than its incorporation into a commercial
 * product is hereby granted without fee, provided that the above copyright
 * notice appear in all copies and that both that copyright notice and this
 * permission notice appear in supporting documentation, and that the name of
 * Brown University not be used in advertising or publicity pertaining to
 * distribution of the software without specific, written prior permission.
 * 
 * BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE,
 * INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR ANY
 * PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY BE LIABLE FOR ANY
 * SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER
 * RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF
 * CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN
 * CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE
 */
package aleph;
import aleph.comm.Address;
import aleph.comm.CommunicationManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * The base class for aleph messages. When a PE receives a message
 * <code>m</code>, it executes that message's <code>run()</code> method.
 * Messages between pairs of PEs are received and executed in FIFO order: if
 * <code>PE0</code> sends <code>m0</code> and then <code>m1</code> to
 * <code>PE1</code>, then <code>PE1</code> runs<code> m0.run()</code> to
 * completion befor starting <code>m1.run()</code>.  A message's
 * <code>run()</code> method should therefore not block for non-trivial
 * durations.  If you need to block, use <code>aleph.AsynchMessage</code>.
 * Messages from different PEs are not synchronized: if <code>PE0</code> sends
 * <code>m0</code> and <code>PE1</code> sends <code>m1</code> to
 * <code>PE2</code>, then <code>m0.run()</code> and <code>m1.run()</code> may
 * be run concurrently.
 *
 * @see aleph.AsynchMessage
 *
 * @author  Maurice Herlihy
 * @date    July 1998
 **/
abstract public class Message implements Serializable, Runnable {

  public static final int HIGH   = 0;
  public static final int MEDIUM = 1;
  public static final int LOW    = 2;

  private static final boolean DEBUG = false;

  public Address from;

  public String stamp;
  
  public transient Message next; // useful for linked lists

  private int priority = MEDIUM;
  
  private int bufSize = 64;     // estimated buffer size for copy

  /**
   * Use serialization to copy a message.  Used by communication managers for
   * local object transmission.  We can't just hand over the message,
   * because sharing between the message and mutable objects causes pernicious
   * bugs.
   *
   * @return disjoint copy
   **/
  public Message copy () {
    Message other = null;
    try {
      MyByteArrayOutputStream byteOut = new MyByteArrayOutputStream(bufSize);
      ObjectOutputStream objectOut = new ObjectOutputStream(byteOut);
      objectOut.writeObject(this);
      byte[] b = byteOut.getByteArray();
      bufSize = (bufSize + b.length) / 2; // rolling average
      ByteArrayInputStream byteIn = new ByteArrayInputStream(b);
      ObjectInputStream objectIn = new ObjectInputStream(byteIn);
      other = (Message) objectIn.readObject();
    } catch (Exception e) {
      Aleph.panic(e);
    }
    return other;
  }

  /**
   * Priority is a hint to the Communication Manager
   * @return requested priority
   **/
  public int getPriority () {return priority;}
  
  /**
   * Priority is a hint to the Communication Manager
   * @param v  Value to assign to priority.
   **/
  public void setPriority (int  v) {this.priority = v;}

  /**
   * Send a message.
   * @param pe target PE
   **/
  public void send (PE pe) throws IOException {
    send(pe.getAddress());
  }

  /**
   * Send a message.
   * @param address target PE's address
   **/
  public void send (Address address) throws IOException {
    if (DEBUG)
      Aleph.debug("Sending " + this);
    CommunicationManager.getManager().send(address, this);
  }

  /**
   * Externalizable subclasses use this method to write out superclass
   * members.
   * @see java.io.Externalizable
   **/
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(from);
    out.writeInt(priority);
  }

  /**
   * Externalizable subclasses my use this method to read back superclass
   * members.
   * @see java.io.Externalizable
   **/
  public void readExternal(ObjectInput in)
    throws IOException, java.lang.ClassNotFoundException {
    from = (Address) in.readObject();
    priority = in.readInt();
  }

  /**
   * Inner class to circumvent ByteArrayOutputStream lossage.
   **/
  class MyByteArrayOutputStream extends java.io.ByteArrayOutputStream {
    public MyByteArrayOutputStream (int size) {
      super(size);
    }
    public byte[] getByteArray () {
      return buf;
    }
  }

}
