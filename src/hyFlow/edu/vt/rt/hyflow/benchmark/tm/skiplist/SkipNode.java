package edu.vt.rt.hyflow.benchmark.tm.skiplist;

import java.util.ArrayList;

import org.deuce.reflection.AddressUtil;
import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.Context;
import org.deuce.transaction.ContextDelegator;

import edu.vt.rt.hyflow.HyFlow;
import edu.vt.rt.hyflow.benchmark.tm.bank.$HY$_IBankAccount;
import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.util.io.Logger;

public class SkipNode<T extends Comparable<? super T>> extends AbstractDistinguishable {
	private String id;
	
	private String next[] = null;
	private T value = null;
	private Integer level;
	
	public static long next__ADDRESS__;
	public static long value__ADDRESS__;
	public static long level__ADDRESS__;
	public static long id__ADDRESS__;
	private $HY$_IBankAccount $HY$_proxy;
	public static long $HY$_proxy__ADDRESS__;
	private Object $HY$_id;
	public static long $HY$_id__ADDRESS__;
	{
		try {
			next__ADDRESS__ = AddressUtil.getAddress(SkipNode.class
					.getDeclaredField("next"));
			value__ADDRESS__ = AddressUtil.getAddress(SkipNode.class
					.getDeclaredField("value"));
			level__ADDRESS__ = AddressUtil.getAddress(SkipNode.class
					.getDeclaredField("level"));
			id__ADDRESS__ = AddressUtil.getAddress(SkipNode.class
					.getDeclaredField("id"));
			$HY$_proxy__ADDRESS__ = AddressUtil.getAddress(SkipNode.class
					.getDeclaredField("$HY$_proxy"));
			$HY$_id__ADDRESS__ = AddressUtil.getAddress(SkipNode.class
					.getDeclaredField("$HY$_id"));
		} catch (Exception e) {
			e.printStackTrace(Logger.levelStream[Logger.DEBUG]);
		}
	}
	
	public SkipNode(String id, int level, T value, int max_pointers) {
		this.id = id;
		this.level = level;
		this.value = value;
		this.next = new String[max_pointers];
		
		AbstractContext context = ContextDelegator.getTopInstance();
		if (context == null)
			HyFlow.getLocator().register(this);
		else
			context.newObject(this); // add it to context publish-set
	}
	
	public SkipNode(String id, int level, T value) {
		this(id, level, value, level+1);
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
	public String get_next(int level, Context __transactionContext__) {
		ContextDelegator.beforeReadAccess(this, next__ADDRESS__, __transactionContext__);
		final String[] old_next = (String[]) ContextDelegator.onReadAccess(this, this.next, next__ADDRESS__, __transactionContext__);
		Logger.debug("SkipNode["+getId()+"].get_next(level="+level+") = ...");
		Logger.debug("SkipNode["+getId()+"].get_next(level="+level+") = "+old_next[level]);
		return old_next[level];
	}
	
	public void set_next(int level, String nxtval, Context __transactionContext__) {
		ContextDelegator.beforeReadAccess(this, next__ADDRESS__, __transactionContext__);
		final String[] old_next = (String[]) ContextDelegator.onReadAccess(this, this.next, next__ADDRESS__, __transactionContext__);
		String[] new_next = (String[])old_next.clone();
		Logger.debug("SkipNode["+getId()+"].set_next(level="+level+", nxtval="+nxtval+")");
		new_next[level] = nxtval;
		ContextDelegator.onWriteAccess(this, new_next, next__ADDRESS__, __transactionContext__);
		Logger.debug("SkipNode["+getId()+"].set_next(level="+level+", nxtval="+nxtval+")");
	}
	
	@SuppressWarnings("unchecked")
	public T get_value(Context __transactionContext__) {
		ContextDelegator.beforeReadAccess(this, value__ADDRESS__, __transactionContext__);
		final T old_value = (T) ContextDelegator.onReadAccess(this, this.value, value__ADDRESS__, __transactionContext__);
		Logger.debug("SkipNode["+getId()+"].get_value() = "+old_value);
		return old_value;
	}
	
	public void set_value(T newval, Context __transactionContext__) {
		ContextDelegator.onWriteAccess(this, newval, value__ADDRESS__, __transactionContext__);
	}
	
	public int get_level(Context __transactionContext__) {
		ContextDelegator.beforeReadAccess(this, level__ADDRESS__, __transactionContext__);
		final Integer old_level = (Integer) ContextDelegator.onReadAccess(this, this.level, level__ADDRESS__, __transactionContext__);
		Logger.debug("SkipNode["+getId()+"].get_level() = "+old_level);
		return old_level;
	}
	
	public void set_level(int newlevel, Context __transactionContext__) {
		Logger.debug("SkipNode["+getId()+"].set_level( newlevel="+newlevel+" )");
		ContextDelegator.onWriteAccess(this, newlevel, level__ADDRESS__, __transactionContext__);
	}

	public void print_debug_info(Context transactionContext) {
		StringBuilder sb = new StringBuilder();
		sb.append("SkipNode.print_debug_info: Node=");
		sb.append(getId());
		sb.append(", value=");
		sb.append(get_value(transactionContext));
		sb.append(", level=");
		int level = get_level(transactionContext);
		sb.append(level);
		sb.append(". Successors:");
		for (int i=0; i<level; i++) {
			sb.append(i);
			sb.append("=");
			sb.append(get_next(i, transactionContext));
			if (i!=level-1)
				sb.append(", ");
		}
		Logger.debug(sb.toString());
	}
}
