package edu.vt.rt.hyflow.benchmark.tm.agent;

import java.util.Random;

import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;

public class Benchmark extends edu.vt.rt.hyflow.benchmark.tm.Benchmark{

	private Random random = new Random(hashCode());

	@Override
	protected Class[] getSharedClasses() {
		return new Class[] { SearchAgent.class };
	}
	
	@Override
	protected void createLocalObjects() {
		Integer id = Network.getInstance().getID();
		int nodes = Network.getInstance().nodesCount();
		Logger.debug("Distributeding " + localObjectsCount + " objects over " + nodes + " nodes.");
		for(int i=0; i<localObjectsCount; i++){
			Logger.debug("Try creating object " + i);
			if((i % nodes)== id){
				Logger.debug("Created locally object " + i);
				try {
					new SearchAgent(String.valueOf(id));
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
		for(int i=0; i<2; ){
			String tracker = String.valueOf((int)(Math.random()*localObjectsCount));
			if(SearchAgent.addTracker(tracker))
				i++;
		}
	}

	@Override
	protected int getOperandsCount() {	return 1; }

	private String[] keywords = new String[]{
			"cl",
			"xx",
			"a",
			"class",
	};
	
	@Override
	protected Object randomId() {
		int obj = random.nextInt(keywords.length);
		return keywords[obj];
	}

	@Override
	protected void readOperation(Object... ids) {
		try {
			if(SearchAgent.exists((String)ids[0]))
				System.out.println("FOUND=======================");;
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void writeOperation(Object... ids) {
//		if(Network.getInstance().getID()!=0)
//			 return;
		
		try {
			StringBuffer str = (StringBuffer)SearchAgent.search((String)ids[0]);
			if(str!=null)
				System.out.println("FOUND==========================:" + str);
			else
				System.out.println("Not Found==========================");
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	protected String getLabel() {
		return "Agent-TM";
	}
	
	@Override
	protected void checkSanity() {
	}	
}

