package edu.vt.rt.hyflow.benchmark.tm.vacation;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Arrays;

import org.deuce.reflection.AddressUtil;
import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.Context;
import org.deuce.transaction.ContextDelegator;
import org.deuce.transaction.TransactionException;

import aleph.GlobalObject;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.dir.IHyFlow;
import edu.vt.rt.hyflow.core.tm.NestedContext;
import edu.vt.rt.hyflow.core.tm.control.ControlContext;
import edu.vt.rt.hyflow.core.tm.undoLog.AbstractLoggableObject;
import edu.vt.rt.hyflow.helper.Atomic;
import edu.vt.rt.hyflow.util.io.Logger;

public class ReservationInfo extends AbstractLoggableObject
	implements 	IHyFlow{							// Implementation specific code for ControlFlowDirecotry


	private String resourceId;
  	public static long resourceId__ADDRESS__;
  	private Integer type;
  	public static long type__ADDRESS__;
  	private Integer price;
  	public static long price__ADDRESS__;
  	private String id;
    public static long id__ADDRESS__;
    private $HY$_IReservationInfo $HY$_proxy;
    public static long $HY$_proxy__ADDRESS__;
    private Object $HY$_id;
    public static long $HY$_id__ADDRESS__;
    public static Object __CLASS_BASE__;
	
	 {
		  	try{
		  		resourceId__ADDRESS__ = AddressUtil.getAddress(ReservationInfo.class.getDeclaredField("resourceId"));
		  		type__ADDRESS__ = AddressUtil.getAddress(ReservationInfo.class.getDeclaredField("type"));
		  		price__ADDRESS__ = AddressUtil.getAddress(ReservationInfo.class.getDeclaredField("price"));
		  		id__ADDRESS__ = AddressUtil.getAddress(ReservationInfo.class.getDeclaredField("id"));
		  		$HY$_proxy__ADDRESS__ = AddressUtil.getAddress(ReservationInfo.class.getDeclaredField("$HY$_proxy"));
		  		$HY$_id__ADDRESS__ = AddressUtil.getAddress(ReservationInfo.class.getDeclaredField("$HY$_id"));
		  	}catch (Exception e) {
		  		e.printStackTrace();
			}
	 }
	 
	public ReservationInfo(String id, String resourceId, int price, int type) {
		this.id = id;
		this.resourceId = resourceId;
		this.price = price;
		this.type = type;
		
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
	public int getType__orig() {
		return type;
	}
	public int getType() {
		try {
			return new Atomic<Integer>(true) {
				@Override
				public Integer atomically(AbstractDistinguishable self,
						Context __transactionContext__) {
					return ((ReservationInfo)self).getType(__transactionContext__);
				}
			}.execute(this);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			// nada
		}
		return 0;
	}
	public int getType(Context __transactionContext__) {
		if($HY$_proxy!=null){
			try {
				return $HY$_proxy.getType($HY$_id, (ControlContext) __transactionContext__);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		ContextDelegator.beforeReadAccess(this, type__ADDRESS__, __transactionContext__);
		return (Integer)ContextDelegator.onReadAccess(this, type, type__ADDRESS__, __transactionContext__);
	}

	
	public int getPrice__orig() {
		return price;
	}
	public int getPrice() {
		try {
			return new Atomic<Integer>(true) {
				@Override
				public Integer atomically(AbstractDistinguishable self,
						Context __transactionContext__) {
					return ((ReservationInfo)self).getPrice(__transactionContext__);
				}
			}.execute(this);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			// nada
		}
		return 0;
	}
	public int getPrice(Context __transactionContext__) {
		if($HY$_proxy!=null){
			try {
				return $HY$_proxy.getPrice($HY$_id, (ControlContext) __transactionContext__);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		ContextDelegator.beforeReadAccess(this, price__ADDRESS__, __transactionContext__);
		return (Integer)ContextDelegator.onReadAccess(this, price, price__ADDRESS__, __transactionContext__);
	}

	
	public String getReservedResource__orig() {
		return resourceId;
	}
	public String getReservedResource() {
		try {
			return new Atomic<String>(true) {
				@Override
				public String atomically(AbstractDistinguishable self,
						Context transactionContext) {
					return ((ReservationInfo)self).getReservedResource(transactionContext);
				}
			}.execute(this);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			// nada
		}
		return null;
	}
	public String getReservedResource(Context __transactionContext__) {
		if($HY$_proxy!=null){
			try {
				return $HY$_proxy.getReservedResource($HY$_id, (ControlContext) __transactionContext__);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		ContextDelegator.beforeReadAccess(this, resourceId__ADDRESS__, __transactionContext__);
		return (String)ContextDelegator.onReadAccess(this, resourceId, resourceId__ADDRESS__, __transactionContext__);
	}
	
	

	@Override
	public void setRemote(Object id, String ownerIP, int ownerPort) {
		$HY$_id = id;
		try {
			$HY$_proxy = (($HY$_IReservationInfo)LocateRegistry.getRegistry(ownerIP, ownerPort).lookup(getClass().getName()));
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
