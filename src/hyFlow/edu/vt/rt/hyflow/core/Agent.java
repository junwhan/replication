package edu.vt.rt.hyflow.core;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import org.deuce.reflection.UnsafeHolder;
import org.deuce.transform.Exclude;
import org.deuce.transform.asm.ByteCodeVisitor;
import org.deuce.transform.asm.ClassByteCode;
import org.deuce.transform.asm.ExcludeIncludeStore;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.instrumentation.HyClassTransformer;
import edu.vt.rt.hyflow.util.io.Logger;

/**
 * A java agent to dynamically instrument distributed transactional supported classes
 * 
 * @author Mohamed M. Saad
 * @since 7.0
 */
@Exclude
public class Agent implements ClassFileTransformer {

	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer)
	throws IllegalClassFormatException {
		try {
			
			if (loader != null &&	// Don't transform classes from the boot classLoader. 
				Boolean.getBoolean("instrument"))	// Check instrumentation enabled.
				return transform(className, classfileBuffer, false).get(0).getBytecode();
		}
		catch(Exception e) {
			Logger.fetal( "Fail on class transform: " + className);
		}
		return classfileBuffer;
	}
	
	
	private List<ClassByteCode> transform(String className, byte[] classfileBuffer, boolean offline)
	throws IllegalClassFormatException {

		ArrayList<ClassByteCode> byteCodes = new ArrayList<ClassByteCode>();
		if (className.startsWith("$") || ExcludeIncludeStore.exclude(className)){
			byteCodes.add(new ClassByteCode( className, classfileBuffer));
			return byteCodes;
		}

		ByteCodeVisitor cv = new org.deuce.transform.asm.ClassTransformer( className, new HyClassTransformer(className));
		byte[] bytecode = cv.visit(classfileBuffer);
		byteCodes.add(new ClassByteCode( className, bytecode));
		return byteCodes;
	}

	public static void premain(String agentArgs, Instrumentation instrumentation) throws Exception{
		HyFlow.readConfigurations();
		UnsafeHolder.getUnsafe();
		Logger.debug("Starting HyFlow agent");
		instrumentation.addTransformer(new Agent());
	}
}
