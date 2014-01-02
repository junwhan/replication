package edu.vt.rt.hyflow.core.tm;

import java.util.List;
import java.util.LinkedList;
import java.util.Random;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.ContextDelegator;
import org.deuce.transaction.TransactionException;
import org.deuce.transform.Exclude;

import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.IDistinguishable;
import edu.vt.rt.hyflow.core.tm.dtl.Context;
import edu.vt.rt.hyflow.helper.Atomic;
import edu.vt.rt.hyflow.helper.StatsAggregator;
import edu.vt.rt.hyflow.util.io.Logger;

/**
 * Base for all Context classes that support nested transactions.
 * @author Alex Turcu
 */
@Exclude
public abstract class NestedContext extends AbstractContext 
{
	// Default nesting type, user-changeable
	public static NestingModel defaultNestingModel = NestingModel.FLAT;
	
	// Nesting relations between contexts
	protected NestedContext parent = null;
	protected NestedContext root = null;
	// Generic list of children not really needed
	// protected List<NestedContext> children = new LinkedList<NestedContext>();
	// Flattened transactions call-stack depth
	protected int depth = 0;
	// Nesting model when the current context was created
	protected NestingModel nestingModel = null;
	
	// Try to emulate a backoff CM
	protected int consecutiveAborts = 0;
	// Haaaaaack!!!!
	public boolean ignoreAborts = false;
	// end haaaaaack
	
	// Initialization, used to configure a nested Context 
	public void configureNesting(NestedContext parent, NestingModel nestingModel)
	{
		
		// Which txn's clock to validate against? Top-level and open/internal-open.
		if (parent == null || nestingModel == NestingModel.OPEN || nestingModel == NestingModel.INTERNAL_OPEN) {
			root = this;
		} else {
			root = parent.root;
		}
		// remember the parent and nesting model
		this.parent = parent;
		this.nestingModel = nestingModel;
		// other initialization needed
		this.ignoreAborts = false;
	}
	
	public NestingModel getNestingModel()
	{
		return nestingModel;
	}
	
	public NestedContext getRoot() 
	{
		return root;
	}
	
	// Initialization
	@Override
	public void init(int atomicBlockId)
	{
		super.init(atomicBlockId);
		Logger.debug(">> Initializing transaction.");
		// TODO: mark for deletion
	}
	
	// Query for top-level status
	public boolean isTopLevel()
	{
		return parent == null;
	}
	
	// Support for flat transactions / function calls
	public void flatCall()
	{
		Logger.debug(">> Flat call!");
		depth++;
	}
	
	public void flatCallReturn()
	{
		Logger.debug(">> Flat call/return!");
		depth--;
	}
	
	// Check for any nested flat calls
	public boolean isFlatFree()
	{
		return depth == 0;
	}
		
	// Commit action
	public boolean commit()
	{
		Logger.debug(">> Attempting commit.");
		if (depth > 0)
		{
			// Null commit model
			// This is simply a flattened transaction, do nothing at this stage
			Logger.debug(">> Null commit model."+depth);
			// will do this later: depth--;
			return true;
		}
		else if (parent == null || nestingModel == NestingModel.OPEN || nestingModel == NestingModel.INTERNAL_OPEN)
		{
			// Top-level commit model
			// This is the top-level transaction and must be committed
			// ignoring the nesting model
			Logger.debug(">> Open/top-level commit model.");
			if (nestingModel == NestingModel.OPEN && parent != null)
				Logger.debug(">> This is a (child) open-nested transaction.");
			else if (nestingModel == NestingModel.INTERNAL_OPEN)
				Logger.debug(">> This is an internal abort/commit action.");
			if (!checkCommit(true))
			{
				if (parent == null) {
					StatsAggregator.get().abortTxn();
				} else {
					StatsAggregator.get().abortSubTxn();
				}
				return false;
			}
			
			reallyCommit();
			// TODO: this is a hack, should count aborts on a transaction block basis
			// TODO: only reset on top-level commit. Would this unnecessarily slow things down? (BUG!)
			// TODO: maybe replacing ignoreAborts with checking for INTERNAL_OPEN could help?
			if ((!ignoreAborts) && (parent == null)) {
				ContextDelegator.getFactory().resetAborts();
			}
			
			return true;
		}
		else if (nestingModel == NestingModel.CLOSED)
		{
			// Merge commit model
			// For closed nested transactions
			Logger.debug("Merge commit model.");
			if (!checkCommit(false))
			{
				StatsAggregator.get().abortSubTxn();
				return false;
			}
			
			mergeIntoParent();
			StatsAggregator.get().commitSubTxn(0, ((Context)this).__get_readset_len());
			return true;
		}
		else 
		{
			// TODO: other cases? Checkpoints maybe?
			Logger.fetal(">> Commit model not yet implemented.");
			assert false : "Nesting type not implemented: "+nestingModel;
			return false;
		}
	}
	
	// Check if parent aborted
	public void checkParent() throws TransactionException
	{
		// For flat nesting, need to pass execution up to next enclosing control block
		if (depth != 0) {
			depth--;
			Logger.debug("Forcing higher level abort (FLAT passing control up!).");
			throw new TransactionException("Forcing higher level abort.");
		}
		
		// Don't check parents for INTERNAL_OPEN
		if (nestingModel == NestingModel.INTERNAL_OPEN)
			return;

		if (!ignoreAborts) {
			ContextDelegator.getFactory().logAbort();
		}
	
		// For closed nesting abort parent transactions
		// This is also used when locking an abstract lock (open nesting) fails
		if (parent != null) {
			if (parent.status == STATUS.ABORTED) {
				ContextDelegator.releaseInstance(this);
				Logger.debug("checkParent() : Forcing higher level abort.");
				
				throw new TransactionException("Forcing higher level abort.");
			}
		}
	}
	
	public void abortContextTree()
	{
		NestedContext current = this;
		// don't recurse, this should be faster
		do 
		{
			current.status = STATUS.ABORTED;
			// TODO: should we stop at commit boundaries?
			// TODO: check the code that is calling this method
			// TODO: talex 09/29/2011
			//if (current.nestingModel == NestingModel.OPEN) {
			//	break;
			//}
			current = current.parent;
		} while(current != null);
	}
	
	// Abstract methods
	protected abstract boolean checkCommit(boolean top);
	protected abstract void reallyCommit();
	protected abstract void mergeIntoParent();
	public abstract void onOpenObject(AbstractDistinguishable obj);
	public abstract void setCurrentOpenNestingAction(Atomic action);
	public void onLockAction(String objname, String lockname, boolean readlock, boolean acquire) {}
}
