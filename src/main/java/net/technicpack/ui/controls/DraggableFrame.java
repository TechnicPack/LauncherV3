/*
 * This file is part of Technic UI Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic UI Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic UI Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic UI Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.ui.controls;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.text.JTextComponent;

public class DraggableFrame extends JFrame {
  private int dragGripX;
  private int dragGripY;

  public DraggableFrame() {
    super();

    String desktop = System.getenv("XDG_SESSION_DESKTOP");
    // Cosmic, for some reason, makes dragging the window completely impossible unless it's
    // done through the window decoration. It also resizes the windows regardless of
    // the setResizable(false) call.
    if (desktop == null || !desktop.equalsIgnoreCase("COSMIC")) {
      setUndecorated(true);
    }

    setResizable(false);
  }

  /**
   * Call this from any child class to make a specific component (like a header or background)
   * responsible for dragging the window.
   */
  protected void registerDragHandle(Container container) {
    MouseAdapter dragListener =
        new MouseAdapter() {
          @Override
          public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
              dragGripX = e.getXOnScreen() - getX();
              dragGripY = e.getYOnScreen() - getY();
            }
          }

          @Override
          public void mouseDragged(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
              setLocation(e.getXOnScreen() - dragGripX, e.getYOnScreen() - dragGripY);

              Toolkit.getDefaultToolkit().sync();
            }
          }
        };

    applyDraggableRecursively(container, dragListener);
  }

  private void applyDraggableRecursively(Component comp, MouseAdapter listener) {
    // 1. Interactive types that should NEVER be turned into drag handles
    if (comp instanceof AbstractButton
        || comp instanceof JComboBox
        || comp instanceof JTextComponent
        || comp instanceof JScrollBar
        || comp instanceof JScrollPane) {
      return;
    }

    // Check if this specific component is already "interested" in the mouse
    boolean hasListeners =
        comp.getMouseListeners().length > 0 || comp.getMouseMotionListeners().length > 0;

    // We only add our drag listener if the component is "silent"
    if (!hasListeners) {
      comp.addMouseListener(listener);
      comp.addMouseMotionListener(listener);
    }

    // Always descend into containers (JPanels, Boxes) to find their silent children
    if (comp instanceof Container) {
      for (Component child : ((Container) comp).getComponents()) {
        applyDraggableRecursively(child, listener);
      }
    }
  }
}
