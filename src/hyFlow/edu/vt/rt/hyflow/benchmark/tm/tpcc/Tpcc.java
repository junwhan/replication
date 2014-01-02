package edu.vt.rt.hyflow.benchmark.tm.tpcc;

import java.util.Random;

import org.deuce.reflection.AddressUtil;
import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.Context;
import org.deuce.transaction.ContextDelegator;
import org.deuce.transaction.TransactionException;

import aleph.dir.DirectoryManager;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.dir.IHyFlow;
import edu.vt.rt.hyflow.core.tm.NestedContext;
import edu.vt.rt.hyflow.core.tm.NestingModel;
import edu.vt.rt.hyflow.core.tm.undoLog.AbstractLoggableObject;
import edu.vt.rt.hyflow.helper.AbstractLockMap;
import edu.vt.rt.hyflow.helper.Atomic;
import edu.vt.rt.hyflow.helper.TrackerLockMap;
import edu.vt.rt.hyflow.util.io.Logger;

public class Tpcc extends AbstractLoggableObject 
{
	//TODO: implement a way to copy from-to the same bucket!!! currently, it livelocks
	private static Random random = new Random();
	private AbstractLockMap locks = null;
	//public TpccItem item = null;
	//public static long item__ADDRESS__;
	public static long locks__ADDRESS__;

	private String id;
	public static long id__ADDRESS__;
	public static long $HY$_proxy__ADDRESS__;
	private Object $HY$_id;
	public static long $HY$_id__ADDRESS__;
	
	// Constants
	public static int NUM_ITEMS = 10; // Correct overall # of items: 100,000
	public static int NUM_WAREHOUSES = 3;
	public static int NUM_DISTRICTS = 3;
	public static int NUM_CUSTOMERS_PER_D = 10;
	public static int NUM_ORDERS_PER_D = 10;
	public static int MAX_CUSTOMER_NAMES = 6;
	
	{
		try {
			locks__ADDRESS__ = AddressUtil.getAddress(Tpcc.class
					.getDeclaredField("locks"));
			//item__ADDRESS__ = AddressUtil.getAddress(TpccItem.class
				//	.getDeclaredField("content"));
			id__ADDRESS__ = AddressUtil.getAddress(Tpcc.class
					.getDeclaredField("id"));
			$HY$_proxy__ADDRESS__ = AddressUtil.getAddress(Tpcc.class
					.getDeclaredField("$HY$_proxy"));
			$HY$_id__ADDRESS__ = AddressUtil.getAddress(Tpcc.class
					.getDeclaredField("$HY$_id"));
		} catch (Exception e) {
			e.printStackTrace(Logger.levelStream[Logger.DEBUG]);
		}
	}
	
