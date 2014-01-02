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

public class TpccHistory extends AbstractLoggableObject 
{	
	public int H_C_ID;
	public int H_C_D_ID;
	public int H_C_W_ID;
	public int H_D_ID;
	public int H_W_ID;
	public String H_DATE;
	public Double H_AMOUNT;
	public String H_DATA;
	
	private Random random = new Random();
	
	public AbstractLockMap locks = null;
	private String id;

	public TpccHistory(String id, int c_id, int d_id) {
		//String id = "item"+Integer.toString(myid)
		this.id = id;
		this.locks = new AbstractLockMap(id);

		this.H_W_ID = random.nextInt(100);
		this.H_D_ID = d_id;
		this.H_C_ID = c_id;
		this.H_DATE = Integer.toString(random.nextInt(100));
		this.H_AMOUNT = 10.0;
		this.H_DATA = Integer.toString(random.nextInt(100));
		this.ts = new Long[6]; //For DATS
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