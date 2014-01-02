package edu.vt.rt.hyflow.benchmark.tm.experiment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.ContextDelegator;

import com.sun.org.apache.xerces.internal.xs.StringList;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.tm.NestedContext;
import edu.vt.rt.hyflow.core.tm.NestingModel;
import edu.vt.rt.hyflow.core.tm.dtl.Context;

public class ExpBucket 
{
	private String baseid;
	private Integer opcount;
	private Integer total;
	private String lockid;
	private List<Integer> ids;
	
	public ExpBucket(String baseid, int opcount, int total, List<Integer> ids) {
		this.baseid = baseid;
		this.opcount = opcount;
		this.total = total;
		this.ids = ids;
		lockid = baseid+"-"+ids.get(0);
	}

	public static List<Integer> makeIdList(int opcount, int total) {
		HashSet<Integer> s = new HashSet<Integer>();
		while (s.size() != opcount) {
			s.add((int) (Math.random() * total));
		}
		List<Integer> res = new ArrayList<Integer>(s.size());
		res.addAll(s);
		return res;
	}
	
	public void get(NestedContext tx)
	{
		// Perform "get" on objects
		for (Integer id : ids) {
			ExpObject a = (ExpObject)HyFlow.getLocator().open(baseid+"-"+id);
			a.get(tx);
		}
	}
	
	public void inc(NestedContext tx) 
	{
		// Perform inc on objects
		for (Integer id : ids) {
			ExpObject a = (ExpObject)HyFlow.getLocator().open(baseid+"-"+id);
			a.inc(tx);
		}
	}
	
	public void dec(NestedContext tx) 
	{
		// Perform dec on objects
		for (Integer id : ids) {
			ExpObject a = (ExpObject)HyFlow.getLocator().open(baseid+"-"+id);
			a.dec(tx);
		}
	}
	
	public void lock(NestedContext tx, boolean readonly)
	{
		if (tx.getNestingModel() != NestingModel.OPEN)
			return;
		// Acquire lock
		((Context)tx).onLockAction(lockid, ".", readonly, true);
	}
	
	public void unlock(NestedContext tx, boolean readonly)
	{
		// Release lock 
		((Context)tx).onLockAction(lockid, ".", readonly, false);
	}
}
