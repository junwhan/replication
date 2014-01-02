package edu.vt.rt.hyflow.core.cm.policy;

import org.deuce.transaction.AbstractContext;

import edu.vt.rt.hyflow.core.tm.control.undoLog.Context;

public class Aggressive extends AbstractContentionPolicy{

	@Override
	public int resolve(AbstractContext context1, AbstractContext context2) {
		if(super.resolve(context1, context2)==0)
			return 0;
		
		if(((Context)context2).rollback(false))
			return 0;
		return -1;
	}

}
