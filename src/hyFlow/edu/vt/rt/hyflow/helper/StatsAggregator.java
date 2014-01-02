package edu.vt.rt.hyflow.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;

public class StatsAggregator {
	
	public static String logDir = null;
	
	// Track all threads
	private static ConcurrentLinkedQueue<StatsAggregator> all = new ConcurrentLinkedQueue<StatsAggregator>(); 
	
	// thread-local storage
	private static ThreadLocal<StatsAggregator> thrdLocals = new ThreadLocal<StatsAggregator>() {
		@Override
		protected StatsAggregator initialValue() {
			StatsAggregator res = new StatsAggregator();
			all.add(res);
			return res;
		}
	};
	
	// Per txn data struct (with only 1 level of O/N or C/N children)
	private class TxnStats {
		public TxnStats() {
			tx_start_time = System.nanoTime();
			subtx_fail_times.add(0L);
			subtx_wait_times.add(0L);
			subtx_fail_count.add(0);
			last_time = tx_start_time;
		}
		// Timing
		long tx_start_time;
		Deque<Long> subtx_fail_times = new ArrayDeque<Long>(8);
		Deque<Long> subtx_succ_times = new ArrayDeque<Long>(8);
		Deque<Long> subtx_wait_times = new ArrayDeque<Long>(8);
		
		long tx_after_commit;
		long root_time = 0L;
		Deque<Long> post_fail_times = new ArrayDeque<Long>(8);
		Deque<Long> post_succ_times = new ArrayDeque<Long>(8);
		long last_time;
		
		// Count
		Deque<Integer> subtx_fail_count = new ArrayDeque<Integer>(8);
		Deque<Integer> post_fail_count = new ArrayDeque<Integer>(8);
		
		// Obj count
		Deque<Integer> subtx_object_count = new ArrayDeque<Integer>(8);
		Deque<Integer> subtx_readset_count = new ArrayDeque<Integer>(8);
		int root_object_count = 0;
		int root_readset_count = 0;

		// Waiting
		Long final_wait_time;
		
		// Success
		boolean finished = false;
		boolean success = false;
		boolean last_abort = false;
		
		// Fail location
		int fail_id = -1;
		int txid = 0;
	}
	
	private TxnStats current = null;
	private LinkedList<TxnStats> history = new LinkedList<TxnStats>();
	private int txcount = 0;
	private String thread_name = Thread.currentThread().getName();
	
	// public access method
	public static StatsAggregator get() {
		return thrdLocals.get();
	}
	
	// private constructor
	private StatsAggregator() {
		
	}
	
	// Call at (re)start of root transaction
	public void startTxn() {
		if (current != null) {
			Logger.debug("StatsAggregator::startTxn(): current != null");
		}
		Logger.debug("StatsAggregator::startTxn()");
		current = new TxnStats();
		current.txid = txcount;
		history.addLast(current);
	}
	
	// Call at (re)start of each (first level) child txn 
	public void startSubTxn() {
		Logger.debug("StatsAggregator::startSubTxn()");
		final long t = System.nanoTime();
		if (! current.finished) {
			if (current.last_abort) {
				// Update wait time 
				final long oldt = current.subtx_wait_times.removeLast();
				current.subtx_wait_times.addLast(oldt + (t - current.last_time));
			} else {
				// Update time in root
				current.root_time += (t - current.last_time);
			}
			current.last_time = t;
		} else {
			// No waiting in post
		}
		current.last_abort = false;
		
	}
	
	// Call after successful commit
	public void commitSubTxn(int objCount, int readsetLen) {
		Logger.debug("StatsAggregator::commitSubTxn()");
		final long t = System.nanoTime();
		if (!current.finished) {
			// Add success time
			current.subtx_succ_times.addLast(t - current.last_time);
			// Initialize next abort slot
			current.subtx_fail_times.addLast(0L);
			current.subtx_wait_times.addLast(0L);
			current.subtx_fail_count.addLast(0);
		} else {
			// Add post success time
			current.post_succ_times.addLast(t - current.last_time);
			// Initialize next post abort slot
			current.post_fail_times.addLast(0L);
			current.post_fail_count.addLast(0);
		}
		//common
		current.subtx_object_count.addLast(objCount);
		current.subtx_readset_count.addLast(objCount);
		current.last_time = t;
		current.last_abort = false;
	}
	
