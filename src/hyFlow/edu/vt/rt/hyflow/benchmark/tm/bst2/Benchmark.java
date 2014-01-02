package edu.vt.rt.hyflow.benchmark.tm.bst2;

import java.util.Random;

import org.deuce.transaction.Context;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.helper.Atomic;
import edu.vt.rt.hyflow.util.network.Network;

public class Benchmark extends edu.vt.rt.hyflow.benchmark.tm.Benchmark{

	private class bstid {
		public String listid;
		public Integer key;
		public bstid() {
			listid = "bst-"+random.nextInt(3);
			key = random.nextInt(100);
		}
		@Override
		public boolean equals(Object obj) {
			if (! (obj instanceof bstid))
				return false;
			bstid o = (bstid) obj;
			if (!o.listid.equals(this.listid))
				return false;
			if (o.key.hashCode() != this.key.hashCode())
				return false;
			return true;
		}
		@Override
		public String toString() {
			return listid+"(key="+key+")";
		}
	}
	
	private Random random = new Random();
	
	@Override
	protected Class[] getSharedClasses() {
		return new Class[] { BST.class, Node.class };
	}
	
	@Override
	protected void createLocalObjects() {
		Integer id = Network.getInstance().getID();
		int nodes = Network.getInstance().nodesCount();
		
		if (nodes >= 3) {
			BST<Integer> bst = new BST<Integer>("bst-"+id);
			for (int j=0; j<40; j++) {
				bst.add(random.nextInt(100));
			}	
		} else {
			for (int i=0; i<3; i++) {
				if ((i % nodes) == id) {
					BST<Integer> bst = new BST<Integer>("bst-"+i);
					for (int j=0; j<40; j++) {
						bst.add(random.nextInt(100));
					}
				}
			}
		}
	}

	@Override
	protected String getLabel() {
		return "BST2-TM";
	}

	@Override
	protected int getOperandsCount() {
		return calls;
	}

	@Override
	protected Object randomId() {
		return new bstid();
	}

	@Override
	protected void readOperation(Object... ids) {
		test_find(ids);
	}

	@Override
	protected void writeOperation(Object... ids) {
		if(Math.random()>0.5){
			test_add(ids);
		} else {
			test_delete(ids);
		}
	}

	int elementsSum = 0;
	@Override
	protected void checkSanity() {
		/*try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		try {
			// nope :(
			System.out.println("Sanity Check:" + ((Network.getInstance().getID()==0) ? bstHandler.sum() : "?") + "/" + elementsSum);
		} catch (Throwable e) {
			e.printStackTrace();
		}*/
	}
	
	void test_find(final Object[] ids) {
		try {
			new Atomic<Object>() {
				@Override
				public Object atomically(AbstractDistinguishable self,
						Context __transactionContext__) {
					for (int i=0; i<calls; i++) {
						bstid id = (bstid) ids[i];
						BST<Integer> bst = (BST<Integer>) HyFlow.getLocator().open(id.listid);
						bst.find(id.key);
					}
					return null;
				}
			}.execute(null);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	void test_add(final Object[] ids) {
		try {
			new Atomic<Object>() {
				@Override
				public Object atomically(AbstractDistinguishable self,
						Context __transactionContext__) {
					for (int i=0; i<calls; i++) {
						bstid id = (bstid) ids[i];
						BST<Integer> bst = (BST<Integer>) HyFlow.getLocator().open(id.listid);
						bst.add(id.key);
					}
					return null;
				}
			}.execute(null);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	void test_delete(final Object[] ids) {
		try {
			new Atomic<Object>() {
				@Override
				public Object atomically(AbstractDistinguishable self,
						Context __transactionContext__) {
					for (int i=0; i<calls; i++) {
						bstid id = (bstid) ids[i];
						BST<Integer> bst = (BST<Integer>) HyFlow.getLocator().open(id.listid);
						bst.delete(id.key);
					}
					return null;
				}
			}.execute(null);
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
