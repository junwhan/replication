package edu.vt.rt.hyflow.benchmark.dsm.list;

import aleph.dir.DirectoryManager;
import aleph.dir.home.HomeDirectory;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.util.network.Network;

public class ListHandler {
	
	private static final String HEAD = "0-list";
	private final DirectoryManager locator;
	
	public ListHandler() {
		locator = HyFlow.getLocator();
		((HomeDirectory)locator).setTimeOut(false);
	}

	public void createList() {
		new Node(HEAD, -1);	// create the head node
	}

	public void add(Integer item) {
		String newNodeId =  Network.getInstance().getID() + "-" + Math.random();	// generate random id
		Node newNode = new Node(newNodeId, item);
		Node head = (Node)locator.open(HEAD);
		newNode.setNext(head.getNext());
		head.setNext(newNodeId);
		edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
		locator.release(head);
	}

	public boolean delete(Integer item) {
		Node pred = null, curr = null;
		try {
			Node head = (Node)locator.open(HEAD);
			pred = head;
			String nextId = pred.getNext();
			if(nextId==null)
				return false;
			curr = (Node)locator.open(nextId);
			try {
				while (!item.equals(curr.getValue())) {
					locator.release(pred);
					pred = curr;
					nextId = curr.getNext();
					if(nextId==null)
						return false;
					curr = (Node)locator.open(nextId);
				}
				pred.setNext(curr.getNext());
				locator.delete(curr);
				curr = null;
				return true;
			} finally {
				if(curr!=null)
					locator.release(curr);
			}
		} finally {
			edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
			locator.release(pred);
		}		
	}

	public boolean find(Integer item) {
		Node pred = null, curr = null;
		try {
			Node head = (Node)locator.open(HEAD);
			pred = head;
			String nextId = pred.getNext();
			if(nextId==null)
				return false;
			curr = (Node)locator.open(nextId);
			try {
				while (!item.equals(curr.getValue())) {
					locator.release(pred);
					pred = curr;
					nextId = curr.getNext();
					if(nextId==null){
//						System.out.println("NOT FOUND");
						return false;
					}
					curr = (Node)locator.open(nextId);
				}
//				System.out.println("FOUND");
				return true;
			} finally {
				locator.release(curr);
			}
		} finally {
			edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
			locator.release(pred);
		}		
	}
	
	public int sum(){
		int sum = 0;
		Node pred = null, curr = null;
		try {
			Node head = (Node)locator.open(HEAD);
			pred = head;
			String nextId = pred.getNext();
			if(nextId==null)
				return 0;
			curr = (Node)locator.open(nextId);
			try {
				while (true) {
					sum += curr.getValue();
					locator.release(pred);
					pred = curr;
					nextId = curr.getNext();
					if(nextId==null){
//						System.out.println("NOT FOUND");
						return sum;
					}
					curr = (Node)locator.open(nextId);
				}
			} finally {
				locator.release(curr);
			}
		} finally {
			edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
			locator.release(pred);
		}		
	}
}
