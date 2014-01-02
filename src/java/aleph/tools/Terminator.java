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

package aleph.tools;
import java.io.*;

/**
 * Files copied from DOS-based file systems often have spurious ^Ms as line
 * terminators, which confuses CVS and diff.  This program fixes it.
 *
 * @author Maurice Herlihy
 * @date January 1999
 **/

public class Terminator {
  public static void main (String[] args) {
    try {
      for (int i = 0; i < args.length; i++) {
        File input = new File(args[i]);
        File output = new File("Terminator.temp");
        BufferedReader reader = new BufferedReader(new FileReader(input));
        BufferedWriter writer = new BufferedWriter(new FileWriter(output));
        String line = reader.readLine();
        while (line != null) {
          writer.write(line, 0, line.length());
          writer.newLine();
          line = reader.readLine();
        }
        reader.close();
        writer.close();
        File backup = new File(args[i] + ".bak");
        boolean ok = input.renameTo(backup);
        if (!ok) {
          System.err.println("failed to create backup for " + args[i]);
          System.exit(0);
        }
        ok = output.renameTo(input);
        if (!ok) {
          System.err.println("failed to rename " + output +
                             " to " + input);
          System.exit(0);
        }
        ok = backup.delete();
        if (!ok) {
          System.err.println("failed to delete " + backup);
          System.exit(0);
        }
      }
    } catch (Exception e) {
      System.err.println(e);
      System.exit(0);
    }
  }
}
