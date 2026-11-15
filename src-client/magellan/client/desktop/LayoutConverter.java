// class magellan.client.desktop.LayoutConverter
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

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JSplitPane;

import org.w3c.dom.Element;

import io.github.andrewauclair.moderndocking.api.DockingAPI;
import io.github.andrewauclair.moderndocking.layouts.ApplicationLayout;
import io.github.andrewauclair.moderndocking.layouts.DockingLayoutNode;
import io.github.andrewauclair.moderndocking.layouts.DockingSimplePanelNode;
import io.github.andrewauclair.moderndocking.layouts.DockingSplitPanelNode;
import io.github.andrewauclair.moderndocking.layouts.DockingTabPanelNode;
import io.github.andrewauclair.moderndocking.layouts.WindowLayout;
import magellan.library.utils.Utils;

/**
 * Converts between Magellan's docking layout XML (one <code>rootwindow</code> element of
 * <code>dock-default.xml</code>) and Modern Docking's {@link ApplicationLayout}.
 * <p>
 * The XML format is the one written by Magellan since 2007 (InfoNode era) and is kept unchanged:
 * <code>rootwindow</code> contains <code>windowbar</code> (direction Up/Down/Left/Right),
 * <code>floatingwindow</code> (x, y, width, height) and one window tree made of
 * <code>splitwindow</code> (divider, horizontal, two <code>split</code> children),
 * <code>tabwindow</code> (<code>tab</code> children with isActive) and <code>view</code> (title =
 * component key).
 * <p>
 * Views whose key is unknown (e.g. a removed plugin) or that occur twice are dropped; splits and tab
 * groups that become empty collapse. Window bars map to the auto-hide toolbars; Modern Docking has
 * no top toolbar, so "Up" becomes the left (west) toolbar.
 */
public class LayoutConverter {
  private static final String DOCKABLE_CLASS = MagellanDockable.class.getName();

  private final DockingAPI docking;
  private final Set<String> knownIds;
  private final boolean showTabs;
  private Set<String> used;

  /**
   * Creates a converter.
   *
   * @param docking the docking instance the layout nodes belong to (may be null in tests)
   * @param knownIds the keys of all registered dockables
   * @param showTabs if false, a single view is restored without a tab
   */
  public LayoutConverter(DockingAPI docking, Collection<String> knownIds, boolean showTabs) {
    this.docking = docking;
    this.knownIds = new HashSet<String>(knownIds);
    this.showTabs = showTabs;
  }

  /**
   * Converts a <code>rootwindow</code> element into an application layout.
   */
  public ApplicationLayout toApplicationLayout(Element rootWindow) {
    used = new HashSet<String>();
    DockingLayoutNode main = null;
    List<WindowLayout> floating = new ArrayList<WindowLayout>();
    List<String> south = new ArrayList<String>();
    List<String> west = new ArrayList<String>();
    List<String> east = new ArrayList<String>();

    for (Element child : Utils.getChildNodes(rootWindow)) {
      String type = child.getNodeName().toLowerCase();
      if (type.equals("windowbar")) {
        String direction = child.getAttribute("direction");
        List<String> ids = new ArrayList<String>();
        collectViews(child, ids);
        if ("down".equalsIgnoreCase(direction)) {
          south.addAll(ids);
        } else if ("right".equalsIgnoreCase(direction)) {
          east.addAll(ids);
        } else {
          west.addAll(ids);
        }
      } else if (type.equals("floatingwindow")) {
        DockingLayoutNode node = null;
        for (Element element : Utils.getChildNodes(child)) {
          node = toNode(element);
          if (node != null) {
            break;
          }
        }
        if (node != null) {
          Point location = new Point(Utils.getIntValue(child.getAttribute("x")),
              Utils.getIntValue(child.getAttribute("y")));
          Dimension size = new Dimension(Utils.getIntValue(child.getAttribute("width")),
              Utils.getIntValue(child.getAttribute("height")));
          floating.add(new WindowLayout(false, location, size, Frame.NORMAL, node));
        }
      } else {
        DockingLayoutNode node = toNode(child);
        if (node != null) {
          main = node;
        }
      }
    }

    WindowLayout mainLayout = new WindowLayout(main);
    mainLayout.setSouthAutoHideToolbarIDs(south);
    mainLayout.setWestAutoHideToolbarIDs(west);
    mainLayout.setEastAutoHideToolbarIDs(east);

    ApplicationLayout layout = new ApplicationLayout(mainLayout);
    for (WindowLayout frame : floating) {
      layout.addFrame(frame);
    }
    return layout;
  }

