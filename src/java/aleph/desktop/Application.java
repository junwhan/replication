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
import aleph.comm.Client;
import aleph.comm.CommunicationManager;
import aleph.dir.DirectoryManager;
import aleph.event.EventManager;
import aleph.trans.TransactionManager;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.StringTokenizer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

/**
 * Manages the application window
 *
 * @author Maurice Herlihy
 * @date   August 1999
 **/
public class Application extends JInternalFrame implements MouseListener, ActionListener {

  private final static Dimension spacer = new Dimension(10,1);
  private final static Font boldFont   = new Font("Dialog", Font.BOLD, 12);
  private final static Font italicFont = new Font("Dialog", Font.ITALIC, 12);

  private JButton goButton;	// start application
  private JButton cancelButton; // cancel running application
  private JTextField cmdField;  // command field
  private JTextArea  textArea;  // output area
  private Client client;        // aleph client
  private Options options;      // user-controlled options
  private DoneListener doneListener; // call when done

  private Font usualFont;

  private Color background, foreground;

  public Application (String[] args, Options theOptions) {
    super("Application", true, false, true, true);
    Container contentPane = getContentPane();
    contentPane.setLayout(new GridBagLayout());
    GridBagConstraints cx = new GridBagConstraints();
    cx.gridwidth = GridBagConstraints.REMAINDER;	// last in row
    cx.weightx = 1.0;		// become wider
    cx.weighty = 0.0;		// no higher
    cx.fill = GridBagConstraints.BOTH;

    GridBagConstraints cxy = new GridBagConstraints();
    cxy.gridwidth = GridBagConstraints.REMAINDER;	// last in row
    cxy.weightx = 1.0;		// become wider
    cxy.weighty = 1.0;		// and higher
    cxy.fill = GridBagConstraints.BOTH;

    GridBagConstraints c = new GridBagConstraints();

    this.options = theOptions;
    // top-to-bottom panels
    JPanel command  = new JPanel(); // command line
    command.setLayout(new GridBagLayout());

    JPanel output   = new JPanel(); // output display
    output.setLayout(new GridBagLayout());

    JPanel buttons  = new JPanel(); // controls
    buttons.setLayout(new GridBagLayout());

    contentPane.add(command, cx);
    contentPane.add(output, cxy);
    contentPane.add(buttons, cx);

    // application panel
    command.setBorder(new TitledBorder(BorderFactory.createEtchedBorder(),
                                       "Command"));

    StringBuffer text = new StringBuffer(80);
    for (int i = 0; i < args.length; i++) {
      text.append(args[i]);
      text.append(' ');
    }
    cmdField = new JTextField(text.toString(), 40);
    cmdField.addActionListener(this);

    // color in text field
    cmdField.setBackground(Config.background);
    cmdField.setForeground(Config.foreground);
    cmdField.setToolTipText("enter application's class name and arguments");
    command.add(cmdField, cx);

    // application output
    output.setBorder(new TitledBorder(BorderFactory.createEtchedBorder(),
                                      "Output"));
    textArea = new JTextArea();
    textArea.setEditable(false);
    textArea.setBackground(Config.background);
    textArea.setForeground(Config.foreground);
    textArea.setToolTipText("application's output appears here");
    usualFont = textArea.getFont();
    textArea.setFont(boldFont);
    textArea.append(Config.banner);
    textArea.append("\n");
    textArea.append(Config.copyright);
    textArea.append("\n");
    textArea.append((new Date()).toString());
    textArea.append("\n\n");
    output.add(new JScrollPane(textArea), cxy);

    // button panel
    buttons.setLayout(new GridLayout(1, 2, 10, 10));
    goButton = new JButton("Go");
    background = goButton.getBackground(); // save original colors
    foreground = goButton.getForeground();
    goButton.setBorder(BorderFactory.createRaisedBevelBorder());
    goButton.addMouseListener(this);
    goButton.addActionListener(this);
    goButton.setToolTipText("run user's application");
    buttons.add(goButton);
    cancelButton = new JButton("Cancel");
    cancelButton.setEnabled(false);
    cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
    cancelButton.addActionListener(this);
    cancelButton.addMouseListener(this);
    cancelButton.setToolTipText("cancel running application");
    buttons.add(cancelButton);

    // compute size
    Dimension preferred = getPreferredSize();
    setBounds(0, 0, preferred.width, preferred.height);
  }

