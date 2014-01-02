package edu.vt.rt.hyflow.benchmark;

import java.util.concurrent.atomic.AtomicInteger;

import aleph.Message;
import edu.vt.rt.hyflow.util.network.Network;

public class BenchmarkNodeComplete extends Message{

	private static final AtomicInteger compelted = new AtomicInteger();
	
	public static void currentNodeComplete(){
//		System.err.println("DONNNNNNNNNE--");
		if(Network.getInstance().getID()==0)
			System.err.println("                      <<<" + compelted.get() + ">>>");
		if(compelted.incrementAndGet()==Network.getInstance().nodesCount()){
			System.err.println("DIE!");
			System.exit(0);
		}
	}
	
	@Override
	public void run() {
		if(compelted.incrementAndGet()==Network.getInstance().nodesCount()){
			System.err.println("DIE!");
			System.exit(0);
		}
	}

}
