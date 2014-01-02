package edu.vt.rt.hyflow.benchmark.tm.vacation;

import java.util.Iterator;

import org.deuce.Atomic;

import aleph.dir.DirectoryManager;
import aleph.dir.NotRegisteredKeyException;
import edu.vt.rt.hyflow.HyFlow;

public class _Vacation {

	@Atomic
	public static void makeReservation(String customerId, int[] objType, String[] objId){
		DirectoryManager locator = HyFlow.getLocator();
		int[] minPrice = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
		String[] minIds = new String[3];
		boolean isFound = false;
		for(int i=0; i<Benchmark.queryPerTransaction; i++)
			try {
				int price = Integer.MAX_VALUE;
				_Reservation car = (_Reservation)locator.open(objId[i], "r");
				if(car.isAvailable())
					price = car.getPrice();
				if(price < minPrice[objType[i]]){
					minPrice[objType[i]] = price; 
					minIds[objType[i]] = objId[i];
					isFound = true;
				}
				
			} catch (NotRegisteredKeyException e) {
			}

		_Customer customer = null;
		if(isFound){
			// create customer
			try {
				customer = (_Customer)locator.open(customerId);
			} catch (NotRegisteredKeyException e) {
				customer = new _Customer(customerId);
			}
		}
		for(int i=0; i<minPrice.length; i++)
			try {
				String reserveId =  "reserve-car-" + Math.random();	// generate random id
				_Reservation reservation = (_Reservation)locator.open(minIds[i]);
				if(reservation.reserve()){
					_ReservationInfo reservationInfo = new _ReservationInfo(reserveId, minIds[i], minPrice[i], i); 
					customer.addReservation(reservationInfo);
				}
			} catch (NotRegisteredKeyException e) {
			}
	}

	@Atomic
	public static void deleteCustomer(String customerId) {
		DirectoryManager locator = HyFlow.getLocator();
		try {
			_Customer customer = (_Customer)locator.open(customerId);
			for(Iterator<String> itr = customer.getReservations().iterator(); itr.hasNext(); ){
				_ReservationInfo reservationInfo = (_ReservationInfo)locator.open(itr.next());
				try {
					((_Reservation)locator.open(reservationInfo.getReservedResource())).release();
				} catch (NotRegisteredKeyException e) {
				}
				locator.delete(reservationInfo);
			}
		} catch (NotRegisteredKeyException e) {
		}
	}
	
	@Atomic
	public static void updateOffers(boolean[] opType, String[] objId, int[] price){
		DirectoryManager locator = HyFlow.getLocator();
		for(int i=0; i<Benchmark.queryPerTransaction; i++)
			if(opType[i])	// add/update
				try{
					((_Reservation)locator.open(objId[i])).setPrice(price[i]);
				}catch (NotRegisteredKeyException e) {
					new _Reservation(objId[i], price[i]);
				}
			else
				try{
					_Reservation reservation = (_Reservation)locator.open(objId[i]);
					reservation.retrieItem();
				}catch (NotRegisteredKeyException e) {
				}
	}
}
