package edu.vt.rt.hyflow.core.cm.policy;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.TransactionException;

import edu.vt.rt.hyflow.core.tm.control.undoLog.Context;

public class Polite extends AbstractContentionPolicy{

	private static final Integer MAX_RETRIES = 4;

	@Override
	public int resolve(AbstractContext context1, AbstractContext context2) {
		if(super.resolve(context1, context2)==0)
			return 0;
		
		if(context1.getContextId() < context2.getContextId()){
			if(((Context)context2).rollback(false))
				return 0;
			return -1;
		}
		
		Integer retries = (Integer)context1.contentionMetadata;
		if(retries==null)
			retries = 0;
		else
			retries++;
		context1.contentionMetadata = retries;
		if(retries>MAX_RETRIES){
			throw new TransactionException();
		}
		return (int)(Math.random()*((int)Math.pow(2, retries) * 10));
	}
}
