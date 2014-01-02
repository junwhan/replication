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

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.beans.*;

/**
 * Desktop manager keeps Aleph frames within desktop
 **/
public class DesktopMgr extends DefaultDesktopManager {

  // We'll tag internal frames that are being resized using a client
  // property with the name RESIZING.  Used in setBoundsForFrame().
  protected static final String RESIZING = "RESIZING";

  public void beginResizingFrame(JComponent f, int dir) {
    f.putClientProperty(RESIZING, Boolean.TRUE);
  }

  public void endResizingFrame(JComponent f) {
    f.putClientProperty(RESIZING, Boolean.FALSE);
  }

  // This is called any time a frame is moved or resized.  This 
  // implementation keeps the frame from leaving the desktop.
  public void setBoundsForFrame(JComponent f, int x, int y, int w, int h) {
    if (f instanceof JInternalFrame == false) {
      super.setBoundsForFrame(f, x, y, w, h); // only deal w/internal frames
    }
    else {
      JInternalFrame frame = (JInternalFrame)f;

      // Figure out if we are being resized (otherwise it's just a move)
      boolean resizing = false;
      Object r = frame.getClientProperty(RESIZING);
      if (r != null && r instanceof Boolean) {
        resizing = ((Boolean)r).booleanValue();
      }

      JDesktopPane desk = frame.getDesktopPane();
      Dimension d = desk.getSize();

      // Nothing all that fancy below, just figuring out how to adjust
      // to keep the frame on the desktop.
      if (x < 0) {              // too far left?
        if (resizing)
          w += x;               // don't get wider!
        x=0;                    // flush against the left side
      }
      else {
        if (x+w>d.width) {      // too far right?
         if (resizing)
           w = d.width-x;       // don't get wider!
         else
           x = d.width-w;       // flush against the right side
        }
      }
      if (y < 0) {              // too high?
        if (resizing)
          h += y;               // don't get taller!
        y=0;                    // flush against the top
      }
      else {
        if (y+h > d.height) {   // too low?
          if (resizing)
            h = d.height - y;   // don't get taller!
          else
            y = d.height-h;     // flush against the bottom
        }
      }

      // Set 'em the way we like 'em
      super.setBoundsForFrame(f, x, y, w, h);
    }
  }
}
