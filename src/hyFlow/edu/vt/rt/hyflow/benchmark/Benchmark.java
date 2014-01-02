package edu.vt.rt.hyflow.benchmark;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.deuce.transaction.ContextDelegator;

import edu.vt.rt.hyflow.HGREVID;
import edu.vt.rt.hyflow.helper.StatsAggregator;
import edu.vt.rt.hyflow.util.io.FileLogger;
import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;

public abstract class Benchmark {

	public final static int calls = Integer.getInteger("calls");
	private static final Long delay = Long.getLong("transactionLength", 0);
	private static final Long TIMEOUT = Long.getLong("timeout");
	public static boolean inner_delay = Boolean.getBoolean("inner_delay");
	public static final boolean outer_delay = Boolean.getBoolean("outer_delay");
	 
	public static long timout(){
		return TIMEOUT + (int)(Math.random()*TIMEOUT);
	}
	public static void processingDelay(){
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	protected int localObjectsCount;
	protected int transactions;
	protected int readPercent;
	protected int threads;
	
	private String logDir = null;
	
	public Benchmark() {
		localObjectsCount = Integer.getInteger("objects");
		if (!inner_delay && !outer_delay) {
			inner_delay = true;
		}
	}
	
	public void run() throws Throwable {
		String id = String.valueOf(Network.getInstance().getID());
		File logDir = FileLogger.init(getLabel(), id);
		this.logDir = logDir.getPath();
		if(Boolean.getBoolean("debug"))
			Logger.redirect(logDir, id);
		
		Logger.progress("Init Environment", 1000);
		initSharedClasses();
		Logger.fetal("Creating Local Objects");
		createLocalObjects();
		Logger.progress("Populated Objects", 10000);
		
		long start = System.currentTimeMillis();
		transactions = Integer.getInteger("transactions");
		readPercent = Integer.getInteger("reads");
		threads = Integer.getInteger("threads");
		
		BenchmarkThread[] testingThreads = new BenchmarkThread[threads];

		for (int i=0; i<testingThreads.length; i++)
			testingThreads[i] = new BenchmarkThread(i);
		
		for (BenchmarkThread thread : testingThreads)
			thread.start();
		for (BenchmarkThread thread : testingThreads)
			thread.join();
		
		int reads=0, writes=0;
		for (BenchmarkThread thread : testingThreads){
			reads+=thread.reads;
			writes+=thread.writes;
		}
		
		StatsAggregator.dumpStats();
		
		String header = 
			"  n="+Network.getInstance().nodesCount() + 
			", t="+threads + 
			", o="+localObjectsCount +
			", x="+transactions + 
			", c="+calls + 
			", %="+readPercent +
			", T="+Integer.getInteger("timeout") +
			", C="+Integer.getInteger("callCost") +
			", L="+Integer.getInteger("transactionLength") +
			", K="+Integer.getInteger("linkDelay") +
			", N="+System.getProperty("defaultNestingModel", "FLAT") +
			", Host="+java.net.InetAddress.getLocalHost().getHostName() +
			", ID="+System.getProperty("testId", "None") +
			", HGREVID="+HGREVID.hgrevid() + 
			".";
		
		FileLogger.out.println(getLabel() + header);
		FileLogger.out.println("Throughput: " + ((float)1000 * transactions * threads / (System.currentTimeMillis()-start)));
		FileLogger.out.println("Reads: " + reads);
		FileLogger.out.println("Writes: " + writes);
		for(String line: result())
			FileLogger.out.println(line);
		
//		for(StackTraceElement[] elements : Thread.getAllStackTraces().values())
//			System.out.println(Arrays.toString(elements));

		System.err.println("Complete...[" + id + "]");

		if(Boolean.getBoolean("sanity")){
			System.out.println("Sanity Check ...");
			checkSanity();
		}
	}

	protected String[] result() { return new String[]{}; }
	
	protected void configure(String args[]){}
	
	abstract protected Object randomId();
	abstract protected int getOperandsCount();
	
	abstract protected void readOperation(Object... ids);
	abstract protected void writeOperation(Object... ids);
	abstract protected void checkSanity();

	abstract protected void createLocalObjects();
	
	protected void initSharedClasses() {}
	
	abstract protected String getLabel();
	
	class BenchmarkThread extends Thread{
		int reads;
		int writes;
		
		public BenchmarkThread(int i) {
			super("Bench_"+i);
		}
		
		@Override
		public void run() {
			Logger.debug("Started");
			Random random = new Random(this.hashCode());
			int operands = getOperandsCount();
			File commit_file = new File(logDir + "/commit");
			for (int i = 0; i < transactions; i++) {
				List<Object> ids = new LinkedList<Object>();
				//while (ids.size() < operands) {
				while (ids.size() < calls) {
					Object id = randomId();
					//Logger.debug("FSTART "+ id);
					boolean exists = false;
					for (Object o: ids) {
						if (o.equals(id)) {
							exists = true;
							break;
						}
					}
					if (!exists)
						ids.add(id);
				}
				Collections.shuffle(ids);
				boolean read = random.nextInt(100) < readPercent;
				//Logger.debug((read? "R" : "W") + Arrays.toString(ids.toArray()));
				//Logger.debug("start iteration "+i);
				//Logger.debug("FSTART "+ ids);
				try {
					long start = System.currentTimeMillis();
					if (read) {
						reads++;
						readOperation(ids.toArray());
					} else {
						writes++;
						writeOperation(ids.toArray());
					}
					//Logger.debug("[" + i + "] \t" + ContextDelegator.getTopInstance().getRetries()  + "\t " + (System.currentTimeMillis() - start) + " \t" + Arrays.toString(ids.toArray()));
					System.err.print(".");
					commit_file.createNewFile();
					
				} catch (Exception e) {
					Logger.debug("Main loop exception!!");
					e.printStackTrace(Logger.levelStream[Logger.DEBUG]);
				}
				//Logger.debug("end iteration "+i);
				//Logger.debug("FEND "+ System.nanoTime());
			}
		}
	}

}
