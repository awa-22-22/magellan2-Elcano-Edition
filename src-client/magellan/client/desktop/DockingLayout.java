// class magellan.client.desktop.DockingLayout
// created on 17.11.2007
//
// Copyright 2003-2007 by magellan project team
//
// Author : $Author: $
// $Id: $
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program (see doc/LICENCE.txt); if not, write to the
// Free Software Foundation, Inc.,
// 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
package magellan.client.desktop;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import io.github.andrewauclair.moderndocking.api.DockingAPI;
import io.github.andrewauclair.moderndocking.layouts.ApplicationLayout;
import magellan.client.utils.ErrorWindow;
import magellan.library.utils.Utils;
import magellan.library.utils.logging.Logger;

/**
 * This class holds all information about a Docking Layout. The layout is kept as XML element
 * (<code>rootwindow</code>, see {@link LayoutConverter}); the active layout is converted to and from
 * the docking framework when it is opened or saved.
 *
 * @author Thoralf Rickert
 * @version 1.0, 17.11.2007
 */
public class DockingLayout {
  private static final Logger log = Logger.getInstance(DockingLayout.class);
  private String name = null;
  private Element root = null;
  private boolean isActive = false;

  /**
   * Creates a new Docking Framework Layout Container.
   */
  public DockingLayout(String name, Element root) {
    setName(name);
    setRoot(root);
  }

  /**
   * Loads a List of docking layouts from the given File.
   *
   * @return the layouts, or null if the file does not contain docking layouts
   */
  public static List<DockingLayout> load(File file) {
    List<DockingLayout> layouts = new ArrayList<DockingLayout>();
    try {
      DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document document = builder.parse(file);
      if (!document.getDocumentElement().getNodeName().equals("dock")) {
        DockingLayout.log.fatal("The file " + file
            + " does NOT contain Docking-layouts for Magellan. Missing XML root element 'dock'");
        return null;
      }

      DockingLayout.load(layouts, document.getDocumentElement());
    } catch (Exception exception) {
      DockingLayout.log.error(exception);
      ErrorWindow errorWindow = new ErrorWindow("Could not load docking layouts.", exception);
      errorWindow.open();
    }
    return layouts;
  }

  /**
   * Loads the Docking Layouts below the given XML element.
   */
  protected static synchronized void load(List<DockingLayout> layouts, Element root) {
    if (root.getNodeName().equalsIgnoreCase("dock")) {
      List<Element> subnodes = Utils.getChildNodes(root);
      DockingLayout.log.info("Found " + subnodes.size() + " Docking layouts.");
      for (Element node : subnodes) {
        DockingLayout.load(layouts, node);
      }
    } else if (root.getNodeName().equalsIgnoreCase("rootwindow")) {
      String layoutName = root.getAttribute("name");
      if (Utils.isEmpty(layoutName)) {
        layoutName = "Standard";
      }
      boolean isActive = Utils.getBoolValue(root.getAttribute("isActive"), true);

      DockingLayout.log.info("Load layout " + layoutName);
      DockingLayout layout = new DockingLayout(layoutName, root);
      layout.setActive(isActive);
      layouts.add(layout);
    }
  }

  /**
   * Shows this layout in the docking framework.
   *
   * @param docking the docking framework
   * @param knownIds the keys of all registered dockables
   * @param showTabs if false, single views are shown without a tab
   */
  public void open(DockingAPI docking, Collection<String> knownIds, boolean showTabs) {
    DockingLayout.log.info("Open docking layout " + name);
    ApplicationLayout layout =
        new LayoutConverter(docking, knownIds, showTabs).toApplicationLayout(root);
    docking.getDockingState().restoreApplicationLayout(layout);
  }

  /**
   * Stores the current state of the docking framework in this layout. Call this for the active
   * layout before it is saved or replaced by another layout.
   */
  public void capture(DockingAPI docking) {
    String xml =
        LayoutConverter.toXml(docking.getDockingState().getApplicationLayout(), name, isActive);
    try {
      root = parse(xml);
    } catch (LayoutException e) {
      DockingLayout.log.error("could not store docking layout " + name, e);
    }
  }

