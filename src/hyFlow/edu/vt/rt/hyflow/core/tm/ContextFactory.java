package edu.vt.rt.hyflow.core.tm;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;


import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.Context;
import org.deuce.transaction.ContextDelegator;
import org.deuce.transaction.TransactionException;
import org.deuce.transform.Exclude;

import edu.vt.rt.hyflow.util.io.Logger;

/**
 * Class that creates, holds and retrieves contexts, 
 * especially usefull for nested transactions.
 * Objects will exist one per thread.
 * @author Alex Turcu
 */
@Exclude
public class ContextFactory
{
	private static Class<?> contextClass = null;
	protected static boolean nesting = false;
	
	protected NestingModel model = NestingModel.FLAT;
	
	protected List<AbstractContext> stack = new ArrayList<AbstractContext>();
	protected int stackIndex = 0;
	
	private int recentAborts = 0;
	
	public ContextFactory()
	{
		// might have used the static {} initializer, but wanted to make sure
		// this code doesn't get executed too early
		synchronized (ContextFactory.class)
		{
			if (ContextFactory.contextClass == null)
			{
				// TODO: should we turn this property back to hyflow then?
				final String className = System.getProperty( "org.deuce.transaction.contextClass");
				if( className != null)
				{
					try {
						ContextFactory.contextClass = Class.forName(className);
						// TODO: check if this works:
						ContextFactory.nesting = NestedContext.class.isAssignableFrom(ContextFactory.contextClass);
						return;
					} catch (ClassNotFoundException e) {
						e.printStackTrace(); // TODO add logger
					}
				}
				ContextFactory.contextClass = org.deuce.transaction.lsa.Context.class;
			}
		}
	}
	
	public void logContextDetails()
	{
		Logger.debug("logContextDetails: " + this.stackIndex + " out of "+this.stack.size());
	}
	
	/**
	 * Sets current nesting model for this thread.
	 */
	public void setNestingModel(NestingModel model)
	{
		// TODO: what about null?
		this.model = model;
	}
	
	/**
	 * Creates or returns an instance of the appropriate context class.
	 * Includes logic for handling nesting.
	 */
	public AbstractContext getInstance()
	{
		Logger.debug(">> Retrieving Context instance.");
		logContextDetails();
		/*try {
			throw new Exception();
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}*/
		if (! ContextFactory.nesting)
		{
			// Selected context class doesn't support nesting.
			// Flatten everything, always return stack[0]
			// TODO: if nesting is not supported, we may need to configure the 
			// code generation accordingly (ie. put back the context variable 
			// as the last parameter in the method calls)
			stackIndex = 0;
			return getNextFromStack();
		}
		else
		{
			// Nesting is supported.
			if (stackIndex == 0)
			{
				// This is the root transaction, give out a fresh context
				final NestedContext res = (NestedContext)getNextFromStack();
				res.configureNesting(null, model);
				return res;
			}
			else if (model == NestingModel.FLAT) // TODO: flat or null?
			{
				// For flat nesting, return context at top of stack
				// and inform context of the increase in call depth
				final NestedContext res = (NestedContext)stack.get(stackIndex-1);
				res.flatCall();
				return res;
			}
			else if (model == NestingModel.OPEN || model == NestingModel.INTERNAL_OPEN) 
			{
				// Fresh context
				NestedContext parent = null;
				//if (model == NestingModel.OPEN) //talex: let's try and give a parent to InternalOpen too
					parent = (NestedContext)stack.get(stackIndex-1);
				Logger.debug("Giving out context to O/N txn: old stackIndex: "+stackIndex+" size:"+stack.size());
				final NestedContext res = (NestedContext)getNextFromStack();
				Logger.debug("Giving out context to O/N txn: new stackIndex: "+stackIndex+" size:"+stack.size());
				res.configureNesting(parent, model);
				return res;
			}
			else // closed
			{
				// Need to check the nesting model of the parent context
				final NestedContext parent = (NestedContext)stack.get(stackIndex-1);
				final NestingModel pnm = parent.getNestingModel();
				// TODO: about time open could do that too ;)
				if (pnm == NestingModel.CLOSED || pnm == NestingModel.FLAT)
				{
					// For closed and flat parent contexts, return fresh context
					// for children transaction
					final NestedContext res = (NestedContext)getNextFromStack();
					res.configureNesting(parent, model);
					return res;
				}
				else if (pnm == NestingModel.OPEN || pnm == NestingModel.INTERNAL_OPEN)
				{
					// When parent is open, flatten children transactions into it
					// TODO: implement open in open!
					parent.flatCall();
					return parent;
				}
				else
				{
					assert false : "Unknown nesting model?";
					return null;
				}
			}	
		}
	}
	
	public AbstractContext getTopInstance() 
	{
		logContextDetails();
		if (stackIndex == 0)
		{
			return null;
		}
		return stack.get(stackIndex-1);
	}
	
	/**
	 * Releases a context instance, after the current transaction completed.
	 */
	public void releaseInstance(Context context)
	{
		Logger.debug("ContextFactory.releaseInstance()");
		// TODO: do we need to actually do anything to that context instance?
		if (stack.get(stackIndex-1) != context)
			Logger.debug("ContextFactory.releaseInstance: Unexpected context instance given.");
		if (ContextFactory.nesting)
		{
			final NestedContext nctx = (NestedContext) context;
			if (nctx.isFlatFree())
			{
				stackIndex--;
			}
			else
			{
				nctx.flatCallReturn();
			}
		}
	}
	
	private AbstractContext getNextFromStack()
	{
		// If contexts are available on stack, get one
		if (stack.size() > stackIndex)
		{
			return stack.get(stackIndex++);
		}
		
		Logger.debug("ContextFactory.getNextFromStack creating new instance!");
		// Otherwise, try to create a new context, put it to stack, and return it
		try {
			AbstractContext res = (AbstractContext)ContextFactory.contextClass.newInstance();
			stack.add(res);
			stackIndex++;
			return res;
		} catch (Exception e) {
			throw new TransactionException(e);
		}
	}
	
	public void logAbort() {
		Logger.debug("Abort count: logAbort()");
		recentAborts++;
	}
	
	public void resetAborts() {
		Logger.debug("Abort count: resetAborts()");
		recentAborts = 0;
	}
	
	public void delayNext() {
		// TODO: How does the ContentionManager fit here? Do we need some
		// architecture changes.
		Logger.debug("Entering ContextFactory.delayNext()");
		if (ContextDelegator.getFactory().recentAborts == 0) {
			Logger.debug("No recent aborts.");
			return;
		}
		try {
			int val = 5+(int)(Math.random()*5*ContextDelegator.getFactory().recentAborts);
			Logger.debug("Will sleep "+val+"ms, recentAborts="+ContextDelegator.getFactory().recentAborts);
			Thread.sleep(val);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (ContextDelegator.getFactory().recentAborts > 10) {
			final AbstractContext ctx = getTopInstance();
			if (ctx instanceof NestedContext) {
				Logger.debug("Too many recentAborts. Aborting context tree.");
				((NestedContext) ctx).abortContextTree();
				((NestedContext) ctx).checkParent();
				return;
			}
		}
	}
}
