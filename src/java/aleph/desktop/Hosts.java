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

import aleph.Config;
import aleph.Aleph;
import aleph.Host;

import java.awt.*;
import java.awt.event.*;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;

/**
 * Manages Hosts
 *
 * @author Maurice Herlihy
 * @date   August 1999
 **/
public class Hosts extends JInternalFrame implements ActionListener, MouseListener {

  private JButton addButton;	// add new Host
  private JButton pingButton;   // ping Hosts
  private static  Dimension spacer = new Dimension(10,1);

  private Color background, foreground;

  // used by add and modify buttons
  private Object[] editDisplay;
  private JTextField hostField;

  // top and bottom panels
  private JPanel hostPanel;
  private JPanel hostButtons;
  private JPanel btnPanel;

  // all host buttons
  private Set buttonSet = new HashSet();
  private Options options;

  // progress bar
  private ProgressMonitor progress;
  private volatile int soFar = 0;
  private Timer timer;

  /**
   * Constructor
   * @param options user-controlled options
   **/
  public Hosts (Options theOptions) {
    super("Hosts", true, false, true, true);
    Container contentPane = getContentPane();
    contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
    this.options = theOptions;
    this.hostField = new JTextField(); // add hosts here
    this.editDisplay = new Object[] {hostField};
    // left-to-right panels
    hostPanel = new JPanel();
    btnPanel = new JPanel();
    contentPane.add(Box.createRigidArea(spacer));
    contentPane.add(hostPanel);
    contentPane.add(Box.createRigidArea(spacer));
    contentPane.add(btnPanel);
    contentPane.add(Box.createRigidArea(spacer));

    // ordem e progreso
    progress = new ProgressMonitor(this,
                                   "Searching for active hosts",
                                   "Working ...",
                                   0,
                                   Host.size());
    if (Host.size() > 1)
      progress.setMillisToDecideToPopup(0);
    timer = new Timer(500, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        SwingUtilities.invokeLater(new Update(progress));
      }
    });
    timer.start();

    // Host panel
    hostPanel.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.gridwidth = GridBagConstraints.REMAINDER;	// last in row
    c.weightx = 1.0;		// become wider
    c.weighty = 0.0;		// no higher
    c.fill = GridBagConstraints.BOTH;
    c.insets = new Insets(5,5,5,5);

    hostButtons = new JPanel();
    hostButtons.setLayout(new BoxLayout(hostButtons, BoxLayout.Y_AXIS));
    hostButtons.setBorder(new TitledBorder(BorderFactory.createEtchedBorder(),
                                           "Hosts"));

    // now fill in one button for each known host and select the live ones
    for (Iterator iter = Host.allHosts(); iter.hasNext(); ) {
      Host host = (Host) iter.next();
      HostButton hostButton = new HostButton(host);
      buttonSet.add(hostButton);
      hostButtons.add(hostButton);
      options.hosts.add(host);
      if (hostButton.isEnabled()) {
	options.selectedHosts.add(host);
	options.liveHosts.add(host);
      }
    }
    hostButtons.setToolTipText("Select which hosts you want to use.");
    hostPanel.add(hostButtons, c);

    // button panel
    btnPanel.setLayout(new GridLayout(1, 2, 10, 10));
    //    btnPanel.setMaximumSize(new Dimension(60, 100));
    //    btnPanel.setPreferredSize(new Dimension(60, 100));
    
    addButton = new JButton("Add");
    background = addButton.getBackground();
    foreground = addButton.getForeground();
    addButton.setBorder(BorderFactory.createRaisedBevelBorder());
    addButton.addMouseListener(this);
    addButton.setToolTipText("add Host to list");
    addButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        hostField.setText("");
        String hostName = (String) JOptionPane.showInputDialog(null,
                                                               "Enter host name",
                                                               "Add Host",
                                                               JOptionPane.QUESTION_MESSAGE,
                                                               null,
                                                               null,
                                                               "");
        if (hostName == null)
          return;               // didn't mean it
        Host host = null;
        try {
          host = new Host(hostName); // bogus?
        } catch (UnknownHostException x) {
          JOptionPane.showMessageDialog(null,
                                        "Cannot find host named " + hostName,
                                        "Error",
                                        JOptionPane.ERROR_MESSAGE);
          return;
        }
        if (options.hosts.contains(host)) {
          JOptionPane.showMessageDialog(null,
                                        "Duplicate host " + hostName,
                                        "Error",
                                        JOptionPane.ERROR_MESSAGE);
          return;
        }
        options.hosts.add(host); // valid host
        HostButton hostButton = new HostButton(host); // give it a button
        hostButtons.add(hostButton);
        options.hosts.add(host);
        if (hostButton.isEnabled()) {// host is live
          options.selectedHosts.add(host);
          options.liveHosts.add(host);
        }
        hostButtons.revalidate();
        hostButtons.repaint();
      }});
    btnPanel.add(addButton);

    pingButton = new JButton("Ping");
    pingButton.setBorder(BorderFactory.createRaisedBevelBorder());
    pingButton.addMouseListener(this);
    pingButton.setToolTipText("Check which hosts are currently running servers.");
    pingButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        progress = new ProgressMonitor(Hosts.this,
                                       "Searching for active hosts",
                                       "Working ...",
                                       0,
                                       Host.size());
        if (Host.size() > 1)
          progress.setMillisToDecideToPopup(0);
        soFar = 0;
        // throw out old set of live hosts
        options.liveHosts.clear();
        for (Iterator iter = buttonSet.iterator(); iter.hasNext();) {
          soFar += 1;
          if (progress.isCanceled()) {
            progress.close();
            Aleph.exit(0);
          }
          HostButton hostButton = (HostButton) iter.next();
          if (hostButton.ping())
            options.liveHosts.add(hostButton.host);
        }
        // unselect any newly-dead hosts
        options.selectedHosts.retainAll(options.liveHosts);
      }});
    btnPanel.add(pingButton);

    Dimension preferred = getPreferredSize();
    setBounds(0, 0, preferred.width, preferred.height);
  }

  public void mouseEntered (MouseEvent e) {
    JButton button = (JButton) e.getSource();
    button.setBackground(Color.magenta);
    button.setForeground(Color.white);
    repaint();
  }

  public void mouseExited (MouseEvent e) {
    JButton button = (JButton) e.getSource();
    button.setBackground(background);
    button.setForeground(foreground);
    repaint();
  }

  public void mouseClicked (MouseEvent e) {}
  public void mousePressed (MouseEvent e) {}
  public void mouseReleased (MouseEvent e) {}

  public void actionPerformed(ActionEvent e) {
  }


  /**
   * Inner class representing host button
   **/
  private class HostButton extends JRadioButton implements ActionListener {

    public Host host;
    public HostButton (Host host) {
      super(host.toString());
      buttonSet.add(this);
      this.host = host;
      soFar++;
      if (host.ping()) {	// live one
	setSelected(true);
	setEnabled(true);
	options.selectedHosts.add(host);
      } else {			// no answer
	setSelected(false);
	setEnabled(false);
      }
      addActionListener(this);
    }
    public boolean ping () {
      boolean wasEnabled = isEnabled();
      if (host.ping()) {	// live one
        if (! wasEnabled) {     // newly alive?
          setSelected(true);    // join the fun
          options.selectedHosts.add(host);
        }
	setEnabled(true);       // enable me!
        return true;
      } else {			// no answer
	setSelected(false);     // don't want you no more
	setEnabled(false);      // don't ask me again
        return false;
      }
    }
    public boolean equals (Object anObject) {
      if ((anObject != null) && (anObject instanceof HostButton)) {
	HostButton other = (HostButton) anObject;
	return (other.host.equals(this.host));
      } else {
	return false;
      }
    }
    public void actionPerformed (ActionEvent e) {
      if (isSelected())
        options.selectedHosts.add(HostButton.this.host);
      else
        options.selectedHosts.remove(HostButton.this.host);
    }
  }

  class Update implements Runnable {
    ProgressMonitor progress;
    public Update (ProgressMonitor progress) {
      this.progress = progress;
    }

    public void run() {
      if (progress.isCanceled()) {
        progress.close();
        Aleph.exit(0);
      } else {
        progress.setProgress(soFar);
        progress.setNote(soFar + " out of " + Host.size());
      }
    }
  }

}
