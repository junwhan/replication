package edu.vt.rt.hyflow.benchmark.dsm.list;

import edu.vt.rt.hyflow.util.network.Network;

public class Benchmark extends edu.vt.rt.hyflow.benchmark.tm.Benchmark{

	@Override
	protected Class[] getSharedClasses() {
		return new Class[] { Node.class };
	}
	
	ListHandler listHandler = new ListHandler();

	@Override
	protected void createLocalObjects() {
		if(Network.getInstance().getID()==0)
			listHandler.createList();
	}

	@Override
	protected String getLabel() {
		return "List-DSM";
	}

	@Override
	protected int getOperandsCount() {
		return 1;
	}

	@Override
	protected Object randomId() {
		return new Integer((int)(Math.random()*localObjectsCount));
	}

	@Override
	protected void readOperation(Object... ids) {
		try {
			listHandler.find((Integer)ids[0]);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void writeOperation(Object... ids) {
		ListHandler list = listHandler;
		if(Math.random()>0.5){
			System.out.println("[ADD]");
			try {
				list.add((Integer)ids[0]);
				elementsSum += (Integer)ids[0];
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}else{
			System.out.println("[DEL]");
			try {
				if(list.delete((Integer)ids[0]))
					elementsSum -= (Integer)ids[0];
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	int elementsSum = 0;
	@Override
	protected void checkSanity() {
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		try {
			System.out.println("Sanity Check:" + ((Network.getInstance().getID()==0) ? listHandler.sum() : "?") + "/" + elementsSum);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
