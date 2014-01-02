package edu.vt.rt.hyflow.core.cm.policy;

import java.util.Iterator;
import java.util.List;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.AbstractContext.STATUS;
import org.deuce.transaction.TransactionException;

import edu.vt.rt.hyflow.util.io.Logger;

public abstract class AbstractContentionPolicy {
	/**
	 * 
	 * @param context1
	 * @param context2
	 * @return	0	Contention solved and current context can continue safely
	 * 			n	Wait n milliseconds and retry
	 * 			-1	retry again
	 * @throws	TransactionException	Contention solved by aborting current context
	 */
	
	public static int conflicts = 0;
	public static int dist_conflicts = 0;
	
	public int resolve(AbstractContext context1, AbstractContext context2){
		Logger.debug(context1 + " vs. " + context2);
		if(!context1.local || context2!=null && !context2.local)
			dist_conflicts++;
		else
			conflicts++;

		if(context1.status.equals(STATUS.ABORTED) || context2!=null && context2.status.equals(STATUS.BUSY))
			throw new TransactionException();
		if(context2==null || context2.status.equals(STATUS.ABORTED) || context2.equals(context1))
			return 0;
		return -1;
	}

	public void init(AbstractContext context){
		context.contentionMetadata = null;
	}
	
	public void open(AbstractContext context){}
	
	public int resolve(AbstractContext context1, List<AbstractContext> context2){
		while(true){
			try {
				Iterator<AbstractContext> itr=context2.iterator();
				int result = 0;
				while(itr.hasNext()){
					result=resolve(context1, itr.next());
					if(result>0)
						return result;
				}
				return result;
			} catch (Exception e) {
				throw new TransactionException();
			}
		}
	}

	public void complete(AbstractContext abstractContext) {
	}
}
