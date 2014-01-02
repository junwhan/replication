package edu.vt.rt.hyflow.benchmark.tm.hashtable;

import java.util.Random;

import org.deuce.transaction.Context;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.helper.Atomic;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;

public class Benchmark extends edu.vt.rt.hyflow.benchmark.tm.Benchmark
{
	public static int MAX_KEY = 1000;
	
	private class hashid {
		public String htid;
		public Integer key;
		public hashid() {
			htid = "hash-"+random.nextInt(3);
			key = random.nextInt(MAX_KEY);
		}
		@Override
		public boolean equals(Object obj) {
			if (! (obj instanceof hashid))
				return false;
			hashid o = (hashid) obj;
			if (!o.htid.equals(this.htid))
				return false;
			if ((o.key.hashCode() % localObjectsCount) != (this.key.hashCode() % localObjectsCount))
				return false;
			return true;
		}
		@Override
		public String toString() {
			return htid+"-b"+(key.hashCode()%localObjectsCount)+"(key="+key+")";
		}
	}
	private Random random = new Random();

	@Override
	protected Class[] getSharedClasses() {
		// TODO Auto-generated method stub
		return new Class[] { Hashtable.class, HashBucket.class };
	}

	@Override
	protected void checkSanity() {
		// TODO Auto-generated method stub
	}

	@Override
	protected void createLocalObjects() {
		Integer id = Network.getInstance().getID();
		int nodes = Network.getInstance().nodesCount();
		
		// TODO: Horrible bug!!! If we create fewer objects than there are nodes,
		// the JVM on the node that didn't create any objects crashes!!!
		// Why does this happen I don't know. I suspect a hidden bug in HyFlow, but
		// could be something else (the instrumentation?!?) Alex
		if (nodes >= 3) {
			new Hashtable("hash-"+id, localObjectsCount);
		} else {
			for (int i=0; i<3; i++) {
				if ((i % nodes) == id) {
					new Hashtable("hash-"+i, localObjectsCount);
				}
			}
		}
	}

	@Override
	protected String getLabel() {
		return "Hashtable-TM";
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
		return new hashid();
	}

	@Override
	protected void readOperation(Object... ids) {
		int r = random.nextInt(2);
		hashid[] i2 = new hashid[ids.length];
		for (int i=0; i<ids.length; i++)
			i2[i] = (hashid)ids[i];
		switch(r) 
		{
		case 0:
			Logger.debug("Read operation: contains.");
			test_contains(calls, i2);
			break;
		case 1:
			Logger.debug("Read operation: get.");
			test_get(calls, i2);
			break;
		}
		
	}

	@Override
	protected void writeOperation(Object... ids) {
		int r = random.nextInt(2);
		hashid[] i2 = new hashid[ids.length];
		for (int i=0; i<ids.length; i++)
			i2[i] = (hashid)ids[i];
		switch(r)
		{
		case 0:
			Logger.debug("Write operation: put.");
			test_put(calls, i2);
			break;
		case 1:
			Logger.debug("Write operation: remove.");
			test_remove(calls, i2);
			break;
		/*case 2:
			Logger.debug("Write operation: move.");
			test_move(i2);
			break;
		case 3:
			Logger.debug("Write operation: copy.");
			test_copy(i2);
			break;*/
		}
		
	}
	
	protected void test_contains(final int count, final hashid... ids) {
		try {
			new Atomic<Object>() {
				@Override
				public Object atomically(AbstractDistinguishable self,
						Context transactionContext) {
					for (int i=0; i<count; i++) {
						Hashtable t = (Hashtable) HyFlow.getLocator().open(ids[i%ids.length].htid, "r");
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
	
	protected void test_get(final int count, final hashid... ids) {
		try {
			new Atomic<Object>() {
				@Override
				public Object atomically(AbstractDistinguishable self,
						Context transactionContext) {
					for (int i=0; i<count; i++) {
						Hashtable t = (Hashtable) HyFlow.getLocator().open(ids[i%ids.length].htid, "r");
						t.get(ids[i%ids.length].key);
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
	
	protected void test_put(final int count, final hashid... ids) {
		try {
			new Atomic<Object>() {
				@Override
				public Object atomically(AbstractDistinguishable self,
						Context transactionContext) {
					for (int i=0; i<count; i++) {
						Hashtable t = (Hashtable) HyFlow.getLocator().open(ids[i%ids.length].htid, "r");
						t.put(ids[i%ids.length].key, random.nextInt());
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
	
	protected void test_remove(final int count, final hashid... ids) {
		try {
			new Atomic<Object>() {
				@Override
				public Object atomically(AbstractDistinguishable self,
						Context transactionContext) {
					for (int i=0; i<count; i++) {
						Hashtable t = (Hashtable) HyFlow.getLocator().open(ids[i%ids.length].htid, "r");
						t.remove(ids[i%ids.length].key);
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
	
	protected void test_move(final hashid... ids) {
		try {
			new Atomic<Object>() {
				@Override
				public Object atomically(AbstractDistinguishable self,
						Context transactionContext) {
					
					final Hashtable t1 = (Hashtable) HyFlow.getLocator().open(ids[0].htid, "r");
					final Hashtable t2 = (Hashtable) HyFlow.getLocator().open(ids[1].htid, "r");
					
					Integer val = (Integer) t1.remove(ids[0].key);
					if (val == null)
						val = new Integer(random.nextInt());
					t2.put(ids[1].key, val);
					if (Benchmark.outer_delay)
						edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
					return null;
				}
			}.execute(null);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	protected void test_copy(final hashid... ids) {
		try {
			new Atomic<Object>() {
				@Override
				public Object atomically(AbstractDistinguishable self,
						Context transactionContext) {
					
					final Hashtable t1 = (Hashtable) HyFlow.getLocator().open(ids[0].htid, "r");
					final Hashtable t2 = (Hashtable) HyFlow.getLocator().open(ids[1].htid, "r");
					
					Integer val = (Integer)t1.get(ids[0].key);
					if (val == null)
						val = new Integer(random.nextInt());
					t2.put(ids[1].key, val);
					if (Benchmark.outer_delay)
						edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
					return null;
				}
			}.execute(null);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
}
