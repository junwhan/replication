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

package aleph.desktop;

import aleph.Aleph;
import aleph.Config;
import java.awt.*;
import java.util.Iterator;
import java.util.Properties;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Display <i>about</i> message.
 **/

public class AllProperties extends JInternalFrame {

  private final static Dimension preferred = new Dimension(400, 400);

  public AllProperties () {
    super("Properties", true, false, true, true);
    Container contentPane = getContentPane();
    contentPane.setLayout(new GridLayout(2, 1, 10, 10));
    contentPane.add(new PropertyPanel("System Properties", System.getProperties()));
    contentPane.add(new PropertyPanel("Aleph Properties", Aleph.getProperties()));
    setPreferredSize(preferred);
    setBounds(0, 0, preferred.width, preferred.height);
  }

  class PropertyPanel extends JPanel {
    public PropertyPanel (String title, Properties properties) {
      super();
      setBorder(new TitledBorder(BorderFactory.createEtchedBorder(),
				 title));
      setLayout(new GridLayout(1, 1, 10, 10));
      JTextArea area = new JTextArea(Math.min(properties.size(), 20), 40);
      area.setEditable(false);
      area.setOpaque(false);
      area.setTabSize(16);

      Iterator iter = properties.keySet().iterator();
      if (iter.hasNext()) {        // first line
        String key = (String) iter.next();
        area.append(key);
        area.append("\t");
        area.append(properties.getProperty(key));
      }
      while (iter.hasNext()) {    // remaining lines
        area.append("\n");
        String key = (String) iter.next();
        area.append(key);
        area.append("\t");
        area.append(properties.getProperty(key));
      }
      add(new JScrollPane(area));
    }
  }

}


