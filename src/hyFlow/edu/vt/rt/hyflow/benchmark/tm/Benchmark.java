package edu.vt.rt.hyflow.benchmark.tm;

import org.deuce.transaction.AbstractContext;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.cm.policy.AbstractContentionPolicy;
import edu.vt.rt.hyflow.core.dir.control.ControlFlowDirectory;
import edu.vt.rt.hyflow.core.tm.dtl2.Context;
import edu.vt.rt.hyflow.util.io.FileLogger;


abstract public class Benchmark  extends edu.vt.rt.hyflow.benchmark.Benchmark{

	abstract protected Class[] getSharedClasses();
	
	@Override
	protected void initSharedClasses() {
		boolean controlFlow = HyFlow.getLocator() instanceof ControlFlowDirectory;
		for(Class<?> clazz : getSharedClasses()){
			try {
				clazz.newInstance();
			} catch (Exception e) {
			}
			if(controlFlow)
				try {
					System.out.println("Create Class Proxy - " + clazz.getName());
					Class.forName(clazz.getPackage().getName() + ".$HY$_Proxy_" + clazz.getSimpleName()).newInstance();
				} catch (Exception e) {
					e.printStackTrace();
				}
		}
	}
	
	@Override
	protected String[] result() {
		return new String[] {
				"Aborts: " + AbstractContext.aborts,
				"Conflicts: " + AbstractContentionPolicy.dist_conflicts,
				"Local Conflicts: " + AbstractContentionPolicy.conflicts,
				"Forwarding: " + Context.forwardings,
		};
	}
}
