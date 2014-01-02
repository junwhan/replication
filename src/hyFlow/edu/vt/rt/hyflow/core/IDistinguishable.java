package edu.vt.rt.hyflow.core;

import java.io.Serializable;

import aleph.comm.Address;

public interface IDistinguishable extends Serializable{
	
	public abstract Object getId();
	public abstract Long[] getTS();
	public abstract void setTS(Long [] ts);
	
	public Address getOwnerNode();
	public void setOwnerNode(Address owner);

	public void setShared(boolean shared);
	public boolean isShared();
	
	public void invalidate();
	public boolean isValid();
}
