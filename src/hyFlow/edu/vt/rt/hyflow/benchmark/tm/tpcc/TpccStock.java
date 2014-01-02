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

public class TpccStock extends AbstractLoggableObject 
{	
	public int S_QUANTITY;
	public String S_DIST_01;
	public String S_DIST_02;
	public String S_DIST_03;
	public String S_DIST_04;
	public String S_DIST_05;
	public String S_DIST_06;
	public String S_DIST_07;
	public String S_DIST_08;
	public String S_DIST_09;
	public String S_DIST_10;
	public int S_YTD;
	public int S_ORDER_CNT;
	public int S_REMOTE_CNT;
	public String S_DATA;
	private Random random = new Random();
	
	public AbstractLockMap locks = null;
	private String id;
	
	private String genData(){
		if (random.nextInt(100) < 10) {
			String data = Integer.toString(random.nextInt(100));
			return data+"_ORIGINAL";
		} else {
			return Integer.toString(random.nextInt(100));
		}
	}
	
	public TpccStock(String id) {
		//String id = "item"+Integer.toString(myid)
		this.id = id;
		this.locks = new AbstractLockMap(id);

		this.S_QUANTITY = 10 + random.nextInt(91);
		this.S_DIST_01 = Integer.toString(random.nextInt(100));
		this.S_DIST_02 = Integer.toString(random.nextInt(100));
		this.S_DIST_03 = Integer.toString(random.nextInt(100));
		this.S_DIST_04 = Integer.toString(random.nextInt(100));
		this.S_DIST_05 = Integer.toString(random.nextInt(100));
		this.S_DIST_06 = Integer.toString(random.nextInt(100));
		this.S_DIST_07 = Integer.toString(random.nextInt(100));
		this.S_DIST_08 = Integer.toString(random.nextInt(100));
		this.S_DIST_09 = Integer.toString(random.nextInt(100));
		this.S_DIST_10 = Integer.toString(random.nextInt(100));
		this.S_YTD = 0;
		this.S_ORDER_CNT = 0;
		this.S_REMOTE_CNT = 0;
		this.S_DATA = genData();
		
		this.ts = new Long[15]; //For DATS
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