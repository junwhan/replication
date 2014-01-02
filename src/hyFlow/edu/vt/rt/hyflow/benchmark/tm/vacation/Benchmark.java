package edu.vt.rt.hyflow.benchmark.tm.vacation;

import java.util.HashSet;
import java.util.Set;

import org.deuce.Atomic;

import edu.vt.rt.hyflow.core.tm.NestingModel;
import edu.vt.rt.hyflow.util.network.Network;


public class Benchmark extends edu.vt.rt.hyflow.benchmark.rmi.Benchmark{
	
	static final int ACTION_MAKE_RESERVATION = 0;
	static final int ACTION_DELETE_CUSTOMER = 1;
	static final int ACTION_UPDATE_TABLES = 2;
	
	static final int RESERVATION_CAR = 0;
	static final int RESERVATION_FLIGHT = 1;
	static final int RESERVATION_ROOM = 2;
	
	static int queryPerTransaction = 10;
	static int queryRange = 10;
	public static NestingModel myNestingModel = NestingModel.CLOSED;
	
	
	@Override
	protected void createLocalObjects() {
		queryRange = localObjectsCount;
	}

	@Override
	protected int getOperandsCount() {	return 0; }

	@Override
	protected Object randomId() { return null; }
	
	private void process(int action) throws Throwable{
		int nid = 0; 
		switch(action){
			case ACTION_MAKE_RESERVATION:{
				int[] objType = new int[queryPerTransaction];
				Set<String> objSet = new HashSet<String>();
				while(objSet.size()<queryPerTransaction){
					String id = "";
					switch((int)(Math.random()*3)){
						case Benchmark.RESERVATION_CAR: id = nid + "-car-"; break;
						case Benchmark.RESERVATION_FLIGHT: id = nid + "-flight-"; break;
						case Benchmark.RESERVATION_ROOM: id = nid + "-room-"; break;
					}
					objSet.add(id + (int)(Math.random()*queryRange));
				}
				String[] objId = objSet.toArray(new String[queryPerTransaction]);
				for(int i=0; i<objType.length; i++){
					if(objId[i].contains("-car-"))
						objType[i] = Benchmark.RESERVATION_CAR;
					if(objId[i].contains("-flight-"))
						objType[i] = Benchmark.RESERVATION_FLIGHT;
					if(objId[i].contains("-room-"))
						objType[i] = Benchmark.RESERVATION_ROOM;
				}
				String customerId = nid + "-" + (int)(Math.random()*queryRange);
				Vacation.makeReservation(customerId, objType, objId);
			}
			break;
			case ACTION_DELETE_CUSTOMER:{
				String customerId = nid + "-" + (int)(Math.random()*queryRange);
				Vacation.deleteCustomer(customerId);
			}
			break;
			case ACTION_UPDATE_TABLES:{
				boolean[] opType = new boolean[queryPerTransaction];
				int[] prices = new int[queryPerTransaction];
				for(int i=0; i<queryPerTransaction; i++){
					opType[i] = (Math.random()>0.5);
					prices[i] = (int)(Math.random()*50)+50;
				}
				Set<String> objSet = new HashSet<String>();
				while(objSet.size()<queryPerTransaction){
					String id = "";
					switch((int)(Math.random()*3)){
						case Benchmark.RESERVATION_CAR: id = nid + "-car-"; break;
						case Benchmark.RESERVATION_FLIGHT: id = nid + "-flight-"; break;
						case Benchmark.RESERVATION_ROOM: id = nid + "-room-"; break;
					}
					objSet.add(id + (int)(Math.random()*queryRange));
				}
				String[] objId = objSet.toArray(new String[queryPerTransaction]);
				Vacation.updateOffers(opType, objId, prices);
			}
			break;
		}
	}
	
	@Override
	protected void readOperation(Object... ids) {
		try {
			process(ACTION_MAKE_RESERVATION);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void writeOperation(Object... ids) {
		try {
			process(Math.random()>0.5 ? ACTION_DELETE_CUSTOMER : ACTION_UPDATE_TABLES);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	protected String getLabel() {
		return "Vacation-TM";
	}


	@Override
	@Atomic
	protected void checkSanity() {
		if(Network.getInstance().getID()!=0)
			 return;

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//TODO
	}

}

