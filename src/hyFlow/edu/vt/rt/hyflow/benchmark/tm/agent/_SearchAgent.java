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

import org.deuce.Atomic;
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
import edu.vt.rt.hyflow.transaction.Remote;
import edu.vt.rt.hyflow.util.io.Logger;

@Exclude
public class _SearchAgent 
		extends AbstractDistinguishable
{

	static Set<String> trackers = new HashSet<String>();
  	private String id;
    private Object info;
	
	public static boolean addTracker(String id){
		return trackers.add(id);
	}
	
	
	public _SearchAgent(String id) {
		this.id = id;
		trackers.add(id);
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
	
	@Remote
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
	
	@Remote
	public URI find(String keyword) throws Throwable {
		URI uri = (URI)info;
		
		File dir = new File("/home/users/msaad/");
		for (File file : dir.listFiles())
			if (match(file, keyword))
				return file.toURI();
		return null;
	}
	
	@Atomic
	public static Object search(String keyword) throws Throwable {
		for (String tracker : trackers) {
			_SearchAgent agent = (_SearchAgent) HyFlow.getLocator().open(tracker);
			URI url = agent.find(keyword);
			if (url != null)
				return agent.transfer(url);
		}
		return null;
	}
	
	@Atomic
	public static boolean exists(String keyword) throws Throwable {
		for (String tracker : trackers) {
			_SearchAgent agent = (_SearchAgent) HyFlow.getLocator().open(tracker);
			URI url = agent.find(keyword);
			if (url != null)
				return true;
		}
		return false;
	}
}
