package edu.vt.rt.hyflow.benchmark.tm.loan;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.deuce.reflection.AddressUtil;
import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.Context;
import org.deuce.transaction.ContextDelegator;
import org.deuce.transaction.TransactionException;
import org.deuce.transform.Exclude;

import aleph.GlobalObject;
import aleph.dir.DirectoryManager;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.dir.IHyFlow;
import edu.vt.rt.hyflow.core.tm.control.ControlContext;
import edu.vt.rt.hyflow.core.tm.undoLog.AbstractLoggableObject;
import edu.vt.rt.hyflow.helper.Atomic;
import edu.vt.rt.hyflow.util.io.Logger;

@Exclude
public class LoanAccount
	extends AbstractLoggableObject				// Implementation specific code for UndoLog context
	implements	IHyFlow							// Implementation specific code for ControlFlowDirecotry
{
	private static final int BRANCHING = 2;
	
	private Integer amount = 0;
  	public static long amount__ADDRESS__;
  	private String id;
    public static long id__ADDRESS__;
    private $HY$_ILoanAccount $HY$_proxy;
    public static long $HY$_proxy__ADDRESS__;
    private Object $HY$_id;
    public static long $HY$_id__ADDRESS__;
    public static Object __CLASS_BASE__;
    
    {
	  	try{
	  		amount__ADDRESS__ = AddressUtil.getAddress(LoanAccount.class.getDeclaredField("amount"));
	  		id__ADDRESS__ = AddressUtil.getAddress(LoanAccount.class.getDeclaredField("id"));
	  		$HY$_proxy__ADDRESS__ = AddressUtil.getAddress(LoanAccount.class.getDeclaredField("$HY$_proxy"));
	  		$HY$_id__ADDRESS__ = AddressUtil.getAddress(LoanAccount.class.getDeclaredField("$HY$_id"));
	  	}catch (Exception e) {
	  		e.printStackTrace();
		}
  	}
    
	public LoanAccount(){}	// 	required for control flow model
	
	public LoanAccount(String id) {
		this.id = id;

		AbstractContext context = ContextDelegator.getTopInstance();
		if(context==null)
			HyFlow.getLocator().register(this); // publish it now	
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
	public void deposit(int dollars, Context __transactionContext__){
		ContextDelegator.beforeReadAccess(this, amount__ADDRESS__, __transactionContext__);
		Integer temp = (Integer)ContextDelegator.onReadAccess(this, amount, amount__ADDRESS__, __transactionContext__) + dollars;
		ContextDelegator.onWriteAccess(this, temp, amount__ADDRESS__, __transactionContext__);
	}
	
	
	public boolean withdraw(int dollars) {
		amount = amount - dollars;
		return amount >= 0;
	}
	public boolean withdraw(int dollars, Context __transactionContext__) {
		ContextDelegator.beforeReadAccess(this, amount__ADDRESS__, __transactionContext__);
		Integer temp = (Integer)ContextDelegator.onReadAccess(this, amount, amount__ADDRESS__, __transactionContext__) - dollars;
		ContextDelegator.onWriteAccess(this, temp, amount__ADDRESS__, __transactionContext__);
		ContextDelegator.beforeReadAccess(this, amount__ADDRESS__, __transactionContext__);
		return (Integer)ContextDelegator.onReadAccess(this, amount, amount__ADDRESS__, __transactionContext__) >= 0;
	}

	
	public Integer checkBalance(Context __transactionContext__) {
		if($HY$_proxy!=null){
			try {
				return $HY$_proxy.checkBalance($HY$_id, (ControlContext) __transactionContext__);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		try{
			ContextDelegator.beforeReadAccess(this, amount__ADDRESS__, __transactionContext__);
			return (Integer)ContextDelegator.onReadAccess(this, amount, amount__ADDRESS__, __transactionContext__);
		} finally{
			// commented to speed up sanity check
//			edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
		}
	}
	public Integer checkBalance() throws Throwable{
		Integer res = new Atomic<Integer>(true) {
			@Override
			public Integer atomically(AbstractDistinguishable self,
					Context __transactionContext__) {
				return ((LoanAccount)self).checkBalance(__transactionContext__);
			}
		}.execute(this);
		return res;
	}
	
	public void borrow(List<String> accountNums, int branching, boolean initiator, int amount) {
		if(!initiator)
			withdraw(amount);	// provide the loan request
		DirectoryManager locator = HyFlow.getLocator();
		accountNums = (List<String>)((LinkedList<String>)accountNums).clone();
		for(int i=0; i<branching && !accountNums.isEmpty(); i++){
			LoanAccount account = (LoanAccount) locator.open(accountNums.remove(0), "w");
			boolean last = (i==branching-1 || accountNums.isEmpty());	// is the last one?
			int loan = last ? amount : (int)(Math.random()*amount);		// randomly have a loan amount from neighbor  
			account.borrow(accountNums, branching, false, loan);		// borrow from others
			deposit(loan);	// add the loaned amount to my money
			amount -= loan;
		}
	}
	
	public void borrow(List<String> accountNums, int branching, boolean initiator, int amount, Context __transactionContext__) {
		if($HY$_proxy!=null){
			try {
				$HY$_proxy.borrow($HY$_id,(ControlContext) __transactionContext__, accountNums, branching, initiator, amount);
				return;
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		if(!initiator)
			withdraw(amount, __transactionContext__);	// provide the loan request
		DirectoryManager locator = HyFlow.getLocator();
		accountNums = (List<String>)((LinkedList<String>)accountNums).clone();
		for(int i=0; i<branching && !accountNums.isEmpty(); i++){
			LoanAccount account = (LoanAccount) locator.open((AbstractContext)__transactionContext__, accountNums.remove(0), "w");
			boolean last = (i==branching-1 || accountNums.isEmpty());	// is the last one?
			int loan = last ? amount : (int)(Math.random()*amount);		// randomly have a loan amount from neighbor  
			account.borrow(accountNums, branching, false, loan, __transactionContext__);		// borrow from others
			//account.borrow(accountNums, branching, false, loan);		// borrow from others
			deposit(loan, __transactionContext__);	// add the loaned amount to my money
			amount -= loan;
		}
	}
	
	
	public static void borrow(String id, List accountNums, int amount, Context context){
		try{
			((LoanAccount)HyFlow.getLocator().open((AbstractContext)context, id, "w")).borrow(accountNums, BRANCHING, true, amount, context);
		} finally{
			//edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
		}
	}
	
	public static void borrow(final String id, final List accountNums, final int amount) throws Throwable{
		new Atomic<Object>(true) {
			@Override
			public Object atomically(AbstractDistinguishable self,
					Context __transactionContext__) {
					borrow(id, (List<String>)((LinkedList<String>)accountNums).clone(), amount, __transactionContext__);
				return null;
			}
		}.execute(null);
		
		
	}

	public int sum(List<String> accountNums, int branching) throws Throwable {
		DirectoryManager locator = HyFlow.getLocator();
		int sum = checkBalance();
		accountNums = (List<String>)((LinkedList<String>)accountNums).clone();
		for(int i=0; i<branching && !accountNums.isEmpty(); i++){
			LoanAccount account = (LoanAccount) locator.open(accountNums.remove(0), "r");
			sum += account.sum(accountNums, branching);
		}
		return sum;
	}
	public int sum(List<String> accountNums, int branching, Context __transactionContext__) {
		if($HY$_proxy!=null){
			try {
				return $HY$_proxy.sum($HY$_id, (ControlContext) __transactionContext__, accountNums, branching);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		
		DirectoryManager locator = HyFlow.getLocator();
		int sum = checkBalance(__transactionContext__);
		accountNums = (List<String>)((LinkedList<String>)accountNums).clone();
		for(int i=0; i<branching && !accountNums.isEmpty(); i++){
			LoanAccount account = (LoanAccount) locator.open((AbstractContext)__transactionContext__, accountNums.remove(0), "r");
			sum += account.sum(accountNums, branching, __transactionContext__);
		}
		return sum;
	}

	
	public static void sum(String id, List accountNums, Context context){
		try{
			((LoanAccount)HyFlow.getLocator().open((AbstractContext)context, id, "r")).sum(accountNums, BRANCHING, context);
		} finally{
			//edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
		}
	}
	public static void sum(final String id, final List accountNums) throws Throwable{
		new Atomic<Object>(true) {
			@Override
			public Object atomically(AbstractDistinguishable self,
					Context __transactionContext__) {
				sum(id, (List<String>)((LinkedList<String>)accountNums).clone(), __transactionContext__);
				return null;
			}
		}.execute(null);
	}

	public void setRemote(Object id, String ownerIP, int ownerPort) {
		$HY$_id = id;
		try {
			$HY$_proxy = (($HY$_ILoanAccount)LocateRegistry.getRegistry(ownerIP, ownerPort).lookup(getClass().getName()));
		} catch (AccessException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			try {
				Logger.debug(Arrays.toString(LocateRegistry.getRegistry(ownerIP, ownerPort).list()));
			} catch (AccessException e1) {
				e1.printStackTrace();
			} catch (RemoteException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	@Override
	public String toString() {
		return getId() + "---" + hashCode();
	}
	
}
