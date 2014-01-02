package edu.vt.rt.hyflow.benchmark.rmi.vacation.rw;

import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.HashSet;
import java.util.Set;

import org.deuce.Atomic;

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
	
	public Benchmark() {
		super();
		if (System.getSecurityManager() == null)
			System.setSecurityManager ( new RMISecurityManager() );
		try {
			LocateRegistry.createRegistry(Network.getInstance().getPort());
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		try {
			new Vacation();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void createLocalObjects() {
		queryRange = localObjectsCount;
	}

	@Override
	protected int getOperandsCount() {	return 0; }

	@Override
	protected Object randomId() { return null; }
	
	private void process(int action) throws Throwable{
		int nodes = Network.getInstance().nodesCount();
		switch(action){
			case ACTION_MAKE_RESERVATION:{
				int[] objType = new int[queryPerTransaction];
				Set<String> objSet = new HashSet<String>();
				while(objSet.size()<queryPerTransaction){
					String oid = "";
					switch((int)(Math.random()*3)){
						case Benchmark.RESERVATION_CAR: oid = "-car-"; break;
						case Benchmark.RESERVATION_FLIGHT: oid = "-flight-"; break;
						case Benchmark.RESERVATION_ROOM: oid = "-room-"; break;
					}
					int id = (int)(Math.random()*queryRange);
					objSet.add(id%nodes + oid + id);
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
				int id = (int)(Math.random()*queryRange);
				String customerId = id%nodes + "-" + id;
				Vacation.makeReservation(customerId, objType, objId);
			}
			break;
			case ACTION_DELETE_CUSTOMER:{
				int id = (int)(Math.random()*queryRange);
				String customerId = id%nodes + "-" + id;
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
					String oid = "";
					switch((int)(Math.random()*3)){
						case Benchmark.RESERVATION_CAR: oid = "-car-"; break;
						case Benchmark.RESERVATION_FLIGHT: oid = "-flight-"; break;
						case Benchmark.RESERVATION_ROOM: oid = "-room-"; break;
					}
					int id = (int)(Math.random()*queryRange);
					objSet.add(id%nodes + oid + id);
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
		return "Vacation-RMI";
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

	public static String getServerId(String accountNum){
		return accountNum.split("-")[0];
	}
}

