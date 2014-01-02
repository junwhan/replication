package edu.vt.rt.hyflow.benchmark.tm.experiment;

import java.util.List;

import org.deuce.transaction.Context;
import org.deuce.transaction.TransactionException;

import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.tm.NestedContext;
import edu.vt.rt.hyflow.core.tm.NestingModel;
import edu.vt.rt.hyflow.helper.Atomic;
import edu.vt.rt.hyflow.util.io.Logger;

class ExpAtomic extends Atomic<Object> 
{
	//Constants
	public static final int BEFORE_OBJ_COUNT = Integer.getInteger("EXP_BEFORE_OBJ_COUNT", 1);
	public static final int STAGE_OBJ_COUNT = Integer.getInteger("EXP_STAGE_OBJ_COUNT", 0);
	public static final int AFTER_OBJ_COUNT = Integer.getInteger("EXP_AFTER_OBJ_COUNT", 0);
	
	public static final int OPS_PER_BUCKET = Integer.getInteger("EXP_OPS_PER_BUCKET", 1);
	
	public static final int OPS_BEFORE = Integer.getInteger("EXP_OPS_BEFORE", 1);
	public static final int OP_GROUPS_STAGE = Integer.getInteger("EXP_OP_GROUPS_STAGE", 0);
	public static final int OPS_AFTER = Integer.getInteger("EXP_OPS_AFTER", 0);
	
	public static final int PERCENT_READ_BEFORE = Integer.getInteger("EXP_PERCENT_READ_BEFORE", 0);
	public static final int PERCENT_READ_STAGE = Integer.getInteger("EXP_PERCENT_READ_STAGE", 50);
	public static final int PERCENT_READ_AFTER = Integer.getInteger("EXP_PERCENT_READ_AFTER", 50);
	
	{
		Logger.debug("before obj count " +  BEFORE_OBJ_COUNT);
		Logger.debug("stage obj count " +  STAGE_OBJ_COUNT);
		Logger.debug("after obj count " +  AFTER_OBJ_COUNT);
		
		Logger.debug("ops per bucket" +  OPS_PER_BUCKET);
		
		Logger.debug("ops before " +  OPS_BEFORE);
		Logger.debug("op groups stage" +  OP_GROUPS_STAGE);
		Logger.debug("ops after" +  OPS_AFTER);
		
		Logger.debug("% r before" +  PERCENT_READ_BEFORE);
		Logger.debug("% r stage" +  PERCENT_READ_STAGE);
		Logger.debug("% r after" +  PERCENT_READ_AFTER);
	}
	
	// Ids before
	List<Integer> ids_before;
	List<Integer> ids_stage;
	List<Integer> ids_after;
	
	// Objects
	ExpBucket b_before[] = new ExpBucket[OPS_BEFORE];
	ExpBucket b_stage[] = new ExpBucket[OP_GROUPS_STAGE];
	ExpBucket b_after[] = new ExpBucket[OPS_AFTER];
	
	boolean readBefore, readStage, readAfter;
	
	public ExpAtomic()
	{
		readBefore = (100*Math.random()) < PERCENT_READ_BEFORE;
		readStage = (100*Math.random()) < PERCENT_READ_STAGE;
		readAfter = (100*Math.random()) < PERCENT_READ_AFTER;
		
		ids_before = ExpBucket.makeIdList(OPS_BEFORE, BEFORE_OBJ_COUNT);
		ids_stage = ExpBucket.makeIdList(OPS_PER_BUCKET*OP_GROUPS_STAGE, STAGE_OBJ_COUNT);
		ids_after = ExpBucket.makeIdList(OPS_AFTER, AFTER_OBJ_COUNT);
	}
	
	@Override
	public Object atomically(AbstractDistinguishable self, Context tx) 
	{
		NestedContext ntx = (NestedContext)tx;
		
		//Before
		for (int i=0; i<OPS_BEFORE; i++) 
		{
			b_before[i] = new ExpBucket("before", 1, BEFORE_OBJ_COUNT, ids_before.subList(i, i+1));
			if (readBefore)
				b_before[i].get(ntx);
			else
				b_before[i].inc(ntx);
		}
		
		//Stage
		try {
			new Atomic<Object>(true) {
				public Object atomically(AbstractDistinguishable self, Context tx) 
				{
					NestedContext ntx = (NestedContext)tx;
					for (int i=0; i<OP_GROUPS_STAGE; i++) {
						b_stage[i] = new ExpBucket("stage", OPS_PER_BUCKET, STAGE_OBJ_COUNT,
								ids_stage.subList(i*OPS_PER_BUCKET, (i+1)*OPS_PER_BUCKET));
						b_stage[i].lock(ntx, readStage);
						if (readStage)
							b_stage[i].get(ntx);
						else
							b_stage[i].inc(ntx);
					}
					if (ntx.getNestingModel() == NestingModel.OPEN) 
					{
						this.m_hasOnAbort = true;
						this.m_hasOnCommit = true;
					}
							
					return null;
				}
				
				@Override
				public void onCommit(Context tx) {
					NestedContext ntx = (NestedContext)tx;
					for (int i=0; i<OP_GROUPS_STAGE; i++) {
						b_stage[i].unlock(ntx, readStage);
					}
				}
				
				@Override
				public void onAbort(Context tx) {
					NestedContext ntx = (NestedContext)tx;
					for (int i=0; i<OP_GROUPS_STAGE; i++) {
						if (!readStage)
							b_stage[i].dec(ntx);
						b_stage[i].unlock(ntx, readStage);
					}
				}
				
			}.execute(null);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			e.printStackTrace();
		}
		
		//After
		for (int i=0; i<OPS_AFTER; i++) 
		{
			b_after[i] = new ExpBucket("after", 1, AFTER_OBJ_COUNT, ids_after.subList(i, i+1));
			if (readAfter)
				b_after[i].get(ntx);
			else
				b_after[i].inc(ntx);
		}
		
		return null;
	}
}