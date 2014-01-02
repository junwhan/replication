package edu.vt.rt.hyflow.core.cm.policy;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.TransactionException;

public class Backoff extends AbstractContentionPolicy{

	@Override
	public int resolve(AbstractContext context1, AbstractContext context2) {
		if(super.resolve(context1, context2)==0)
			return 0;
		
		Boolean slept = (Boolean)context1.contentionMetadata;
		if(slept==null){
			context1.contentionMetadata = true;
			int backoff = (int)(10*Math.random()+1)*10;
//			System.out.println(context1 + " sleeps " + backoff);
			return backoff;
		}
		else{
//			System.out.println(context1 + " aborted");
			throw new TransactionException();
		}
	}
}
	