package edu.vt.rt.hyflow.core.tm.dtl;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.deuce.transform.Exclude;

import edu.vt.rt.hyflow.core.AbstractDistinguishable;
import edu.vt.rt.hyflow.core.tm.dtl2.field.ReadObjectAccess;
import edu.vt.rt.hyflow.core.tm.dtl2.field.WriteObjectAccess;

/**
 * Represents the transaction write set.
 *  
 * @author Guy Korland
 * @since 0.7
 */
@Exclude
public class WriteSet implements Iterable<WriteObjectAccess>{
	
	final private HashMap<Integer, WriteObjectAccess> writeSet = new HashMap<Integer, WriteObjectAccess>( 50);
	final private BloomFilter bloomFilter = new BloomFilter();
	
	public void clear() {
		bloomFilter.clear();
		writeSet.clear();
	}

	public boolean isEmpty() {
		return writeSet.isEmpty();
	}

	public Iterator<WriteObjectAccess> iterator() {
		// Use the value and not the key since the key might hold old key.
		// Might happen if the same field was update more than once.
		return writeSet.values().iterator();
	}

	public void put(WriteObjectAccess write) {
		// Add to bloom filter
		bloomFilter.add( write.hashCode());

		// Add to write set
		writeSet.put( write.hashCode(), write);
	}
	
	public WriteObjectAccess contains(ReadObjectAccess read) {
		// Check if it is already included in the write set
		return bloomFilter.contains(read.hashCode()) ? writeSet.get( read.hashCode()): null;
	}
	
	public WriteObjectAccess get(Object object){
		WriteObjectAccess writeFieldAccess = writeSet.get(object.hashCode());
		if(writeFieldAccess==null)
			put(writeFieldAccess = new WriteObjectAccess(object));
		return writeFieldAccess;
	}
	
	public int size() {
		return writeSet.size();
	}

	public AbstractDistinguishable[] sortedItems() {
		Map<Object, AbstractDistinguishable> set = new HashMap<Object, AbstractDistinguishable>();
		//System.out.println("Constructing Sorted list");
		for(WriteObjectAccess field: this){
			Object obj = field.getObject();
			if(obj instanceof AbstractDistinguishable){
				AbstractDistinguishable dist = (AbstractDistinguishable) obj;
				set.put(dist.getId(), dist);
			}
		}
		//System.out.println("Writelist " + set.size());
		AbstractDistinguishable[] items = set.values().toArray(new AbstractDistinguishable[0]);
		Arrays.sort(items, new Comparator<AbstractDistinguishable>() {
			@Override
			public int compare(AbstractDistinguishable o1, AbstractDistinguishable o2) {
				return ((AbstractDistinguishable)o1).getId().hashCode() - ((AbstractDistinguishable)o2).getId().hashCode();
			}
		});
		//System.out.println("Sorted writelist " + Arrays.toString(items));
		return items;
	}
	
	/**
	 * Merges current writeset into the writeset of the parent transaction.
	 */
	public void mergeInto(WriteSet other) {
		// For every object stored in this write-set
		for (WriteObjectAccess fieldAccess : this) {
			// Get existing object from other write-set or create a new one
			WriteObjectAccess otherFieldAccess = other.get(fieldAccess.getObject());
			// Merge fields into otherFieldAccess
			fieldAccess.mergeInto(otherFieldAccess);
		}
	}
	
}
