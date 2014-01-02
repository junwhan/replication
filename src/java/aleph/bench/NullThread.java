/*
 * Aleph Toolkit
 *
 * Copyright 1997, Brown University, Providence, RI.
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
import aleph.Join;
import aleph.PE;
import aleph.RemoteThread;
import java.util.Iterator;

/** 
 * Test Null remote thread creation.
 *
 * @author  Maurice Herlihy
 * @date    November 97
 **/
public class NullThread {

  static class MyThread extends RemoteThread {
    public void run() {
    }
  }

  public static void main(String[] args) {

    MyThread thread = new MyThread();

    Join join = new Join();

    int howMany = PE.numPEs();
    int iterations = 100;
    try {
      if ( args.length > 0)
	iterations = Integer.parseInt(args[0]);
    } catch (NumberFormatException e) {
      System.err.println("usage: Null <#iterations>");
      Aleph.exit(1);
    }

    System.out.println("iterations: " + iterations);
    System.out.println("#PEs\ttime");
    for (int i = 0; i < PE.numPEs(); i++) {
      long duration = 0;
      for (int j = 0; j < iterations; j++) {
        Iterator e = PE.roundRobin();
        long start = System.currentTimeMillis();
        for (int k = 0; k < i; k++)
          thread.start((PE) e.next(), join);
        join.waitFor();
        long stop = System.currentTimeMillis();
        duration += (stop - start);
      }
      System.out.println(Integer.toString(i+1) + "\t" +
                         (double) duration / (double) iterations);
    }
  }
}
