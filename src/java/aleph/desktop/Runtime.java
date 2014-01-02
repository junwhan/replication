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
import aleph.Constants;

import java.util.Properties;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;

/**
 * Manages the runtime window
 *
 * @author Maurice Herlihy
 * @date   August 1999
 **/
public class Runtime extends JInternalFrame {
  
  private final static Dimension filler    = new Dimension(10,10);
  private final static Dimension rowSize   = new Dimension(400, 70);
  private final static Dimension preferred = new Dimension(400, 450);

  private JRadioButton laconic, loquacious, bloviating;
  private Options options;

  public Runtime (Options theOptions) {
    super("Runtime", true, false, true, true);
    Container contentPane = getContentPane();
    contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
    this.options = theOptions;
    int initial = options.numPEs;
    if (initial == 0)
      options.numPEs = options.selectedHosts.size();
    Slider peSlider = new Slider(options.numPEs, Config.maxPEs);
    peSlider.setToolTipText("controls number of processing elements (PEs)");
    contentPane.add(Box.createRigidArea(filler));
    contentPane.add(peSlider);

    // verbosity panel
    contentPane.add(Box.createRigidArea(filler));
    Radio verbosity = new Radio("Verbosity Level",
                                new String[] {"laconic", "loquacious", "bloviating"},
                                Math.min(Constants.BLOVIATING,Aleph.getVerbosity()));
    verbosity.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        String command = e.getActionCommand();
        if (command.equals("laconic"))
          Aleph.setVerbosity(Constants.LACONIC);
        else if (command.equals("loquacious"))
          Aleph.setVerbosity(Constants.LOQUACIOUS);
        else if (command.equals("bloviating"))
          Aleph.setVerbosity(Constants.BLOVIATING);
        else Aleph.setVerbosity(Constants.BLOVIATING);
      }});
    verbosity.setToolTipText("How much information do you need?");
    contentPane.add(verbosity);

    // compiler panel
    final String YES = "yes";
    final String NO  = "no";
    contentPane.add(Box.createRigidArea(filler));
    Radio compiler = new Radio("Use Java Compiler?",
                               new String[] {YES, NO},
                               0);
    compiler.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        String command = e.getActionCommand();
        if (command.equals(YES)) {
          Properties properties = Aleph.getProperties();
          properties.remove("aleph.noCompiler");
        } else {
          Aleph.setProperty("aleph.noCompiler", YES);
        }
      }});
    compiler.setToolTipText("YES runs faster, but NO debugs better.");
    contentPane.add(compiler);

    // log panel
    contentPane.add(Box.createRigidArea(filler));
    Radio log = new Radio("Log Application's Output?",
                          new String[] {NO, YES},
                          0);
    log.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        String command = e.getActionCommand();
        options.logging = command.equals(YES);
      }});
    log.setToolTipText("YES will dump application output into a .log file");
    contentPane.add(log);

    // dialog panel
    contentPane.add(Box.createRigidArea(filler));
    Radio popup = new Radio("Use pop-up windows for warnings, errors, etc?",
                             new String[] {YES, NO},
                             0);
    popup.addActionListener(new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        String command = e.getActionCommand();
        Aleph.setPopup(command.equals(YES));
      }});
    popup.setToolTipText("YES use popup windows, NO use standard error");
    contentPane.add(popup);
    Dimension preferred = getPreferredSize();
    setBounds(0, 0, preferred.width, preferred.height);
  }

  /**
   * Inner class: creates a slider with label + value in upper right-hand
   * corner.  Mininum value is 1, with 32 minor ticks and 8 major ticks.
   **/
  class Slider extends JSlider {
    private TitledBorder border;

    public Slider (int initial, int max) {
      super(0, max, initial);
      border = new TitledBorder(BorderFactory.createEtchedBorder(),
                                "Number PEs: " + getValue());
      putClientProperty( "JSlider.isFilled", Boolean.TRUE );
      setBorder(border);
      setMinimumSize(rowSize);
      setMaximumSize(rowSize);
      setPreferredSize(rowSize);

      setPaintTicks(true);
      setMajorTickSpacing(max / 8);
      setMinorTickSpacing(max / 32);

      setPaintLabels( true );
      setSnapToTicks( true );
    
      addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
          int value = getValue();
          if (value == 0) {
            value = 1;
            setValue(1);
          }
          border.setTitle("Number PEs: " + value);
          options.numPEs = value;
          Slider.this.repaint();
        }
      });
    }
  }

  /**
   * Inner class: box of radio buttons
   **/
  class Radio extends JPanel {
    JRadioButton[] button;    
    public Radio (String title, String[] label, int select) {
      super();
      setMinimumSize(rowSize);
      setMaximumSize(rowSize);
      setPreferredSize(rowSize);
      setBorder(new TitledBorder(BorderFactory.createEtchedBorder(),
                                 title));
      button = new JRadioButton[label.length];
      // create buttons
      for (int i = 0; i < label.length; i++)
        button[i] = new JRadioButton(label[i]);
      // ensure mutual exclusion  
      ButtonGroup group = new ButtonGroup();
      for (int i = 0; i < button.length; i++) {
        group.add(button[i]);
        add(button[i]);
      }
      // select one
      button[select].setSelected(true);
    }
    public void addActionListener (ActionListener listener) {
      for (int i = 0; i < button.length; i++)
        button[i].addActionListener(listener);
    }
  }

}
