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

import aleph.Host;
import aleph.Aleph;
import aleph.Config;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.beans.*;

/**
 * Top-level class for managing the Aleph desktop.
 * @author Maurice Herlihy
 * @date Oct 1999
 **/

public class Desktop extends JFrame {

  private JDesktopPane desk;
  private IconPolice iconPolice = new IconPolice();
  private Options options = new Options();

  public Desktop(String[] args) {
    super(Config.banner);
    Splash splash = new Splash();
    new Thread(splash).start();
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        setVisible(false);
        dispose();
        System.exit(0);
      }
    });

    desk = new JDesktopPane();
    setContentPane(desk);

    // Install our custom desktop manager
    DesktopManager manager = new DesktopMgr();
    desk.setDesktopManager(manager);

    createMenuBar();
    loadBackgroundImage();

    Container contentPane;

    // Application window
    Application application = new Application(args, options);
    application.addVetoableChangeListener(iconPolice);
    desk.add(application, JLayeredPane.PALETTE_LAYER);  

    // hosts
    Hosts hosts = new Hosts(options);
    hosts.addVetoableChangeListener(iconPolice);
    desk.add(hosts, JLayeredPane.PALETTE_LAYER);  

    // Run-time window
    Runtime runtime = new Runtime(options);
    runtime.addVetoableChangeListener(iconPolice);
    desk.add(runtime, JLayeredPane.PALETTE_LAYER);

    //communication
    Managers cManagers = new Managers("Communication",
                                      Config.commRegistry,
                                      Config.communicationManager,
                                      "aleph.communicationManager");
    cManagers.addVetoableChangeListener(iconPolice);
    desk.add(cManagers, JLayeredPane.PALETTE_LAYER);  

    //directory
    Managers dManagers = new Managers("Directory",
                                      Config.dirRegistry,
                                      Config.directoryManager,
                                      "aleph.directoryManager");
    dManagers.addVetoableChangeListener(iconPolice);
    desk.add(dManagers, JLayeredPane.PALETTE_LAYER);  

    //event
    Managers eManagers = new Managers("Event",
                                      Config.eventRegistry,
                                      Config.eventManager,
                                      "aleph.eventManager");
    eManagers.addVetoableChangeListener(iconPolice);
    desk.add(eManagers, JLayeredPane.PALETTE_LAYER);  

    //transaction
    Managers tManagers = new Managers("Transaction",
                                      Config.transRegistry,
                                      Config.transactionManager,
                                      "aleph.transManager");
    tManagers.addVetoableChangeListener(iconPolice);
    desk.add(tManagers, JLayeredPane.PALETTE_LAYER);  

    //properties
    AllProperties properties = new AllProperties();
    properties.addVetoableChangeListener(iconPolice);
    desk.add(properties, JLayeredPane.PALETTE_LAYER);  

    splash.done();
    setSize(600, 600);  
    setVisible(true);
    try {
      cManagers.setIcon(true);
      dManagers.setIcon(true);
      eManagers.setIcon(true);
      tManagers.setIcon(true);
      hosts.setIcon(true);
      runtime.setIcon(true);
      properties.setIcon(true);
    } catch (Exception e) {
      Aleph.panic(e);
    }
  }

  protected void createMenuBar() {
    JMenuBar mb = new JMenuBar();

    JMenu fileMenu = new JMenu("File");
    fileMenu.add(new TileAction(desk)); // add tiling capability
    fileMenu.add(new ShutdownAction()); // shut down all servers
    fileMenu.add(new ExitAction()); // close down self
    mb.add(fileMenu);

    JMenu aboutMenu = new JMenu("About");
    aboutMenu.add(new AboutAction());
    mb.add(aboutMenu);
    setJMenuBar(mb);
  }

  // Here we load a background image for our desktop.
  protected void loadBackgroundImage() {
    ImageIcon icon = new ImageIcon(Config.alephDirectory + Config.screenImage);
    JLabel l = new JLabel(icon);
    l.setBounds(0,0,icon.getIconWidth(),icon.getIconHeight());

    // Place the image in the lowest possible layer so nothing
    // can ever be painted under it.
    desk.add(l, new Integer(Integer.MIN_VALUE));
  }

  class ExitAction extends AbstractAction {
    public ExitAction () {
      super("Exit");
    }
    public void actionPerformed( ActionEvent ev) {
      System.exit(0);
    }
  }

  class AboutAction extends AbstractAction {
    public AboutAction () {
      super("About");
    }
    public void actionPerformed( ActionEvent ev) {
      JTextArea area = new JTextArea(Config.about.length, 20);
      area.setEditable(false);
      area.setOpaque(false);
      area.append(Config.about[0]);
      for (int i = 1; i < Config.about.length; i++) {
        area.append("\n");
        area.append(Config.about[i]);
      }
      JOptionPane.showMessageDialog(null,
                                    area,
                                    "About Aleph",
                                    JOptionPane.INFORMATION_MESSAGE);
    }
  }

  class ShutdownAction extends AbstractAction {
    public ShutdownAction () {
      super("Shutdown");
    }
    public void actionPerformed (ActionEvent ev) {
      // do you really mean it?
      StringBuffer question = new StringBuffer("Really shut down ");
      if (options.liveHosts.size() == 1)
        question.append(" the server?");
      else if (options.liveHosts.size() == 2)
        question.append(" both servers?");
      else {
        question.append("all ");
        question.append(Integer.toString(options.liveHosts.size()));
        question.append(" servers?");
      }
          
      // ask before shutting down servers?
      if (Aleph.getConfirm() && ! options.liveHosts.isEmpty()) {
        int yeah = JOptionPane.showConfirmDialog(null,
                                                 question.toString(),
                                                 "Think",
                                                 JOptionPane.OK_CANCEL_OPTION);
        if (yeah != JOptionPane.OK_OPTION)
          return;
      }
      StringBuffer warning = new StringBuffer();
      boolean ok = true;      // so far so good
      for (Iterator iter = options.liveHosts.iterator();
           iter.hasNext();) { // enumerate live hosts
        Host host = (Host) iter.next();
        if (! host.stop()) {  // did we shut it down?
          warning.append(" "); // if not, whine about it
          warning.append(host.toString());
          ok = false;
        }
      }
      if (!ok)
        JOptionPane.showMessageDialog(null,
                                      warning,
                                      "Shutdown not confirmed",
                                      JOptionPane.WARNING_MESSAGE);
      System.exit(0);
    }
  }

  // A simple vetoable change listener that insists that there is always at
  // least one noniconified frame (just as an example of the vetoable 
  // properties).
  class  IconPolice implements VetoableChangeListener {
    public void vetoableChange(PropertyChangeEvent ev)
      throws PropertyVetoException {

      String name = ev.getPropertyName();
      if (name.equals(JInternalFrame.IS_ICON_PROPERTY)
          && (ev.getNewValue() == Boolean.TRUE)) {
        JInternalFrame[] frames = desk.getAllFrames();
        int count = frames.length;
        int nonicons = 0; // how many are not icons?
        for (int i=0; i<count; i++) {
          if (frames[i].isIcon() == false) {
            nonicons++;
          }
        }
        if (nonicons <= 1) {
          throw new PropertyVetoException("Invalid Iconification!", ev);
        }
      }
    }
  }

  // A simple test program.
  public static void main(String[] args) {
    Desktop td = new Desktop(args);
  }

}
