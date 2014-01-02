package edu.vt.rt.hyflow.core.tm;

/**
 * Enumeration for supported nesting types.
 */
public enum NestingModel
{
	OPEN, CLOSED, FLAT, INTERNAL_OPEN;
	
	public static NestingModel fromString(String s) {
		final String s2 = s.toLowerCase();
		if (s2.equals("open")) 
			return OPEN;
		else if (s2.equals("closed"))
			return CLOSED;
		else
			return FLAT;
	}
}
