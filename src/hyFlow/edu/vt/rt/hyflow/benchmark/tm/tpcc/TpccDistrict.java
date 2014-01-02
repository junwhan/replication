package edu.vt.rt.hyflow.benchmark.tm.tpcc;

import java.util.Random;
import org.deuce.reflection.AddressUtil;
import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.Context;
import org.deuce.transaction.ContextDelegator;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.tm.undoLog.AbstractLoggableObject;
import edu.vt.rt.hyflow.helper.AbstractLockMap;
import edu.vt.rt.hyflow.util.io.Logger;

public class TpccDistrict extends AbstractLoggableObject 
{	
	public String  D_NAME;
	public String  D_STREET_1;
	public String  D_STREET_2;
	public String  D_CITY;
	public String  D_STATE;
	public String  D_ZIP;

	public double D_TAX;
	public double D_YTD; 
	public int D_NEXT_O_ID;
	private Random random = new Random();
	
	public AbstractLockMap locks = null;
	private String id;

	public TpccDistrict(String id) {
		//String id = "item"+Integer.toString(myid)
		this.id = id;
		this.locks = new AbstractLockMap(id);

		this.D_NAME = Integer.toString(random.nextInt(100));
		this.D_STREET_1 = Integer.toString(random.nextInt(100));
		this.D_STREET_2 = Integer.toString(random.nextInt(100));
		this.D_CITY = Integer.toString(random.nextInt(100));
		this.D_STATE = Integer.toString(random.nextInt(100));
		this.D_ZIP = Integer.toString(random.nextInt(100)) + "11111";
		this.D_TAX = (double)(random.nextInt(2000) * 0.0001);
		this.D_YTD = (double)30000.0;
		this.D_NEXT_O_ID = 3001;
		this.ts = new Long[9]; //For DATS
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