	// Call on abort, before delay
	public void abortSubTxn() {
		Logger.debug("StatsAggregator::abortSubTxn()");
		final long t = System.nanoTime();
		if (!current.finished) {
			// Update abort time in current slot
			final long oldt = current.subtx_fail_times.removeLast();
			current.subtx_fail_times.addLast(oldt + (t - current.last_time));
			// Update abort count in current slot
			final int oldc = current.subtx_fail_count.removeLast();
			current.subtx_fail_count.addLast(oldc + 1);
		} else {
			// Update post abort time in current slot
			final long oldt = current.post_fail_times.removeLast();
			current.post_fail_times.addLast(oldt + (t - current.last_time));
			// Update post abort count in current slot
			final int oldc = current.post_fail_count.removeLast();
			current.post_fail_count.addLast(oldc + 1);
		}
		current.last_time = t;
		current.last_abort = true;
	}
	
	// Call on successful root txn commit
	public void commitTxn(int objCount, int readsetCount) {
		Logger.debug("StatsAggregator::commitTxn()");
		final long t = System.nanoTime();
		current.tx_after_commit = t - current.tx_start_time;
		current.root_object_count = objCount;
		current.root_readset_count = readsetCount;
		current.success = true;
		current.finished = true;
		current.last_time = t;
		// Initialize first fail slot
		current.post_fail_times.addLast(0L);
		current.post_fail_count.addLast(0);
		txcount++;
	}
	
	// Call on root abort
	public void abortTxn() {
		Logger.debug("StatsAggregator::abortTxn()");
		final long t = System.nanoTime();
		current.tx_after_commit = t - current.tx_start_time;
		current.success = false;
		current.finished = true;
		current.last_time = t;
		// Remember subtxn where we aborted
		current.fail_id = current.subtx_succ_times.size();
		// Initialize first fail slot
		current.post_fail_times.addLast(0L);
		current.post_fail_count.addLast(0);
	}
	
	// Call on root finished, i.e. after delay
	public void endTxn() {
		Logger.debug("StatsAggregator::endTxn()");
		final long t = System.nanoTime();
		current.final_wait_time = t - current.last_time;
		current.last_time = t;
		current = null;
	}
	
	public static void dumpStats() throws IOException {
		// Open file
		File outFile = new File(logDir, String.valueOf(Network.getInstance().getID()) + ".stats");
  		if(!outFile.exists()) outFile.createNewFile(); 
  		PrintStream out = new PrintStream(new FileOutputStream(outFile,true));
  		
  		int i = 0;
		for (StatsAggregator aggr : all) {
			for (TxnStats current : aggr.history) {
				out.println("thread="+aggr.thread_name);
				out.println("tx number="+current.txid);
				out.println("subtx fail times="+current.subtx_fail_times);
				out.println("subtx fail count="+current.subtx_fail_count);
				out.println("subtx wait times="+current.subtx_wait_times);
				out.println("subtx succ times="+current.subtx_succ_times);
				out.println("post fail count="+current.post_fail_count);
				out.println("post fail times="+current.post_fail_times);
				out.println("post succ times="+current.post_succ_times);
				out.println("tx after commit="+current.tx_after_commit);
				out.println("root time="+current.root_time);
				out.println("final wait time="+current.final_wait_time);
				out.println("root object count="+current.root_object_count);
				out.println("root readset count="+current.root_readset_count);
				out.println("subtx object count="+current.subtx_object_count);
				out.println("subtx readset count="+current.subtx_readset_count);
				out.println("success="+current.success);
				out.println("fail id="+current.fail_id);
				out.println();
			}
			i++;
		}
	}
	
}