  public void mouseEntered (MouseEvent e) {
    if (e.getSource().equals(goButton) && goButton.isEnabled()) {
      goButton.setBackground(Color.green);
    } else if (e.getSource().equals(cancelButton) && cancelButton.isEnabled()) {
      cancelButton.setForeground(Color.white);
      cancelButton.setBackground(Color.red);
    }
    repaint();
  }

  public void mouseExited (MouseEvent e) {
    JButton button = (JButton) e.getSource();
    button.setBackground(background);
    cancelButton.setForeground(foreground);
    button.repaint();
  }

  public void mouseClicked (MouseEvent e) {}
  public void mousePressed (MouseEvent e) {}
  public void mouseReleased (MouseEvent e) {}

  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();
    if (source.equals(goButton) || source.equals(cmdField)) { // go for it
      if (options.selectedHosts.isEmpty()) {
        Aleph.error("no active hosts");
        return;
      }
      CommunicationManager.setManager(Aleph.getProperty("aleph.communicationManager"));
      DirectoryManager.setManager(Aleph.getProperty("aleph.directoryManager"));
      EventManager.setManager(Aleph.getProperty("aleph.eventManager"));
      TransactionManager.setManager(Aleph.getProperty("aleph.transactionManager"));
      String line = cmdField.getText();
      textArea.setFont(usualFont);
      textArea.append(line);
      textArea.append("\n\n");
      StringTokenizer toke = new StringTokenizer(line);
      if (toke.countTokens() == 0) {
        JOptionPane.showMessageDialog(null,
                                      "command field is empty",
                                      "Cannot run application",
                                      JOptionPane.ERROR_MESSAGE);
        return;
      } 
      String[] args = new String[toke.countTokens()];
      int i = 0;
      while (toke.hasMoreTokens())
	args[i++] = toke.nextToken();

      // If default numPEs is zero, make one for each live host.
      int numPEs = options.numPEs;
      if (options.numPEs == 0)
        numPEs = options.liveHosts.size();

      cmdField.setEditable(false); // freeze command field
      goButton.setEnabled(false); // freeze go button
      cancelButton.setEnabled(true); // enable cancel

      // encode remaining options as properties
      Aleph.setProperty("aleph.verbosity", Integer.toString(Aleph.getVerbosity()));

      // create output streams
      OutputStream[] output = new OutputStream[numPEs];
      OutputStream out = new TextAreaOutputStream(textArea);
      for (int j = 0; j < numPEs; j++)
        output[j] = out;

      // Create and start client
      doneListener = new DoneListener();
      try {
        client = new Client(doneListener,
                            numPEs,
                            options.selectedHosts,
                            args,
                            output,
                            options.logging);
        client.start();
      } catch (Exception x) {
        Aleph.panic(x);
      }
    } else if (source.equals(cancelButton)) {
      textArea.append("Shutting down PEs\n");
      client.exit();
      ActionEvent done = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Done");
      doneListener.actionPerformed(done);
    } else {
      Aleph.panic("Unknown button: " + source);
    }
  }

  /**
   * Called when all PEs are done.
   **/
  class DoneListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      textArea.setCaretPosition(textArea.getText().length());
      textArea.setVisible(true);
      cmdField.setEditable(true); // unfreeze command field
      goButton.setEnabled(true);
      cancelButton.setEnabled(false);
    }
  }
  
  /**
   * Inner class to redirect normal output to console window
   **/
  private class TextAreaOutputStream extends OutputStream {
    JTextArea textArea;
    public TextAreaOutputStream (JTextArea textArea) {
      this.textArea = textArea;
    }

    public void write (int b) throws IOException {
      byte[] a = {(byte) b};
      write(a);
    }

    public void write (byte b[]) throws IOException {
      textArea.append(new String(b));
    }

    public void write (byte b[],
		       int off,
		       int len) throws IOException {
      textArea.append(new String(b, off, len));
    }

  }
}

