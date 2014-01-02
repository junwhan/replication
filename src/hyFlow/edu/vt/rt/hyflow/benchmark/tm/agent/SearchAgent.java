package edu.vt.rt.hyflow.benchmark.tm.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URI;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.deuce.reflection.AddressUtil;
import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.Context;
import org.deuce.transaction.ContextDelegator;
import org.deuce.transaction.TransactionException;
import org.deuce.transform.Exclude;

import aleph.GlobalObject;
import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.dir.IHyFlow;
import edu.vt.rt.hyflow.core.tm.control.ControlContext;
import edu.vt.rt.hyflow.core.tm.undoLog.AbstractLoggableObject;
import edu.vt.rt.hyflow.util.io.Logger;

@Exclude
public class SearchAgent extends AbstractLoggableObject // Implementation
															// specific code for
															// UndoLog context
		implements IHyFlow // Implementation specific code for
												// ControlFlowDirecotry
{

	static Set<String> trackers = new HashSet<String>();
  	private String id;
    public static long id__ADDRESS__;
    private Object info;
    public static long info__ADDRESS__;
    private $HY$_ISearchAgent $HY$_proxy;
    public static long $HY$_proxy__ADDRESS__;
    private Object $HY$_id;
    public static long $HY$_id__ADDRESS__;
    public static Object __CLASS_BASE__;

    {
	  	try{
	  		id__ADDRESS__ = AddressUtil.getAddress(SearchAgent.class.getDeclaredField("id"));
	  		info__ADDRESS__ = AddressUtil.getAddress(SearchAgent.class.getDeclaredField("info"));
	  		$HY$_proxy__ADDRESS__ = AddressUtil.getAddress(SearchAgent.class.getDeclaredField("$HY$_proxy"));
	  		$HY$_id__ADDRESS__ = AddressUtil.getAddress(SearchAgent.class.getDeclaredField("$HY$_id"));
	  	}catch (Exception e) {
	  		e.printStackTrace();
		}
  	}
	
	public static boolean addTracker(String id){
		return trackers.add(id);
	}
	
	public SearchAgent() {}	// 	required for control flow model
	
	public SearchAgent(String id) {
		this.id = id;
		trackers.add(id);
		

		AbstractContext context = ContextDelegator.getInstance();
		if(context.getContextId()==null)
			HyFlow.getLocator().register(this); // publish it now	
		else
			context.newObject(this);	// add it to context publish-set 
	}

	@Override
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
	
	
	private boolean match(File file, String keyword, Context __transactionContext__) {
		if(file.isDirectory())
			return false;
		Scanner scanner = null;
		try {
			scanner = new Scanner(new FileInputStream(file));
			while (scanner.hasNextLine())
				if (scanner.nextLine().indexOf(keyword) > 0)
					return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if(scanner!=null)
				scanner.close();
		}
		return false;
	}
	private boolean match(File file, String keyword) throws Throwable{
		if(file.isDirectory())
			return false;
		Scanner scanner = null;
		try {
			scanner = new Scanner(new FileInputStream(file));
			while (scanner.hasNextLine())
				if (scanner.nextLine().indexOf(keyword) > 0)
					return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if(scanner!=null)
				scanner.close();
		}
		return false;
	}
	
	

	public Object transfer(URI name) {
		info = name;
		
		Scanner scanner = null;
		StringBuffer buffer = new StringBuffer();
		try {
			scanner = new Scanner(new FileInputStream(new File(name)));
			while (scanner.hasNextLine())
				buffer.append(scanner.nextLine());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally{
			if(scanner!=null)
				scanner.close();
		}
		return buffer;
	}
	public Object transfer(URI name, Context __transactionContext__) {
		if($HY$_proxy!=null){
			try {
				return $HY$_proxy.transfer($HY$_id, (ControlContext) __transactionContext__, name);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		
		ContextDelegator.onWriteAccess(this, name, info__ADDRESS__, __transactionContext__);
		
		Scanner scanner = null;
		StringBuffer buffer = new StringBuffer();
		try {
			scanner = new Scanner(new FileInputStream(new File(name)));
			while (scanner.hasNextLine())
				buffer.append(scanner.nextLine());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally{
			if(scanner!=null)
				scanner.close();
		}
		return buffer;
	}
	
	
	
	public URI find(String keyword) throws Throwable {
		URI uri = (URI)info;
		
		File dir = new File("/home/users/msaad/");
		for (File file : dir.listFiles())
			if (match(file, keyword))
				return file.toURI();
		return null;
	}
	public URI find(String keyword, Context __transactionContext__) {
		if($HY$_proxy!=null){
			try {
				return $HY$_proxy.find($HY$_id, (ControlContext) __transactionContext__, keyword);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		
		ContextDelegator.beforeReadAccess(this, info__ADDRESS__, __transactionContext__);
		URI uri = (URI)ContextDelegator.onReadAccess(this, info, info__ADDRESS__, __transactionContext__);
		File dir = new File("/home/users/msaad/");
		for (File file : dir.listFiles())
			if (match(file, keyword, __transactionContext__))
				return file.toURI();
		return null;
	}
	
	
	
	public static Object search(String keyword, Context __transactionContext__) {
		try{
			for (String tracker : trackers) {
				SearchAgent agent = (SearchAgent) HyFlow.getLocator().open(tracker);
				URI url = agent.find(keyword, __transactionContext__);
				if (url != null)
					return agent.transfer(url, __transactionContext__);
			}
			return null;
		} finally{
			edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
		}
	}
	public static Object search(String keyword) throws Throwable{
		Throwable throwable = null;
		Context context = ContextDelegator.getInstance();
		boolean commit = true;
		Object result = null;
		for (int i = 0x7fffffff; i > 0; i--) {
			context.init(2);
			try {
				result = search(keyword, context);
			} catch (TransactionException ex) {
				commit = false;
			} catch (Throwable ex) {
				throwable = ex;
			}
			if (commit) {
				if (context.commit()) {
					if (throwable != null) {
						throw throwable;
					} else {
						return result;
					}
				}
			} else {
				context.rollback();
				commit = true;
			}
		}
		throw new TransactionException("Failed to commit the transaction in the defined retries.");
	}
	
	
	
	public static boolean exists(String keyword, Context __transactionContext__) {
		try {
			for (String tracker : trackers) {
				SearchAgent agent = (SearchAgent) HyFlow.getLocator().open(tracker);
				URI url = agent.find(keyword, __transactionContext__);
				if (url != null)
					return true;
			}
			return false;
		} finally{
			edu.vt.rt.hyflow.benchmark.Benchmark.processingDelay();
		}
	}
	public static boolean exists(String keyword) throws Throwable{
		Throwable throwable = null;
		Context context = ContextDelegator.getInstance();
		boolean commit = true;
		boolean result = false;
		for (int i = 0x7fffffff; i > 0; i--) {
			context.init(3);
			try {
				result = exists(keyword, context);
			} catch (TransactionException ex) {
				commit = false;
			} catch (Throwable ex) {
				throwable = ex;
			}
			if (commit) {
				if (context.commit()) {
					if (throwable != null) {
						throw throwable;
					} else {
						return result;
					}
				}
			} else {
				context.rollback();
				commit = true;
			}
		}
		throw new TransactionException("Failed to commit the transaction in the defined retries.");
	}

	
	
	@Override
	public void setRemote(Object id, String ownerIP, int ownerPort) {
		$HY$_id = id;
		try {
			$HY$_proxy = (($HY$_ISearchAgent)LocateRegistry.getRegistry(ownerIP, ownerPort).lookup(getClass().getName()));
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
}
