package edu.vt.rt.hyflow.core.instrumentation;

import org.deuce.objectweb.asm.Opcodes;
import org.deuce.objectweb.asm.Type;
import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.Context;
import org.deuce.transaction.ContextDelegator;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.transaction.Remote;

public class ITypeInternalName {
	public static final String DISTINGUISHABLE 		= Type.getInternalName(AbstractDistinguishable.class);
	public static final String HYFLOW 				= Type.getInternalName(HyFlow.class);
	public static final String REMOTE 				= Type.getDescriptor(Remote.class);
//	public static final String REMOTE_CALLER 		= Type.getInternalName(RemoteCaller.class);
	public static final String CONTEXT_DELEGATOR 	= Type.getInternalName(ContextDelegator.class);
	public static final String ABSTRACT_CONTEXT 	= Type.getInternalName(AbstractContext.class);
	public static final Type CONTEXT 				= Type.getType(Context.class);

	public static final String $HY$_STATE = "$HY$_state";
	public static final String $HY$_IMMUTABLE = "$HY$_immutable_";
	
	public static final String SET_OBJECT_STATE = "setObjectState";
	public static final String GET_OBJECT_STATE = "getObjectState";
	
	/**
	 * @see edu.vt.rt.hyflow.core.dir.IObjectState
	 */
	public static final int LOCAL_OBJECT 	= Opcodes.ICONST_0;
//	public static final int COPY_OBJECT		= Opcodes.ICONST_1;
	public static final int REMOTE_OBJECT 	= Opcodes.ICONST_2;
	public static final int DEFAULT_STATE 	= REMOTE_OBJECT;
}
