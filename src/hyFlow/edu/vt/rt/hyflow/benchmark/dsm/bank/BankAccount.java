package edu.vt.rt.hyflow.benchmark.dsm.bank;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.ContextDelegator;
import org.deuce.transaction.TransactionException;

import aleph.GlobalObject;
import aleph.dir.DirectoryManager;
import aleph.dir.home.TimeoutException;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.util.io.Logger;

public class BankAccount
	extends 	AbstractDistinguishable
{
	private Integer amount = 0;
	private String id;
			
	public BankAccount(String id) {
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
	public void deposit(int dollars)  throws Throwable{
		amount = amount + dollars;
	}

	public Integer checkBalance() {
		return amount;
	}
	
	public boolean withdraw(int dollars) {
		amount = amount - dollars;
		return amount >= 0;
	}

	public static long getTotalBalance(String subAccountNum1, String subAccountNum2) {
		DirectoryManager locator = HyFlow.getLocator();
		BankAccount subAccount1 = null;
		BankAccount subAccount2 = null;
		while(true){
			boolean locked = false;
			try {
				subAccount1 = (BankAccount) locator.open(subAccountNum1, "r");
				locked = true;
				Logger.debug("Lock " + subAccountNum1);
				subAccount2 = (BankAccount) locator.open(subAccountNum2, "r");
				Logger.debug("Lock " + subAccountNum2);
				break;
			} catch (TimeoutException e) {
				Logger.debug("Timeout!");
				if(locked){
					locator.release(subAccount1);
					Logger.debug("Release " + subAccountNum1);
				}
			}
		}
		try {
			long balance = 0;
			
			for(int i=0; i<Benchmark.calls; i++)
				balance += subAccount1.checkBalance();
			
			edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
			
			for(int i=0; i<Benchmark.calls; i++)
				balance += subAccount2.checkBalance();
		} finally{		
			locator.release(subAccount1);
			Logger.debug("Release " + subAccountNum1);
			locator.release(subAccount2);
			Logger.debug("Release " + subAccountNum2);
		}
		return 0;
	}
	
	public static boolean transfer(String accountNum1, String accountNum2, int amount) {
		BankAccount account1 = null;
		BankAccount account2 = null;		
		DirectoryManager locator = HyFlow.getLocator();
		while(true){
			boolean locked = false;
			try {
				account1 = (BankAccount) locator.open(accountNum1);
				locked = true;
				Logger.debug("Lock " + accountNum1);
				account2 = (BankAccount) locator.open(accountNum2);
				Logger.debug("Lock " + accountNum2);
				break;
			} catch (TimeoutException e) {
				Logger.debug("Timeout");
				if(locked){
					locator.release(account1);
					Logger.debug("Release " + accountNum1);
				}
			}
		}
		try {
			for(int i=0; i<Benchmark.calls; i++)
				account1.withdraw(amount);
			
			edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
			
			try {
				for(int i=0; i<Benchmark.calls; i++)
					account2.deposit(amount);
			}catch(TransactionException e){
				throw e;
			} catch (Throwable e) {
				e.printStackTrace();
			}
		} finally{		
			locator.release(account1);
			Logger.debug("Release " + accountNum1);
			locator.release(account2);
			Logger.debug("Release " + accountNum2);
		}
		
		return true;
	}
}
