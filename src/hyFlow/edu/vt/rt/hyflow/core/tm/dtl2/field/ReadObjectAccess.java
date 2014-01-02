package edu.vt.rt.hyflow.core.tm.dtl2.field;

import org.deuce.transform.Exclude;

import edu.vt.rt.hyflow.core.tm.dtl2.LockTable;

/**
 * Represents a base class for field write access.  
 * @author Mohamed M. Saad
 */
@Exclude
public class ReadObjectAccess{
	protected Object reference;
	private int hash;

	public ReadObjectAccess(){}
	
	public ReadObjectAccess( Object reference){
		init(reference);
	}
	
	public void init( Object reference){
		this.reference = reference;
		this.hash = reference.hashCode();
	}

	@Override
	public boolean equals( Object obj){
		ReadObjectAccess other = (ReadObjectAccess)obj;
		return reference == other.reference;
	}

	public Object getObject(){
		return reference;
	}
	
	@Override
	final public int hashCode(){
		return hash;
	}
	
	public void clear(){
		reference = null;
	}
}
