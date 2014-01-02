package edu.vt.rt.hyflow.benchmark.tm.list;

import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.dir.IHyFlow;
import edu.vt.rt.hyflow.transaction.Remote;

public class _Node 
	extends 	AbstractDistinguishable{

	private String id;
	private Integer value;
	private String nextId;
	
	public _Node(String id, Integer value) {
		this.id = id;
		this.value = value;
	}
	
	@Remote
	public void setNext(String nextId){
		this.nextId = nextId;
	}

	@Remote
	public String getNext(){
		return nextId;
	}
	
	@Remote
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
