package edu.vt.rt.hyflow.benchmark.tm.bank;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Arrays;

import org.deuce.reflection.AddressUtil;
import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.Context;
import org.deuce.transaction.ContextDelegator;
import org.deuce.transaction.TransactionException;
import org.deuce.transform.Exclude;
import org.deuce.transform.asm.AtomicAnnotationVisitor;

import aleph.dir.DirectoryManager;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.benchmark.Benchmark;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.dir.IHyFlow;
import edu.vt.rt.hyflow.core.tm.NestedContext;
import edu.vt.rt.hyflow.core.tm.NestingModel;
import edu.vt.rt.hyflow.core.tm.control.ControlContext;
import edu.vt.rt.hyflow.core.tm.undoLog.AbstractLoggableObject;
import edu.vt.rt.hyflow.helper.AbstractLock;
import edu.vt.rt.hyflow.helper.AbstractLockMap;
import edu.vt.rt.hyflow.helper.Atomic;
import edu.vt.rt.hyflow.util.io.Logger;

@Exclude
public class BankAccount extends AbstractLoggableObject // Implementation
														// specific code for
														// UndoLog context
		implements IHyFlow // Implementation specific code for
							// ControlFlowDirecotry
{

	private Integer amount = 0;
	private AbstractLockMap locks = null;

	public static long amount__ADDRESS__;
	public static long locks__ADDRESS__;

	private String id;
	public static long id__ADDRESS__;
	private $HY$_IBankAccount $HY$_proxy;
	public static long $HY$_proxy__ADDRESS__;
	private Object $HY$_id;
	public static long $HY$_id__ADDRESS__;
	public static Object __CLASS_BASE__;

	{
		try {
			amount__ADDRESS__ = AddressUtil.getAddress(BankAccount.class
					.getDeclaredField("amount"));
			locks__ADDRESS__ = AddressUtil.getAddress(BankAccount.class
					.getDeclaredField("locks"));
			id__ADDRESS__ = AddressUtil.getAddress(BankAccount.class
					.getDeclaredField("id"));
			$HY$_proxy__ADDRESS__ = AddressUtil.getAddress(BankAccount.class
					.getDeclaredField("$HY$_proxy"));
			$HY$_id__ADDRESS__ = AddressUtil.getAddress(BankAccount.class
					.getDeclaredField("$HY$_id"));
		} catch (Exception e) {
			e.printStackTrace(Logger.levelStream[Logger.DEBUG]);
		}
	}

	public BankAccount() {
	} // required for control flow model

	public BankAccount(String id) {
		this.id = id;
		this.locks = new AbstractLockMap(id);

		// TODO: this might break old logic. msaad needs to check this later.
		AbstractContext context = ContextDelegator.getTopInstance();
		if (context == null)
			HyFlow.getLocator().register(this);
		else
			context.newObject(this); // add it to context publish-set
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
	
	public void deposit(int dollars) {
		Logger.debug("un-transactional deposit()");
		amount = amount + dollars;
	}

	public static void deposit(final String accountNum, final int dollars) {
		try {
			new Atomic<Object>(true) {
				@Override
				public Object atomically(AbstractDistinguishable self,
						Context __transactionContext__) {
					Logger.debug("BEGIN deposit(context)" + accountNum);
					/*
					 * if($HY$_proxy!=null){ try { $HY$_proxy.deposit($HY$_id,
					 * (ControlContext) __transactionContext__, dollars); return
					 * null; } catch (RemoteException e) {
					 * e.printStackTrace(Logger.levelStream[Logger.DEBUG]); } }
					 */

					DirectoryManager locator = HyFlow.getLocator();
					BankAccount account = (BankAccount) locator
							.open(accountNum);

					if (((NestedContext)__transactionContext__).getNestingModel() == NestingModel.OPEN) {
						// Lock
						ContextDelegator.beforeReadAccess(account,
								locks__ADDRESS__, __transactionContext__);
						AbstractLockMap locks2 = (AbstractLockMap) ContextDelegator
								.onReadAccess(account, account.locks,
										locks__ADDRESS__, __transactionContext__);
						locks2 = locks2.lock(0, __transactionContext__);
						ContextDelegator.onWriteAccess(account, locks2,
								locks__ADDRESS__, __transactionContext__);
						// account.lock.lock(__transactionContext__);
	
						m_hasOnAbort = true;
						m_hasOnCommit = true;
					}

					ContextDelegator.beforeReadAccess(account,
							amount__ADDRESS__, __transactionContext__);
					Integer temp = (Integer) ContextDelegator.onReadAccess(
							account, account.amount, amount__ADDRESS__,
							__transactionContext__)
							+ dollars;
					ContextDelegator.onWriteAccess(account, temp,
							amount__ADDRESS__, __transactionContext__);
					Logger.debug("END deposit(context)");
					return null;
				}

				@Override
				public void onAbort(Context __transactionContext__) {
					Logger.debug("BEGIN onAbort deposit(context)" + accountNum);
					DirectoryManager locator = HyFlow.getLocator();
					BankAccount account = (BankAccount) locator
							.open(accountNum);

					ContextDelegator.beforeReadAccess(account,
							amount__ADDRESS__, __transactionContext__);
					Integer temp = (Integer) ContextDelegator.onReadAccess(
							account, account.amount, amount__ADDRESS__,
							__transactionContext__)
							- dollars;
					ContextDelegator.onWriteAccess(account, temp,
							amount__ADDRESS__, __transactionContext__);

					// UnLock
					ContextDelegator.beforeReadAccess(account,
							locks__ADDRESS__, __transactionContext__);
					AbstractLockMap locks2 = (AbstractLockMap) ContextDelegator
							.onReadAccess(account, account.locks,
									locks__ADDRESS__, __transactionContext__);
					locks2 = locks2.unlock(0, __transactionContext__);
					ContextDelegator.onWriteAccess(account, locks2,
							locks__ADDRESS__, __transactionContext__);
					// account.lock.unlock(__transactionContext__);
					Logger.debug("END onAbort deposit(context)");
				}

				@Override
				public void onCommit(Context __transactionContext__) {
					Logger
							.debug("BEGIN onCommit deposit(context)"
									+ accountNum);
					DirectoryManager locator = HyFlow.getLocator();
					BankAccount account = (BankAccount) locator
							.open(accountNum);
					// UnLock
					ContextDelegator.beforeReadAccess(account,
							locks__ADDRESS__, __transactionContext__);
					AbstractLockMap locks2 = (AbstractLockMap) ContextDelegator
							.onReadAccess(account, account.locks,
									locks__ADDRESS__, __transactionContext__);
					locks2 = locks2.unlock(0, __transactionContext__);
					ContextDelegator.onWriteAccess(account, locks2,
							locks__ADDRESS__, __transactionContext__);
					// account.lock.unlock(__transactionContext__);
					Logger.debug("END onCommit deposit(context)");
				}

			}.execute(null);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			// nada
		}
	}

	/*
	 * public boolean withdraw(int dollars) {
	 * Logger.debug("BEGIN/END withdraw()"); amount = amount - dollars; return
	 * amount >= 0; }
	 */
	public static boolean withdraw(final String accountNum, final int dollars) {
		boolean res = false;
		try {
			res = new Atomic<Boolean>(true) {
				@Override
				public Boolean atomically(AbstractDistinguishable self,
						Context __transactionContext__) {
					Logger.debug("BEGIN withdraw(Context)" + accountNum);
					/*
					 * if($HY$_proxy!=null){ try { return
					 * $HY$_proxy.withdraw($HY$_id, (ControlContext)
					 * __transactionContext__, dollars); } catch
					 * (RemoteException e) {
					 * e.printStackTrace(Logger.levelStream[Logger.DEBUG]); } }
					 */
					DirectoryManager locator = HyFlow.getLocator();
					BankAccount account = (BankAccount) locator
							.open(accountNum);
					
					if (((NestedContext)__transactionContext__).getNestingModel() == NestingModel.OPEN) {
						// Lock
						ContextDelegator.beforeReadAccess(account,
								locks__ADDRESS__, __transactionContext__);
						AbstractLockMap locks2 = (AbstractLockMap) ContextDelegator
								.onReadAccess(account, account.locks,
										locks__ADDRESS__, __transactionContext__);
						locks2 = locks2.lock(0, __transactionContext__);
						ContextDelegator.onWriteAccess(account, locks2,
								locks__ADDRESS__, __transactionContext__);
						// account.lock.lock(__transactionContext__);
						m_hasOnAbort = true;
						m_hasOnCommit = true;
					}

					ContextDelegator.beforeReadAccess(account,
							amount__ADDRESS__, __transactionContext__);
					Integer temp = (Integer) ContextDelegator.onReadAccess(
							account, account.amount, amount__ADDRESS__,
							__transactionContext__)
							- dollars;
					ContextDelegator.onWriteAccess(account, temp,
							amount__ADDRESS__, __transactionContext__);
					ContextDelegator.beforeReadAccess(account,
							amount__ADDRESS__, __transactionContext__);
					final boolean res = (Integer) ContextDelegator
							.onReadAccess(account, account.amount,
									amount__ADDRESS__, __transactionContext__) >= 0;
					Logger.debug("END withdraw(Context)");
					return res;
				}

				@Override
				public void onAbort(Context __transactionContext__) {
					Logger.debug("BEGIN onAbort withdraw(context) "
							+ accountNum);
					DirectoryManager locator = HyFlow.getLocator();
					BankAccount account = (BankAccount) locator
							.open(accountNum);

					ContextDelegator.beforeReadAccess(account,
							amount__ADDRESS__, __transactionContext__);
					Integer temp = (Integer) ContextDelegator.onReadAccess(
							account, account.amount, amount__ADDRESS__,
							__transactionContext__)
							+ dollars;
					ContextDelegator.onWriteAccess(account, temp,
							amount__ADDRESS__, __transactionContext__);
					// UnLock
					ContextDelegator.beforeReadAccess(account,
							locks__ADDRESS__, __transactionContext__);
					AbstractLockMap locks2 = (AbstractLockMap) ContextDelegator
							.onReadAccess(account, account.locks,
									locks__ADDRESS__, __transactionContext__);
					locks2 = locks2.unlock(0, __transactionContext__);
					ContextDelegator.onWriteAccess(account, locks2,
							locks__ADDRESS__, __transactionContext__);
					// account.lock.unlock(__transactionContext__);
					Logger.debug("END onAbort withdraw(context)");
				}

				@Override
				public void onCommit(Context __transactionContext__) {
					Logger.debug("BEGIN onCommit withdraw(context)"
							+ accountNum);
					DirectoryManager locator = HyFlow.getLocator();
					BankAccount account = (BankAccount) locator
							.open(accountNum);
					// UnLock
					ContextDelegator.beforeReadAccess(account,
							locks__ADDRESS__, __transactionContext__);
					AbstractLockMap locks2 = (AbstractLockMap) ContextDelegator
							.onReadAccess(account, account.locks,
									locks__ADDRESS__, __transactionContext__);
					locks2 = locks2.unlock(0, __transactionContext__);
					ContextDelegator.onWriteAccess(account, locks2,
							locks__ADDRESS__, __transactionContext__);
					// account.lock.unlock(__transactionContext__);
					Logger.debug("END onCommit withdraw(context)");
				}

			}.execute(null);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			// nada
		}
		return res;
	}

	/*
	 * public Integer checkBalance() throws Throwable{
	 * Logger.debug("BEGIN/END checkBalance()"); return amount; }
	 */
	public static long checkBalance(final String accountNum) {
		long res = 0;
		try {
			res = new Atomic<Integer>() {
				@Override
				public Integer atomically(AbstractDistinguishable self,
						Context __transactionContext__) {
					Logger.debug("BEGIN checkBalance(Context)");
					/*
					 * if($HY$_proxy!=null){ try { return
					 * $HY$_proxy.checkBalance($HY$_id, (ControlContext)
					 * __transactionContext__); } catch (RemoteException e) {
					 * e.printStackTrace(Logger.levelStream[Logger.DEBUG]); } }
					 */
					DirectoryManager locator = HyFlow.getLocator();
					BankAccount account = (BankAccount) locator
							.open(accountNum);
					ContextDelegator.beforeReadAccess(account,
							amount__ADDRESS__, __transactionContext__);
					final Integer res = (Integer) ContextDelegator
							.onReadAccess(account, account.amount,
									amount__ADDRESS__, __transactionContext__);
					Logger.debug("END checkBalance(Context)");
					return res;
				}
			}.execute(null);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			// nada
		}
		return res;
	}
	
	public static ThreadGroup checkBalanceSPN(final String accountNum) {
		long res = 0;
		try {
			res = new Atomic<Integer>() {
				@Override
				public Integer atomically(AbstractDistinguishable self,
						Context __transactionContext__) {
					Logger.debug("BEGIN checkBalance(Context)");
					/*
					 * if($HY$_proxy!=null){ try { return
					 * $HY$_proxy.checkBalance($HY$_id, (ControlContext)
					 * __transactionContext__); } catch (RemoteException e) {
					 * e.printStackTrace(Logger.levelStream[Logger.DEBUG]); } }
					 */
					DirectoryManager locator = HyFlow.getLocator();
					BankAccount account = (BankAccount) locator
							.open(accountNum);
					ContextDelegator.beforeReadAccess(account,
							amount__ADDRESS__, __transactionContext__);
					final Integer res = (Integer) ContextDelegator
							.onReadAccess(account, account.amount,
									amount__ADDRESS__, __transactionContext__);
					Logger.debug("END checkBalance(Context)");
					return res;
				}
			}.execute(null);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			// nada
		}
		//return res;
		return null;
	}
	public static long totalBalance(final Object... ids) {
		Long res = null;
		try {
			res = new Atomic<Long>() {
				@Override
				public Long atomically(AbstractDistinguishable self,
						Context __transactionContext__) {
					//Logger.debug("BEGIN totalBalance(Context)");
					try {
						// DirectoryManager locator = HyFlow.getLocator();
						// BankAccount account1 = (BankAccount)
						// locator.open(accountNum1);
						// ContextDelegator.beforeReadAccess(account1, 0,
						// __transactionContext__);
						// BankAccount account2 = (BankAccount)
						// locator.open(accountNum2);

						long balance = 0;
						Thread t;
						for (int i = 0; i < Benchmark.calls; i++){
							balance += BankAccount.checkBalance(String.valueOf(ids[i]));
							//Logger.debug("BEGIN totalBalance 1 (Context)");
							//t = new Thread(BankAccount.checkBalanceSPN(accountNum1), "SPN");
							//t.start();
						}
						try {
							for (int i = 0; i < Benchmark.calls; i++){
								//Logger.debug("BEGIN totalBalance 2 (Context)");
								//t = new Thread(BankAccount.checkBalanceSPN(accountNum2), "SPN");
								//t.start();
								balance += BankAccount.checkBalance(String.valueOf(ids[i]));
							}
							
						} catch (TransactionException e) {
							throw e;
						} catch (Throwable e) {
							e.printStackTrace(Logger.levelStream[Logger.DEBUG]);
						}
						Logger.debug("END totalBalance(Context)");
						return balance;
					} finally {
						edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
					}
				}
			}.execute(null);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			// nada
		}
		return res;
	}

	public static void transfer(final Object... ids) {
		try {
			new Atomic<Object>() {
				@Override
				public Object atomically(AbstractDistinguishable self,
						Context __transactionContext__) {
					Logger.debug("BEGIN transfer(Context)");
					try {
						/*
						 * DirectoryManager locator = HyFlow.getLocator();
						 * BankAccount account1 = (BankAccount)
						 * locator.open(accountNum1);
						 * ContextDelegator.beforeReadAccess(account1, 0,
						 * __transactionContext__); BankAccount account2 =
						 * (BankAccount) locator.open(accountNum2);
						 */
						final int amount = 10;
						for (int i = 0; i < Benchmark.calls; i++)
							BankAccount.withdraw(String.valueOf(ids[i]), amount);

						try {
							for (int i = 0; i < Benchmark.calls; i++)
								BankAccount.deposit(String.valueOf(ids[i]), amount);
						} catch (TransactionException e) {
							throw e;
						} catch (Throwable e) {
							e.printStackTrace(Logger.levelStream[Logger.DEBUG]);
						}
					} finally {
						edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
					}
					Logger.debug("END transfer(Context)");
					return null;
				}
			}.execute(null);
		} catch (TransactionException e) {
			throw e;
		} catch (Throwable e) {
			// nada
		}
	}

	@Override
	public void setRemote(Object id, String ownerIP, int ownerPort) {
		$HY$_id = id;
		try {
			$HY$_proxy = (($HY$_IBankAccount) LocateRegistry.getRegistry(
					ownerIP, ownerPort).lookup(getClass().getName()));
		} catch (AccessException e) {
			e.printStackTrace(Logger.levelStream[Logger.DEBUG]);
		} catch (RemoteException e) {
			e.printStackTrace(Logger.levelStream[Logger.DEBUG]);
		} catch (NotBoundException e) {
			e.printStackTrace(Logger.levelStream[Logger.DEBUG]);
		} catch (Exception e) {
			try {
				Logger.debug(Arrays.toString(LocateRegistry.getRegistry(
						ownerIP, ownerPort).list()));
			} catch (AccessException e1) {
				e1.printStackTrace(Logger.levelStream[Logger.DEBUG]);
			} catch (RemoteException e1) {
				e1.printStackTrace(Logger.levelStream[Logger.DEBUG]);
			}
		}
	}

	@Override
	public String toString() {
		return getId() + "---" + hashCode();
	}
}
