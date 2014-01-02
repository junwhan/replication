package edu.vt.rt.hyflow.core.tm.control;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.TransactionException;
import org.deuce.transform.Exclude;

import aleph.PE;
import aleph.comm.Address;
import edu.vt.rt.hyflow.core.tm.control.vote.VoteReply;
import edu.vt.rt.hyflow.core.tm.control.vote.VoteRequest;
import edu.vt.rt.hyflow.core.tm.control.vote.VoteResult;
import edu.vt.rt.hyflow.util.io.Logger;

/**
 * Control flow Base Context class
 *
 * @author Mohamed M. Saad
 * @since	1.0
 */
@Exclude
abstract public class ControlContext extends AbstractContext{

	protected static int WRITE_SET_INDEX 		= 0;
//	protected static int READ_SET_INDEX 		= 1;
	protected static int NEIGHBOR_INDEX 		= 2;
	protected static int NEIGHBOR_TREE_INDEX 	= 3;
	protected static int VOTE_ACTIVE_INDEX 		= 4;
	protected static int VOTE_DECISION_INDEX	= 5;
	protected static int FINISHED_INDEX			= 6;
	protected static int CONTEXT_INDEX			= 7;
	protected static int STATUS_INDEX			= 8;
	
	protected static int METADATA_SIZE			= 9;
	
	protected static final int ACTIVE 	= 0;
	protected static final int BUSY 	= 1;
	protected static final int ABORTED 	= 2;
	
	abstract protected boolean rollback(Long txnId);
	abstract protected boolean release(Long txnId);
	
	protected static ControlContext handler;
	
	public static Map<Long, Object[]> registery = Collections.synchronizedMap(new HashMap<Long, Object[]>());
	
	protected Set<Address> neighbors;
	
	abstract protected boolean tryCommit(Long txnId);
	
	public void addNeighbor(Address address){
		neighbors.add(address);
	}
	
	protected boolean isAborted(){
		Object[] metadata;
		boolean a = status.equals(STATUS.ABORTED) || (metadata=registery.get(txnId))==null || ((AtomicInteger)metadata[STATUS_INDEX]).get()==ABORTED;
		if(a)
			Logger.debug("Aborted");
		return a;
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj !=null && obj instanceof ControlContext && txnId.equals(((ControlContext)obj).txnId);
	}
	
