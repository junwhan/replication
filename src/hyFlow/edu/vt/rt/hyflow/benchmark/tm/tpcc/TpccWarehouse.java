package edu.vt.rt.hyflow.benchmark.tm.tpcc;

import java.io.Serializable;
import java.util.List;
import java.util.Random;
import org.deuce.reflection.AddressUtil;
import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.Context;
import org.deuce.transaction.ContextDelegator;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.tm.undoLog.AbstractLoggableObject;
import edu.vt.rt.hyflow.helper.AbstractLockMap;
import edu.vt.rt.hyflow.util.io.Logger;

public class TpccWarehouse extends AbstractLoggableObject 
{	
	public String W_NAME;
	public String W_STREET_1; 
	public String  W_STREET_2; 
	public String  W_CITY; 
	public String W_STATE;
	public String W_ZIP; 
	public float W_TAX; 
	public float W_YTD; 
		
	private Random random = new Random();
	public AbstractLockMap locks = null;
	private String id;

	public TpccWarehouse(String id) {			
		this.id = id;
		this.locks = new AbstractLockMap(id);
		
		this.W_NAME = Integer.toString(random.nextInt(100));
		this.W_STREET_1 = Integer.toString(random.nextInt(100));
		this.W_STREET_2 = Integer.toString(random.nextInt(100));
		this.W_CITY = Integer.toString(random.nextInt(100));
		this.W_STATE = Integer.toString(random.nextInt(100));
		this.W_ZIP = Integer.toString(random.nextInt(100)) + "11111";
		this.W_TAX = (float)(random.nextInt(2000) * 0.0001);
		this.W_YTD = (float)300000.0;
		this.ts = new Long[8]; //For DATS
		AbstractContext context = ContextDelegator.getTopInstance();
		if (context == null)
			HyFlow.getLocator().register(this);
		else
			context.newObject(this); 
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