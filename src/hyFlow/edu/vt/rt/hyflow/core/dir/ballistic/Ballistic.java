package edu.vt.rt.hyflow.core.dir.ballistic;

import java.io.IOException;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import org.deuce.transaction.AbstractContext;

import aleph.Aleph;
import aleph.GlobalObject;
import aleph.PE;
import aleph.comm.Address;
import aleph.comm.CommunicationManager;
import aleph.dir.DirectoryManager;
import edu.vt.rt.hyflow.core.dir.ballistic.maximalSet.MaximalSet;
import edu.vt.rt.hyflow.core.dir.ballistic.strucure.Structure;
import edu.vt.rt.hyflow.util.network.Network;


public class Ballistic extends DirectoryManager
{
	
	//Just to TEST
	//static int count=0;
	
	private static Hashtable<GlobalObject, Object> hashtable = new Hashtable<GlobalObject, Object>(); //record the object and its key
	

	static List<LogicalNode> logic = new LinkedList<LogicalNode>();
	
	static Structure struc;
	static int move_distance = 4;
	static int lookup_distance = 10;

	static int nodeID;
	
	int number_of_node;

	private static Ballistic theManager;
	private static final boolean DEBUG = false;

	static boolean check;
	
	//Constructor
	public Ballistic()
	{
		theManager = this;
		
		number_of_node = Integer.parseInt(System.getProperty("NumberOfNodes").trim());
		nodeID = Integer.parseInt(System.getProperty("MyId").trim());
		
		
		int[][] original_matrix = CreateTabl(number_of_node);	
		List<List<Integer>> leaders = constructMaxSet(CreateTabl(number_of_node));
		struc = new Structure(leaders, original_matrix);
		
		Aleph.register("Ballistic Directory", this);
		if(DEBUG)
			Aleph.debug(this.toString());
		
		
		
		System.out.println("Ballistic Manager was created on node " +nodeID);
		
		int maxlevel =0; //maximum level of the structure

		while(struc.getStructure(lookup_distance,move_distance, nodeID, maxlevel)!= null)
			maxlevel++;
		
	//	System.out.println("max level is"+maxlevel);
		
		//We create the structure of logical nodes
		LogicalNode newln = new LogicalNode();
		for(int i=0;i<=maxlevel;i++)
		{
			logic.add(newln);
		}
	//	System.out.println("size of logical node array is"+logic.size());
		
	/*	System.out.println("structure is  " + struc.getStructure(lookup_distance,move_distance, nodeID, 0));
		System.out.println("structure is  " + struc.getStructure(lookup_distance,move_distance, nodeID, 1));
		System.out.println("structure is  " + struc.getStructure(lookup_distance,move_distance, nodeID, 2));
		System.out.println("structure is  " + struc.getStructure(lookup_distance,move_distance, nodeID, 3));
		System.out.println("structure is  " + struc.getStructure(lookup_distance,move_distance, nodeID, 4));
		System.out.println("structure is  " + struc.getStructure(lookup_distance,move_distance, nodeID, 5));
		System.out.println("structure is  " + struc.getStructure(lookup_distance,move_distance, nodeID, 6));
	
	*/	
	
	}
	