	public void TpccInit() {
		//Logger.debug("Creating local objects here.");
		try{
			for (int id=0; id<NUM_ITEMS; id++) {
				final String myid = "item_"+Integer.toString(id);
				new TpccItem(myid);
			}	 
			for (int id = 0; id < NUM_WAREHOUSES; id++) {
				final String myid = "warehouse_"+Integer.toString(id);
				new TpccWarehouse(myid);
				for (int s_id=0; s_id<NUM_ITEMS; s_id++){
					final String smyid = myid+"_stock_"+ Integer.toString(s_id);
					new TpccStock(smyid);
				}
				for (int d_id = 0; d_id < NUM_DISTRICTS; d_id++) {
					String dmyid = myid+"_"+ Integer.toString(d_id);
					new TpccDistrict(dmyid);
					for (int c_id = 0; c_id < NUM_CUSTOMERS_PER_D; c_id++) {
						String cmyid = myid + "_customer_" + Integer.toString(c_id);
						new TpccCustomer(cmyid);
						String hmyid = myid + "_history_" + Integer.toString(c_id);
						new TpccHistory(hmyid, c_id, d_id);
					}
				}
				for (int o_id = 0; o_id < NUM_ORDERS_PER_D; o_id++) {
					String omyid = myid+"_order_" + Integer.toString(o_id);
					new TpccOrder(omyid);
					String olmyid = myid+"_orderline_" + Integer.toString(o_id);
					new TpccOrderline(olmyid);
				}	
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
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
	public static void orderStatus(final int count) {
		try {
			new Atomic<Object>(true) {
				@Override
				public Object atomically(AbstractDistinguishable self, Context transactionContext) {
					try {
						DirectoryManager locator = HyFlow.getLocator();
						final String myid = "warehouse_"+Integer.toString(random.nextInt(NUM_WAREHOUSES));
						final String cmyid = myid+"_customer_" + Integer.toString(random.nextInt(NUM_CUSTOMERS_PER_D));
						TpccCustomer customer = new TpccCustomer().TpccCustomerGet(cmyid, transactionContext, "r", new int[17]);
						
						final String omyid = myid+"_order_" + Integer.toString(random.nextInt(NUM_ORDERS_PER_D));
						//final TpccOrder order = (TpccOrder)locator.open(omyid, "r", 0);
						TpccOrder order = new TpccOrder().TpccOrderGet(omyid, transactionContext, "r");

						float olsum = (float)0;
						int i = 0;
						while (i < order.content.O_OL_CNT) {
							if(i< NUM_ORDERS_PER_D){
								final String olmyid = myid+"_orderline_" + Integer.toString(i);
								//TpccOrderline orderline = (TpccOrderline)locator.open(olmyid, "r", 0);
								TpccOrderline orderline = new TpccOrderline().TpccOrderlineGet(olmyid, transactionContext, "r");
								if(orderline != null){
									olsum += orderline.content.OL_AMOUNT;
									i += 1;
								}
							}
							else i += 1;
						}
						return null;
					} finally {
						edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
					}
				}
			}.execute(null);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	public static void delivery(final int count) {
		for (int d_id = 0; d_id < NUM_DISTRICTS; d_id++) {
			final String myid = "warehouse_"+Integer.toString(random.nextInt(NUM_WAREHOUSES));
			final String omyid = myid+"_order_" + Integer.toString(random.nextInt(NUM_ORDERS_PER_D));
			final String cmyid = myid+ "_customer_" + Integer.toString(random.nextInt(NUM_CUSTOMERS_PER_D));
			try {
				new Atomic<Object>(true) {
					@Override
					public Object atomically(AbstractDistinguishable self,Context transactionContext) {
						try {
							//TpccOrder order = (TpccOrder)HyFlow.getLocator().open(omyid, "r", 0);
							TpccOrder order = new TpccOrder().TpccOrderGet(omyid, transactionContext, "r");
							//order.delete();
							float olsum = (float)0;
							if(order != null){	
								String crtdate = new java.util.Date().toString();
								int i = 1;
								while (i < order.content.O_OL_CNT) {
									if(i < NUM_ORDERS_PER_D){
										final String olmyid = myid+"_orderline_" + Integer.toString(i);
										//TpccOrderline orderline = (TpccOrderline)HyFlow.getLocator().open(olmyid, "r", 0);
										TpccOrderline orderline = new TpccOrderline().TpccOrderlineGet(olmyid, transactionContext, "r");
										if(orderline != null){
											olsum += orderline.content.OL_AMOUNT;
											i += 1;
										}
									}
									else i += 1;
								}
							}
							int [] commute = new int[18];
							commute[14] = 1;
							commute[17] = 1;
							TpccCustomer customer = new TpccCustomer().TpccCustomerGet(cmyid, transactionContext, "w", commute);
							if(customer != null && customer.content != null){
								customer.content.C_BALANCE += olsum;
								customer.content.C_DELIVERY_CNT += 1;
							}
							return null;
						} finally {
							edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
						}
					}
	
				}.execute(null);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void stockLevel(final int count) {
		int i=0;
		while (i < 20) {
				try {
					new Atomic<Object>(true) {
						@Override
						public Object atomically(AbstractDistinguishable self, Context transactionContext) {
							try {
								final String myid = "warehouse_"+Integer.toString(random.nextInt(NUM_WAREHOUSES));
								final String omyid = myid+"_order_" + Integer.toString(random.nextInt(NUM_ORDERS_PER_D));
								//TpccOrder order = (TpccOrder)HyFlow.getLocator().open(omyid, "r", 0);
								TpccOrder order = new TpccOrder().TpccOrderGet(omyid, transactionContext, "r");
								if (order != null) {
									int j = 1;
									while (j < order.content.O_OL_CNT) {
										if(j < NUM_ORDERS_PER_D){
											final String olmyid = myid+"_orderline_" + Integer.toString(j);
											int [] commute = new int [10];
											TpccOrderline orderline = (TpccOrderline)HyFlow.getLocator().open(olmyid, "r", commute);
										}
										j += 1;
									}
								}
								int k = 1;
								while (k <= 10) {
									String wid = "warehouse_"+Integer.toString(random.nextInt(NUM_WAREHOUSES));
									if(k<NUM_ITEMS){
										String smyid = wid+"_stock_"+ Integer.toString(k);
										HyFlow.getLocator().open(smyid, "r", new int [1] );
										k += 1;
									}
									else k += 1;
								}
								return null;
							} finally {
								edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
							}
						}
					}.execute(null);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			 
			i += 1;
		}
		
	}
	
	public static void newOrder(final int count) {
		final int w_id = random.nextInt(NUM_WAREHOUSES);
		final String myid = "warehouse_"+Integer.toString(w_id);
		final int d_id = random.nextInt(NUM_DISTRICTS);
		final String dmyid = myid+"_"+ Integer.toString(d_id);
	
		try {
			new Atomic<Object>(true) {
				@Override
				public Object atomically(AbstractDistinguishable self,Context transactionContext) {
					TpccWarehouse warehouse = (TpccWarehouse)HyFlow.getLocator().open(myid, "r", new int [1]);
					TpccDistrict district = (TpccDistrict)HyFlow.getLocator().open(dmyid, "w", new int [1]);
					
					if (((NestedContext)transactionContext).getNestingModel() == NestingModel.OPEN) {
						((NestedContext)transactionContext).onLockAction(
								district.getId().toString(), dmyid, true, true);
						m_hasOnAbort = true;
						m_hasOnCommit = true;
					}	
					double D_TAX = district.D_TAX;
					int o_id = district.D_NEXT_O_ID;
					district.D_NEXT_O_ID = o_id + 1;
					int c_id = random.nextInt(NUM_CUSTOMERS_PER_D);
					if(o_id < NUM_CUSTOMERS_PER_D) c_id = o_id;
					final String cmyid = myid+ "_customer_" + Integer.toString(c_id);
					TpccCustomer customer = new TpccCustomer().TpccCustomerGet(cmyid, transactionContext, "r", new int [17]);
					if(customer != null){
						double C_DISCOUNT = customer.content.C_DISCOUNT;
						String C_LAST = customer.content.C_LAST;
						String C_CREDIT = customer.content.C_CREDIT;
					}
					// Create entries in ORDER and NEW-ORDER
					final String omyid = myid+"_order_" + Integer.toString(random.nextInt(NUM_ORDERS_PER_D));
					TpccOrder order = new TpccOrder(omyid);
					order.content.O_C_ID = c_id;
					order.content.O_CARRIER_ID = Integer.toString(random.nextInt(1000));
					order.content.O_ALL_LOCAL = true;
					int i = 1;
					while (i <= order.content.O_CARRIER_ID.length()) {
						final int i_id = random.nextInt(NUM_ITEMS);
						final String item_id = "item_"+Integer.toString(i_id);
						TpccItem item = new TpccItem().TpccItemGet(item_id, transactionContext, "r");
						//TpccItem item = (TpccItem)HyFlow.getLocator().open(item_id, "w");
						if (item != null) {	
							float I_PRICE = item.content.I_PRICE;
							String I_NAME = item.content.I_NAME;
							String I_DATA = item.content.I_DATA;
						}
						final String smyid = myid+"_stock_"+ Integer.toString(random.nextInt(NUM_ITEMS));
						TpccStock stock = (TpccStock)HyFlow.getLocator().open(smyid, "w", new int [1]);
						int S_QUANTITY = stock.S_QUANTITY;
								
						String S_DATA = stock.S_DATA;
						if (S_QUANTITY - 10 > i) {
							stock.S_QUANTITY = S_QUANTITY - i;
						} else {
							stock.S_QUANTITY = S_QUANTITY - i + 91;
						}
						stock.S_YTD = stock.S_YTD + i;
						stock.S_ORDER_CNT = stock.S_ORDER_CNT + 1;
						
						String olmyid = myid+"_orderline_" + Integer.toString(random.nextInt(1000)+NUM_ORDERS_PER_D);
						TpccOrderline orderLine = new TpccOrderline(olmyid);
						orderLine.content.OL_QUANTITY = random.nextInt(1000);
						orderLine.content.OL_I_ID = i_id;
						orderLine.content.OL_SUPPLY_W_ID = w_id; 
						//orderLine.OL_AMOUNT = (int)(orderLine.OL_QUANTITY  * I_PRICE);
						orderLine.content.OL_DELIVERY_D = null;
						orderLine.content.OL_DIST_INFO = Integer.toString(d_id);
						
						i += 1;
					}
					
					return null;
				}
				@Override
				public void onAbort(Context __transactionContext__) {
					// TODO: automatic lock release on abort/commit, override only for special cases
					final TpccDistrict district = (TpccDistrict)HyFlow.getLocator().open(dmyid, "w", new int [1]);
					int o_id = district.D_NEXT_O_ID;
					district.D_NEXT_O_ID = o_id - 1;
					((NestedContext)__transactionContext__).onLockAction(
							district.getId().toString(), dmyid, true, false);
				}
				@Override
				public void onCommit(Context __transactionContext__) {
					final TpccDistrict district = (TpccDistrict)HyFlow.getLocator().open(dmyid, "w", new int [1]);
					((NestedContext)__transactionContext__).onLockAction(
							district.getId().toString(), dmyid, true, false);
				}
			}.execute(null);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public static void payment(final int count) {
		final float h_amount = (float)(random.nextInt(500000) * 0.01);
		final int w_id = random.nextInt(NUM_WAREHOUSES);
		final String myid = "warehouse_"+Integer.toString(w_id);
		final int c_id = random.nextInt(NUM_CUSTOMERS_PER_D);
		final String cmyid = myid+ "_customer_" + Integer.toString(c_id);
		final int d_id = random.nextInt(NUM_DISTRICTS);
		final String dmyid = myid+"_"+ Integer.toString(d_id);
		try {
			new Atomic<Object>(true) {
				@Override
				public Object atomically(AbstractDistinguishable self, Context transactionContext) {
					// Open Wairehouse Table
					TpccWarehouse warehouse = (TpccWarehouse)HyFlow.getLocator().open(myid, "w", new int [1]);
					warehouse.W_YTD += h_amount;
					if (((NestedContext)transactionContext).getNestingModel() == NestingModel.OPEN) {
						((NestedContext)transactionContext).onLockAction(
								warehouse.getId().toString(), myid, true, true);
						m_hasOnAbort = true;
						m_hasOnCommit = true;
					}	
										
					// In DISTRICT table
					TpccDistrict district = (TpccDistrict)HyFlow.getLocator().open(dmyid, "w", new int [1]);
					district.D_YTD += h_amount;
					if (((NestedContext)transactionContext).getNestingModel() == NestingModel.OPEN) {
						((NestedContext)transactionContext).onLockAction(
								district.getId().toString(), dmyid, true, true);
					
					}	
					//TpccCustomer customer = (TpccCustomer)HyFlow.getLocator().open(cmyid, "w");
					TpccCustomer customer = (TpccCustomer)HyFlow.getLocator().open(cmyid, "w", new int [1]);
					customer.content.C_BALANCE -= h_amount;
					customer.content.C_YTD_PAYMENT += h_amount;
					customer.content.C_PAYMENT_CNT += 1;
					if (((NestedContext)transactionContext).getNestingModel() == NestingModel.OPEN) {
						((NestedContext)transactionContext).onLockAction(
							customer.getId().toString(), cmyid, true, true);
					}			
					return null;
				}
				@Override
				public void onAbort(Context __transactionContext__) {
					// TODO: automatic lock release on abort/commit, override only for special cases
					final TpccWarehouse warehouse = (TpccWarehouse)HyFlow.getLocator().open(myid, "w", new int [1]);
					warehouse.W_YTD -= h_amount;
					((NestedContext)__transactionContext__).onLockAction(
							warehouse.getId().toString(), myid, true, false);
					final TpccDistrict district = (TpccDistrict)HyFlow.getLocator().open(dmyid, "w", new int [1]);
					district.D_YTD -= h_amount;
					((NestedContext)__transactionContext__).onLockAction(
							district.getId().toString(), dmyid, true, false);
					final TpccCustomer customer = (TpccCustomer)HyFlow.getLocator().open(cmyid, "w", new int [1]);
					customer.content.C_BALANCE += h_amount;
					customer.content.C_YTD_PAYMENT -= h_amount;
					customer.content.C_PAYMENT_CNT -= 1;
					((NestedContext)__transactionContext__).onLockAction(
							customer.getId().toString(), cmyid, true, false);
				}
				@Override
				public void onCommit(Context __transactionContext__) {
					final TpccWarehouse warehouse = (TpccWarehouse)HyFlow.getLocator().open(myid, "w", new int [1]);
					((NestedContext)__transactionContext__).onLockAction(
							warehouse.getId().toString(), myid, true, false);
					final TpccDistrict district = (TpccDistrict)HyFlow.getLocator().open(dmyid, "w", new int [1]);
					((NestedContext)__transactionContext__).onLockAction(
							district.getId().toString(), dmyid, true, false);
					final TpccCustomer customer = (TpccCustomer)HyFlow.getLocator().open(cmyid, "w", new int [1]);
					((NestedContext)__transactionContext__).onLockAction(
							customer.getId().toString(), cmyid, true, false);
				}
			}.execute(null);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
}

