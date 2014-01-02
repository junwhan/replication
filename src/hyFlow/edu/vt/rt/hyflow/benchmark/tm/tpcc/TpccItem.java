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

public class TpccItem extends AbstractLoggableObject 
{		
	public class Tuple implements Serializable
	{
		public String I_IM_ID;
		public String I_NAME;
		public float I_PRICE;
		public String I_DATA;
		public Tuple(){
			this.I_IM_ID = Integer.toString(random.nextInt(100));
			this.I_NAME = Integer.toString(random.nextInt(100));
			this.I_PRICE = random.nextFloat();
			this.I_DATA = Integer.toString(random.nextInt(100));
		}
		
	}
	public Tuple content = null;
	
	private Random random = new Random();
	public AbstractLockMap locks = null;
	private String id;
	private Long [] ts;
	public static long content__ADDRESS__;
	public static long locks__ADDRESS__;

	{
		try {
			locks__ADDRESS__ = AddressUtil.getAddress(TpccItem.class
					.getDeclaredField("locks"));
			content__ADDRESS__ = AddressUtil.getAddress(TpccItem.class
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
	public TpccItem(String id) {
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
	
	public TpccItem(){}

	public TpccItem TpccItemGet(String id, Context __transactionContext__, String mode){
		try{
			TpccItem item = (TpccItem)HyFlow.getLocator().open(id, mode);

			return item;
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