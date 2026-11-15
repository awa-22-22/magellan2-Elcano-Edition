// class magellan.client.desktop.MagellanDockable
//
// Copyright 2003-2026 by magellan project team
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program (see doc/LICENCE.txt); if not, write to the
// Free Software Foundation, Inc.,
// 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
//
package magellan.client.desktop;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JPanel;

import io.github.andrewauclair.moderndocking.Dockable;

/**
 * Wraps one dock component (map, overview, plugin dock, ...) for the Modern Docking framework. The
 * persistent ID is the component key, e.g. {@link MagellanDesktop#MAP_IDENTIFIER}; it is also the
 * view title stored in the docking layout file.
 */
public class MagellanDockable extends JPanel implements Dockable {
  private final String key;
  private final Component component;
  private final String title;
  private final String tooltip;

  /**
   * Creates a dockable for the given component.
   *
   * @param key the component key, used as persistent ID
   * @param component the wrapped component
   * @param title the tab title
   * @param tooltip the tab tooltip, may be null
   */
  public MagellanDockable(String key, Component component, String title, String tooltip) {
    super(new BorderLayout());
    this.key = key;
    this.component = component;
    this.title = title != null ? title : key;
    this.tooltip = tooltip;
    setName(key);
    add(component, BorderLayout.CENTER);
  }

  /**
   * Returns the wrapped component.
   */
  public Component getComponent() {
    return component;
  }

  public String getPersistentID() {
    return key;
  }

  public String getTabText() {
    return title;
  }

  @Override
  public String getTitleText() {
    return title;
  }

  @Override
  public String getTabTooltip() {
    return tooltip;
  }

  /**
   * Magellan's dock components do their own scrolling. An additional scroll pane around them would
   * give them their preferred size instead of the visible size (e.g. huge empty areas in the orders
   * editor).
   */
  @Override
  public boolean isWrappableInScrollpane() {
    return false;
  }

  /**
   * Tells the wrapped component that its dock has become visible.
   */
  void fireShown() {
    if (component instanceof DockVisibilityListener) {
      ((DockVisibilityListener) component).dockShown();
    }
  }

  /**
   * Tells the wrapped component that its dock has been hidden or closed.
   */
  void fireHidden() {
    if (component instanceof DockVisibilityListener) {
      ((DockVisibilityListener) component).dockHidden();
    }
  }
}
