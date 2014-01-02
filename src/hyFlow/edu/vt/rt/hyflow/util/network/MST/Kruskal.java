// This code computes a Minimum Spanning Tree by using the algorithm of 
// Kruskal. The graph should be given in the weighted link list
// representation. Have to locate Graph.dat and Network.dat in the current dir. 
// Assume : We know an object holding node.
package edu.vt.rt.hyflow.util.network.MST;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

public class Kruskal
{
  // this class describes a graph edge 
  private static class Edge 
  {
    String node1;              // label of the first edge node
    String node2;              // label of the second edge node
    double weight;             // edge weight
    public Edge(String n1, String n2, double w)  // constructor
    {
      node1 = n1;              // set up endnode labels
      node2 = n2;
      weight = w;              // set up the edge weight
    }
  }

  // this class describes a graph vertex set
  private static class VertexSet
  {
    String label;              // vertex label
    VertexSet next;            // pointer to the next vertex in the set
    VertexSet head;            // pointer to the first vertex in the set
    VertexSet tail;            // pointer to the last vertex in the set
    public VertexSet(String l) // constructor
    {
      label = l;               // set up the node label
      next = null;             // reference to the next set node
      head = tail = this;      // references to the set tail and the head
    }
  }
  
    // storage for graph vertices 
  private static TreeMap<String, VertexSet> vertices;
    // the Map data structure is used to get access to vertices by labels as
    // keys
  private static TreeSet<Edge> edges;    // storage for graph edges
    // the SortedSet data structure is used to automatically sort the edges
    // accoding to their weights

  public static void main(String[] args)
  {
	String type = args[0];          // Request type  
	String sid = args[1];  			// Source ID (My ID)
    String ohn = args[2];			// An object holding node (Dest ID)
    String oid = "10";				// Object ID
    
    readGraph();                      // read graph encoding

    // control output of the graph edges sorted by weight
    System.out.println("Edges sorted according to the weight:");
    for (Iterator<Edge> it=edges.iterator(); it.hasNext(); )
    {
      Edge e = it.next();
      if (! vertices.containsKey(e.node2))
      {
        System.out.println("Undefined node " + e.node2);
        System.exit(0);
      }
      System.out.println(e.node1 + " - " + e.node2 + ": " + e.weight);
    }

    // the set A will store the MST edges 
    ArrayList<Edge> A = new ArrayList<Edge>();   // A = emptyset
    // main loop over all edges
    //System.out.println("\nRunning the algorithm:");
    for (Iterator<Edge> it=edges.iterator(); it.hasNext(); )
    {
      Edge e = it.next();                // fetch an edge from the list
      String c1 = e.node1;               // retrieve the edge endpoints
      String c2 = e.node2;
      //System.out.print("Checking edge " + c1 + " - " + c2 + " : ");
      if (findSet(c1) != findSet(c2))    // check whether they are in diff.
      {                                  // vertex sets of the partition
        A.add(e);                        // if not - accept the edge
        union(e.node1, e.node2);         // unite the corresp. vertex sets
        //System.out.println("accept");

        // control output of the tree vertices
        //System.out.print("   tree vertices: ");
        VertexSet v = vertices.get(e.node1);
        //for (v=v.head; v!=null; v=v.next)
        //  System.out.print(v.label + " ");
        //System.out.println();
      }
      //else                               // the edge endpoints belong to the
        //System.out.println("reject");    // same vertex set; reject the edge
    }

    //End of constructing MST
    
    System.out.println("My id: "+ sid +" Object Holding Node: "+ ohn);

    //Finding the next node on MST 
    
    //for (Iterator<Edge> it=A.iterator(); it.hasNext(); )
    //{
    //  Edge e = it.next();  
    //  System.out.println(e.node1 + " - " + e.node2 + ": " + e.weight);
    //}
     
    //System.out.println("\nMST edges:");
    double w = 0;
    int iter = 0;
    StringBuffer PathSet = new StringBuffer();
    
    String inode = sid;
    Iterator<Edge> it=A.iterator();
    PathSet.append(inode);
    while(true)
    //for (Iterator<Edge> it=A.iterator(); it.hasNext(); )
    {
      Edge e = it.next();  
      
      if (inode.equals(e.node1)){
    	  inode = e.node2;
    	  PathSet.append(inode);
    	  //System.out.println("Visit: " + inode);
      } 
      else if(inode.equals(e.node2))
      {
    	  inode = e.node1;
    	  PathSet.append(inode);
    	  //System.out.println("Visit: " + inode); 	
      }  
      if(inode.equals(ohn)) break;  //Found the object holding node
      if(!it.hasNext()) it = A.iterator();
    	 
    }
    //System.out.println("Final Result1: " + PathSet.toString());
    int index;
    for (index=PathSet.length()-1; index>=0;index-- ){
    	//if(sid.equals(PathSet.substring(index,index+s))) break; 
    	//Note: s is the size of node name
    	
    	if(sid.equals(PathSet.substring(index,index+1))) break;
    }	
    inode = PathSet.substring(index+1,index+2);
    System.out.println("The Next Node "+PathSet.charAt(index+1));
    //System.out.println("Total MST weight: " + w);
    
     
//    TMPacket tpacket;
//    tpacket = new TMPacket(type,ohn,inode,sid,oid);
    /*
    try{
    	BufferedReader in = new BufferedReader(new FileReader("Network.dat"));
    	while(Number < numNodes)
		{
			// Read in a line
			String line = in.readLine();
		
			// Skip the rest of the loop if line starts with a "#"
			if(line.charAt(0) == '#')
				continue;

			Number++;
		}    
    } // end of try
	catch (IOException e) {
		System.out.println("Network File not found.");
		System.exit(0);
	}
*/	
  }

