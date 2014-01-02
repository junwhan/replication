package edu.vt.rt.hyflow.benchmark.tm.deque;

import java.util.Random;

import org.deuce.transaction.Context;
import org.deuce.transaction.TransactionException;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.helper.Atomic;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;

public class Benchmark extends edu.vt.rt.hyflow.benchmark.tm.Benchmark 
{
	public static int MAX_PRI = 5;

	public class dequeid {
		public int priority;
		public dequeid() {
			priority = random.get().nextInt(MAX_PRI);
		}
		@Override
		public boolean equals(Object o) {
			return false;
		}
	}
	
	public static ThreadLocal<Random> random = new ThreadLocal<Random>() {
		protected Random initialValue() {
			return new Random();
		}
	};
	
	@Override
	protected Class[] getSharedClasses() {
		return new Class[] { Deque.class, DequeNode.class } ;
	}

	@Override
	protected Object randomId() {
		return new dequeid();
	}

	@Override
	protected int getOperandsCount() {
		return 1;
	}
	
	private void producerConsumerOp() {
		try {
			final int opt  = random.get().nextInt(3);
			if (opt == 0) {
				app_producer();
			} else if (opt == 1) {
				app_processor();
			} else {
				app_consumer();
			}
		} catch(Throwable e) {
			e.printStackTrace(Logger.levelStream[Logger.DEBUG]);
		}
	}

	@Override
	protected void readOperation(Object... ids) {
		producerConsumerOp();
		/*try {
			final int opt  = random.get().nextInt(2);
			if (opt == 0) {
				test_peekfront(ids);
			} else {
				test_peekback(ids);
			}
		} catch (Throwable e) {
			e.printStackTrace(Logger.levelStream[Logger.DEBUG]);
		}*/
	}

	@Override
	protected void writeOperation(Object... ids) {
		producerConsumerOp();
		/*try {
			final int opt  = random.get().nextInt(4);
			if (opt == 0) {
				test_pushfront(ids);
			} else if (opt == 1) {
				test_pushback(ids);
			} else if (opt==2) {
				test_popfront(ids);
			} else {
				test_popback(ids);
			}
		} catch (Throwable e) {
			e.printStackTrace(Logger.levelStream[Logger.DEBUG]);
		}*/
	}

	@Override
	protected void checkSanity() {
	}

	@Override
	protected void createLocalObjects() {
		Integer id = Network.getInstance().getID();
		new Deque<Integer>("deq"+id);
	}

	@Override
	protected String getLabel() {
		return "Deque-TM";
	}
	
	private void test_peekfront(Object[] ids) throws Throwable {
		new Atomic<Object>() {
			@SuppressWarnings("unchecked")
			@Override
			public Object atomically(AbstractDistinguishable self, Context c) {
				Deque<Integer> d = (Deque<Integer>)HyFlow.getLocator().open("deq0");
				// Useless, peeks at the same obj
				for (int i=0; i<calls; i++)
					d.peekFront();
				return null;
			}
		}.execute(null);
	}
	
	private void test_peekback(Object[] ids) throws Throwable {
		new Atomic<Object>() {
			@SuppressWarnings("unchecked")
			@Override
			public Object atomically(AbstractDistinguishable self, Context c) {
				Deque<Integer> d = (Deque<Integer>)HyFlow.getLocator().open("deq0");
				// Useless, peeks at the same obj
				for (int i=0; i<calls; i++)
					d.peekBack();
				return null;
			}
		}.execute(null);
	}
	
	
	private void test_pushfront(Object[] ids) throws Throwable {
		new Atomic<Object>() {
			@SuppressWarnings("unchecked")
			@Override
			public Object atomically(AbstractDistinguishable self, Context c) {
				Deque<Integer> d = (Deque<Integer>)HyFlow.getLocator().open("deq0");
				for (int i=0; i<calls; i++)
					d.pushFront(random.get().nextInt());
				return null;
			}
		}.execute(null);
	}
	
	private void test_pushback(Object[] ids) throws Throwable {
		new Atomic<Object>() {
			@SuppressWarnings("unchecked")
			@Override
			public Object atomically(AbstractDistinguishable self, Context c) {
				Deque<Integer> d = (Deque<Integer>)HyFlow.getLocator().open("deq0");
				for (int i=0; i<calls; i++)
					d.pushBack(random.get().nextInt());
				return null;
			}
		}.execute(null);
	}
	
	private void test_popfront(Object[] ids) throws Throwable {
		new Atomic<Object>() {
			@SuppressWarnings("unchecked")
			@Override
			public Object atomically(AbstractDistinguishable self, Context c) {
				Deque<Integer> d = (Deque<Integer>)HyFlow.getLocator().open("deq0");
				for (int i=0; i<calls; i++)
					d.popFront();
				return null;
			}
		}.execute(null);
	}
	
	private void test_popback(Object[] ids) throws Throwable {
		new Atomic<Object>() {
			@SuppressWarnings("unchecked")
			@Override
			public Object atomically(AbstractDistinguishable self, Context c) {
				Deque<Integer> d = (Deque<Integer>)HyFlow.getLocator().open("deq0");
				for (int i=0; i<calls; i++)
					d.popBack();
				return null;
			}
		}.execute(null);
	}
	
	private void app_producer() throws Throwable {
		new Atomic<Object>() {
			@SuppressWarnings("unchecked")
			@Override
			public Object atomically(AbstractDistinguishable self, Context c) {
				int val = random.get().nextInt();
				Logger.debug("DequeApp::produce(); val="+val);
				processingDelay();
				Deque<Integer> d0 = (Deque<Integer>)HyFlow.getLocator().open("deq0");
				d0.pushBack(val);
				Logger.debug("DequeApp::produce(); done");
				return null;
			}
		}.execute(null);
	}
	
	private void app_processor() throws Throwable {
		new Atomic<Object>() {
			@SuppressWarnings("unchecked")
			@Override
			public Object atomically(AbstractDistinguishable self, Context c) {
				Logger.debug("DequeApp::process();");
				Deque<Integer> d0 = (Deque<Integer>)HyFlow.getLocator().open("deq0");
				Integer val = d0.popFront();
				if (val == null) {
					Logger.debug("DequeApp::process(); no input, retrying later");
					return null;
					//throw new TransactionException("Empty in-queue");
				}
				Logger.debug("DequeApp::process(); got "+val+", sending to out");
				
				processingDelay();
				
				Deque<Integer> d1 = (Deque<Integer>)HyFlow.getLocator().open("deq1");
				d1.pushBack(val);
				Logger.debug("DequeApp::process(); done");
				return null;
			}
		}.execute(null);
	}
	
	private void app_consumer() throws Throwable {
		new Atomic<Object>() {
			@SuppressWarnings("unchecked")
			@Override
			public Object atomically(AbstractDistinguishable self, Context c) {
				Logger.debug("DequeApp::consume();");
				Deque<Integer> d1 = (Deque<Integer>)HyFlow.getLocator().open("deq1");
				Integer val = d1.popFront();
				if (val == null) {
					Logger.debug("DequeApp::consume(); no output, retrying later");
					return null;
					//throw new TransactionException("Empty out-queue");
				}
				Logger.debug("DequeApp::consume(); got "+val);
				processingDelay();
				return null;
			}
		}.execute(null);
	}

}
