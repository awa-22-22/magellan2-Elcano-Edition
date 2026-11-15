// class magellan.client.desktop.LayoutConverterTest
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.swing.JSplitPane;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Element;

import io.github.andrewauclair.moderndocking.layouts.ApplicationLayout;
import io.github.andrewauclair.moderndocking.layouts.DockingLayoutNode;
import io.github.andrewauclair.moderndocking.layouts.DockingSimplePanelNode;
import io.github.andrewauclair.moderndocking.layouts.DockingSplitPanelNode;
import io.github.andrewauclair.moderndocking.layouts.DockingTabPanelNode;
import io.github.andrewauclair.moderndocking.layouts.WindowLayout;
import magellan.library.utils.Utils;

/**
 * Tests the conversion between Magellan's docking layout XML and Modern Docking layouts.
 */
public class LayoutConverterTest {

  private static final List<String> DEFAULT_IDS = Arrays.asList(MagellanDesktop.OVERVIEW_IDENTIFIER,
      MagellanDesktop.MAP_IDENTIFIER, MagellanDesktop.MINIMAP_IDENTIFIER,
      MagellanDesktop.DETAILS_IDENTIFIER, MagellanDesktop.NAMEDESCRIPTION_IDENTIFIER,
      MagellanDesktop.ORDERS_IDENTIFIER, MagellanDesktop.MESSAGES_IDENTIFIER,
      MagellanDesktop.TASKS_IDENTIFIER, MagellanDesktop.HISTORY_IDENTIFIER,
      MagellanDesktop.BOOKMARKS_IDENTIFIER);

  private static Element parse(String xml) throws Exception {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
        .getDocumentElement();
  }

  /**
   * Describes the structure of a layout element as text; empty window bars are ignored.
   */
  private static String describe(Element element) {
    String type = element.getNodeName().toLowerCase();
    StringBuilder result = new StringBuilder();
    if (type.equals("windowbar")) {
      if (Utils.getChildNodes(element).isEmpty())
        return "";
      result.append("bar(").append(element.getAttribute("direction").toLowerCase());
    } else if (type.equals("splitwindow")) {
      result.append("split(").append(element.getAttribute("horizontal")).append(",")
          .append(String.format(Locale.ROOT, "%.3f",
              Double.parseDouble(element.getAttribute("divider"))));
    } else if (type.equals("tab")) {
      result.append(Boolean.valueOf(element.getAttribute("isActive")) ? "*" : "");
    } else if (type.equals("view")) {
      return element.getAttribute("title");
    } else if (type.equals("floatingwindow")) {
      result.append("float(").append(element.getAttribute("x")).append(",")
          .append(element.getAttribute("y"));
    } else {
      result.append(type).append("(");
    }
    for (Element child : Utils.getChildNodes(element)) {
      String text = describe(child);
      if (!text.isEmpty()) {
        result.append(" ").append(text);
      }
    }
    if (type.equals("tab"))
      return result.toString().trim();
    result.append(")");
    return result.toString();
  }

  private static String roundTrip(Element root, List<String> ids, boolean showTabs)
      throws Exception {
    ApplicationLayout layout = new LayoutConverter(null, ids, showTabs).toApplicationLayout(root);
    return LayoutConverter.toXml(layout, root.getAttribute("name"),
        Boolean.valueOf(root.getAttribute("isActive")));
  }

  @Test
  public void defaultLayoutSurvivesRoundTrip() throws Exception {
    Element root = DockingLayout.createDefaultLayout("Standard", true);
    String xml = roundTrip(root, DEFAULT_IDS, true);
    Element result = parse(xml);

    assertEquals(describe(root), describe(result));
    assertEquals("Standard", result.getAttribute("name"));
    assertEquals("true", result.getAttribute("isActive"));
  }

  @Test
  public void defaultLayoutSurvivesRoundTripWithoutTabs() throws Exception {
    Element root = DockingLayout.createDefaultLayout("Standard", true);
    assertEquals(describe(root), describe(parse(roundTrip(root, DEFAULT_IDS, false))));
  }

