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

public class TpccCustomer extends AbstractLoggableObject 
{	
	public class Tuple implements Serializable
	{  
		public String C_FIRST;
		public String C_MIDDLE;
		public String C_LAST;
		public String C_STREET_1;
		public String C_STREET_2;
		public String C_CITY;
		public String C_STATE;
		public String C_ZIP;
		public String C_PHONE;
		public String C_SINCE;
		public String C_CREDIT;
		public Double C_CREDIT_LIM;
		public Double C_DISCOUNT;
		public Double C_BALANCE;
		public Double C_YTD_PAYMENT;
		public int C_PAYMENT_CNT;
		public int C_DELIVERY_CNT;
		public String C_DATA;
		public Tuple(){
			this.C_FIRST = Integer.toString(random.nextInt(100));
			this.C_MIDDLE = "OE";
			this.C_LAST = Integer.toString(random.nextInt(100));
			this.C_STREET_1 = Integer.toString(random.nextInt(100));
			this.C_STREET_2 = Integer.toString(random.nextInt(100));
			this.C_CITY = Integer.toString(random.nextInt(100));
			this.C_STATE = Integer.toString(random.nextInt(100));
			this.C_ZIP = Integer.toString(random.nextInt(100)) + "11111";
			this.C_PHONE = Integer.toString(random.nextInt(100));
			this.C_SINCE = Integer.toString(random.nextInt(100));
			this.C_CREDIT = genCredit();
			this.C_CREDIT_LIM = (double)50000.0;
			this.C_DISCOUNT = (double)(random.nextInt(5000) * 0.0001);
			this.C_BALANCE = (double)10.0;
			this.C_YTD_PAYMENT = (double)10.0;
			this.C_PAYMENT_CNT = 1;
			this.C_DELIVERY_CNT = 0;
			this.C_DATA = Integer.toString(random.nextInt(100));
		}
		private String genCredit(){
			if (random.nextInt(100) < 90) return "GC"; 
			else return "BC";
		}
	}
	private Random random = new Random();
	
	public AbstractLockMap locks = null;
	private String id;
	private Long [] ts;
	
	public static long content__ADDRESS__;
	public static long locks__ADDRESS__;
	public Tuple content = null;
	{
		try {
			locks__ADDRESS__ = AddressUtil.getAddress(TpccCustomer.class
					.getDeclaredField("locks"));
			content__ADDRESS__ = AddressUtil.getAddress(TpccCustomer.class
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
	
	public TpccCustomer(String id) {
		this.id = id;
		this.locks = new AbstractLockMap(id);
		this.content = new Tuple();

		this.ts = new Long[18]; //For DATS
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
	public TpccCustomer(){}

	public TpccCustomer TpccCustomerGet(String id, Context __transactionContext__, String mode, int [] commute){
		try{
			TpccCustomer customer = (TpccCustomer)HyFlow.getLocator().open(id, mode, commute);
			if(customer != null){
				if(mode.equals("w"))
					ContextDelegator.onWriteAccess(customer, customer.content, customer.content__ADDRESS__, __transactionContext__);
			}
			return customer;
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}
}