	public static void voteReply(Long txnId, Address from, Boolean decision){
		// PHASE II - receive replies
		Object[] metadata = registery.get(txnId);
		if(metadata==null)
			return;	// late reply

		synchronized (metadata){
			if(!(Boolean)metadata[VOTE_ACTIVE_INDEX])	// early reply
				try {
					Logger.debug("Race reply/request...");
					metadata.wait();
					Logger.debug("Request arrived");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}
		
		Set<Address> neighbors = (Set<Address>)metadata[NEIGHBOR_INDEX];
		synchronized (neighbors) {
//			Logger.debug("Reply received... [" + decision + "] "+ from + "/" + neighbors);
			if(decision==null){
				Logger.debug(txnId + ":Access " + from);
				Set<Address> set = ((Set<Address>)metadata[NEIGHBOR_TREE_INDEX]);
//				if(set!=null)
					set.remove(from);
			}
			else if(!decision)
				metadata[VOTE_DECISION_INDEX] = decision;
			if(from!=null)
				getNeighbors(txnId).remove(from);
			switch(neighbors.size()){
				case 0: 
					// PHASE III - publish decision
					Logger.debug(txnId + ": I'm coordinator");
					voteResult(txnId, null, (Boolean)metadata[VOTE_DECISION_INDEX]);
					break;
				case 1:
					// forward to the last neighbor
					Logger.debug(txnId + ": Send collected decision [" + decision + "/" + (Boolean)metadata[VOTE_DECISION_INDEX] + "] to " + neighbors.iterator().next());
					try {
						new VoteReply(txnId, (Boolean)metadata[VOTE_DECISION_INDEX]).send(neighbors.iterator().next());
					} catch (IOException e) {	e.printStackTrace(); }
					break;
			}
		}
	}

	
	public static void voteResult(Long txnId, Address from, Boolean result){
		Logger.debug(txnId + ":" + result);
		Logger.debug(txnId + ":+++++++++ RESULT ++++++++");
		Object[] metadata = registery.get(txnId);
		if(metadata==null)
			return;	// late reply
		synchronized (metadata) {
			if((Boolean)metadata[FINISHED_INDEX])
				return;
			metadata[FINISHED_INDEX] = true;
		}
		Logger.debug(txnId + ":============== RESULT ===============");
		if(metadata[CONTEXT_INDEX]!=null){	// originating node
			Logger.debug(txnId + ":try synch...");
			synchronized (metadata[NEIGHBOR_INDEX]) {
				metadata[VOTE_DECISION_INDEX] = result;
				Logger.debug(txnId + ":notify");
				metadata[NEIGHBOR_INDEX].notifyAll();	// wake transaction thread
				Logger.debug(txnId + ":done..");
			}
		}else{
			if(result)
				Logger.debug(txnId + ": COMMIT REMOTE");	// commit at remote node
			else
				Logger.debug(txnId + ": ABORT REMOTE");	// rollback at remote node
			
			if(!result)
				handler.rollback(txnId);
			handler.release(txnId);
		}
		Logger.debug(txnId + ":sending... " + ((Set<Address>)metadata[NEIGHBOR_TREE_INDEX]));
		VoteResult resultMessage = new VoteResult(txnId, result);
		for(Address neighbor: ((Set<Address>)metadata[NEIGHBOR_TREE_INDEX])){
			Logger.debug(neighbor.toString());
			if(!neighbor.equals(from))
				try {
					resultMessage.send(neighbor);
					Logger.debug(txnId + ": Sent ---------------------------------------->" + neighbor);
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
	
	public static Set<Address> getNeighbors(Long key){
		Object[] data = registery.get(key);
		return data==null ? new HashSet<Address>() : (Set<Address>)data[NEIGHBOR_INDEX];
	}

	public static void abort(Long txnId) {
		Object[] metadata = registery.get(txnId);
		if(metadata==null)	// already aborted before
			return;
		Set<Address> neighbors = (Set<Address>)metadata[NEIGHBOR_INDEX];
		synchronized (neighbors) {
//			System.out.println("Abort...");
			handler.rollback(txnId);
			if(handler.release(txnId))
				for(Address address: neighbors)	// forward abort to neighbors
					try {
						new DistributedAbort(txnId).send(address);
					} catch (IOException e) {
						e.printStackTrace();
					}
			neighbors.notifyAll();
		}
	}

	public static void vote(Long txnId, Address from){
		Object[] metadata = registery.get(txnId);

		boolean duplica = true;
		if(metadata!=null)
			synchronized (metadata) {
				if(!(Boolean)metadata[VOTE_ACTIVE_INDEX]){	// check call-graph loop
					duplica = false;
					metadata[VOTE_ACTIVE_INDEX] = true;	// set active voting procedure flag
					((AtomicInteger)metadata[STATUS_INDEX]).compareAndSet(ACTIVE, BUSY);
					Logger.debug(txnId + ":Tree");
					Set<Address> tree = (Set<Address>)(metadata[NEIGHBOR_TREE_INDEX] = new HashSet<Address>());
					Logger.debug(txnId + ":Tree done");
					for(Address neighbor: (Set<Address>)metadata[NEIGHBOR_INDEX])
						tree.add(neighbor);
					metadata[VOTE_DECISION_INDEX] = handler.tryCommit(txnId);	// make my decision
					Logger.debug(txnId + ": make my decision");
				}
			}
		
		if(duplica){
			if(from!=null)
				try {
					Logger.debug(txnId + ": FAKE ---------------------- reply to " + from);
					new VoteReply(txnId, null).send(from);
				} catch (IOException e) {
					e.printStackTrace();
				}
			return;
		}

		try {
			Set<Address> neighbors = (Set<Address>)metadata[NEIGHBOR_INDEX];
			synchronized (neighbors) {
				// PHASE I - publish vote request
//				Logger.debug(from);
				boolean waitReply = false;
				for(Address neighbor: neighbors){
					if(!neighbor.equals(from)){
						Logger.debug(txnId + ": Sending vote request to " + neighbor);
						waitReply = true;
						new VoteRequest(txnId).send(neighbor);
					}else
						Logger.debug(txnId + ": No Forwarding to parent");
				}
				
				synchronized (metadata) {
					metadata.notifyAll();
				}
					
//				metadata[VOTE_DECISION_INDEX] = ((AtomicInteger)metadata[STATUS_INDEX]).get()==ABORTED ? false : tryCommit(txnId);	// make my decision
//				while(neighbors.size()>1){			// collect votes
				if(waitReply){
					Logger.debug(txnId + ": " + neighbors.size() + " wait reply");
					Logger.debug(txnId + ": wait decision....");
					try {	neighbors.wait(); 	} catch (InterruptedException e) {	e.printStackTrace();	}
				}else
					voteReply(txnId, null, (Boolean)metadata[VOTE_DECISION_INDEX]);
//					Logger.debug(txnId + ": respond reply");
//				}
			}
			if(registery.get(txnId)==null){
//				System.out.println("Asynch Force Abort");
				return;
			}
				
			Logger.debug(txnId + ": got decision....");
			
			if(metadata[CONTEXT_INDEX]!=null){
				Logger.debug(txnId + ": originator");
//				Logger.debug(txnId + ": wait..........");
//				synchronized (metadata[CONTEXT_INDEX]) {
//					try {
//						metadata[CONTEXT_INDEX].wait();
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//				}
//				Logger.debug("Done...");
				if(!(Boolean)metadata[VOTE_DECISION_INDEX])
					throw new TransactionException();	// aborts
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Address lastExecuter;
	
	public Address getLastExecuter(){
		return lastExecuter;
	}
	
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeObject(PE.thisPE().getAddress());
	}
	
	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		super.readExternal(in);
		lastExecuter = (Address) in.readObject();
	}

}