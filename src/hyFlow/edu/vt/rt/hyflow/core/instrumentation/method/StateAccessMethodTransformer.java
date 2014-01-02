package edu.vt.rt.hyflow.core.instrumentation.method;

import org.deuce.objectweb.asm.AnnotationVisitor;
import org.deuce.objectweb.asm.Attribute;
import org.deuce.objectweb.asm.Label;
import org.deuce.objectweb.asm.MethodVisitor;
import org.deuce.objectweb.asm.Opcodes;

import edu.vt.rt.hyflow.core.instrumentation.ITypeInternalName;

public class StateAccessMethodTransformer implements MethodVisitor{
	
	private MethodVisitor mv;
	private String classInternalName;
	private boolean setter;
	
	public StateAccessMethodTransformer(MethodVisitor mv, String classInternalName, boolean setter) {
		this.mv = mv;
		this.classInternalName = classInternalName;
		this.setter = setter;
	}
	
	@Override
	public void visitCode() {
		mv.visitCode();
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		if(setter){
			mv.visitVarInsn(Opcodes.ILOAD, 1);
			mv.visitFieldInsn(Opcodes.PUTFIELD, classInternalName, ITypeInternalName.$HY$_STATE, "I");
			mv.visitInsn(Opcodes.RETURN);
		}else{
			mv.visitFieldInsn(Opcodes.GETFIELD, classInternalName, ITypeInternalName.$HY$_STATE, "I");
			mv.visitInsn(Opcodes.IRETURN);
		}
	}
	
	public void visitMaxs(int maxStack, int maxLocals) {
		int max = setter ? 2 : 1;
		mv.visitMaxs(max, max);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		return mv.visitAnnotation(desc, visible);
	}

	@Override
	public AnnotationVisitor visitAnnotationDefault() {
		return mv.visitAnnotationDefault();
	}

	@Override
	public void visitAttribute(Attribute attr) {
	}

	@Override
	public void visitEnd() {
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name,
			String desc) {
	}

	@Override
	public void visitFrame(int type, int local, Object[] local2, int stack,
			Object[] stack2) {
	}

	@Override
	public void visitIincInsn(int var, int increment) {
	}

	@Override
	public void visitInsn(int opcode) {
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
	}

	@Override
	public void visitLabel(Label label) {
	}

	@Override
	public void visitLdcInsn(Object cst) {
	}

	@Override
	public void visitLineNumber(int line, Label start) {
	}

	@Override
	public void visitLocalVariable(String name, String desc, String signature,
			Label start, Label end, int index) {
	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name,
			String desc) {
	}

	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
	}

	@Override
	public AnnotationVisitor visitParameterAnnotation(int parameter,
			String desc, boolean visible) {
		return mv.visitParameterAnnotation(parameter, desc, visible);
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
	}

	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
	}

	@Override
	public void visitVarInsn(int opcode, int var) {
	}
}
