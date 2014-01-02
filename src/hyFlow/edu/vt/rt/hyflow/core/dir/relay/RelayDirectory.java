package edu.vt.rt.hyflow.core.dir.relay;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Vector;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.AbstractContext.STATUS;
import org.deuce.transaction.TransactionException;

import aleph.Aleph;
import aleph.GlobalObject;
import aleph.PE;
import aleph.dir.DirectoryManager;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.util.io.Logger;

public abstract  class RelayDirectory extends DirectoryManager {

  private static RelayDirectory theManager; // convenient access for messages

  // id -> object status
  private Hashtable<GlobalObject, Status> arrow  = new Hashtable<GlobalObject, Status>();

  private static final boolean DEBUG = false;

  
  /**
   * Constructor
   **/
  public RelayDirectory() {
    theManager = this;
    Aleph.register("Relay Directory", this);
    if (DEBUG)
      Aleph.debug(this.toString());
    
    new Thread(){
    	@Override
    	public void run() {
    		try {
				Thread.sleep(30000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.err.println("\n\n\n\n\n");
			for(Entry<GlobalObject, Status> arr:arrow.entrySet()){
				System.err.println(arr.getKey() + " : " + arr.getValue());
			}
    	}
    }.start();
  }

  /**
   * @see aleph.dir.DirectoryManager#newObject
   * @param key    Global object
   * @param object Initial state for global object.
   * @param hint   <code>String</code> passed to directory
   **/
  public synchronized void newObject (GlobalObject key, Object object, String hint) {
    Status status = new Status(PE.thisPE());
    status.busy   = false;
    status.owner  = null;
    status.object = object;
    arrow.put(key, status);
  }

  /**
   * @see aleph.dir.DirectoryManager#importObject
   * @param key    Global object
   **/
  public void importObject (GlobalObject key) {}

  /**
   * @see aleph.dir.DirectoryManager#open
   * @param object The object to open.
   * @param mode   Mode in which to open object.
   **/
  public synchronized Object open (AbstractContext context, GlobalObject key, String mode) {
		Logger.debug("TryOpen " + key + " for context " + context);
    Status status  = (Status) arrow.get(key);
    PE     thisPE  = PE.thisPE();
    int    myIndex = thisPE.getIndex();
    try {
      if (status == null)  {      // object is unknown
        status = new Status(thisPE);
        arrow.put(key, status);   // legalize it
        FindMessage find = new FindMessage(context, key);
        if (DEBUG) {
          Aleph.debug("open: home is " + key.getHome());
          Aleph.debug("open: find is " + find);
          Aleph.debug("open: route is " + route(key.getHome()));
        }
        find.path.add(thisPE);
        Logger.debug("New Object, Sending find msg " + find + " to " + route(key.getHome()));
        find.send(route(key.getHome()));
      } else if (status.count == 0 && ! status.direction.equals(thisPE)
                 && ! status.busy) { // known elsewhere
        FindMessage find = new FindMessage(context, key);
        find.path.add(thisPE); //add to the path vector
//        find.test = true;
        //System.out.println("this PE is " + thisPE.getIndex());
        //System.out.println("last element is " + find.path.lastElement().getIndex());
        //System.out.println("path vector size is " + find.path.size());
        Logger.debug("Sening find msg " + find + " to " + status.direction);
        find.send(status.direction);
        if (DEBUG)
          Aleph.debug("open sending " + find + " to " + status.direction);
        //status.direction = thisPE;  // flip arrow
      }
      /* wait for the object to appear */
      status.count += 1;
      if (DEBUG)
        Aleph.debug("open waits, status = " + status);
      while (status.object == null || status.busy && status.owner!=null && !status.owner.status.equals(STATUS.ABORTED)) {
    	  Logger.debug("Waiting for object " + status.object  + " " + status + " " + status.owner);
        try { wait();} catch (InterruptedException e) {}
      }
      status.count--;
      if(context.status.equals(STATUS.ABORTED)){
    	  Logger.debug("Meanwhile aborted context " + context);
    	  return status.object;
      }
      status.busy = true;
      status.owner = context;
      if (DEBUG)
        Aleph.debug("open returns, status = " + status);
    } catch (IOException e) {
      Aleph.panic(e);
    }
    Logger.debug("Open " + key + " for context " + context);
    return status.object;
  }

  /**
   * @see aleph.dir.DirectoryManager#release
   * @param object Formerly interesting object.
   **/
  public synchronized void release (AbstractContext context, GlobalObject object){
	  Logger.debug("TryRelease :" + object + " for context " + context);
    try {
      Status status = (Status) arrow.get(object);
      if (DEBUG)
        Aleph.debug("release called\t" + status);
      status.owner = null;
      status.busy = false;
      if (status.count > 0)       // local suitors
        notifyAll();              // wake up and smell the coffee
      else
        if (status.pending != null) { // remote suitor
          if (status.object == null)
            Aleph.panic("null object" + status); // debugging
          //status.direction = status.pending;
          //(new GrantMessage(object, status.object,status.pending)).send(status.pending);
          //System.out.println("PE " + PE.thisPE().getIndex() + " releases object to PE " + status.pending.getIndex());
         // if (status.pendingPath.size() != 0) {
        	  GrantMessage grant = new GrantMessage(object, status.object, status.pendingPath, status.pending);
              status.direction = grant.movePath.lastElement();
              status.object  = null;
              status.pending = null;
              grant.send(grant.movePath.lastElement());  
          //} else {
        	  //status.direction = status.pending;
        	  //(new GrantMessage(object, status.object,status.pending)).send(status.pending);
          //}
          Logger.debug("PE " + PE.thisPE().getIndex() + " releases object " + object + "/" + grant.object + " to PE " + grant.movePath.lastElement().getIndex());
          
        }
      if (DEBUG)
        Aleph.debug("release returns\t" + status);
    } catch (IOException e) {
      Aleph.panic(e);
    }
    Logger.debug("Released :" + object + " from context " + context);
  }

  /**
   * @return who we are
   **/
  public String getLabel () {
    return "Relay";
  }

  /* PEs are arranged into a binary tree by index */
  private static int parent (int i) {	// parent's index
    return (i - 1) >> 1;
  }
  private static int left (int i) {	// left child's index
    return (i << 1) + 1;
  }
  private static int right (int i) {	// right child's index
    return (i + 1) << 1;
  }
  private static int left (int i, int d) { // left grandchild's index
    if (d == 1)
      return left(i);
    else
      return left(left(i, d-1));
  }
  private static int right (int i, int d) { // right grandchild's index
    if (d == 1)
      return right(i);
    else
      return right(right(i, d-1));
  }

  private PE parentPE () {
    int myIndex = PE.thisPE().getIndex();
    if (DEBUG)
      Aleph.debug("parentPE: " + myIndex + " parent " + parent(myIndex));
    if (myIndex > 0)
      return PE.getPE(parent(myIndex));
    else
      return null;
  }

  private PE leftPE () {
    int myIndex = PE.thisPE().getIndex();
    if (DEBUG)
      Aleph.debug("leftPE: " + myIndex + " left " + left(myIndex));
    int i = left(myIndex);
    if (i < PE.numPEs())
      return PE.getPE(i);
    else
      return null;
  }

  private PE rightPE () {
    int myIndex = PE.thisPE().getIndex();
    int i = right(myIndex);
    if (DEBUG)
      Aleph.debug("rightPE: myIndex=" + myIndex + ", right=" + i);
    if (i < PE.numPEs())
      return PE.getPE(i);
    else
      return null;
  }

  private static int depth(int i) {	// depth of node in three
    int d = 0;
    int j = i + 1;
    while (j > 0) {
      j = (j >> 1);
      d++;
    }
    return d;
  }

  private static boolean isDescendant(int i0, int i1) {
    if (i0 == i1)
      return true;
    int d0 = depth(i0);
    int d1 = depth(i1);
    if (d0 >= d1)
      return false;
    int e = (d1 - d0);		// depth difference
    int lb = left(i0, e);	// left grandchild
    int rb = right(i0, e);	// right grandchild
    return (lb <= i1) && (i1 <= rb);
  }

  public String toString() {
    StringBuffer s = new StringBuffer("neighbors{");
    s.append(parentPE());
    s.append(" ");
    s.append(leftPE());
    s.append(" ");
    s.append(rightPE());
    s.append("}");
    return s.toString();
  }

  /**
   * Route message through tree for specific pe
   * @param pe destination pe
   * @return which way to send the message
   **/
  private PE route (PE pe) {
    int myIndex = PE.thisPE().getIndex();
    if (DEBUG) {
      Aleph.debug("route(" + pe + ") from " + PE.thisPE());
      Aleph.debug("left(" + myIndex + ") = " + left(myIndex));
      Aleph.debug("right(" + myIndex + ") = " + right(myIndex));
    }

    int i = pe.getIndex();
    if (DEBUG)
      Aleph.debug("route: i = " + i + " myIndex = " + myIndex);
    if (isDescendant(myIndex,i)) {
      if (isDescendant(left(myIndex), i)) {
	if (DEBUG)
	  Aleph.debug("route returns leftPE " + leftPE());
	return leftPE();
      } else {
	if (DEBUG)
	  Aleph.debug("route returns rightPE " + rightPE());
	return rightPE();
      }
    } else {
      if (DEBUG)
	Aleph.debug("route returns parentPE " + parentPE());
      return parentPE();
    }
  }

  /**
   * Inner class with object's location and synchronization information
   **/
  private static class Status {
    Object  object;		// object itself, or null
    PE      pending;		// next in line
    boolean busy;		// in use?
    int     count;              // how many local threads waiting?
    PE      direction;		// it went that-a-way
    Vector <PE> pendingPath; 
    AbstractContext owner;
    
    Status(PE direction) {
      this.direction = direction;
      this.object    = null;
      this.pending   = null;
      this.busy      = false;
      this.owner	 = null;
      pendingPath = new Vector <PE> ();
    }
    public String toString() {
      return "Status[object " + object +
        ", direction " + direction +
        ", pending " + pending +
        ", count " + count +
        ", owner " + owner +
        ", busy " + busy + "]";
    }
  }

  /* Private message classes */
  private static class FindMessage
    extends aleph.Message{

    private static final boolean DEBUG = false;

    PE from;
    AbstractContext context;
    GlobalObject key;
    PE requestor;
    Vector<PE> path; //record the path a find message travels
   // boolean test;

    FindMessage(AbstractContext context, GlobalObject key) {
      PE pe = PE.thisPE();
      this.from = pe;
      this.context = context;
      this.key = key;
      requestor = pe;
      path = new Vector <PE>();
      this.path.add(pe);
    }

    public FindMessage() {};
//    /**
//     * @see java.io.Externalizable
//     **/
//    public void writeExternal(ObjectOutput out) throws IOException {
//      super.writeExternal(out);
//      out.writeObject(from);
//      out.writeObject(key);
//      out.writeObject(requestor);
//      out.writeObject(path);
//    }
//    /**
//     * @see java.io.Externalizable
//     **/
//    public void readExternal(ObjectInput in)
//      throws IOException, java.lang.ClassNotFoundException {
//      super.readExternal(in);
//      from       = (PE) in.readObject();
//      key        = (GlobalObject) in.readObject();
//      requestor  = (PE) in.readObject();
//      path       = (Vector<PE>) in.readObject();
//    }
//    
    public void run() {
      try{
    	  Logger.debug("Receving find message " + this);
        synchronized (theManager) {
          Status status = (Status) theManager.arrow.get(key);
          PE newDirection = from;
          if (RelayDirectory.DEBUG)
            Aleph.debug(this + " called\t " + status);
          if (status == null) {
            /* Object is unknown.
             * Install status record for object.
             * Flip arrow toward incoming edge.
             * send toward object's id.home
             */
            status = new Status(newDirection); // install status
            theManager.arrow.put(key, status);
            this.from = PE.thisPE();
            this.path.add(from);
            PE direction = theManager.route(key.getHome());
            Logger.debug("New Object, forwarding find msg " + this + " to " + direction);
            this.send(direction);
          } else {
            /* Object is known.
             * Flip arrow toward incoming edge.
             * Check whether object is local.
             */
            PE direction = status.direction;
            //this.path.add(this.from);
            //System.out.println("find msg from this " + this.from.getIndex());
            //System.out.println("find msg from " + from.getIndex());
            //status.direction = newDirection; // flip arrow
            if (direction.equals(PE.thisPE())) { // object is local
              if (status.busy || status.count > 0 || status.object == null) {
                // object is real busy
                status.pending = requestor; // so get in line
                status.pendingPath = (Vector) this.path.clone();
                
                try {
              	  HyFlow.getConflictManager().resolve(context, status.owner);
              	  status.owner = null;
              	  status.busy = false;
              	  theManager.notifyAll();
                } catch (TransactionException e) {
                }
                
                if (RelayDirectory.DEBUG)
                  Aleph.debug(requestor + " getting in line");
              } else {            // object is free, send it
                if (RelayDirectory.DEBUG)
                  Aleph.debug("granting to " + requestor);
                if (status.object == null)
                  Aleph.panic("null object" + status); // debugging
                GrantMessage grant = new GrantMessage(key, status.object, this.path, requestor);
                //System.out.println("find msg from " + this.from.getIndex());
                //System.out.println("last element is " + this.path.lastElement().getIndex());
                //System.out.println("path vector size is " + this.path.size());
                //System.out.println("test bit is " + this.test);
                //System.out.println("initial move vector size is " + grant.movePath.size());
                //if (this.path.size() != 0) {
             		status.direction = this.path.lastElement();
                	status.object = null;
                	grant.send(this.path.lastElement());
                	int pathSize = grant.movePath.size();
                	Logger.debug("PE " + PE.thisPE().getIndex() + " moves object " + key + "/" + grant.object + " to " + requestor.getIndex());
                	Logger.debug("PE "+ PE.thisPE().getIndex() + " sends a grant msg to PE " + grant.movePath.lastElement().getIndex());
                	if (pathSize >= 2 ) {
                		for ( int i = 1; i != pathSize; i++) 
                			Logger.debug("PE " + grant.movePath.elementAt(pathSize - i).getIndex() + " sens a grant msg to PE " + grant.movePath.elementAt(pathSize-i-1).getIndex());
                		}

               //}
                	//else {
                		//grant.send(requestor);
                		//status.direction = requestor;}
              }
            } else {          // object is somewhere else
              if (RelayDirectory.DEBUG)
                Aleph.debug("forwarding to " + direction);
              this.from = PE.thisPE();
              this.path.add(PE.thisPE()); //add to the path vector
              Logger.debug("forwarding find msg " + this + " to " + direction);
              this.send(direction);
            }
          }
          if (RelayDirectory.DEBUG)
            Aleph.debug(this + " returned\t " + status);
        }
      } catch (IOException e) {
        Aleph.panic(e);
      }
    }
    public String toString() {
      StringBuffer s = new StringBuffer("FindMessage[from ");
      s.append(from);
      s.append(", key ");
      s.append(key);
      s.append(", requestor ");
      s.append(requestor);
      s.append("]");
      return s.toString();
    }
  }

  /**
   * This message conveys the object from one owner to the next.
   * It is sent directly, not through the tree.
   **/
  private static class GrantMessage
    extends aleph.Message {

    private static final boolean DEBUG = false;

    GlobalObject key;
    Object       object;
    Vector<PE> movePath; //record the path a grant message travels
    PE requestor;

    GrantMessage(GlobalObject key, Object object, PE requestor) {
      this.key = key;
      this.object = object;
      this.requestor = requestor;
      movePath = new Vector<PE> ();
    }

    GrantMessage(GlobalObject key, Object object, Vector<PE> path, PE requestor) {
        this.key = key;
        this.object = object;
        movePath = new Vector<PE> ();
        this.movePath = (Vector) path.clone();
        this.requestor = requestor;
      }

    public GrantMessage() {}
    public void run() {
    try {
      synchronized (theManager) {
    Status status = (Status) theManager.arrow.get(key);
    Logger.debug("grant msg received for " + " object " + key + "/" + this.object);
    Logger.debug("move vector size is " + this.movePath.size());
    if (this.movePath.size() != 0) {
    	this.movePath.removeElementAt(this.movePath.size()-1);
        if (this.movePath.size() != 0) {
        	status.direction = this.movePath.lastElement(); //flip arrow
        	this.send(this.movePath.lastElement());
			} else {
        			status.direction = this.requestor;
					this.send(this.requestor);
        			}
    } else {	
        	if (RelayDirectory.DEBUG)
        		Aleph.debug(this + " called\t" + status);
        	status.direction = this.requestor;
        	status.object = object;
        	status.busy   = false;
        	status.owner  = null;
        	if (RelayDirectory.DEBUG)
        		Aleph.debug(this + " returns\t" + status);
        	theManager.notifyAll();}
      }
    } catch (IOException e) {
        Aleph.panic(e);
    }
    }
    /**
     * @see java.io.Externalizable
     **/
//    public void writeExternal(ObjectOutput out) throws IOException {
//      super.writeExternal(out);
//      out.writeObject(key);
//      out.writeObject(object);
//      out.writeObject(movePath);
//      out.writeObject(requestor);
//    }
//
//    /**
//     * @see java.io.Externalizable
//     **/
//    public void readExternal(ObjectInput in)
//      throws IOException, java.lang.ClassNotFoundException {
//      super.readExternal(in);
//      key      = (GlobalObject) in.readObject();
//      object   = in.readObject();
//      movePath = (Vector<PE>) in.readObject();
//      requestor = (PE) in.readObject();
//    }

    public String toString () {
      StringBuffer s = new StringBuffer("GrantMessage[key ");
      s.append(key.toString());
      s.append(", object ");
      s.append(object.toString());
      s.append("]");
      return s.toString();
    }
  }

  public static void main(String[] args) {
    int i0 = 0;
    int i1 = 1;
    try {
      if ( args.length > 0)
        i0 = Integer.parseInt(args[0]);
      if ( args.length > 1)
        i1 = Integer.parseInt(args[1]);
    } catch (NumberFormatException e) {
      System.err.println("usage: Matrix <#dimension> <#tests>");
      Aleph.exit(1);
    }
    System.out.println("depth " + i0 + " = " + depth(i0));
    System.out.println("depth " + i1 + " = " + depth(i1));
    System.out.println("descendant(" + i0 + ", " + i1 + ") = " + isDescendant(i0,i1));
  }

}