  /**
   * Writes this Docking Layout as XML into the buffer.
   */
  public void save(StringBuffer buffer) {
    if (root == null)
      return;
    root.setAttribute("name", name);
    root.setAttribute("isActive", String.valueOf(isActive));
    try {
      StringWriter writer = new StringWriter();
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.transform(new DOMSource(root), new StreamResult(writer));
      writer.close();

      String output = writer.toString();
      output = output.replaceAll("<\\?xml version=\"1.0\" encoding=\".*\"\\?>", "");

      buffer.append("\r\n ").append(output).append("\r\n");
    } catch (Exception exception) {
      DockingLayout.log.error("error during dock layout save process.", exception);
    }
  }

  private static Element parse(String xml) throws LayoutException {
    try {
      DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document document =
          builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
      return document.getDocumentElement();
    } catch (ParserConfigurationException e) {
      throw new LayoutException("could not parse layout", e);
    } catch (SAXException e) {
      throw new LayoutException("could not parse layout", e);
    } catch (IOException e) {
      throw new LayoutException("could not parse layout", e);
    }
  }

  /**
   * Creates the default layout.
   *
   * @throws LayoutException on internal error
   */
  public static Element createDefaultLayout(String name, boolean isActive) throws LayoutException {
    StringBuffer buffer = new StringBuffer();
    buffer
        .append("<rootwindow name='" + name + "' isActive='" + isActive + "'>\r\n");
    buffer
        .append("   <windowbar direction='Down'>\r\n" +
            "   </windowbar>\r\n" +
            "   <splitwindow divider='0.3' horizontal='true'>\r\n" +
            "    <split>\r\n" +
            "     <splitwindow divider='0.6' horizontal='false'>\r\n" +
            "      <split>\r\n" +
            "       <tabwindow>\r\n" +
            "        <tab isActive='true'>\r\n" +
            "         <view title='")
        .append(toHTML(MagellanDesktop.OVERVIEW_IDENTIFIER)).append("'>\r\n" +
            "         </view>\r\n" +
            "        </tab>\r\n" +
            "       </tabwindow>\r\n" +
            "      </split>\r\n" +
            "      <split>\r\n" +
            "       <tabwindow>\r\n" +
            "        <tab isActive='true'>\r\n" +
            "         <view title='")
        .append(toHTML(MagellanDesktop.MAP_IDENTIFIER)).append("'>\r\n" +
            "         </view>\r\n" +
            "        </tab>\r\n" +
            "        <tab isActive='false'>\r\n" +
            "         <view title='")
        .append(toHTML(MagellanDesktop.MINIMAP_IDENTIFIER)).append("'>\r\n" +
            "         </view>\r\n" +
            "        </tab>\r\n" +
            "       </tabwindow>\r\n" +
            "      </split>\r\n" +
            "     </splitwindow>\r\n" +
            "    </split>\r\n" +
            "    <split>\r\n" +
            "     <splitwindow divider='0.67' horizontal='false'>\r\n" +
            "      <split>\r\n" +
            "       <splitwindow divider='0.5' horizontal='true'>\r\n" +
            "        <split>\r\n" +
            "         <tabwindow>\r\n" +
            "          <tab isActive='true'>\r\n" +
            "           <view title='")
        .append(toHTML(MagellanDesktop.DETAILS_IDENTIFIER)).append("'>\r\n" +
            "           </view>\r\n" +
            "          </tab>\r\n" +
            "         </tabwindow>\r\n" +
            "        </split>\r\n" +
            "        <split>\r\n" +
            "         <splitwindow divider='0.2' horizontal='false'>\r\n" +
            "          <split>\r\n" +
            "           <tabwindow>\r\n" +
            "            <tab isActive='true'>\r\n" +
            "             <view title='")
        .append(toHTML(MagellanDesktop.NAMEDESCRIPTION_IDENTIFIER)).append("'>\r\n" +
            "             </view>\r\n" +
            "            </tab>\r\n" +
            "           </tabwindow>\r\n" +
            "          </split>\r\n" +
            "          <split>\r\n" +
            "           <splitwindow divider='0.67' horizontal='false'>\r\n" +
            "            <split>\r\n" +
            "             <tabwindow>\r\n" +
            "              <tab isActive='true'>\r\n" +
            "               <view title='")
        .append(toHTML(MagellanDesktop.ORDERS_IDENTIFIER)).append("'>\r\n" +
            "               </view>\r\n" +
            "              </tab>\r\n" +
            "             </tabwindow>\r\n" +
            "            </split>\r\n" +
            "            <split>\r\n" +
            "             <tabwindow>\r\n" +
            "              <tab isActive='true'>\r\n" +
            "               <view title='")
        .append(toHTML(MagellanDesktop.MESSAGES_IDENTIFIER)).append("'>\r\n"
            +
            "               </view>\r\n" +
            "              </tab>\r\n" +
            "             </tabwindow>\r\n" +
            "            </split>\r\n" +
            "           </splitwindow>\r\n" +
            "          </split>\r\n" +
            "         </splitwindow>\r\n" +
            "        </split>\r\n" +
            "       </splitwindow>\r\n" +
            "      </split>\r\n" +
            "      <split>\r\n" +
            "       <splitwindow divider='0.67' horizontal='true'>\r\n"
            +
            "        <split>\r\n" +
            "         <tabwindow>\r\n" +
            "          <tab isActive='true'>\r\n" +
            "           <view title='")
        .append(toHTML(MagellanDesktop.TASKS_IDENTIFIER)).append("'>\r\n" +
            "           </view>\r\n" +
            "          </tab>\r\n" +
            "         </tabwindow>\r\n" +
            "        </split>\r\n" +
            "        <split>\r\n" +
            "         <splitwindow divider='0.5' horizontal='true'>\r\n"
            +
            "          <split>\r\n" +
            "           <tabwindow>\r\n" +
            "            <tab isActive='true'>\r\n" +
            "             <view title='")
        .append(toHTML(
            MagellanDesktop.HISTORY_IDENTIFIER)).append("'>\r\n" +
                "             </view>\r\n" +
                "            </tab>\r\n" +
                "           </tabwindow>\r\n" +
                "          </split>\r\n" +
                "          <split>\r\n" +
                "           <tabwindow>\r\n" +
                "            <tab isActive='true'>\r\n" +
                "             <view title='")
        .append(toHTML(MagellanDesktop.BOOKMARKS_IDENTIFIER)).append("'>\r\n" +
            "             </view>\r\n" +
            "            </tab>\r\n" +
            "           </tabwindow>\r\n" +
            "          </split>\r\n" +
            "         </splitwindow>\r\n" +
            "        </split>\r\n" +
            "       </splitwindow>\r\n" +
            "      </split>\r\n" +
            "     </splitwindow>\r\n" +
            "    </split>\r\n" +
            "   </splitwindow>\r\n" +
            " </rootwindow>\r\n");

    DocumentBuilder builder;
    try {
      builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document document = builder.parse(new ByteArrayInputStream(buffer.toString().getBytes(StandardCharsets.UTF_8)));
      return document.getDocumentElement();
    } catch (ParserConfigurationException e) {
      throw new LayoutException("could not create default layout", e);
    } catch (SAXException e) {
      throw new LayoutException("could not create default layout", e);
    } catch (IOException e) {
      throw new LayoutException("could not create default layout", e);
    }
  }

