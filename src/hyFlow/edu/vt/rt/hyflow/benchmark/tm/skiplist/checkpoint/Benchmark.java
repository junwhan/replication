package edu.vt.rt.hyflow.benchmark.tm.skiplist.checkpoint;

import java.util.Random;

import org.deuce.transaction.Context;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.helper.Atomic;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;

public class Benchmark extends edu.vt.rt.hyflow.benchmark.tm.Benchmark
{
	
	private class skipid {
		public String slid;
		public Integer key;
		public skipid() {
			slid = "skiplist-"+random.nextInt(3);
			key = random.nextInt(100);
		}
		@Override
		public boolean equals(Object obj) {
			if (! (obj instanceof skipid))
				return false;
			skipid o = (skipid) obj;
			if (!o.slid.equals(this.slid))
				return false;
			if (o.key.hashCode() != this.key.hashCode())
				return false;
			return true;
		}
		@Override
		public String toString() {
			return slid+"(key="+key+")";
		}
	}
	
	public static final int MAX_KEY = 100;
	private Random random = new Random();

	@Override
	protected Class[] getSharedClasses() {
		// TODO Auto-generated method stub
		return new Class[] { SkipNode.class, SkipList.class };
	}

	@Override
	protected void checkSanity() {
		// TODO Auto-generated method stub
	}

	@Override
	protected void createLocalObjects() {
		Integer id = Network.getInstance().getID();
		int nodes = Network.getInstance().nodesCount();
		
		if (nodes >= 3) {
			new SkipList<Integer>("skiplist-"+id);
		} else {
			for (int i=0; i<3; i++) {
				if ((i % nodes) == id) {
					new SkipList<Integer>("skiplist-"+i);
				}
			}
		}
	}

	@Override
	protected String getLabel() {
		return "Skiplist-CP-TM";
	}

	@Override
	protected int getOperandsCount() {
		if (calls < 2)
			return 2;
		else
			return calls;
	}

	@Override
	protected Object randomId() {
		return new skipid();
	}

	@Override
	protected void readOperation(Object... ids) {
		int r = random.nextInt(5);
		skipid[] i2 = new skipid[ids.length];
		for (int i=0; i<ids.length; i++)
			i2[i] = (skipid)ids[i];
		if (r == 0) {
			Logger.debug("Read operation: print all.");
			test_print(calls, i2);
		} else {
			Logger.debug("Read operation: contains.");
			test_contains(calls, i2);
		}
	}

	@Override
	protected void writeOperation(Object... ids) {
		int r = random.nextInt(2);
		skipid[] i2 = new skipid[ids.length];
		for (int i=0; i<ids.length; i++)
			i2[i] = (skipid)ids[i];
		switch(r)
		{
		case 0:
			Logger.debug("Write operation: put.");
			test_insert(calls, i2);
			break;
		case 1:
			Logger.debug("Write operation: remove.");
			test_delete(calls, i2);
			break;
		}
	}
	
	protected void test_contains(final int count, final skipid... ids) {
		try {
			new Atomic<Object>() {
				@SuppressWarnings("unchecked")
				@Override
				public Object atomically(AbstractDistinguishable self,
						Context transactionContext) {
					for (int i=0; i<count; i++) {
						SkipList<Integer> t = (SkipList<Integer>) HyFlow.getLocator().open(ids[i%ids.length].slid);
						t.contains(ids[i%ids.length].key);
						if (Benchmark.outer_delay)
							edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
					}
					return null;
				}
			}.execute(null);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	protected void test_print(final int count, final skipid... ids) {
		try {
			new Atomic<Object>() {
				@SuppressWarnings("unchecked")
				@Override
				public Object atomically(AbstractDistinguishable self,
						Context transactionContext) {
					for (int i=0; i<count; i++) {
						SkipList<Integer> t = (SkipList<Integer>) HyFlow.getLocator().open(ids[i%ids.length].slid);
						t.printAll();
						if (Benchmark.outer_delay)
							edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
					}
					return null;
				}
			}.execute(null);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	protected void test_insert(final int count, final skipid... ids) {
		try {
			new Atomic<Object>() {
				@SuppressWarnings("unchecked")
				@Override
				public Object atomically(AbstractDistinguishable self,
						Context transactionContext) {
					for (int i=0; i<count; i++) {
						SkipList<Integer> t = (SkipList<Integer>) HyFlow.getLocator().open(ids[i%ids.length].slid);
						t.insert(ids[i%ids.length].key);
						if (Benchmark.outer_delay)
							edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
					}
					return null;
				}
			}.execute(null);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	protected void test_delete(final int count, final skipid... ids) {
		try {
			new Atomic<Object>() {
				@SuppressWarnings("unchecked")
				@Override
				public Object atomically(AbstractDistinguishable self,
						Context transactionContext) {
					for (int i=0; i<count; i++) {
						SkipList<Integer> t = (SkipList<Integer>) HyFlow.getLocator().open(ids[i%ids.length].slid);
						t.delete(ids[i%ids.length].key);
						if (Benchmark.outer_delay)
							edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
					}
					return null;
				}
			}.execute(null);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
}
