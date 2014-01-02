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

import java.awt.*;
import java.awt.event.*;

import java.util.Map;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;

/**
 * Manages managers
 *
 * @author Maurice Herlihy
 * @date   August 1999
 **/
public class Managers extends JInternalFrame implements ActionListener, MouseListener {

  private JButton addButton;	// add new Manager
  private JButton removeButton; // remove existing Manager
  private JButton modifyButton; // modify Manager
  private static  Dimension spacer = new Dimension(10,1);

  private Color background, foreground;

  private JList list;
  private DefaultListModel model;

  private JTextField aliasField;
  private JTextField classField;

  // used by add and modify buttons
  private Object[] editDisplay;
  private JTextField shadowAliasField;
  private JTextField shadowClassField;

  private JPanel details;
  private Map map;
  private String property;

  /**
   * Constructor
   * @param map carries alias to class name
   * @param selected alias to select initially
   * @param property aleph property we are controlling
   **/
  public Managers (String title, Map theMap, String selected, String theProperty) {
    super(title, true, false, true, true);
    Container contentPane = getContentPane();
    contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

    this.map = theMap;
    this.property = theProperty;
    Aleph.setProperty(property, (String) map.get(selected)); // initialize property
    JPanel manPanel = new JPanel();
    JPanel btnPanel = new JPanel();
    contentPane.add(Box.createRigidArea(spacer));
    contentPane.add(manPanel);
    contentPane.add(Box.createRigidArea(spacer));
    contentPane.add(btnPanel);
    contentPane.add(Box.createRigidArea(spacer));

    // Manager panel
    manPanel.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.gridwidth = GridBagConstraints.REMAINDER;	// last in row
    c.weightx = 1.0;		// become wider
    c.weighty = 0.0;		// no higher
    c.fill = GridBagConstraints.BOTH;
    c.insets = new Insets(5,5,5,5);

    JPanel alias = new JPanel();
    alias.setLayout(new GridBagLayout());
    alias.setBorder(new TitledBorder(BorderFactory.createEtchedBorder(),
                                       "Alias"));
    model = new DefaultListModel();
    list = new JList(model);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    Object[] objects = map.keySet().toArray();
    for (int i = 0; i < objects.length; i++)
      model.addElement((String) objects[i]);
    list.setSelectedValue(selected, true);
    list.setToolTipText("select which manager you want to use");
    // on selection, update details panel and property
    list.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        String selection = (String) list.getSelectedValue();
        Aleph.setProperty(property, (String) map.get(selection));
        aliasField.setText(selection);
        classField.setText((String) map.get(selection));
        details.repaint();
      }});
    alias.add(list, c);

    manPanel.add(alias, c);

    // Manager details
    details = new JPanel();
    details.setLayout(new GridBagLayout());
    details.setBorder(new TitledBorder(BorderFactory.createEtchedBorder(),
                                      "Details"));
    
    c.gridwidth = GridBagConstraints.RELATIVE;
    c.weightx = 0.0;
    details.add(new JLabel("Alias"), c);
    c.gridwidth = GridBagConstraints.REMAINDER;
    c.weightx = 1.0;
    aliasField = new JTextField(selected);
    aliasField.setEditable(false);
    details.add(aliasField, c);

    // initialize editing fields
    shadowAliasField = new JTextField();
    shadowClassField = new JTextField();
    editDisplay      = new Object[] {"Alias", shadowAliasField,
                                     "Class", shadowClassField};

    c.gridwidth = GridBagConstraints.RELATIVE;
    c.weightx = 0.0;
    details.add(new JLabel("Class"), c);
    c.weightx = 1.0;
    c.gridwidth = GridBagConstraints.REMAINDER;
    classField = new JTextField(map.get(selected).toString());
    classField.setEditable(false);
    details.add(classField, c);
    details.setToolTipText("details about the selected manager");
    manPanel.add(details, c);

    // button panel
    btnPanel.setLayout(new GridLayout(1, 3, 10, 10));
    
    addButton = new JButton("Add");
    background = addButton.getBackground();
    foreground = addButton.getForeground();
    addButton.setBorder(BorderFactory.createRaisedBevelBorder());
    addButton.addMouseListener(this);
    addButton.setToolTipText("add Manager to list");
    addButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        shadowAliasField.setText("");
        shadowClassField.setText("");
        int result = JOptionPane.showOptionDialog(null,
                                                  editDisplay,
                                                  "Add Manager",
                                                  JOptionPane.OK_CANCEL_OPTION,
                                                  JOptionPane.QUESTION_MESSAGE,
                                                  null, null, null);
        if (result == JOptionPane.OK_OPTION) {
          String newAlias = shadowAliasField.getText();
          String newClass = shadowClassField.getText();
          if (map.containsKey(newAlias)) {
            int confirm = JOptionPane.showConfirmDialog(null,
                                                        "Replace binding for " + newAlias + "?",
                                                        "Alias Exists",
                                                        JOptionPane.OK_CANCEL_OPTION);
            if (confirm != JOptionPane.OK_OPTION)
              return;
          }
          aliasField.setText(newAlias);
          classField.setText(newClass);
          map.put(newAlias, newClass);
          model.removeElement(newAlias);
          model.addElement(newAlias);
          list.setSelectedValue(newAlias, true);
        }
      }});
    btnPanel.add(addButton);

    removeButton = new JButton("Remove");
    removeButton.setBorder(BorderFactory.createRaisedBevelBorder());
    removeButton.addMouseListener(this);
    removeButton.setToolTipText("remove Manager from list");
    removeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (map.size() == 1) {
          JOptionPane.showMessageDialog(null,
                                        "You cannot remove the last manager",
                                        "Error",
                                        JOptionPane.ERROR_MESSAGE);
        } else {
          String oldAlias = aliasField.getText();
          int confirm = JOptionPane.showConfirmDialog(null,
                                                      "Really remove " + oldAlias + "?",
                                                      "Are You Sure?",
                                                      JOptionPane.OK_CANCEL_OPTION);
          if (confirm != JOptionPane.OK_OPTION)
            return;
          map.remove(oldAlias);
          model.removeElementAt(list.getSelectedIndex());
          list.setSelectedIndex(0);
          String selection = (String) list.getSelectedValue();
          aliasField.setText(selection);
          classField.setText((String) map.get(selection));
        }
      }});
 
    btnPanel.add(removeButton);

    modifyButton = new JButton("Modify");
    modifyButton.setBorder(BorderFactory.createRaisedBevelBorder());
    modifyButton.addMouseListener(this);
    modifyButton.setToolTipText("modify Manager in list");
    modifyButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        shadowAliasField.setText(aliasField.getText());
        shadowClassField.setText(classField.getText());
        int result = JOptionPane.showOptionDialog(null,
                                                  editDisplay,
                                                  "Modify Manager",
                                                  JOptionPane.OK_CANCEL_OPTION,
                                                  JOptionPane.QUESTION_MESSAGE,
                                                  null, null, null);
        if (result == JOptionPane.OK_OPTION) {
          String oldAlias = aliasField.getText();
          String newAlias = shadowAliasField.getText();
          String newClass = shadowClassField.getText();
          if (oldAlias != newAlias && map.containsKey(newAlias)) {
            int confirm = JOptionPane.showConfirmDialog(null,
                                                        "Replace binding for " + newAlias + "?",
                                                        "Alias Exists",
                                                        JOptionPane.OK_CANCEL_OPTION);
            if (confirm != JOptionPane.OK_OPTION)
              return;
          }
          aliasField.setText(newAlias);
          classField.setText(newClass);
          map.remove(oldAlias);
          map.put(newAlias, newClass);
          model.removeElement(oldAlias);
          model.removeElement(newAlias);
          model.addElement(newAlias);
          list.setSelectedValue(newAlias, true);
        }
      }});
    btnPanel.add(modifyButton);
    Dimension preferred = getPreferredSize();
    setBounds(0, 0, preferred.width, preferred.height);
  }

  public void mouseEntered (MouseEvent e) {
    JButton button = (JButton) e.getSource();
    button.setBackground(Color.cyan);
    repaint();
  }

  /**
   * Tell the manager what to do when user selects a different manager.
   * @param listener call this ListSelectionListener
   **/
  public void addListSelectionListener(ListSelectionListener listener) {
    list.addListSelectionListener(listener);
  }

  public void mouseExited (MouseEvent e) {
    JButton button = (JButton) e.getSource();
    button.setBackground(background);
    repaint();
  }

  public void mouseClicked (MouseEvent e) {}
  public void mousePressed (MouseEvent e) {}
  public void mouseReleased (MouseEvent e) {}

  public void actionPerformed(ActionEvent e) {
  }

}
