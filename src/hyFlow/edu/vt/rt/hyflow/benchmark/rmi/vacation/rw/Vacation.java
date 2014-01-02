package edu.vt.rt.hyflow.benchmark.rmi.vacation.rw;

import java.rmi.AccessException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import aleph.comm.tcp.Address;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;

public class Vacation extends UnicastRemoteObject
				implements IVacation{
	
	public Vacation() throws RemoteException {
		try {
			UnicastRemoteObject.unexportObject(this, true);
		} catch (Exception e) {
			Logger.error("RMI unexporting");
		}
		Remote stub = UnicastRemoteObject.exportObject(this, 0);

		// Bind the remote object's stub in the registry
		Registry registry = LocateRegistry.getRegistry(Network.getInstance().getPort());
		registry.rebind("vacation", stub);
	}
	

	public ICustomer createCustomer(String customerId) throws RemoteException{
		return new Customer(customerId);
	}
		
	public void createReservation(String id, int price) throws RemoteException{
		new Reservation(id, price);
	}
	
	private static Object[] open(String... accountNums){
		Object[] obj = new Object[accountNums.length];
		Address[] server = new Address[accountNums.length];
		for(int i=0; i<accountNums.length; i++){
			String accNum = accountNums[i];
			server[i] = (Address) Network.getAddress(Benchmark.getServerId(accNum));
			try {
				obj[i]=LocateRegistry.getRegistry(server[i].inetAddress.getHostAddress(), server[i].port).lookup(accNum);
			} catch (NotBoundException e) {
			} catch (AccessException e) {
				e.printStackTrace();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		return new Object[] {obj, server};
	}
	
	public static void makeReservation(String customerId, int[] objType, String[] objId) throws RemoteException{
		int[] minPrice = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
		String[] minIds = new String[3];
		IReservation[] reservIds = new IReservation[3];
		boolean isFound = false;
		Object[] reservations = null;
		Address[] reservationsServers;
		ICustomer customer;
		Address customerServer;
		
		while(true){
			int i=0;
			try {
				Object[] data = open(objId);
				reservations = (Object[])data[0]; 
				reservationsServers = (Address[])data[1];
				
				data = open(customerId);
				customer = (data==null ? null : (ICustomer)((Object[])data[0])[0]);
				customerServer = (data==null ? null : ((Address[])data[1])[0]);
				
				for(; i<reservations.length; i++){
					IReservation reservation = (IReservation)reservations[i];
					if(reservation!=null)
						reservation.rLock();
				}
				
				if(customer!=null)
					customer.wLock();
				
				break;
			}catch (NoSuchObjectException e) {
				for(int j=0; j<i; j++){
					IReservation reservation = (IReservation)reservations[j];
					if(reservation!=null)
						reservation.rUnlock();
				}
				return;
			}catch (InterruptedException e) {
				for(int j=0; j<i; j++){
					IReservation reservation = (IReservation)reservations[j];
					if(reservation!=null)
						reservation.rUnlock();
				}
			}
		}
		try {
			
			for(int i=0; i<Benchmark.queryPerTransaction; i++){
				int price = Integer.MAX_VALUE;
				IReservation car = (IReservation)reservations[i];
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
				if(customer==null)
					try {
						Address address = (Address) Network.getAddress(Benchmark.getServerId(customerId));
						IVacation vacation = (IVacation)LocateRegistry.getRegistry(address.inetAddress.getHostAddress(), address.port).lookup("vacation");
						customer = vacation.createCustomer(customerId);
					} catch (RemoteException e1) {
						e1.printStackTrace();
					} catch (NotBoundException e) {
						e.printStackTrace();
					}
				
				for(int i=0; i<minPrice.length; i++)
					if(minPrice[i]!=Integer.MAX_VALUE){
						String reserveId =  Network.getInstance().getID() + "-reserve-" + Math.random();	// generate random id
						IReservation reservation = reservIds[i];
						if(reservation.reserve()){	//TODO upgrade lock to W
							try {
								ReservationInfo reservationInfo = new ReservationInfo(reserveId, minIds[i], minPrice[i], i);
								customer.addReservation((String)reservationInfo.getId());
								Logger.info(">>>>>>>>>>Reservation done...");
							} catch (RemoteException e) {
								e.printStackTrace();
							} 
						}
					}
			}

		} finally{
			for(int i=0; i<reservations.length; i++){
				IReservation reservation = (IReservation)reservations[i];
				if(reservation!=null)
					reservation.rUnlock();
			}
			if(customer!=null)
				customer.wUnlock();
		}
	}


	public static void deleteCustomer(String customerId) throws RemoteException {
		ICustomer customer = null;
		Object[] data = null;
		while(true)
			try{
				data = open(customerId);
				customer = (data==null ? null : (ICustomer)((Object[])data[0])[0]);
				Address customerServer = (data==null ? null : ((Address[])data[1])[0]);
				
				if(customer==null)
					return;
				
				customer.wLock();
				break;
			}catch (NoSuchObjectException e) {
				return;
			}catch (InterruptedException e) {
			}
			
		data = open(customer.getReservations().toArray(new String[0]));
		Object[] reservationsInfo = (Object[])data[0]; 
		Address[] reservationsInfoServers = (Address[])data[1];

		for(int i=0; i<reservationsInfo.length; i++){
			IReservationInfo reservationInfo = (IReservationInfo)reservationsInfo[i];
			if(reservationInfo==null)
				continue;
			String reservationId = reservationInfo.getReservedResource();
			while(true)
				try{
					data = open(reservationId);
					IReservation reservation = (data==null ? null : (IReservation)((Object[])data[0])[0]);
					Address reservationServer = (data==null ? null : ((Address[])data[1])[0]);
					if(reservation!=null){
						reservation.wLock();
						reservation.release();
						reservation.wUnlock();
					}
					break;
				}catch (InterruptedException e) {
				}
			reservationInfo.destroy();
		}
		
		customer.destroy();
		Logger.info(">>>>>>>>>>Customer deleted...");
	}
	
	
	public static void updateOffers(boolean[] opType, String[] objId, int[] price) throws RemoteException{
		Object[] reservations = null;
		Address[] reservationsServers;
		while(true){
			int i=0;
			try {
				Object[] data = open(objId);
				reservations = (Object[])data[0]; 
				reservationsServers = (Address[])data[1];
				
				for(; i<reservations.length; i++){
					IReservation reservation = (IReservation)reservations[i];
					if(reservation!=null)
						reservation.wLock();
				}
				
				break;
			}catch (InterruptedException e) {
				for(int j=0; j<i; j++){
					IReservation reservation = (IReservation)reservations[j];
					if(reservation!=null)
						reservation.wUnlock();
				}
			}
		}
		try {
			for(int i=0; i<Benchmark.queryPerTransaction; i++){
				IReservation reservation = (IReservation)reservations[i];
				if(reservation==null){
					try {
						Address address = (Address) Network.getAddress(Benchmark.getServerId(objId[i]));
						IVacation vacation = (IVacation)LocateRegistry.getRegistry(address.inetAddress.getHostAddress(), address.port).lookup("vacation");
						vacation.createReservation(objId[i], price[i]);
						Logger.info(">>>>>>>>>>Add Item...");
					} catch (NotBoundException e) {
						e.printStackTrace();
					}
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
				IReservation reservation = (IReservation)reservations[i];
				if(reservation!=null)
					reservation.wUnlock();
			}
		}
	}
}
