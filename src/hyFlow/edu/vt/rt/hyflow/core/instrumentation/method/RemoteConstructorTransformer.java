package edu.vt.rt.hyflow.core.instrumentation.method;

import org.deuce.objectweb.asm.Label;
import org.deuce.objectweb.asm.MethodAdapter;
import org.deuce.objectweb.asm.MethodVisitor;
import org.deuce.objectweb.asm.Opcodes;

import edu.vt.rt.hyflow.core.instrumentation.ITypeInternalName;

public class RemoteConstructorTransformer extends MethodAdapter {

	public RemoteConstructorTransformer(MethodVisitor mv) {
		super(mv);
	}
	
	@Override
	public void visitInsn(int opcode) {
		if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW) {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, ITypeInternalName.CONTEXT_DELEGATOR, "getInstance", "()Lorg/deuce/transaction/AbstractContext;");
			mv.visitVarInsn(Opcodes.ASTORE, 3);
			mv.visitVarInsn(Opcodes.ALOAD, 3);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ITypeInternalName.ABSTRACT_CONTEXT, "getContextId", "()Ljava/lang/Long;");
			Label l5 = new Label();
			mv.visitJumpInsn(Opcodes.IFNONNULL, l5);
			mv.visitTypeInsn(Opcodes.NEW, "aleph/GlobalObject");
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, ITypeInternalName.DISTINGUISHABLE, "getId", "()Ljava/lang/Object;");
			mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "aleph/GlobalObject", "<init>", "(Ljava/lang/Object;Ljava/lang/Object;)V");
			Label l7 = new Label();
			mv.visitJumpInsn(Opcodes.GOTO, l7);
			mv.visitLabel(l5);
			mv.visitVarInsn(Opcodes.ALOAD, 3);
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ITypeInternalName.ABSTRACT_CONTEXT, "newObject", "(L" + ITypeInternalName.DISTINGUISHABLE +";)V");
			mv.visitLabel(l7);
		}
		super.visitInsn(opcode);
	}
	
	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		mv.visitMaxs(maxStack + 2, maxLocals + 2);
	}
}
