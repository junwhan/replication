package edu.vt.rt.hyflow.core.instrumentation;

import java.lang.annotation.Annotation;

import org.deuce.objectweb.asm.AnnotationVisitor;
import org.deuce.objectweb.asm.ClassWriter;
import org.deuce.objectweb.asm.MethodAdapter;
import org.deuce.objectweb.asm.MethodVisitor;
import org.deuce.objectweb.asm.Opcodes;
import org.deuce.objectweb.asm.Type;
import org.deuce.transform.Exclude;

import edu.vt.rt.hyflow.core.instrumentation.method.RemoteConstructorTransformer;
import edu.vt.rt.hyflow.core.instrumentation.method.RemoteMethodTransformer;
import edu.vt.rt.hyflow.core.instrumentation.method.StateAccessMethodTransformer;
import edu.vt.rt.hyflow.util.io.Logger;

public class HyClassTransformer extends ClassWriter{	//ClassTransformer{

	private boolean distinguishable = false;
	private boolean exclude = false;
	private String className;
	final static public String EXCLUDE_DESC = Type.getDescriptor(Exclude.class);
	final static private String ANNOTATION_NAME = Type.getInternalName(Annotation.class);
	
	public HyClassTransformer(String className) {
		super(ClassWriter.COMPUTE_MAXS);
		this.className = className;
	}
	
	@Override
	public void visit(int version, int access, String name, String signature,
			String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		for (String string : interfaces) 
			if(string.equals(ITypeInternalName.DISTINGUISHABLE)){
				Logger.debug("Transforming .... " + name);
				distinguishable = true;
				// Keep going, we may want to exclude later...
			}
			else if (string.equals(ANNOTATION_NAME)) {
				exclude = true;
				return;
			}
	}
	
	/**
	 * Checks if the class is marked as {@link Exclude @Exclude}
	 */
	@Override
	public AnnotationVisitor visitAnnotation( String desc, boolean visible) {
		exclude = exclude ? exclude : EXCLUDE_DESC.equals(desc);
		return super.visitAnnotation(desc, visible);
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature,
			String[] exceptions) {
		MethodVisitor originalMethod =  super.visitMethod(access, name, desc, signature, exceptions);

		if(!Boolean.getBoolean("instrument"))
			return originalMethod;
		
		if(!distinguishable || exclude)
			return originalMethod;
		
		if(name.equals("<init>"))
			return new RemoteConstructorTransformer(originalMethod);
		
		
		boolean stateGetter = name.equals(ITypeInternalName.GET_OBJECT_STATE);
		boolean stateSetter = name.equals(ITypeInternalName.SET_OBJECT_STATE);
		if(stateGetter || stateSetter)
			return new StateAccessMethodTransformer(originalMethod, className, stateSetter);
		
		
		Type[] args = Type.getArgumentTypes(desc);
		if(args.length>0 && args[args.length-1].equals(ITypeInternalName.CONTEXT))
			return  new RemoteMethodTransformer(originalMethod, className, name, desc);
		else
			return new MethodAdapter(originalMethod){
				@Override
				public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
					if(desc.equals(ITypeInternalName.REMOTE))
						return null;
					return super.visitAnnotation(desc, visible);
				}
			};
	}
	
	@Override
	public void visitEnd() {
		if(Boolean.getBoolean("instrument")){
			if(distinguishable && !exclude)
				super.visitField(Opcodes.ACC_PUBLIC, ITypeInternalName.$HY$_STATE, Type.INT_TYPE.getDescriptor(), null, ITypeInternalName.DEFAULT_STATE).visitEnd();
		}
		super.visitEnd();
	}
	
}