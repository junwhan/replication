package edu.vt.rt.hyflow.benchmark.tm.test;

import edu.vt.rt.hyflow.core.AbstractDistinguishable;

public class TestObject extends AbstractDistinguishable {
	private static final long serialVersionUID = 1L;
	Integer id;
	Integer data;
	public TestObject(Integer id) {
		this.id = id;
		this.data = 500;
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
	public Integer getData(){
		return data;
	}

	public void setData(int value){
		data+=value;
	}
}

