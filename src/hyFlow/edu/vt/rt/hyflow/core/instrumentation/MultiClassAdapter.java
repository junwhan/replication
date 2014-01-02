package edu.vt.rt.hyflow.core.instrumentation;

import org.deuce.objectweb.asm.AnnotationVisitor;
import org.deuce.objectweb.asm.Attribute;
import org.deuce.objectweb.asm.ClassVisitor;
import org.deuce.objectweb.asm.FieldVisitor;
import org.deuce.objectweb.asm.MethodVisitor;

public class MultiClassAdapter implements ClassVisitor {
	protected ClassVisitor[] cvs;
	
	public MultiClassAdapter(ClassVisitor[] cvs) {
		this.cvs = cvs;
	}

	public void visit(int version, int access, String name, String signature,
			String superName, String[] interfaces) {
		for (ClassVisitor cv : cvs)
			cv.visit(version, access, name, signature, superName, interfaces);
	}

	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		AnnotationVisitor t = null;
		for (ClassVisitor cv : cvs)
			t = cv.visitAnnotation(desc, visible);
		return t;
	}

	public void visitAttribute(Attribute attr) {
		for (ClassVisitor cv : cvs)
			cv.visitAttribute(attr);
	}

	public void visitEnd() {
		for (ClassVisitor cv : cvs)
			cv.visitEnd();
	}

	public FieldVisitor visitField(int access, String name, String desc,
			String signature, Object value) {
		FieldVisitor t = null;
		for (ClassVisitor cv : cvs)
			cv.visitField(access, name, desc, signature, value);
		return t;
	}

	public void visitInnerClass(String name, String outerName,
			String innerName, int access) {
		for (ClassVisitor cv : cvs)
			cv.visitInnerClass(name, outerName, innerName, access);
	}

	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		MethodVisitor t = null;
		for (ClassVisitor cv : cvs)
			t = cv.visitMethod(access, name, desc, signature, exceptions);
		return t;
	}

	public void visitOuterClass(String owner, String name, String desc) {
		for (ClassVisitor cv : cvs)
			cv.visitOuterClass(owner, name, desc);
	}

	public void visitSource(String source, String debug) {
		for (ClassVisitor cv : cvs)
			cv.visitSource(source, debug);
	}
}