  private static String toHTML(String str) {
    return str.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll(
        "\"", "&quot;").replaceAll("'", "&apos;");
  }

  /**
   * Returns the value of name.
   *
   * @return Returns name.
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the value of name.
   *
   * @param name The value for name.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the value of root.
   *
   * @return Returns root.
   */
  public Element getRoot() {
    return root;
  }

  /**
   * Sets the value of root.
   *
   * @param root The value for root.
   */
  public void setRoot(Element root) {
    this.root = root;
  }

  /**
   * Returns the value of isActive.
   *
   * @return Returns isActive.
   */
  public boolean isActive() {
    return isActive;
  }

  /**
   * Sets the value of isActive.
   *
   * @param isActive The value for isActive.
   */
  public void setActive(boolean isActive) {
    this.isActive = isActive;
  }

  /**
   * An exception during docking layout handling.
   */
  public static class LayoutException extends Exception {

    /**
     * @see Exception#Exception()
     */
    public LayoutException() {
      super();
    }

    /**
     * @see Exception#Exception(String)
     */
    public LayoutException(String message) {
      super(message);
    }

    /**
     * @see Exception#Exception(Throwable)
     */
    public LayoutException(Throwable cause) {
      super(cause);
    }

    /**
     * @see Exception#Exception(String, Throwable)
     */
    public LayoutException(String message, Throwable cause) {
      super(message, cause);
    }

  }

}
