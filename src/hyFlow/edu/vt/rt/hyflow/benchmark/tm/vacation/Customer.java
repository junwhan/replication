package edu.vt.rt.hyflow.benchmark.tm.vacation;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.deuce.reflection.AddressUtil;
import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.Context;
import org.deuce.transaction.ContextDelegator;
import org.deuce.transaction.TransactionException;

import com.sun.rmi.rmid.ExecOptionPermission;

import aleph.GlobalObject;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.benchmark.tm.bank.BankAccount;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.dir.IHyFlow;
import edu.vt.rt.hyflow.core.tm.NestedContext;
import edu.vt.rt.hyflow.core.tm.control.ControlContext;
import edu.vt.rt.hyflow.core.tm.undoLog.AbstractLoggableObject;
import edu.vt.rt.hyflow.helper.Atomic;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.benchmark.tm.vacation.Benchmark;

public class Customer extends AbstractLoggableObject
	implements 	IHyFlow{							// Implementation specific code for ControlFlowDirecotry

  	private List<String> reservations = new LinkedList<String>();
  	public static long reservations__ADDRESS__;
  	private String id;
    public static long id__ADDRESS__;
    private $HY$_ICustomer $HY$_proxy;
    public static long $HY$_proxy__ADDRESS__;
    private Object $HY$_id;
    public static long $HY$_id__ADDRESS__;
    public static Object __CLASS_BASE__;

	{
		try {
			reservations__ADDRESS__ = AddressUtil.getAddress(Customer.class.getDeclaredField("reservations"));
			id__ADDRESS__ = AddressUtil.getAddress(Customer.class.getDeclaredField("id"));
			$HY$_proxy__ADDRESS__ = AddressUtil.getAddress(Customer.class.getDeclaredField("$HY$_proxy"));
			$HY$_id__ADDRESS__ = AddressUtil.getAddress(Customer.class.getDeclaredField("$HY$_id"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Customer(){}	// 	required for control flow model
	
	public Customer(String id) {
		this.id = id;
		
		AbstractContext context = ContextDelegator.getTopInstance();
		if(context==null)
			HyFlow.getLocator().register(this); // publish it now	
		else
			context.newObject(this);	// add it to context publish-set 
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
	public void addReservation__orig(ReservationInfo reservation) {
		reservations.add((String)reservation.getId());
	}
	public void addReservation(final ReservationInfo reservation) {
		try {
			new Atomic<Object>(true) {
				@Override
				public Object atomically(AbstractDistinguishable self,
						Context __transactionContext__) {
					addReservation(reservation, __transactionContext__);
					return null;
				}
			}.execute(this);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			// nada
		}
	}
	public void addReservation(ReservationInfo reservation, Context __transactionContext__) {
		if($HY$_proxy!=null){
			try {
				$HY$_proxy.addReservation($HY$_id, (ControlContext) __transactionContext__, reservation);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		ContextDelegator.beforeReadAccess(this, reservations__ADDRESS__, __transactionContext__);
		List<String> list = (List<String>)ContextDelegator.onReadAccess(this, reservations, reservations__ADDRESS__, __transactionContext__);
		list.add((String)reservation.getId());
		ContextDelegator.onWriteAccess(this, list, reservations__ADDRESS__, __transactionContext__);
	}

	public List<String> getReservations__orig() {
		return reservations;
	}
	public List<String> getReservations() {
		List<String> result = null;
		try {
			result = new Atomic<List<String>>(true) {
				@Override
				public List<String> atomically(AbstractDistinguishable self,
						Context __transactionContext__) {
					return ((Customer)self).getReservations(__transactionContext__);
				}
			}.execute(this);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			// nada
		}
		return result;
	}
	public List<String> getReservations(Context __transactionContext__) {
		if($HY$_proxy!=null){
			try {
				return $HY$_proxy.getReservations($HY$_id, (ControlContext) __transactionContext__);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		ContextDelegator.beforeReadAccess(this, reservations__ADDRESS__, __transactionContext__);
		return (List<String>)ContextDelegator.onReadAccess(this, reservations, reservations__ADDRESS__, __transactionContext__);
	}

	@Override
	public void setRemote(Object id, String ownerIP, int ownerPort) {
		$HY$_id = id;
		try {
			$HY$_proxy = (($HY$_ICustomer)LocateRegistry.getRegistry(ownerIP, ownerPort).lookup(getClass().getName()));
		} catch (AccessException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			try {
				Logger.debug(Arrays.toString(LocateRegistry.getRegistry(ownerIP, ownerPort).list()));
			} catch (AccessException e1) {
				e1.printStackTrace();
			} catch (RemoteException e1) {
				e1.printStackTrace();
			}
		}
	}

}
