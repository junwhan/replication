package edu.vt.rt.hyflow.benchmark;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Iterator;

import aleph.PE;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.util.io.FileLogger;
import edu.vt.rt.hyflow.util.network.Network;


public class Application {

	public static void main(String[] args) throws NumberFormatException, Throwable {
		if(args.length<2){
			System.err.println("Missing Arguments\nApplication <Benchmark> <id> [<custom config>]");
			System.exit(1);
		}
				
		boolean agent = false;
		RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
		for(Iterator<String> itr=runtimeMXBean.getInputArguments().iterator(); itr.hasNext(); )
			if(itr.next().startsWith("-javaagent:"))
				agent = true;
		
		int nodeId = Integer.parseInt(args[1]);
		
		if(!agent){
			System.err.println("No instrumentation agent was defined.");
			HyFlow.readConfigurations();
			Network.getInstance().setID(nodeId);
		}
		
		if(args[0].contains("rmi."))
			Network.PE_PORT_SHIFT = 1000;
		HyFlow.start(nodeId);

		Benchmark benchmark = (Benchmark)Class.forName("edu.vt.rt.hyflow.benchmark." + args[0] + ".Benchmark").newInstance();
		if(args.length>2){
			String[] configs = new String[args.length-2];
			System.arraycopy(args, 2, configs, 0, configs.length);
			benchmark.configure(configs);
		}
		benchmark.run();
		
		PE.thisPE().populate(new BenchmarkNodeComplete(), false);
		
		BenchmarkNodeComplete.currentNodeComplete();
		
		while(true){
			Thread.sleep(2000);
		}		
	}

}
