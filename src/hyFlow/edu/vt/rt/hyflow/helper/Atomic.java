package edu.vt.rt.hyflow.helper;

import org.deuce.transaction.Context;
import org.deuce.transaction.ContextDelegator;
import org.deuce.transaction.TransactionException;

import edu.vt.rt.hyflow.benchmark.tm.vacation.Benchmark;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.tm.ContextFactory;
import edu.vt.rt.hyflow.core.tm.NestedContext;
import edu.vt.rt.hyflow.core.tm.NestingModel;
import edu.vt.rt.hyflow.core.tm.control.ControlContext;
import edu.vt.rt.hyflow.util.io.Logger;

public abstract class Atomic<T> 
{
	// Set default nesting model here:
	private NestingModel nestingModel = NestingModel.FLAT;
	protected boolean m_hasOnCommit = false;
	protected boolean m_hasOnAbort = false;
	
	public Atomic() {
		// default (empty) constructor
		// use flat nesting
	}
	
	public Atomic(boolean configurable) {
		if (configurable) {
			this.nestingModel = NestingModel.fromString(
				System.getProperty("defaultNestingModel", "FLAT"));
		}
	}
	
	public Atomic(NestingModel nestingModel) {
		this.nestingModel = nestingModel;
	}
	
	public NestingModel getNestingModel() {
		return nestingModel;
	}
	
	// override this
	public abstract T atomically(AbstractDistinguishable self, Context __transactionContext__);
	
	public T execute(AbstractDistinguishable self) throws Throwable 
	{
		Logger.debug("BEGIN ATOMIC BLOCK");
		Throwable throwable = null;
		ContextDelegator.setNestingModel(nestingModel);
		Context context = ContextDelegator.getInstance();
		T ret = null;
		boolean commit = true;
		//long result = 0;
		for (int i = 0; i < 0x7fffffff; i++) {
			context.init(3);
			((NestedContext)context).setCurrentOpenNestingAction(this);
			try {
				ret = atomically(self, context);
			} catch (TransactionException ex) {
				Logger.debug("TransactionException #1");
				ex.printStackTrace(Logger.levelStream[Logger.DEBUG]);
				commit = false;
			} catch (Throwable ex) {
				throwable = ex;
			}
			if (commit) {
				if (context.commit()) {
					ContextDelegator.releaseInstance(context);
					if (throwable != null) {
						Logger.debug("EXCEPTION IN ATOMIC BLOCK");
						throwable.printStackTrace(Logger.levelStream[Logger.DEBUG]);
						throw throwable;
					} else {
						Logger.debug("END ATOMIC BLOCK");
						return ret;
					}
				} else {
					Logger.debug("Commit failed :<");
					context.rollback();
					// TODO: we don't want casts here
					((edu.vt.rt.hyflow.core.tm.dtl.Context)context).doOnAbort();
					((NestedContext)context).checkParent();
				}
			} else {
				if(context instanceof ControlContext)
					ControlContext.abort(((ControlContext)context).getContextId());
				else 
					// rollback does checkParent!
					context.rollback();
				commit = true;
			}
			// Ugly hack, contention manager should handle this instead :(
			// If last was commit, no sleep
			// Otherwise, some sleep, depending on the number of recent aborts
			ContextDelegator.getFactory().delayNext();
			if (((NestedContext)context).isTopLevel()) {
				StatsAggregator.get().endTxn();
			}
		}
		ContextDelegator.releaseInstance(context);
		Logger.debug("END ATOMIC BLOCK (FAILED)");
		throw new TransactionException("Failed to commit the transaction in the defined retries.");
	}
	
	public void onCommit(Context __transactionContext__) {
		
	}
	
	public void onAbort(Context __transactionContext__) {
		
	}
	
	public boolean hasOnCommit() {
		return m_hasOnCommit;
	}
	
	public boolean hasOnAbort() {
		return m_hasOnAbort;
	}
}
