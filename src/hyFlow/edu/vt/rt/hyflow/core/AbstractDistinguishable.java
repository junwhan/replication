package edu.vt.rt.hyflow.core;

import java.io.Serializable;

import edu.vt.rt.hyflow.util.network.Network;

import aleph.comm.Address;

public abstract class AbstractDistinguishable implements IDistinguishable{
	
	Address owner;
	public Address getOwnerNode(){
		return owner;
	}
	public void setOwnerNode(Address owner){
		//this.owner = owner;
		this.owner = Network.getInstance().getCoordinator();
	}
	
	boolean shared;
	public void setShared(boolean shared) {
		this.shared = shared;
	}
	public boolean isShared() {
		return shared;
	}

	private boolean valid = true;
	public void invalidate() {
		valid = false;
	}
	
	public boolean isValid() {
		return valid;
	}

	

}
