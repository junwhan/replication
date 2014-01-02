package edu.vt.rt.hyflow.helper;

import org.deuce.reflection.AddressUtil;
import org.deuce.transaction.Context;
import org.deuce.transaction.ContextDelegator;
import org.deuce.transaction.TransactionException;

import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.tm.NestedContext;
import edu.vt.rt.hyflow.util.io.Logger;

// DTL2 should make this work as expected
public class AbstractLock extends AbstractDistinguishable {
	private Boolean locked = false;
	private Object id = null;
	
	public static long locked__ADDRESS__;
    public static Object __CLASS_BASE__;
    public static AbstractLock watch = null;
    
    
    {
	  	try{
	  		locked__ADDRESS__ = AddressUtil.getAddress(AbstractLock.class.getDeclaredField("locked"));
	  	}catch (Exception e) {
	  		e.printStackTrace(Logger.levelStream[Logger.DEBUG]);
		}
  	}
    
    public AbstractLock(String id) {
    	this.id = id;
    }
    
	public void lock(Context __transactionContext__) {
		Logger.debug("Locking abstract "+id);
		ContextDelegator.beforeReadAccess(this, locked__ADDRESS__, __transactionContext__);
		Boolean temp = (Boolean)ContextDelegator.onReadAccess(this, this.locked, locked__ADDRESS__, __transactionContext__);
		
		if (temp) {
			Logger.debug("Already locked, rollback back to top-level transaction.");
			((NestedContext)__transactionContext__).abortContextTree();
			throw new TransactionException("Already locked, full abort.");
		}
		//locked = true;
		ContextDelegator.onWriteAccess(this, new Boolean(true), locked__ADDRESS__, __transactionContext__);
		
		///////////////////////////
		AbstractLock.watch = this;
	}
	public void unlock(Context __transactionContext__) {
		Logger.debug("Un-Locking abstract "+id);
		ContextDelegator.onWriteAccess(this, new Boolean(false), locked__ADDRESS__, __transactionContext__);
	}
	@Override
	public Object getId() {
		// TODO Auto-generated method stub
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
