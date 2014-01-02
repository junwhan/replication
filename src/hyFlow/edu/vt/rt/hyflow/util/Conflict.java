package edu.vt.rt.hyflow.util;

public class Conflict {
	public static void main(String[] args) {
		for(float o=10; o<=120; o+=10){
			System.out.print((int)o);
			for(float n=24; n<=120; n+=24){
				double conf = (1 - n/o * Math.pow(1 - 1.0/o, n-1) - Math.pow(1 - 1.0/o, n));
				System.out.print("\t\t" +(int)(conf*100) + "%");
			}
			System.out.println();
		}
	}
}
