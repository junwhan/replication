package aleph.desktop;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class LookFeel extends JMenu {

  public LookFeel(Frame frame) {
    super("Look&Feel");

    LnFListener listener = new LnFListener(frame);

    JRadioButtonMenuItem metal = new
      JRadioButtonMenuItem("Metal");
    metal.addActionListener(listener);

    JRadioButtonMenuItem motif = new
      JRadioButtonMenuItem("Motif");
    motif.addActionListener(listener);

    JRadioButtonMenuItem windows = new
      JRadioButtonMenuItem("Windows");
    windows.addActionListener(listener);

    ButtonGroup group = new ButtonGroup();
    group.add(metal);
    group.add(motif);
    group.add(windows);

    add(metal);
    add(motif);
    add(windows);

  }

  private class LnFListener implements ActionListener {
    Frame frame;

    public LnFListener(Frame f) {
      frame = f;
    }

    public void actionPerformed(ActionEvent e) {
      String lnfName = null;

      if (e.getActionCommand().equals("Metal")) {
        lnfName = "javax.swing.plaf.metal.MetalLookAndFeel";
      } else if (e.getActionCommand().equals("Motif")) {
        lnfName = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
      } else {
        lnfName = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
      }

      try {
        UIManager.setLookAndFeel(lnfName);
        SwingUtilities.updateComponentTreeUI(frame);
      }
      catch (UnsupportedLookAndFeelException ex1) {
        System.err.println("Unsupported LookAndFeel: " + lnfName);
      }
      catch (ClassNotFoundException ex2) {
        System.err.println("LookAndFeel class not found: " + lnfName);
      }
      catch (InstantiationException ex3) {
        System.err.println("Could not load LookAndFeel: " + lnfName);
      }
      catch (IllegalAccessException ex4) {
        System.err.println("Cannot use LookAndFeel: " + lnfName);
      }
    }
  }

}
