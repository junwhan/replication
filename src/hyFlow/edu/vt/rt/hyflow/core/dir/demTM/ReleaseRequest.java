package edu.vt.rt.hyflow.core.dir.demTM;


import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.deuce.transaction.AbstractContext;

import aleph.Aleph;
import aleph.GlobalObject;
import aleph.PE;
import aleph.dir.DirectoryManager;

/**
 * Server to client message asking for object back.<br>
 * @see aleph.trans.ReleaseResponse
 * @see aleph.thread.Message
 *
 * @author Maurice Herlihy
 * @date   March 1997
 **/

public class ReleaseRequest
  extends aleph.AsynchMessage implements Constants, Externalizable {

  static demTMDirectory directory = (demTMDirectory) DirectoryManager.getManager();

  GlobalObject key;
  PE pe;
  AbstractContext context;
  
  static final boolean DEBUG = false; // Ah-Oogah! Dive! Dive! Dive!

  /**
   * Constructor
   **/
  public ReleaseRequest(AbstractContext context, GlobalObject key) {
    this.key = key;
    this.pe  = PE.thisPE();
    this.context = context;
  }

  /**
   * No-arg constructor used to make class Externalizable
   * @see java.io.Externalizable
   **/
  public ReleaseRequest() {}

  /**
   * @see java.io.Externalizable
   **/
  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeObject(context);
    out.writeObject(key);
    out.writeObject(pe);
  }

  /**
   * @see java.io.Externalizable
   **/
  public void readExternal(ObjectInput in)
    throws IOException, java.lang.ClassNotFoundException {
    super.readExternal(in);
    context = (AbstractContext) in.readObject();
    key = (GlobalObject) in.readObject();
    pe = (PE) in.readObject();
  }

  public String toString() {
    return "ReleaseRequest[from: " + from + ", " + key + "]";
  }

  public void run() {
    try {
      directory.releaseRequest(pe, context, key);
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }

}
