/*
 * @(#)Context.java   05/01/2008
 *
 * Copyright 2008 GigaSpaces Technologies Inc.
 */

package org.deuce.transaction;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import org.deuce.transform.Exclude;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.util.network.Network;

/**
 * All the STM implementations should implement this interface.
 * Using the -Dorg.deuce.transaction.contextClass property one can
 * switch between the different implementations. 
 *
 * @author	Guy Korland
 * @since	1.0
 */
@Exclude
public abstract class AbstractContext implements Context, Serializable, Externalizable
{
	private static final long serialVersionUID = 1L;
	public static int aborts = 0;

	public Object contentionMetadata;
	public enum STATUS { ABORTED, ACTIVE, BUSY }
	public STATUS status = STATUS.ACTIVE;
	protected Long txnId;
	public boolean local = true;
	private int lastRetries = 0;
	private int retries = 0;
	private int transactions = 0;
	
	@Override
	public void init(int atomicBlockId) {
		status = STATUS.ACTIVE;
		HyFlow.getConflictManager().init(this);

		// talex: TODO / IMPORTANT
		// We may want to switch back to the original
		// using the atomicBlockId variable, otherwise
		// we have no way of differentiating between
		// nested transactions (do we?)
		txnId = Network.getInstance().getID() * 1000000000 + transactions * 10000000 + retries * 1000 + Thread.currentThread().getId(); // atomicBlockId;
		retries++;
		transactions++;
	}
	
	public int getRetries(){
		return lastRetries;
	}
	
	public void complete() {
		lastRetries = retries;
		retries = 0;
		HyFlow.getConflictManager().complete(this);
	}
	
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		contentionMetadata = in.readObject();
		txnId = (Long)in.readObject();
		local = false;
	}
	
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(contentionMetadata);
		out.writeObject(txnId);
	}
	
	public void newObject(AbstractDistinguishable object){}
	
	public void delete(AbstractDistinguishable deleted) {}

	public Long getContextId(){
		return txnId;
	}
	
	@Override
	public String toString() {
		return "[" + txnId + (local ? "" : "(R)") + "|" + status + "]";
	}
}
