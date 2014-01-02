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

package aleph.desktop;
import aleph.Aleph;
import aleph.Config;

import java.awt.*;
import java.awt.event.*;

import java.util.StringTokenizer;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.border.*;
import javax.accessibility.*;

/**
 * Calls Aleph from the swing console.
 *
 * @author Maurice Herlihy
 * @date   August 1999
 **/

public class Splash extends JWindow implements Runnable {

  private volatile boolean done = false;

  public Splash () {
    super();
    JPanel content = (JPanel) getContentPane();
    int width = 240;
    int height = 300;
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    int x = (screen.width - width) / 2;
    int y = (screen.height - height) / 2;
    setBounds(x, y, width, height);

    JLabel label  = new JLabel(new ImageIcon(Config.alephDirectory + Config.splashImage));

    content.setBackground(Color.white);
    JLabel copyright = new JLabel(Config.copyright, JLabel.CENTER);
    JLabel title = new JLabel(Config.banner, JLabel.CENTER);
    copyright.setFont(new Font("Sans-Serif", Font.BOLD, 12));
    content.add(title, BorderLayout.NORTH);
    content.add(label, BorderLayout.CENTER);
    content.add(copyright, BorderLayout.SOUTH);
    content.setBorder(BorderFactory.createLineBorder(Config.background, 10));
  }

  public synchronized void run() {
    setVisible(true);
    while (! done) {
      try {wait();} catch (InterruptedException e) {}
      setVisible(false);
    }
  }

  public synchronized void done () {
    done = true;
    notifyAll();
  }

  public static void main (String[] args) {
    Splash splash = new Splash();
    new Thread(splash).start();
    System.exit(0);
  }
}
