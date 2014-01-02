package edu.vt.rt.hyflow.util.network;

import java.io.IOException;

import aleph.PE;
import aleph.PEGroup;
import aleph.comm.Address;

public class Network {

	private static final Integer BASE_PORT = Integer.getInteger("basePort", 5000);
	private static final Long linkDelay = Long.getLong("linkDelay", 0);
	private static final Long callDelay = Long.getLong("callCost", 0);
	private static final String PARENT_IP = System.getProperty("parentIP", "127.0.0.1");
	private static final String NODES_COUNT = System.getProperty("nodes", "1");
	private static final long LOCAL_REMOTE_DIFF = 0;
	private int id;
	
	private static final int terminate = Integer.getInteger("terminateIdle") * 1000;
	
	public static void linkDelay(boolean callConstructionCost, Address remote){
		// TODO use matrix of local/remote addresses
		Address localhost = PE.thisPE().getAddress();
		boolean local = ((aleph.comm.tcp.Address)localhost).inetAddress.equals(((aleph.comm.tcp.Address)remote).inetAddress);
		long delay = linkDelay;
		if(local)
			delay += LOCAL_REMOTE_DIFF;
		setIdle(false);
		//if(callConstructionCost)
		//	callCostDelay();
		//try {
		//	Thread.sleep(delay);
		//} catch (InterruptedException e) {
		//	e.printStackTrace();
		//}
	}

	public static void callCostDelay(){
		setIdle(false);
		//try {
		//	Thread.sleep(callDelay);
		//} catch (InterruptedException e) {
		//	e.printStackTrace();
		//}
	}

	private static Network instance;
	public static Network getInstance(){
		if(instance==null)
			synchronized(Network.class){
				if(instance==null){
					instance = new Network();
					if(terminate>0)
						new Thread("Idle"){
							@Override
							public void run() {
								while(true){
									setIdle(true);
									try {
										sleep(terminate);
									} catch (InterruptedException e) {
									}
									if(Network.idle){	// still idle for whole 1 minute
										System.err.println("IDLE NODE TERMINATION!!");
										System.exit(0);
									}
								}
							}
						}.start();
				}
			}
		return instance;
	}
	
	private static boolean idle = false; 
	public static void setIdle(boolean idle){
		Network.idle = idle;
	}
	
	public static void init(int id){
		Network network = Network.getInstance();
		network.id = id;

		try {
			network.loadConfig();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		new PEGroup("hyflow");
		
	}

	public static int PE_PORT_SHIFT = 0;
	
	private void loadConfig() throws IOException {
		System.setProperty("aleph.myPort", String.valueOf(BASE_PORT + PE_PORT_SHIFT + this.id));
		System.setProperty("aleph.parent", PARENT_IP + "#" + (BASE_PORT + PE_PORT_SHIFT));
		System.setProperty("aleph.index", String.valueOf(id));
		System.setProperty("aleph.label", "[" + id + "]");
		System.setProperty("aleph.numPEs", String.valueOf(id==0 ? NODES_COUNT : 0));
	}

	public int getID(){
		return id;
	}
	
	public void setID(int id){
		this.id = id;
	}
	
	public int getPort(){
		return BASE_PORT + this.id;
	}
	
	public int nodesCount(){
		return Integer.parseInt(NODES_COUNT);
	}
	
	// VT RT testbed
    static String[] ips = new String[]{     
            "10.1.1.24",    // mario index 0
            "10.1.1.25",    // luigi index 1
            "10.1.1.26",    // yoshi index 2
            "10.1.1.30",    // lost index 3
            "10.1.1.30",    // lost index 4
            "10.1.1.29",    // rosella index 5
            "10.1.1.29",    // rosella index 6
    };

	
	static Integer machines = Integer.getInteger("machines");
	public static Address getAddress(String id){
		Integer index = Integer.parseInt(id);
		return new aleph.comm.tcp.Address((machines==1 ? "127.0.0.1": ips[index%machines]) + "#" + (BASE_PORT + index));
	}
	
	public static Address getCoordinator(){
		Integer index = Integer.parseInt("0");
		return new aleph.comm.tcp.Address((machines==1 ? "127.0.0.1": ips[index%machines]) + "#" + (BASE_PORT + index));
	}
}
