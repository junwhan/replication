package edu.vt.rt.hyflow.benchmark.tm.tpcc;

import java.util.Random;
import org.deuce.reflection.AddressUtil;
import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.Context;
import org.deuce.transaction.ContextDelegator;
import java.io.Serializable;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.tm.undoLog.AbstractLoggableObject;
import edu.vt.rt.hyflow.helper.AbstractLockMap;
import edu.vt.rt.hyflow.util.io.Logger;

public class TpccOrderline extends AbstractLoggableObject 
{	
	public class Tuple implements Serializable
	{ 
		public int OL_I_ID; 
		public int OL_SUPPLY_W_ID; 
		public String OL_DELIVERY_D; 
		public int OL_QUANTITY; 
		public int OL_AMOUNT; 
		public String OL_DIST_INFO;
		public Tuple(){
			this.OL_I_ID = 1 + random.nextInt(100000);
			this.OL_SUPPLY_W_ID = random.nextInt(1000);
			this.OL_DELIVERY_D = Integer.toString(random.nextInt(100));
			this.OL_QUANTITY = 5;
			this.OL_AMOUNT = genAmount(random.nextInt(3000));
			this.OL_DIST_INFO = Integer.toString(random.nextInt(100));
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
	private int genAmount(int a){
		if (a < 2101) return 0; 
		else { 
			return (1 + random.nextInt(999999));
		}
		
	}
	public TpccOrderline(){}
	public TpccOrderline(String id) {
		//String id = "item"+Integer.toString(myid)
		this.id = id;
		this.locks = new AbstractLockMap(id);
		this.content = new Tuple();
		this.ts = new Long[6]; //For DATS				
		AbstractContext context = ContextDelegator.getTopInstance();
		if (context == null)
			HyFlow.getLocator().register(this);
		else
			context.newObject(this); 
	}
	public TpccOrderline TpccOrderlineGet(String id, Context __transactionContext__, String mode){
		try{
			TpccOrderline orderline = (TpccOrderline)HyFlow.getLocator().open(id, mode);
			if(orderline != null){
				if(mode.equals("w")){
					//ContextDelegator.onReadAccess(order, order.content, order.content__ADDRESS__, __transactionContext__);
				//else 
					ContextDelegator.onWriteAccess(orderline, orderline.content, orderline.content__ADDRESS__, __transactionContext__);
				}
			}
			return orderline;
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