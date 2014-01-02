package edu.vt.rt.hyflow.benchmark.tm.tpcc;

import java.util.Random;

//import org.deuce.Atomic;

import aleph.dir.DirectoryManager;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;
import edu.vt.rt.hyflow.helper.Atomic;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import org.deuce.transaction.Context;

public class Benchmark extends edu.vt.rt.hyflow.benchmark.tm.Benchmark
{

	private Random random = new Random();
	@Override
	protected Class[] getSharedClasses() {
		return new Class[] { Tpcc.class };
	}
	
	@Override
	protected void checkSanity() {
		// TODO Auto-generated method stub
	}

	@Override
	protected void createLocalObjects() {
		Integer id = Network.getInstance().getID();
		//int nodes = Network.getInstance().nodesCount();
		if(id == 0){
			new Tpcc().TpccInit();
		}	
	}
	
	@Override
	protected String getLabel() {
		return "Tpcc-TM";
	}

	@Override
	protected int getOperandsCount() {
		if (calls < 2)
			return 2;
		else
			return calls;
	}

	@Override
	protected Object randomId() {
		return random.nextInt(100);
	}

	@Override
	protected void readOperation(Object... ids) {
		int r = random.nextInt(3);
		try{
			switch(r) 
			{
			case 0:
				Logger.debug("Read operation: orderStatus.");
				Tpcc.orderStatus(calls);
				//test_orderStatus(calls);
				break;
			case 1:
				Logger.debug("Read operation: delivery.");
				Tpcc.delivery(calls);
				//test_orderStatus(calls);
				break;
			case 2:
				Logger.debug("Read operation: stock Level.");
				Tpcc.stockLevel(calls);
				//test_orderStatus(calls);
				break;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		} 		
	}

	@Override
	protected void writeOperation(Object... ids) {
		int r = random.nextInt(2);
		try{
			switch(r)
			{
			case 0:
				Logger.debug("Write operation: new_order.");
				Tpcc.newOrder(calls);
				break;
			case 1:
				Logger.debug("Write operation: payment.");
				Tpcc.payment(calls);
				break;
	
			}
		} catch (Throwable e) {
			e.printStackTrace();
		} 	
	}
	
	protected void test_orderStatus(final int count) {
		try {
			new Atomic<Object>() {
				@Override
				public Object atomically(AbstractDistinguishable self,
						Context transactionContext) {
					for (int i=0; i<count; i++) {
						final String myid = "warehouse_"+Integer.toString(random.nextInt(Tpcc.NUM_WAREHOUSES));
						final String omyid = myid+"_order_" + Integer.toString(random.nextInt(Tpcc.NUM_ORDERS_PER_D));
						final TpccOrder order = (TpccOrder)HyFlow.getLocator().open(omyid, "w", new int [1]);
						order.content.O_CARRIER_ID = Integer.toString(100);
						if (Benchmark.outer_delay)
							edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
					}
					return null;
				}
			}.execute(null);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	
}
