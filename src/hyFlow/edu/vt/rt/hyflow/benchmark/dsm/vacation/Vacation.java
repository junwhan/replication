package edu.vt.rt.hyflow.benchmark.dsm.vacation;

import java.util.Iterator;

import aleph.comm.tcp.Address;
import aleph.dir.DirectoryManager;
import aleph.dir.NotRegisteredKeyException;
import aleph.dir.home.TimeoutException;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;

public class Vacation{
	
	
	public static void makeReservation(String customerId, int[] objType, String[] objId){
		int[] minPrice = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
		String[] minIds = new String[3];
		Reservation[] reservIds = new Reservation[3];
		boolean isFound = false;
		Reservation[] reservations = new Reservation[objId.length];
		Customer customer = null;
		DirectoryManager locator = HyFlow.getLocator();
		while(true){
			int i=0;
			try {
				for(; i<objId.length; i++)
					try {
						reservations[i] = (Reservation)locator.open(objId[i]);
					} catch (NotRegisteredKeyException e) {
					}
					
				try {
					customer = (Customer)locator.open(customerId);
				} catch (NotRegisteredKeyException e) {
				}
				
				break;
			}catch (TimeoutException e) {
				for(int j=0; j<i; j++){
					if(reservations[j]!=null)
						locator.release(reservations[j]);
				}
			}
		}
		try {
			
			for(int i=0; i<Benchmark.queryPerTransaction; i++){
				int price = Integer.MAX_VALUE;
				Reservation car = (Reservation)reservations[i];
				if(car==null)
					continue;
				if(car.isAvailable())
					price = car.getPrice();
				if(price < minPrice[objType[i]]){
					minPrice[objType[i]] = price; 
					minIds[objType[i]] = objId[i];
					reservIds[objType[i]] = car;
					isFound = true;
				}
			}
	
			if(isFound){
				// create customer
				if(customer==null){
					customer = new Customer(customerId);
					Logger.info(">>>>>>>>>>Customer added..." + customerId);
				}
				
				for(int i=0; i<minPrice.length; i++)
					if(minPrice[i]!=Integer.MAX_VALUE){
						String reserveId = "-reserve-" + Math.random();	// generate random id
						Reservation reservation = reservIds[i];
						if(reservation.reserve()){
							ReservationInfo reservationInfo = new ReservationInfo(reserveId, minIds[i], minPrice[i], i);
							customer.addReservation((String)reservationInfo.getId());
							Logger.info(">>>>>>>>>>Reservation done...");
						}
					}
			}

		} finally{
			for(int i=0; i<reservations.length; i++){
				Reservation reservation = (Reservation)reservations[i];
				if(reservation!=null)
					locator.release(reservation);
			}
			if(customer!=null)
				locator.release(customer);
		}
	}


	public static void deleteCustomer(String customerId){
		Customer customer = null;
		DirectoryManager locator = HyFlow.getLocator();
		while(true)
			try{
				try {
					customer = (Customer)locator.open(customerId);
				} catch (NotRegisteredKeyException e) {
					return;
				}
				
				break;
			}catch (TimeoutException e) {
			}
			
		for(Iterator<String> itr=customer.getReservations().iterator(); itr.hasNext(); ){
			String reservationInfoId = itr.next();
			
			ReservationInfo reservationInfo;
			while(true)
				try{
					reservationInfo = (ReservationInfo)locator.open(reservationInfoId);
					break;
				}catch (TimeoutException e) {
				}
				
			if(reservationInfo==null)
				continue;
			String reservationId = reservationInfo.getReservedResource();
			while(true)
				try{
					Reservation reservation = (Reservation)locator.open(reservationId);
					if(reservation!=null){
						reservation.release();
						locator.release(reservation);
					}
					break;
				}catch (TimeoutException e) {
				}
			locator.delete(reservationInfo);
		}
		
		locator.delete(customer);
		Logger.info(">>>>>>>>>>Customer deleted..." + customerId);
		try {
			customer = (Customer)locator.open(customerId);
			System.out.println("How!!!!!!!!!!!!!!");
		} catch (NotRegisteredKeyException e) {
			return;
		}
	}
	
	
	public static void updateOffers(boolean[] opType, String[] objId, int[] price){
		Reservation[] reservations = new Reservation[objId.length];
		DirectoryManager locator = HyFlow.getLocator();
		while(true){
			int i=0;
			try {
				for(; i<objId.length; i++)
					try {
						reservations[i] = (Reservation)locator.open(objId[i]);
					} catch (NotRegisteredKeyException e) {
					}
					
				break;
			}catch (TimeoutException e) {
				for(int j=0; j<i; j++){
					if(reservations[j]!=null)
						locator.release(reservations[j]);
				}
			}
		}
		try {
			for(int i=0; i<Benchmark.queryPerTransaction; i++){
				Reservation reservation = (Reservation)reservations[i];
				if(reservation==null){
					new Reservation(objId[i], price[i]);
					Logger.info(">>>>>>>>>>Add Item...");
					continue;
				}
				if(opType[i]){	// add/update
					Logger.info(">>>>>>>>>>Update Price...");
					reservation.setPrice(price[i]);
				}
				else{
					Logger.info(">>>>>>>>>>Retire Item...");
					reservation.retrieItem();
				}
			}
		} finally{
			for(int i=0; i<reservations.length; i++){
				Reservation reservation = (Reservation)reservations[i];
				if(reservation!=null)
					locator.release(reservation);
			}
		}
	}
}