  @Test
  public void horizontalSplitIsSideBySide() throws Exception {
    Element root = parse("<rootwindow name='x'><splitwindow divider='0.3' horizontal='true'>"
        + "<split><view title='A'/></split><split><view title='B'/></split>"
        + "</splitwindow></rootwindow>");
    ApplicationLayout layout =
        new LayoutConverter(null, Arrays.asList("A", "B"), true).toApplicationLayout(root);
    DockingSplitPanelNode split =
        (DockingSplitPanelNode) layout.getMainFrameLayout().getRootNode();
    assertEquals(JSplitPane.HORIZONTAL_SPLIT, split.getOrientation());
    assertEquals(0.3, split.getDividerProportion(), 1e-6);
  }

  @Test
  public void unknownViewsAreDroppedAndSplitsCollapse() throws Exception {
    Element root = parse("<rootwindow name='x'><splitwindow divider='0.5' horizontal='true'>"
        + "<split><tabwindow><tab isActive='true'><view title='MAP'/></tab></tabwindow></split>"
        + "<split><tabwindow><tab isActive='true'><view title='REMOVED_PLUGIN'/></tab></tabwindow>"
        + "</split></splitwindow></rootwindow>");
    ApplicationLayout layout =
        new LayoutConverter(null, DEFAULT_IDS, true).toApplicationLayout(root);
    DockingLayoutNode node = layout.getMainFrameLayout().getRootNode();
    assertTrue(node instanceof DockingTabPanelNode);
    assertEquals("MAP", ((DockingTabPanelNode) node).getSelectedTabID());
  }

  @Test
  public void layoutWithoutKnownViewsIsEmpty() throws Exception {
    Element root = parse("<rootwindow name='x'><tabwindow><tab><view title='GONE'/></tab>"
        + "</tabwindow></rootwindow>");
    ApplicationLayout layout =
        new LayoutConverter(null, DEFAULT_IDS, true).toApplicationLayout(root);
    assertNull(layout.getMainFrameLayout().getRootNode());
    assertEquals("rootwindow()", describe(parse(LayoutConverter.toXml(layout, "x", true))));
  }

  @Test
  public void duplicateViewsAreUsedOnce() throws Exception {
    Element root = parse("<rootwindow name='x'><tabwindow>"
        + "<tab isActive='true'><view title='MAP'/></tab><tab><view title='MAP'/></tab>"
        + "<tab><view title='OVERVIEW'/></tab></tabwindow></rootwindow>");
    assertEquals("rootwindow( tabwindow( * MAP OVERVIEW))",
        describe(parse(roundTrip(root, DEFAULT_IDS, true))));
  }

  @Test
  public void tabOrderAndSelectionArePreserved() throws Exception {
    Element root = parse("<rootwindow name='x'><tabwindow>"
        + "<tab><view title='A'/></tab><tab isActive='true'><view title='B'/></tab>"
        + "<tab><view title='C'/></tab></tabwindow></rootwindow>");
    ApplicationLayout layout =
        new LayoutConverter(null, Arrays.asList("A", "B", "C"), true).toApplicationLayout(root);
    DockingTabPanelNode tabs = (DockingTabPanelNode) layout.getMainFrameLayout().getRootNode();
    List<String> order = new ArrayList<String>();
    for (DockingSimplePanelNode tab : tabs.getPersistentIDs()) {
      order.add(tab.getPersistentID());
    }
    assertEquals(Arrays.asList("A", "B", "C"), order);
    assertEquals("B", tabs.getSelectedTabID());
  }

  @Test
  public void singleViewWithoutTabsIsSimplePanel() throws Exception {
    Element root = parse("<rootwindow name='x'><tabwindow><tab isActive='true'>"
        + "<view title='MAP'/></tab></tabwindow></rootwindow>");
    ApplicationLayout layout =
        new LayoutConverter(null, DEFAULT_IDS, false).toApplicationLayout(root);
    assertTrue(layout.getMainFrameLayout().getRootNode() instanceof DockingSimplePanelNode);
    // written back as tab window, like before
    assertEquals("rootwindow( tabwindow( * MAP))",
        describe(parse(LayoutConverter.toXml(layout, "x", true))));
  }

