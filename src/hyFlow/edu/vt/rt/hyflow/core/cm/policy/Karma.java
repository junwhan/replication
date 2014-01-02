package edu.vt.rt.hyflow.core.cm.policy;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.TransactionException;

import edu.vt.rt.hyflow.core.tm.control.undoLog.Context;
import edu.vt.rt.hyflow.util.io.Logger;

public class Karma extends AbstractContentionPolicy{

	@Override
	public void init(AbstractContext context) {
		super.init(context);
		context.contentionMetadata = 0;
	}
	
	@Override
	public void open(AbstractContext context) {
		super.open(context);
		if (context == null)
			return;
		context.contentionMetadata = 1 + ((Integer)context.contentionMetadata);
		Logger.debug("ContextManager<Karma> open(context) invoked.");
	}
	
	@Override
	public int resolve(AbstractContext context1, AbstractContext context2) {
		if(super.resolve(context1, context2)==0)
			return 0;
		
		Integer opendObjects1 = (Integer)context1.contentionMetadata;
		Integer opendObjects2 = (Integer)context2.contentionMetadata;
		
		Logger.debug("ContentionManager<Karma>; " + context1+":"+opendObjects1 + " vs " + context2+":"+opendObjects2);
		if(opendObjects1 < opendObjects2 || opendObjects1.equals(opendObjects2) && context1.getContextId() < context2.getContextId())
			throw new TransactionException();
		else{
			if(((Context)context2).rollback(false))
				return 0;
			return -1;
		}
	}
}
