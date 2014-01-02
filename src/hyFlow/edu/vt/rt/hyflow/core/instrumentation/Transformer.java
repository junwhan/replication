package edu.vt.rt.hyflow.core.instrumentation;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.deuce.transform.asm.ByteCodeVisitor;
 
public class Transformer implements ClassFileTransformer {
 
	public Transformer() {
		super();
	}
 
	public byte[] transform(ClassLoader loader, String className, Class<?> redefiningClass, ProtectionDomain domain, byte[] bytes) throws IllegalClassFormatException {
//		ByteCodeVisitor cv = new HyClassTransformer(className);
		ByteCodeVisitor cv = new ByteCodeVisitor(className, new HyClassTransformer(className));
//		ByteCodeVisitor cv = new ClassTransformer(className, new HyClassTransformer(className));
		byte[] bytecode = cv.visit(bytes);
		return bytecode;
	}
}
