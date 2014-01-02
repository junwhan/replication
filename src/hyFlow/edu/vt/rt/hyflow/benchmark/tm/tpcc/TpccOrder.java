package edu.vt.rt.hyflow.benchmark.tm.tpcc;

import java.io.Serializable;
import java.util.Random;
import org.deuce.reflection.AddressUtil;
import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.Context;
import org.deuce.transaction.ContextDelegator;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.tm.undoLog.AbstractLoggableObject;
import edu.vt.rt.hyflow.helper.AbstractLockMap;
import edu.vt.rt.hyflow.util.io.Logger;

public class TpccOrder extends AbstractLoggableObject 
{	
	public class Tuple implements Serializable
	{ 
		public int O_C_ID;
		public String O_ENTRY_D;
		public String O_CARRIER_ID;
		public int O_OL_CNT;
		public Boolean O_ALL_LOCAL;
		public Tuple(){
			this.O_C_ID = random.nextInt(100);
			this.O_ENTRY_D = Integer.toString(random.nextInt(100));
			this.O_CARRIER_ID = Integer.toString(random.nextInt(100));
			this.O_OL_CNT = 5 + random.nextInt(11);
			this.O_ALL_LOCAL = true;
		}
	}
	private Random random = new Random();
	public Tuple content = null;
	public AbstractLockMap locks = null;
	private String id;
	private Long [] ts;
	public static long content__ADDRESS__;
	public static long locks__ADDRESS__;
	{
		try {
			locks__ADDRESS__ = AddressUtil.getAddress(TpccOrder.class
					.getDeclaredField("locks"));
			content__ADDRESS__ = AddressUtil.getAddress(TpccOrder.class
					.getDeclaredField("content"));
		} catch (Exception e) {
			e.printStackTrace(Logger.levelStream[Logger.DEBUG]);
		}
	}
	@Override
	public Long[] getTS(){
		return ts;
	}
	@Override
	public void setTS(Long [] ts){
		// for DATS
		this.ts = ts;
	}
	public TpccOrder(String id) {
		//String id = "item"+Integer.toString(myid)
		this.id = id;
		this.locks = new AbstractLockMap(id);
		this.content = new Tuple();
		this.ts = new Long[5]; //For DATS
		AbstractContext context = ContextDelegator.getTopInstance();
		if (context == null)
			HyFlow.getLocator().register(this);
		else
			context.newObject(this); 
	}
	public TpccOrder(){}

	public TpccOrder TpccOrderGet(String id, Context __transactionContext__, String mode){
		try{
			TpccOrder order = (TpccOrder)HyFlow.getLocator().open(id, mode);
			
			return order;
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}
	@Override
	public Object getId() {
		return id;
	}

}