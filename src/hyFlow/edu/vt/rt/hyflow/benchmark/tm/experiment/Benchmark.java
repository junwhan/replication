package edu.vt.rt.hyflow.benchmark.tm.experiment;

import java.util.Random;

import org.deuce.transaction.Context;
import org.deuce.transaction.TransactionException;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.tm.NestedContext;
import edu.vt.rt.hyflow.core.tm.NestingModel;
import edu.vt.rt.hyflow.helper.Atomic;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;

public class Benchmark extends edu.vt.rt.hyflow.benchmark.tm.Benchmark
{
	
	@Override
	protected Class[] getSharedClasses() {
		return new Class[] { ExpObject.class };
	}

	@Override
	protected void checkSanity() {
	}

	@Override
	protected void createLocalObjects() {
		Integer id = Network.getInstance().getID();
		
		// Create all objects on node 0, hope it doesn't crash! :)
		if (id == 0) {
			//TODO: create all objects and buckets
			for (int i=0; i<ExpAtomic.BEFORE_OBJ_COUNT; i++) {
				new ExpObject("before-"+i);
			}
			for (int i=0; i<ExpAtomic.STAGE_OBJ_COUNT; i++) {
				new ExpObject("stage-"+i);
			}
			for (int i=0; i<ExpAtomic.AFTER_OBJ_COUNT; i++) {
				new ExpObject("after-"+i);
			}
		} else {
			//talex: See Ticket #5
			new ExpObject("dontcrashonme-"+id);
		}
	}

	@Override
	protected String getLabel() {
		return "Experiment-TM";
	}

	@Override
	protected int getOperandsCount() {
		return 0;
	}

	@Override
	protected Object randomId() {
		return Math.random();
	}

	protected void rootOperation() throws Throwable {
		ExpAtomic a = new ExpAtomic();
		a.execute(null);
	}

	@Override
	protected void writeOperation(Object... ids) {
		try {
			rootOperation();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	protected void readOperation(Object... ids) {
		try {
			rootOperation();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
