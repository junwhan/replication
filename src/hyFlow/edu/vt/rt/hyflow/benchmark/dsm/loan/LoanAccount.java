package edu.vt.rt.hyflow.benchmark.dsm.loan;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.ContextDelegator;
import org.deuce.transform.Exclude;

import aleph.GlobalObject;
import aleph.dir.DirectoryManager;
import aleph.dir.home.TimeoutException;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.util.io.Logger;

@Exclude
public class LoanAccount
	extends 	AbstractDistinguishable
{
private static final int BRANCHING = 2;
	
	private Integer amount = 0;
  	private String id;
    
	public LoanAccount(String id) {
		this.id = id;
		
		AbstractContext context = ContextDelegator.getInstance();
		if(context.getContextId()==null)
			new GlobalObject(this, ((AbstractDistinguishable)this).getId());	// publish it now	
		else
			context.newObject(this);	// add it to context publish-set 
	}
	
	public Object getId() {
		return id;
	}
	private Long [] ts;
	@Override
	public Long[] getTS(){
		return ts;
	}
	@Override
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

	public Integer checkBalance() {
		return amount;
	}

	public void borrow(List<String> accountNums, int branching, boolean initiator, int amount, Map<String, LoanAccount> opened) {
		if(!initiator)
			withdraw(amount);	// provide the loan request

		List<String> tempAccountNums = (List<String>)((LinkedList<String>)accountNums).clone();
		for(int i=0; i<branching && !tempAccountNums.isEmpty(); i++){
			LoanAccount account = opened.get(tempAccountNums.remove(0));
			boolean last = (i==branching-1 || tempAccountNums.isEmpty());	// is the last one?
			int loan = last ? amount : (int)(Math.random()*amount);		// randomly have a loan amount from neighbor  
			account.borrow(tempAccountNums, branching, false, loan, opened);		// borrow from others
			deposit(loan);	// add the loaned amount to my money
			amount -= loan;
		}
	}
	public static void borrow(String id, List<String> accountNums, int amount){
		DirectoryManager locator = HyFlow.getLocator();
		Map<String, LoanAccount> opened = new HashMap<String, LoanAccount>();
		while(true){
			try {
				opened.clear();
				opened.put(id, (LoanAccount)locator.open(id, "w"));
				for(String accNum : accountNums)
					opened.put(accNum, (LoanAccount)locator.open(accNum, "w"));
				break;
			} catch (TimeoutException e) {
				Logger.debug("Timeout!");
				for(LoanAccount acc : opened.values()){
					locator.release(acc);
					Logger.debug("Release " + acc);
				}
			}
		}
		
		opened.get(id).borrow(accountNums, BRANCHING, true, amount, opened);
		edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
		
		for(LoanAccount acc : opened.values()){
			locator.release(acc);
			Logger.debug("Release " + acc);
		}
	}
	
	
	public int sum(List<String> accountNums, int branching, Map<String, LoanAccount> opened) {
		int sum = checkBalance();
		accountNums = (List<String>)((LinkedList<String>)accountNums).clone();
		for(int i=0; i<branching && !accountNums.isEmpty(); i++){
			LoanAccount account = opened.get(accountNums.remove(0));
			sum += account.sum(accountNums, branching, opened);
		}
		return sum;
	}
	public static void sum(String id, List<String> accountNums){
		DirectoryManager locator = HyFlow.getLocator();
		Map<String, LoanAccount> opened = new HashMap<String, LoanAccount>();
		while(true){
			try {
				opened.clear();
				opened.put(id, (LoanAccount)locator.open(id, "r"));
				for(String accNum : accountNums)
					opened.put(accNum, (LoanAccount)locator.open(accNum, "r"));
				break;
			} catch (TimeoutException e) {
				Logger.debug("Timeout!");
				for(LoanAccount acc : opened.values()){
					locator.release(acc);
					Logger.debug("Release " + acc);
				}
			}
		}

		opened.get(id).sum(accountNums, BRANCHING, opened);
		edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
		
		for(LoanAccount acc : opened.values()){
			locator.release(acc);
			Logger.debug("Release " + acc);
		}
	}
	
	@Override
	public String toString() {
		return id;
	}
}
