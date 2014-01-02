package edu.vt.rt.hyflow.core.instrumentation.method;

import org.deuce.objectweb.asm.AnnotationVisitor;
import org.deuce.objectweb.asm.Label;
import org.deuce.objectweb.asm.MethodAdapter;
import org.deuce.objectweb.asm.MethodVisitor;
import org.deuce.objectweb.asm.Opcodes;
import org.deuce.objectweb.asm.Type;

import edu.vt.rt.hyflow.core.instrumentation.ITypeInternalName;
import edu.vt.rt.hyflow.util.io.Logger;

/*
 * 
 Example
 ~~~~~~
 
T foo(....,){
}

              ;;;;;
              ;;;;;  
  		 INSTRUMENTATION
              ;;;;;
            ..;;;;;.. 
             ':::::'
               ':`     
               
T foo(...., Context context){
}

T foo(....){
   if(Hy_state == REMOTE){
      return HyFlow.getRemoteCaller().execute(id, "foo", .....);
   }
      
   Context cx = Context.getInstance();
   for(int i=0; i<retries; i++){
      context.init();
      try{
         T t = foo(...., cx);
         cx.commit();
      	 return t;
      }catch(TransactionException ){
      	 cx.rollback;
      }catch(Exception e){
      	throw e;
      }
    }
   throw new TransactionException("Failed to commit the transaction in the defined retries").;
}
 
 */
public class RemoteMethodTransformer extends MethodAdapter{
	
	private boolean remote = false;
	private String classInternalName;
	private String method;
	private String desc;
	
	public RemoteMethodTransformer(MethodVisitor mv, String classInternalName, String method, String desc) {
		super(mv);
		this.classInternalName = classInternalName;
		this.method= method;
		this.desc = desc;
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		if(ITypeInternalName.REMOTE.equals(desc))
			remote = true;
		Logger.debug(method + " " + desc);
		return super.visitAnnotation(desc, visible);
	}
	
	private void log(){
		mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mv.visitLdcInsn("Log: " + method + desc + System.nanoTime());
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
	}
	
	@Override
	public void visitCode() {
		mv.visitCode();

		if(!remote)
			return;
		
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitFieldInsn(Opcodes.GETFIELD, classInternalName, ITypeInternalName.$HY$_STATE, "I");
		mv.visitInsn(ITypeInternalName.DEFAULT_STATE);
		Label l1 = new Label();
		mv.visitJumpInsn(Opcodes.IF_ICMPEQ, l1);

		Type[] types = Type.getArgumentTypes(desc);
		mv.visitIntInsn(Opcodes.BIPUSH, types.length);
		mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
		mv.visitInsn(Opcodes.DUP);
		int offset = types.length + 1;
		mv.visitVarInsn(Opcodes.ASTORE, offset);
		
		for( int i=0 ; i<types.length ;++i) {
			mv.visitVarInsn(Opcodes.ALOAD, offset);
			mv.visitIntInsn(Opcodes.BIPUSH, i);
			switch( types[i].getSort()) {
				case Type.BOOLEAN:
				case Type.BYTE:
				case Type.CHAR:
				case Type.SHORT:
				case Type.INT:
					mv.visitVarInsn(Opcodes.ILOAD, i + 1);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
					break;
				case Type.LONG:
					mv.visitVarInsn(Opcodes.LLOAD, i + 1);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
					break;
				case Type.FLOAT:
					mv.visitVarInsn(Opcodes.FLOAD, i + 1);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
					break;
				case Type.DOUBLE:
					mv.visitVarInsn(Opcodes.DLOAD, i + 1);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
					break;
				default:
					mv.visitVarInsn(Opcodes.ALOAD, i + 1);
				break;
			}
			mv.visitInsn(Opcodes.AASTORE);
		}
		
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
//		mv.visitMethodInsn(Opcodes.INVOKESTATIC, ITypeInternalName.HYFLOW, "getRemoteCaller", "(Ljava/lang/Class;)L" + ITypeInternalName.REMOTE_CALLER + ";");
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitFieldInsn(Opcodes.GETFIELD, classInternalName, "id", "Ljava/lang/String;");
		mv.visitLdcInsn(method);
		mv.visitVarInsn(Opcodes.ALOAD, offset);

//		mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, ITypeInternalName.REMOTE_CALLER, "execute", "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");
		
		Type type = Type.getReturnType(desc);
		switch( type.getSort()) {
			case Type.VOID:
				mv.visitInsn(Opcodes.POP);
				mv.visitInsn(Opcodes.RETURN);
				break;
			case Type.BOOLEAN:
				mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean");
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z");
				mv.visitInsn(Opcodes.IRETURN);
				break;
			case Type.BYTE:
				mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Byte");
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B");
				mv.visitInsn(Opcodes.IRETURN);
				break;
			case Type.CHAR:
				mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Char");
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Char", "charValue", "()C");
				mv.visitInsn(Opcodes.IRETURN);
				break;
			case Type.SHORT:
				mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Short");
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S");
				mv.visitInsn(Opcodes.IRETURN);
				break;
			case Type.INT:
				mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer");
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I");
				mv.visitInsn(Opcodes.IRETURN);
				break;
			case Type.LONG:
				mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Long");
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J");
				mv.visitInsn(Opcodes.LRETURN);
				break;
			case Type.FLOAT:
				mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Float");
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F");
				mv.visitInsn(Opcodes.FRETURN);
				break;
			case Type.DOUBLE:
				mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Double");
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D");
				mv.visitInsn(Opcodes.DRETURN);
				break;
			default:
				mv.visitTypeInsn(Opcodes.CHECKCAST, type.getInternalName());
				mv.visitInsn(Opcodes.ARETURN);
			break;
		}
		
		mv.visitLabel(l1);
		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
	}
	
	public void visitMaxs(int maxStack, int maxLocals) {
		mv.visitMaxs(remote ? maxStack : maxStack + 4, maxLocals);
	}
}
