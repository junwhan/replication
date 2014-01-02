package edu.vt.rt.hyflow.benchmark.tm.loan;

import java.util.LinkedList;
import java.util.List;

import org.deuce.Atomic;
import org.deuce.transform.Exclude;

import aleph.dir.DirectoryManager;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.transaction.Remote;

@Exclude
public class _LoanAccount
	extends 	AbstractDistinguishable
{
	private static final int BRANCHING = 2;
	
	private Integer amount = 0;
  	private String id;
    
	public _LoanAccount(String id) {
		this.id = id;
	}
	
	public Object getId() {
		return id;
	}
	private Long [] ts;
	public Long[] getTS(){
		return ts;
	}
	public void setTS(Long [] ts){
		// for DATS
		this.ts = ts;
	}

	public void deposit(int dollars){
		amount = amount + dollars;
	}
	
	public boolean withdraw(int dollars) {
		amount = amount - dollars;
		return amount >= 0;
	}

	@Remote
	@Atomic
	public Integer checkBalance() {
		return amount;
	}

	@Remote
	public void borrow(List<String> accountNums, int branching, boolean initiator, int amount) {
		if(!initiator)
			withdraw(amount);	// provide the loan request
		DirectoryManager locator = HyFlow.getLocator();
		accountNums = (List<String>)((LinkedList<String>)accountNums).clone();
		for(int i=0; i<branching && !accountNums.isEmpty(); i++){
			_LoanAccount account = (_LoanAccount) locator.open(accountNums.remove(0), "w");
			boolean last = (i==branching-1 || accountNums.isEmpty());	// is the last one?
			int loan = last ? amount : (int)(Math.random()*amount);		// randomly have a loan amount from neighbor  
			account.borrow(accountNums, branching, false, loan);		// borrow from others
			deposit(loan);	// add the loaned amount to my money
			amount -= loan;
		}
	}
	
	@Atomic
	public static void borrow(String id, List accountNums, int amount){
		((_LoanAccount)HyFlow.getLocator().open(id, "w")).borrow(accountNums, BRANCHING, true, amount);
	}
	
	@Remote
	public int sum(List<String> accountNums, int branching) {
		DirectoryManager locator = HyFlow.getLocator();
		int sum = checkBalance();
		accountNums = (List<String>)((LinkedList<String>)accountNums).clone();
		for(int i=0; i<branching && !accountNums.isEmpty(); i++){
			_LoanAccount account = (_LoanAccount) locator.open(accountNums.remove(0), "r");
			sum += account.sum(accountNums, branching);
		}
		return sum;
	}
	

	@Atomic
	public static void sum(String id, List accountNums){
		((_LoanAccount)HyFlow.getLocator().open(id, "r")).sum(accountNums, BRANCHING);
	}
	
	@Override
	public String toString() {
		return id;
	}
}
