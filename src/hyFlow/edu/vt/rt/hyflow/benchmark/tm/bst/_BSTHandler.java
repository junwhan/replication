package edu.vt.rt.hyflow.benchmark.tm.bst;

import org.deuce.Atomic;
import org.deuce.transaction.Context;

import aleph.dir.DirectoryManager;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.util.network.Network;

public class _BSTHandler {

	private static final String HEAD = "tree";

	public void createTree() {
		new _Node(HEAD, -1);	// create the head node
	}

	@Atomic
	public void add(Integer value){
		DirectoryManager locator = HyFlow.getLocator();
		String next = HEAD;
		String prev = null;
		boolean right = true;
		do{	
			_Node node = (_Node)locator.open(next, "r");
			if(value >= node.getValue()){
				prev = next;
				next = node.getRightChild();
				right = true;
			}else{
				prev = next;
				next = node.getLeftChild();
				right = false;
			}
		}while(next!=null);
		
		_Node prevNode = (_Node)locator.open(prev);		//open previous node for write
		
		String newNodeId =  Network.getInstance().getID() + "-" + Math.random();	// generate random id
		new _Node(newNodeId, value);	// create the node
		
		if(right)
			prevNode.setRightChild(newNodeId);
		else
			prevNode.setLeftChild(newNodeId);
	}

	@Atomic
	public boolean delete(Integer value){
		DirectoryManager locator = HyFlow.getLocator();
		String next = HEAD;
		String prev = null;
		boolean right = true;
		do{
			_Node node = (_Node)locator.open(next, "r");
			if(value > node.getValue()){
				prev = next;
				next = node.getRightChild();
				right = true;
			}else if(value < node.getValue()){
				prev = next;
				next = node.getLeftChild();
				right = false;
			}else{
				_Node prevNode = (_Node)locator.open(prev);		//open previous node for write
				_Node deletedNode = (_Node)locator.open(next);	//reopen for write to be deleted
				String replacement;
				if(deletedNode.getLeftChild()==null){
					replacement = deletedNode.getRightChild();
				}else if(deletedNode.getRightChild()==null){
					replacement = deletedNode.getLeftChild();
				}else{	// get left most in right tree
					String next2 = deletedNode.getRightChild();
					_Node currNode2 = null;
					_Node prevNode2 = null;
					do{
						prevNode2 = currNode2;
						replacement = next2;
						
						currNode2 = (_Node)locator.open(next2, "r");
						next2 = currNode2.getLeftChild();
					}while(next2!=null);
					if(prevNode2!=null){	// disconnect replacement node from its parent
						_Node prevNode2w = (_Node)locator.open(prevNode2.getId());	//open previous node for write
						prevNode2w.setLeftChild(null);
					}
				}
				if(right)
					prevNode.setRightChild(replacement);
				else
					prevNode.setLeftChild(replacement);
				locator.delete(deletedNode);
				return true;
			}
		}while(next!=null);
		return false;
	}

	@Atomic
	public boolean find(Integer value){
		DirectoryManager locator = HyFlow.getLocator();
		String next = HEAD;
		do{	
			_Node node = (_Node)locator.open(next, "r");
			if(value > node.getValue()){
				next = node.getRightChild();
			}else if(value < node.getValue()){
				next = node.getLeftChild();
			}else{
				System.out.println("FOUND!");
				return true;
			}
		}while(next!=null);
		System.out.println("NOT FOUND!");
		return false;
	}
	
	@Atomic
	public int sum(){
		DirectoryManager locator = HyFlow.getLocator();
		return sum((_Node)locator.open(HEAD)) + 1;
	}
	private int sum(_Node node){
		int sum = node.getValue();
		DirectoryManager locator = HyFlow.getLocator();
		if(node.getLeftChild()!=null)
			sum += sum((_Node)locator.open(node.getLeftChild()));
		if(node.getRightChild()!=null)
			sum += sum((_Node)locator.open(node.getRightChild()));
		return sum;
	}
}
