package edu.vt.rt.hyflow.core.tm.undoLog;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.Context;

import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.tm.control.ControlContext;
import edu.vt.rt.hyflow.util.io.Logger;

abstract public class AbstractLoggableObject extends AbstractDistinguishable{
	private static final int FREE = 0;
	private static final int WRITER = 0;
	private static final int READER = 0;
	
	private AtomicInteger flag = new AtomicInteger();
	private AbstractContext writer = null;
	private Map<Long, AbstractContext> readers = new ConcurrentHashMap<Long, AbstractContext>();
	
	public boolean own(AbstractContext context){
		Logger.debug(context + ": try own:" + getId() + " owned-by " + readers.size());
		if(flag.compareAndSet(FREE, WRITER))
			return true;
		return isMeOwn(context);
	}
	
	public boolean isMeOwn(AbstractContext context){
		return isMeOwn(context.getContextId());
	}

	public boolean isMeOwn(Long id){
		return writer!=null && writer.getContextId().equals(id);
	}
	
	public AbstractContext getOwner(){
		return writer;
	}
	
	public void release(AbstractContext context){
		if(flag.get()==WRITER){
			writer = null;
			flag.set(FREE);
		}
		else if(flag.get()==READER){
			readers.remove(context.getContextId());
			if(readers.size()==0)
				flag.set(FREE);
		}
		Logger.debug(getId() + ": released");
	}

	public boolean isFree(Long id) {
		return flag.get()!=WRITER || isMeOwn(id);
	}
}