  @Test
  public void windowBarsBecomeAutoHideToolbars() throws Exception {
    Element root = parse("<rootwindow name='x'>"
        + "<windowbar direction='Down'><view title='MAP'/></windowbar>"
        + "<windowbar direction='Up'><view title='TASKS'/></windowbar>"
        + "<windowbar direction='Right'><tabwindow><tab><view title='HISTORY'/></tab></tabwindow>"
        + "</windowbar>"
        + "<tabwindow><tab isActive='true'><view title='OVERVIEW'/></tab></tabwindow>"
        + "</rootwindow>");
    ApplicationLayout layout =
        new LayoutConverter(null, DEFAULT_IDS, true).toApplicationLayout(root);
    WindowLayout main = layout.getMainFrameLayout();
    assertEquals(Arrays.asList("MAP"), main.getSouthAutoHideToolbarIDs());
    assertEquals(Arrays.asList("TASKS"), main.getWestAutoHideToolbarIDs());
    assertEquals(Arrays.asList("HISTORY"), main.getEastAutoHideToolbarIDs());

    assertEquals("rootwindow( bar(down MAP) bar(left TASKS) bar(right HISTORY)"
        + " tabwindow( * OVERVIEW))", describe(parse(LayoutConverter.toXml(layout, "x", true))));
  }

  @Test
  public void floatingWindowsAreKept() throws Exception {
    Element root = parse("<rootwindow name='x'>"
        + "<floatingwindow x='100' y='50' width='400' height='300' isVisible='true'>"
        + "<tabwindow><tab isActive='true'><view title='MINIMAP'/></tab></tabwindow>"
        + "</floatingwindow>"
        + "<tabwindow><tab isActive='true'><view title='MAP'/></tab></tabwindow></rootwindow>");
    ApplicationLayout layout =
        new LayoutConverter(null, DEFAULT_IDS, true).toApplicationLayout(root);
    assertEquals(1, layout.getFloatingFrameLayouts().size());
    WindowLayout frame = layout.getFloatingFrameLayouts().get(0);
    assertEquals(new Point(100, 50), frame.getLocation());
    assertEquals(400, frame.getSize().width);
    assertEquals(300, frame.getSize().height);

    assertEquals("rootwindow( float(100,50 tabwindow( * MINIMAP)) tabwindow( * MAP))",
        describe(parse(LayoutConverter.toXml(layout, "x", true))));
  }

  @Test
  public void namesAndKeysAreEscaped() throws Exception {
    Element root = parse("<rootwindow name='x'><tabwindow><tab isActive='true'>"
        + "<view title='NAME&amp;DESCRIPTION'/></tab></tabwindow></rootwindow>");
    ApplicationLayout layout =
        new LayoutConverter(null, DEFAULT_IDS, true).toApplicationLayout(root);
    Element result = parse(LayoutConverter.toXml(layout, "Tom's <layout>", false));
    assertEquals("Tom's <layout>", result.getAttribute("name"));
    assertEquals("rootwindow( tabwindow( * NAME&DESCRIPTION))", describe(result));
  }

  @Test
  public void invalidDividerFallsBackToHalf() throws Exception {
    Element root = parse("<rootwindow name='x'><splitwindow divider='NaN' horizontal='false'>"
        + "<split><view title='A'/></split><split><view title='B'/></split>"
        + "</splitwindow></rootwindow>");
    ApplicationLayout layout =
        new LayoutConverter(null, Arrays.asList("A", "B"), true).toApplicationLayout(root);
    assertEquals(0.5,
        ((DockingSplitPanelNode) layout.getMainFrameLayout().getRootNode()).getDividerProportion(),
        1e-9);
  }
}
