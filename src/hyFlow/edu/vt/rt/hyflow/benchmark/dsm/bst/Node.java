package edu.vt.rt.hyflow.benchmark.dsm.bst;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.ContextDelegator;

import aleph.GlobalObject;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;

public class Node 
	extends 	AbstractDistinguishable{

	private String id;
	private Integer value;
	private String rightId;
	private String leftId;
	
	public Node(String id, Integer value) {
		this.id = id;
		this.value = value;

		AbstractContext context = ContextDelegator.getInstance();
		if(context.getContextId()==null)
			new GlobalObject(this, ((AbstractDistinguishable)this).getId());	// publish it now	
		else
			context.newObject(this);	// add it to context publish-set 
	}
	
	public void setRightChild(String rightId){
		this.rightId = rightId;
	}
	
	public void setLeftChild(String leftId){
		this.leftId = leftId;
	}

	public String getRightChild(){
		return rightId;
	}

	public String getLeftChild(){
		return leftId;
	}

	public Integer getValue(){
		return value;
	}
	
	@Override
	public Object getId() {
		return id;
	}
	private Long [] ts;
	@Override
	public Long[] getTS(){
		return ts;
	}
	@Override
	public void setTS(Long [] ts){
		// for DATS
		this.ts = ts;
	}
}
