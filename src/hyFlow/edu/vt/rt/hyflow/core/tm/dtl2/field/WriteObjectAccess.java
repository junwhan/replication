package edu.vt.rt.hyflow.core.tm.dtl2.field;

import java.util.HashMap;
import java.util.Map.Entry;

import org.deuce.reflection.UnsafeHolder;
import org.deuce.transform.Exclude;

/**
 * Represents a base class for field write access.  
 * @author Mohamed M. Saad
 */
@Exclude
public class WriteObjectAccess extends ReadObjectAccess{

	private HashMap<Long, Object> values = new HashMap<Long, Object>();
	
	public WriteObjectAccess(Object reference) {
		super(reference);
	}

	/**
	 * Commits the value in memory.
	 */
	public void put(){
		for (Entry<Long, Object> field : values.entrySet()) {
			UnsafeHolder.getUnsafe().putObject(reference, field.getKey(), field.getValue());
		}
	}

	public void set(long field, Object value) {
		values.put(field, value);
	}

	public Object getValue(long field) {
		return values.get(field);
	}
	
	/**
	 * Merges fields.
	 */
	public void mergeInto(WriteObjectAccess other) {
		// For every field
		for (Entry<Long, Object> field : values.entrySet()) {
			// Insert it in the other object, possibly overwriting old values.
			other.set(field.getKey(), field.getValue());
		}
	}
}