  private DockingLayoutNode toNode(Element element) {
    if (element == null)
      return null;
    String type = element.getNodeName().toLowerCase();
    if (type.equals("splitwindow")) {
      List<Element> splits = Utils.getChildNodes(element, "split");
      if (splits.size() != 2)
        return null;
      DockingLayoutNode left = toNode(Utils.getChildNode(splits.get(0)));
      DockingLayoutNode right = toNode(Utils.getChildNode(splits.get(1)));
      if (left == null)
        return right;
      if (right == null)
        return left;
      int orientation = Boolean.valueOf(element.getAttribute("horizontal"))
          ? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT;
      double divider = 0.5;
      try {
        divider = saneDivider(Double.parseDouble(element.getAttribute("divider")));
      } catch (NumberFormatException e) {
        // keep 0.5
      }
      return new DockingSplitPanelNode(docking, left, right, orientation, divider, "");
    } else if (type.equals("tabwindow")) {
      List<String> ids = new ArrayList<String>();
      String selected = null;
      for (Element tab : Utils.getChildNodes(element, "tab")) {
        int before = ids.size();
        collectViews(tab, ids);
        if (selected == null && ids.size() > before
            && Boolean.valueOf(tab.getAttribute("isActive"))) {
          selected = ids.get(before);
        }
      }
      return tabNode(ids, selected);
    } else if (type.equals("view")) {
      List<String> ids = new ArrayList<String>();
      collectViews(element, ids);
      return tabNode(ids, null);
    } else {
      List<Element> children = Utils.getChildNodes(element);
      return children.isEmpty() ? null : toNode(children.get(0));
    }
  }

  /**
   * Adds the keys of all known, not yet used views below (and including) the element.
   */
  private void collectViews(Element element, List<String> ids) {
    if (element == null)
      return;
    if (element.getNodeName().equalsIgnoreCase("view")) {
      String id = element.getAttribute("title");
      if (id != null && knownIds.contains(id) && used.add(id)) {
        ids.add(id);
      }
    }
    for (Element child : Utils.getChildNodes(element)) {
      collectViews(child, ids);
    }
  }

  private DockingLayoutNode tabNode(List<String> ids, String selected) {
    if (ids.isEmpty())
      return null;
    if (selected == null) {
      selected = ids.get(0);
    }
    if (ids.size() == 1 && !showTabs)
      return new DockingSimplePanelNode(docking, selected, DOCKABLE_CLASS, "", selected, selected);

    // the constructor adds the selected tab first; adding it again moves it to its position
    DockingTabPanelNode node =
        new DockingTabPanelNode(docking, selected, DOCKABLE_CLASS, "", selected, selected);
    for (String id : ids) {
      node.addTab(id, DOCKABLE_CLASS, "", id, id);
    }
    return node;
  }

  /**
   * Converts an application layout into a <code>rootwindow</code> element (as XML text).
   *
   * @param layout the layout
   * @param name the layout name
   * @param isActive whether the layout is the active one
   */
  public static String toXml(ApplicationLayout layout, String name, boolean isActive) {
    StringBuilder buffer = new StringBuilder();
    buffer.append("<rootwindow name='").append(attribute(name)).append("' isActive='")
        .append(isActive).append("'>\r\n");

    WindowLayout main = layout.getMainFrameLayout();
    if (main != null) {
      windowBar(buffer, "Down", main.getSouthAutoHideToolbarIDs());
      windowBar(buffer, "Left", main.getWestAutoHideToolbarIDs());
      windowBar(buffer, "Right", main.getEastAutoHideToolbarIDs());
    }

    for (WindowLayout frame : layout.getFloatingFrameLayouts()) {
      if (frame.getRootNode() == null) {
        continue;
      }
      Point location = frame.getLocation();
      Dimension size = frame.getSize();
      buffer.append("  <floatingwindow x='").append(location.x).append("' y='").append(location.y)
          .append("' width='").append(size.width).append("' height='").append(size.height)
          .append("' isVisible='true'>\r\n");
      node(buffer, frame.getRootNode(), "   ");
      buffer.append("  </floatingwindow>\r\n");
    }

    if (main != null) {
      node(buffer, main.getRootNode(), "  ");
    }
    buffer.append("</rootwindow>\r\n");
    return buffer.toString();
  }

