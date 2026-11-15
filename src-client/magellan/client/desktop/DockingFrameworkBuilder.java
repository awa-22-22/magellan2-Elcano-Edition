package magellan.client.desktop;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import javax.swing.JComponent;
import javax.swing.JMenu;

/**
 * Builds and manages the docking desktop. Implementations hide the docking library; callers only
 * use the component keys (e.g. {@link MagellanDesktop#MAP_IDENTIFIER}).
 */
public interface DockingFrameworkBuilder {

  /**
   * Replaces the current layout by the given one.
   */
  void setActiveLayout(DockingLayout layout);

  /**
   * Creates the "Desktop" menu with the layout sub menu and one entry per dock.
   */
  public JMenu createDesktopMenu(Map<String, Component> components, Properties settings,
      ActionListener listener);

  /**
   * Builds the desktop for the given components and loads the layouts from the file.
   *
   * @return the component containing all docks
   */
  JComponent buildDesktop(Map<String, Component> components, File file);

  /**
   * Returns all components managed by the desktop.
   */
  Collection<Component> getComponentsUsed();

  /**
   * Sets the client settings.
   */
  void setProperties(Properties settings);

  /**
   * Refreshes the list of layouts in the layout menu.
   */
  void updateLayoutMenu();

  /**
   * Writes all layouts to the given file.
   */
  void write(File file) throws IOException;

  /**
   * Opens or closes the dock with the given key.
   */
  void setVisible(String viewName, boolean setVisible);

  /**
   * Returns true if the dock with the given key is open.
   */
  boolean isVisible(String viewName);

  /**
   * Opens the dock with the given key (if necessary) and brings it to front.
   */
  void restoreView(String viewName);

  /**
   * Shows or hides the tab of docks that are alone in their area.
   */
  void setTabVisibility(boolean b);

  /**
   * Deletes the active layout.
   */
  void deleteCurrentLayout();

  /**
   * Adds the layouts from the given file.
   */
  void addLayouts(File selectedFile);

  /**
   * Returns the layout with the given name or null.
   */
  Object getLayout(String newLayoutName);

  /**
   * Creates a new layout with the default arrangement and activates it.
   */
  void createNewLayout(String newLayoutName);

}
