package org.deuce.transform.asm;

import java.util.HashSet;

import org.deuce.transform.util.IgnoreTree;

/**
 * Holds the include/exclude information for the classes to instrument.
 *  
 * @author guy
 * @since 1.1
 */
public class ExcludeIncludeStore {

	final private IgnoreTree excludeTree;
	final private IgnoreTree includeTree;
	final private HashSet<String> excludeClass = new HashSet<String>();
	
	final private static ExcludeIncludeStore excludeIncludeStore = new ExcludeIncludeStore();
	static{
		excludeIncludeStore.excludeClass.add("java/lang/Object");
		excludeIncludeStore.excludeClass.add("java/lang/Thread");
		excludeIncludeStore.excludeClass.add("java/lang/Throwable");
		excludeIncludeStore.excludeClass.add("edu/vt/rt/hyflow/HyFlow");
		excludeIncludeStore.excludeClass.add("org/deuce/transaction/ContextDelegator");
		
	}

	
	private ExcludeIncludeStore(){

		String property = System.getProperty("org.deuce.exclude");
		if( property == null)
			property = "java.*,sun.*,org.eclipse.*,org.junit.*,junit.*,aleph.*";
		excludeTree = new IgnoreTree( property);

		property = System.getProperty("org.deuce.include");
		if( property == null)
			property = "";
		includeTree = new IgnoreTree( property);
	}
	
	public static boolean exclude(String className){
		if(excludeIncludeStore.excludeClass.contains(className))
			return true;
		return excludeIncludeStore.excludeTree.contains(className) && !excludeIncludeStore.includeTree.contains(className);
	} 
	
}
