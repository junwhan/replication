package edu.vt.rt.hyflow.core.cm.policy;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.TransactionException;

import edu.vt.rt.hyflow.core.tm.control.ControlContext;
import edu.vt.rt.hyflow.core.tm.control.undoLog.Context;

public class Priority extends AbstractContentionPolicy{

	@Override
	public void init(AbstractContext context) {
		super.init(context);
	}
	
	@Override
	public void open(AbstractContext context) {
		super.open(context);
	}
	
	@Override
	public int resolve(AbstractContext context1, AbstractContext context2) {
		if(super.resolve(context1, context2)==0)
			return 0;
		
		if(context1.getContextId() > context2.getContextId())
			throw new TransactionException();
		else{
			if(((Context)context2).rollback(false))
				return 0;
			return -1;
		}
	}
}
