package edu.vt.rt.hyflow.benchmark.rmi;


public abstract class Benchmark extends edu.vt.rt.hyflow.benchmark.Benchmark{
	
	@Override
	protected String[] result() {
		return new String[] {
				"Timeouts: " + Lockable.timouts,
		};
	}
}
