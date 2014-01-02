package edu.vt.rt.hyflow.core.tm.empty;

import org.deuce.reflection.UnsafeHolder;
import org.deuce.transaction.AbstractContext;
import org.deuce.transform.Exclude;

/**
 * Empty Context implementation
 *
 * @author Mohamed M. Saad
 * @since	1.0
 */
@Exclude
final public class Context extends AbstractContext{

	@Override
	public void beforeReadAccess(Object obj, long field) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onWriteAccess(Object obj, Object value, long field) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean commit() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean rollback() {
		// TODO Auto-generated method stub
		return true;
	}
	
	@Override
	public Object onReadAccess(Object obj, Object value, long field) {
		return value;
	}

	@Override
	public boolean onReadAccess(Object obj, boolean value, long field) {
		return value;
	}

	@Override
	public byte onReadAccess(Object obj, byte value, long field) {
		return value;
	}

	@Override
	public char onReadAccess(Object obj, char value, long field) {
		return value;
	}

	@Override
	public short onReadAccess(Object obj, short value, long field) {
		return value;
	}

	@Override
	public int onReadAccess(Object obj, int value, long field) {
		return value;
	}

	@Override
	public long onReadAccess(Object obj, long value, long field) {
		return value;
	}

	@Override
	public float onReadAccess(Object obj, float value, long field) {
		return value;
	}

	@Override
	public double onReadAccess(Object obj, double value, long field) {
		return value;
	}

	@Override
	public void onWriteAccess(Object obj, boolean value, long field) {
		UnsafeHolder.getUnsafe().putBoolean(obj, field, value);
	}

	@Override
	public void onWriteAccess(Object obj, byte value, long field) {
		UnsafeHolder.getUnsafe().putByte(obj, field, value);
	}

	@Override
	public void onWriteAccess(Object obj, char value, long field) {
		UnsafeHolder.getUnsafe().putChar(obj, field, value);
	}

	@Override
	public void onWriteAccess(Object obj, short value, long field) {
		UnsafeHolder.getUnsafe().putShort(obj, field, value);
	}

	@Override
	public void onWriteAccess(Object obj, int value, long field) {
		UnsafeHolder.getUnsafe().putInt(obj, field, value);
	}

	@Override
	public void onWriteAccess(Object obj, long value, long field) {
		UnsafeHolder.getUnsafe().putLong(obj, field, value);
	}

	@Override
	public void onWriteAccess(Object obj, float value, long field) {
		UnsafeHolder.getUnsafe().putFloat(obj, field, value);
	}

	@Override
	public void onWriteAccess(Object obj, double value, long field) {
		UnsafeHolder.getUnsafe().putDouble(obj, field, value);
	}

}