  private static void windowBar(StringBuilder buffer, String direction, List<String> ids) {
    if (ids == null || ids.isEmpty())
      return;
    buffer.append("  <windowbar direction='").append(direction).append("'>\r\n");
    for (String id : ids) {
      view(buffer, id, "   ");
    }
    buffer.append("  </windowbar>\r\n");
  }

  private static void node(StringBuilder buffer, DockingLayoutNode node, String offset) {
    if (node instanceof DockingSplitPanelNode) {
      DockingSplitPanelNode split = (DockingSplitPanelNode) node;
      if (split.getLeft() == null || split.getRight() == null) {
        node(buffer, split.getLeft() != null ? split.getLeft() : split.getRight(), offset);
        return;
      }
      boolean horizontal = split.getOrientation() == JSplitPane.HORIZONTAL_SPLIT;
      buffer.append(offset).append("<splitwindow divider='")
          .append((float) saneDivider(split.getDividerProportion())).append("' horizontal='")
          .append(horizontal).append("'>\r\n");
      buffer.append(offset).append(" <split>\r\n");
      node(buffer, split.getLeft(), offset + "  ");
      buffer.append(offset).append(" </split>\r\n");
      buffer.append(offset).append(" <split>\r\n");
      node(buffer, split.getRight(), offset + "  ");
      buffer.append(offset).append(" </split>\r\n");
      buffer.append(offset).append("</splitwindow>\r\n");
    } else if (node instanceof DockingTabPanelNode) {
      DockingTabPanelNode tabs = (DockingTabPanelNode) node;
      buffer.append(offset).append("<tabwindow>\r\n");
      for (DockingSimplePanelNode tab : tabs.getPersistentIDs()) {
        String id = tab.getPersistentID();
        buffer.append(offset).append(" <tab isActive='").append(id.equals(tabs.getSelectedTabID()))
            .append("'>\r\n");
        view(buffer, id, offset + "  ");
        buffer.append(offset).append(" </tab>\r\n");
      }
      buffer.append(offset).append("</tabwindow>\r\n");
    } else if (node instanceof DockingSimplePanelNode) {
      // written as a tab window with one tab, like InfoNode did
      buffer.append(offset).append("<tabwindow>\r\n");
      buffer.append(offset).append(" <tab isActive='true'>\r\n");
      view(buffer, ((DockingSimplePanelNode) node).getPersistentID(), offset + "  ");
      buffer.append(offset).append(" </tab>\r\n");
      buffer.append(offset).append("</tabwindow>\r\n");
    }
    // other node types (anchors) and null are not used by Magellan
  }

  private static void view(StringBuilder buffer, String id, String offset) {
    buffer.append(offset).append("<view title='").append(attribute(id)).append("'>\r\n");
    buffer.append(offset).append("</view>\r\n");
  }

  /**
   * Escapes a value for an XML attribute in single or double quotes.
   */
  static String attribute(String value) {
    if (value == null)
      return "";
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("'", "&apos;").replace("\"", "&quot;");
  }

  /**
   * Returns the divider proportion, or 0.5 if it is not a usable value (e.g. a split pane that was
   * never laid out).
   */
  private static double saneDivider(double divider) {
    if (Double.isNaN(divider) || divider <= 0 || divider >= 1)
      return 0.5;
    return divider;
  }
}