  // this method returns a pointer to the vertex set to which the vertex
  // with the given label belongs
  public static VertexSet findSet(String c)
  {
    VertexSet v = vertices.get(c);
    return(v.head);
  }

  // this method unites two vertex sets given by the vertex labels of their
  // representatives
  public static void union(String c1, String c2)
  {
    // get pointers to the corresp. vertex structures
    VertexSet v1 = vertices.get(c1);
    VertexSet v2 = vertices.get(c2);

    v1.head.tail.next = v2.head;   // append the second list to the first one
    v1.head.tail = v2.head.tail;   // set up pointer to the end of list
    for (VertexSet v=v2.head; v!=null; v=v.next)
      v.head = v1.head;            // set up pointers to the rep of the list
  }

  // this method reads graph encoding from file
  public static void readGraph() 
  {
    try{

    	//BufferedReader in = new BufferedReader(new FileReader("mst.dat"));
    	Scanner inFile = new Scanner(new BufferedReader(new FileReader("Graph.dat")));
    	
    // the comparator is necessary to compare the edges by weight; they
    // will automatically sorted by weight in the tree data structure
    Comparator<Edge> c = new Comparator<Edge>()
    {
    	public int compare(Edge e1, Edge e2)
    	{
    		double weight1 = e1.weight;
    		double weight2 = e2.weight;
    		if (weight1 < weight2) return(-1);
    		else if (weight1 > weight2) return(1);
    		else 
    		{
   				String s1 = e1.node1;
   				String s2 = e2.node1;
   				return(s1.compareTo(s2));
   			}
   		}
   	};

    vertices = new TreeMap<String, VertexSet>(); // storage for graph vertices
    edges = new TreeSet<Edge>(c);      // storage for the graph edges

    while (inFile.hasNextLine())       // read all file records 
    {
    	String s = inFile.nextLine();    // read a text line from file
    	if (s.length() > 0 && s.charAt(0) != '#')  // ignore comments
    	{
    		StringTokenizer sTk = new StringTokenizer(s);
    		String vertexLabel1 = sTk.nextToken();  // tokenize the read string
    		if (vertices.containsKey(vertexLabel1))
    		{
    			System.out.println("Multiple declaration of vertex " + vertexLabel1
                + " in the adjacency list");
    			System.exit(0);
    		}
    		VertexSet v = new VertexSet(vertexLabel1);  // retrieve the node label
    		vertices.put(vertexLabel1, v); // adding the vertex into the hash
   
   			while (sTk.hasMoreTokens())    // loop over all edges incident with
    		{                              // the current vertex
    			String vertexLabel2 = sTk.nextToken();  // second endnode label

    			double edgeWeight = 0;
    			try
    			{
    				edgeWeight = Double.parseDouble(sTk.nextToken());  //edge wgt 
    			}
    			catch(NumberFormatException nfe)
    			{
    				System.out.println("Wrong label/weight combination in the " +
    						"adjacency list of " + vertexLabel1);
    				System.exit(0);
    			}
    			catch(NoSuchElementException nfe)
    			{
    				System.out.println("Wrong adjacency list for node " + vertexLabel1);
    				System.exit(0);
    			}

    			if (vertexLabel1.compareTo(vertexLabel2) < 0) // endnode of edges
    			{                            // are stored in the alphabetic order
    				Edge e = new Edge(vertexLabel1, vertexLabel2, edgeWeight);
    				edges.add(e);              // add edge to the tree
    			}
        }
      }
    }
    
    } // end of try
	catch (IOException e) {
		System.out.println("Graph File for MST not found.");
		System.exit(0);
	}
    
  }
}