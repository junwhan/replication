package edu.vt.rt.hyflow.core.cm.policy;

import java.util.Date;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.TransactionException;

public class Timestamp extends AbstractContentionPolicy{

	@Override
	public void init(AbstractContext context) {
		super.init(context);
		context.contentionMetadata = new Date().getTime();
	}
	
	@Override
	public int resolve(AbstractContext context1, AbstractContext context2) {
		if(super.resolve(context1, context2)==0)
			return 0;
		
		Long startTime1 = (Long)context1.contentionMetadata;
		Long startTime2 = (Long)context2.contentionMetadata;
		
		if(startTime1==null || startTime2!=null && startTime1 > startTime2)
			throw new TransactionException();
		else{
			context2.rollback();
			return 0;
		}
	}
}
