/*
 * Aleph Toolkit
 *
 * Copyright 1999, Brown University, Providence, RI.
 * 
 *                         All Rights Reserved
 * 
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose other than its incorporation into a
 * commercial product is hereby granted without fee, provided that the
 * above copyright notice appear in all copies and that both that
 * copyright notice and this permission notice appear in supporting
 * documentation, and that the name of Brown University not be used in
 * advertising or publicity pertaining to distribution of the software
 * without specific, written prior permission.
 * 
 * BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE,
 * INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR ANY
 * PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY BE LIABLE FOR
 * ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package aleph.bench;
import aleph.Aleph;
import aleph.AlephException;
import aleph.GlobalObject;
import aleph.Join;
import aleph.PE;
import aleph.PEGroup;
import aleph.RemoteThread;
import java.util.Iterator;

/** 
 * Simple shared counter benchmark.
 *
 * @author  Maurice Herlihy
 * @date    January 1999
 **/
public class Counter {

  static class CounterThread extends RemoteThread {
    GlobalObject object;
    CounterThread(GlobalObject object) {
      this.object = object;
    }
    public void run() {
      try{
        SharedCounter shared = (SharedCounter) object.open("w");
        shared.value++;
        System.out.println(shared.value);
        object.release();
      } catch (AlephException e) {}
    }
  }

  public static void main(String[] args) {
	  new PEGroup("hell");
	  
	SharedCounter counter = new SharedCounter();
    GlobalObject object = new GlobalObject(counter);
    CounterThread thread = new CounterThread(object);

    Join join = new Join();

    int howMany = PE.numPEs();
    int iterations = 100;
    try {
      if ( args.length > 0)
        iterations = Integer.parseInt(args[0]);
    } catch (NumberFormatException e) {
      System.err.println("usage: Counter <#iterations>");
      Aleph.exit(1);
    }

    System.out.println("Counter iterations: " + iterations);
    System.out.println("#PEs\ttime");
    for (int i = 0; i < PE.numPEs(); i++) {
      long duration = 0;
      for (int j = 0; j < iterations; j++) {
        Iterator e = PE.roundRobin();
        long start = System.currentTimeMillis();
        for (int k = 0; k <= i; k++)
          thread.start((PE) e.next(), join);
        join.waitFor();
        long stop = System.currentTimeMillis();
        duration += (stop - start);
      }
      System.out.println(Integer.toString(i+1) + "\t" +
			 (double) duration / (double) iterations);
      System.out.flush();	// impatient?
    }
    
    System.out.println(((SharedCounter)object.open("r")).value);
  }
  public static class SharedCounter implements java.io.Serializable {
    public long value;
  }
}