	@Override
	/********
	 * This is the Publish function
	 */
	public void newObject(GlobalObject key, Object object, String hint) 
	{
		hashtable.put(key, object);
		try {
			new Publish(object,0,key).send(getHomeParent(0));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Address getHomeParent(int level){
		List<List<Integer>> parents = struc.getStructure(lookup_distance,move_distance, nodeID, level);
		if (parents != null)
		{
	//		System.out.println("home parent1 : " + parents.get(2).get(0)+ "at level "+level);
			return Network.getAddress(parents.get(2).get(0).toString());
		}
		return null;
	}
	
	static class Publish extends aleph.Message
	{
		GlobalObject key;
		Object object;
		int level;

		Publish(Object object, int level,GlobalObject key)
		{
			this.key = key;
			this.object=object;
			this.level=level;

			System.out.println("in publish");
		}
		@Override
		public void run() 
		{
	/*		System.out.println("before-//////////////////");
			for(int i =0;i<=7;i++)
			{
				System.out.println("we have child "+ logic.get(i).childpath.get(key) + " at level " +i);
			}
	*/		
			LogicalNode ln = new LogicalNode();
			ln.childpath.putAll(logic.get(level).childpath);
			ln.childpath.put(key, from);
			logic.set(level+1, ln);
			
/*			System.out.println("after-**////////////////////////****");
/*			for(int i =0;i<=7;i++)
			{
				System.out.println("we have child "+ logic.get(i).childpath.get(key) + " at level " +i);
			}
*/
			level++;
//			System.out.println("child at level "+ level+" : " + logic.get(level).childpath.get(key));
						
			Address homeParent = ((Ballistic)DirectoryManager.getManager()).getHomeParent(level);
			if (homeParent != null)
			{
				try {
					System.out.println("calling resgister");
					send(homeParent);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}	
		}
	}
	
	@Override
	//Open function will call either lookup or move in function of the mode
	public synchronized Object open(AbstractContext context, GlobalObject key, String mode, int commute) {
		
		System.out.println("Opening the object with key " + key);
		
		if(hashtable.containsKey(key)) //We check if the object is not already present
		{
			System.out.println("Object already present");
		}
		else{
			try{	
				switch(mode.charAt(0)){
					case 'r': lookup(key); break;
					case 'w': move(key);
				}
			}catch (Exception e) {
				e.printStackTrace();
			}
			
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return hashtable.get(key);
	}
	
	// This is the lookup function
	void lookup(GlobalObject key) throws IOException
	{
//		System.out.println("lnode " + nodeID + " at level 0");
		Address home = getHomeParent(0);
		Address original_caller = PE.thisPE().getAddress();
		System.out.println("We call up with lookup");
		// We call the up function with the boolean FALSE for lookup
		CommunicationManager.getManager().send(home, new Up(1,false,original_caller,key,0));
	}

		
	//Move Function
	void move(GlobalObject key) throws IOException
	{
//		System.out.println("unode " + nodeID + " at level 0");

		// We get the home parent of the node
		Address home = getHomeParent(0);
		// we set up the new path 
		CommunicationManager.getManager().send(home, new addchild(PE.thisPE().getAddress(),1,key));
		
//		System.out.println("We call up with move");
		// We call the up function with the boolean FALSE for lookup
		Address original_caller = PE.thisPE().getAddress();
		CommunicationManager.getManager().send(home, new Up(1,true,original_caller,key,0));
	}

	//The Up function go up in the structure looking for a node with a path.
	static class Up extends aleph.Message
	{
		int level;
		boolean cut;
		Address original_caller;
		GlobalObject key;
		int position; //position in the parents
		
		Up(int level, boolean cut, Address original_caller, GlobalObject key,int position)
		{
			this.level = level;
			this.cut= cut;
			this.original_caller=original_caller;
			this.key=key;
			this.position=position;
		}
		
		public void run()
		{
	//		System.out.println("We go up at node " + nodeID + " at level " + level);
			List<List<Integer>> parents = struc.getStructure(lookup_distance,move_distance, nodeID, level);
			
			if(parents != null)
			{
				Address home = Network.getAddress(parents.get(2).get(0).toString());
				Address ph = null ;
				
				int i;
				//move or lookup condition
				int j=0;
				if(cut==true)
					j=1;
				
		//		System.out.println("we check at position "+position);
		//		System.out.println("list of parents is before moving"+parents.get(j));
				//We put the home parent at the end
				int k= parents.get(j).indexOf(parents.get(2).get(0));
				int swap=parents.get(j).get(k);
				parents.get(j).remove(k);
				parents.get(j).add(swap);
		//		System.out.println("list of parents is after moving"+parents.get(j));
				
				if(position<parents.get(j).size())
				{	
					ph = Network.getAddress(parents.get(j).get(position).toString());
				
	//				System.out.println("home address is "+home);
	//				System.out.println("ph address is "+ph);
				
						try
						{
		//					System.out.println("We test the path at "+ph + "at level "+(level+1));
							CommunicationManager.getManager().send(ph, new testpath(level+1, key,cut,PE.thisPE().getAddress(),original_caller,position));
							
						}catch(IOException e){
							e.printStackTrace();
						}
				}
				else
				{
					try
					{
		//				System.out.println("Position out of bound ");
				
						if(cut == true)
						{
			//				System.out.println("we call addchild to set this node " +nodeID + "as child at level "+(level+1));
							CommunicationManager.getManager().send(home, new addchild(PE.thisPE().getAddress(),level+1,key));
						}
			//			System.out.println("we go back up with node "+home.toString()+  " at level "+(level+1));
						CommunicationManager.getManager().send(home, new Up(level+1, cut, original_caller, key, 0));
						
					}catch(IOException e){
						e.printStackTrace();
					}				
				}
			}
		}
	}

	
	//The testpath function checks if there is a registered child at a specified level for the node		
	static class testpath extends aleph.Message
		{
			int level;
			GlobalObject key;
			Address caller;
			boolean cut;
			int position;
			Address original_caller;
	
			testpath(int level,GlobalObject key,boolean cut,Address caller,Address original_caller,int position)
			{
				this.level=level;
				this.key=key;
				this.caller=caller;
				this.cut=cut;
				this.position = position;
				this.original_caller=original_caller;
			}
			
			public void run()
			{
				System.out.println("in testpath ");
				
		//		System.out.println("logic size is " +logic.size());
				
				if(logic.size()!=0)
				{
		//			System.out.println("we check for a path at level "+level);
					
					if(logic.get(level).childpath.containsKey(key)) //We found a down path
					{
	///					System.out.println("BANZAIII!!");
						new downpath(level,caller,cut,original_caller,key).run();
						
						if(cut==true)
						{
		//					System.out.println("we call addchild at level "+(level+1)+ "with node "+caller.toString());
		//					new addchild(caller,level+1,key).run();
	//						System.out.println("we call addchild at level "+(level)+ "with node "+caller.toString());
							new addchild(caller,level,key).run();
						}
				
						
						return;
					}
					else//we callback "up" wiht the next postion
					{
					
						try
						{
		//					System.out.println("1: no path found ");							
		//					System.out.println("1: we call node "+caller.toString() +"at level "+(level-1));
							 CommunicationManager.getManager().send(caller, new Up(level-1,cut,original_caller,key,position+1));
		
						}catch(IOException e){
							e.printStackTrace();
						}
					}	
				}
				else//we callback "up" wiht the next postion
				{
					try
					{
		//				System.out.println("2: no path found ");							
		//				System.out.println("2: we call node "+caller.toString() +"at level "+(level-1));
						 CommunicationManager.getManager().send(caller, new Up(level-1,cut,original_caller,key,position+1));
	
					}catch(IOException e){
						e.printStackTrace();
					}
				}			
			}
		}
	
	//Addchild add a "child "node at the specified level
	static class addchild extends aleph.Message
	{
		int level;
		Address caller;
		GlobalObject key;

		addchild(Address caller, int level, GlobalObject key)
		{
			this.level=level;
			this.caller = caller;
			this.key=key;
		}
		
		public void run()
		
		{
			System.out.println("in addchild");
			
			LogicalNode ln= new LogicalNode();
			ln.childpath.putAll(logic.get(level).childpath);
			ln.childpath.put(key, caller);
			logic.set(level, ln);

//			System.out.println("We add  the child " +caller +"at the level "+level);	
		}
	}

	//The downpath function goes down in the structure follwoing the objec path until it reach the object
	static class downpath extends aleph.Message
	{
		int level;
		Address caller;
		boolean cut;
		Address original_caller;
		GlobalObject key;

		downpath(int level, Address caller, boolean cut, Address original_caller,GlobalObject key)
		{
			this.level=level;
			this.caller = caller;
			this.cut =cut;
			this.original_caller = original_caller;
			this.key = key;
		}
		
		public void run()
		
		{
			System.out.println("We go down on node "+nodeID+ "at level "+level);
			
			if((level ==0)&&(hashtable.containsKey(key)))
			{
//				System.out.println("move object to do");
			
				Object object;
				if(cut ==true)
				{
					System.out.println("we remove the object " +hashtable.get(key) +"and send it to " + original_caller);
					object =	hashtable.remove(key);
					
	/*				try
					{
						 CommunicationManager.getManager().send(original_caller, new MoveObject(key,object));

					}catch(IOException e){
						e.printStackTrace();
					}
	*/			}
				else
				{
					object = hashtable.get(key);
				}
				
				try
				{
					 CommunicationManager.getManager().send(original_caller, new MoveObject(key,object));

				}catch(IOException e){
					e.printStackTrace();
				}
				
				
			}
			else
			{
	
				Address p = logic.get(level).childpath.get(key) ;
	//			System.out.println("the logical node at level "+level + "contains the address " +logic.get(level).childpath.get(key));
				
				if(cut==true)
				{
	/*				System.out.println("we remove the address "+logic.get(level).childpath.get(key).toString() +"at level "+level);
					System.out.println("before-****************************");
					for(int i =0;i<=7;i++)
					{
						System.out.println("we have child "+ logic.get(i).childpath.get(key) + "at level " +i);
					}
					
		*/			
					logic.get(level).childpath.remove(key);
					
					
		/*			System.out.println("afte*****************************");
					for(int i =0;i<=7;i++)
					{
						System.out.println("we have child "+ logic.get(i).childpath.get(key) + "at level " +i);
					}
					
					
					System.out.println("we now have the address "+logic.get(level).childpath.get(key) );		
			*/	}
				
				try
				{
	//				System.out.println("We call downpath at level "+ (level-1) + "with node " +p.toString());							
			
					 CommunicationManager.getManager().send(p, new downpath(level-1,caller,cut,original_caller,key));

				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}
	}

	
	
	//MOveOBject move the object from one node to another
	static public class MoveObject extends aleph.Message 
	{
		Object object;
		GlobalObject key;

		public MoveObject(GlobalObject key,Object object) {
		this.object = object;
		this.key=key;
		
		
		}

		public void run() {
		// This code on the other node
		Object manager = DirectoryManager.getManager();
		synchronized (manager) {
			hashtable.put(key, object);
			manager.notifyAll();
		}
		System.out.println("Hi I received this object " + object);
		System.out.println("which is" + hashtable.get(key));
		}
	}
	
	
	@Override
	public synchronized void release(AbstractContext context, GlobalObject object) {
		
/*		System.out.println("before---------------------------------");
		for(int i =0;i<=7;i++)
		{
			System.out.println("we have child "+ logic.get(i).childpath.get("123") + "at level " +i);
		}
*/		System.out.println("IN REALEASE--------------------------------");
		System.out.println("Releasing the object" + object);
	}

	@Override
	public String getLabel() {
		return "Ballistic";
	}

	
	static class LogicalNode 
	{
	 //Address of the child
		Hashtable<GlobalObject, Address> childpath = new Hashtable<GlobalObject, Address>();
		LogicalNode()
		{}
	}

	
	private int[][] CreateTabl(int index) 
	{
		 int [][]tab = new int[index][index];

		 //JUST FOR TEST !!!!
/*		 tab[0][1]=7; tab[0][2]=2; tab[0][3]=2; tab[0][4]=7; tab[0][5]=1; tab[0][6]=1; tab[0][7]=3;
		  tab[1][2]=2; tab[1][3]=3; tab[1][4]=8; tab[1][5]=2; tab[1][6]=2; tab[1][7]=9;
		   tab[2][3]=7; tab[2][4]=9; tab[2][5]=1; tab[2][6]=3; tab[2][7]=3;
		    tab[3][4]=3; tab[3][5]=2; tab[3][6]=5; tab[3][7]=1;
		    tab[4][5]=4; tab[4][6]=6; tab[4][7]=1;
		     tab[5][6]=2; tab[5][7]=3;
		     tab[6][7]=2;
*/		 
		 
		 
		 for(int i =0;i<(index);i++)
		    {
		    	for(int j=i;j<(index);j++)
		    	{	
		    		if(i==j)
		    			tab[i][j]=0;
		    		
		    		else
		    			tab[i][j] = j;
//		    			tab[i][j] = Math.abs(j-i);
//		    			tab[i][j] = 1;
		    			
		
		    		tab[j][i] = tab[i][j];
		    	}
		    }
		
/*		System.out.println("tabletest");
		for(int i=0;i<tab.length;i++)
		{
			for(int j=0;j<tab.length;j++)
			{
				System.out.print(tab[i][j]);
			}
			System.out.println("");
		}
*/		 
		return tab;
	}
	
	
	private List<List<Integer>> constructMaxSet(int[][] costMetric)
	{
		int level = 1;
		MaximalSet set = new MaximalSet(costMetric);
		List<List<Integer>> leaders = new LinkedList<List<Integer>>();
		boolean check = false;
		while (check == false)
		{
			List<Integer> leader = set.getMaximalSet(level++);
			leaders.add(leader);

			for (int i = 0; i < costMetric.length; i++)
			{
				if (leader.contains(i + 1))
				{
					for (int j = 0; j < costMetric.length; j++)
						if (!leader.contains(j + 1))
							costMetric[i][j] = -1;
				} else
					for (int j = 0; j < costMetric.length; j++)
						costMetric[i][j] = -1;
			}
			if (leader.size() == 1)
				check = true;
		}
		return leaders;
	}	
	

}