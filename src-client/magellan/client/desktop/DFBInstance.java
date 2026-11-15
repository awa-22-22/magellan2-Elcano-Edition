/*
 *  Copyright (C) 2000-2004 Roger Butenuth, Andreas Gampe,
 *                          Stefan Goetz, Sebastian Pappert,
 *                          Klaas Prause, Enno Rehling,
 *                          Sebastian Tusk, Ulrich Kuester,
 *                          Ilja Pavkovic
 *
 * This file is part of the Eressea Java Code Base, see the
 * file LICENSING for the licensing information applying to
 * this file.
 *
 */

package magellan.client.desktop;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;

import org.w3c.dom.Element;

import io.github.andrewauclair.moderndocking.DockableTabPreference;
import io.github.andrewauclair.moderndocking.api.DockingAPI;
import io.github.andrewauclair.moderndocking.app.Docking;
import io.github.andrewauclair.moderndocking.app.RootDockingPanel;
import io.github.andrewauclair.moderndocking.event.DockingEvent;
import io.github.andrewauclair.moderndocking.settings.Settings;
import magellan.client.Client;
import magellan.client.actions.desktop.LayoutCheckboxMenuItem;
import magellan.client.actions.desktop.LayoutDeleteAction;
import magellan.client.actions.desktop.LayoutExportAction;
import magellan.client.actions.desktop.LayoutImportAction;
import magellan.client.actions.desktop.LayoutNewAction;
import magellan.client.actions.desktop.LayoutSaveAction;
import magellan.client.desktop.DockingLayout.LayoutException;
import magellan.client.utils.ErrorWindow;
import magellan.library.utils.Encoding;
import magellan.library.utils.PropertiesHelper;
import magellan.library.utils.Resources;
import magellan.library.utils.logging.Logger;

/**
 * This is a factory for working with the Modern Docking framework.
 *
 * @author Thoralf
 * @version 1.0
 */
public class DFBInstance implements DockingFrameworkBuilder {
  private static final Logger log = Logger.getInstance(DockingFrameworkBuilder.class);

  private List<Component> componentsUsed;
  /** the dockables by component key */
  private Map<String, MagellanDockable> dockables = new LinkedHashMap<String, MagellanDockable>();
  private RootDockingPanel rootPanel = null;
  private boolean showTabs = true;

  private JMenu layoutMenu = null;
  private JCheckBoxMenuItem hideTabs = null;
  private LayoutDeleteAction deleteMenu = null;
  private List<DockingLayout> layouts = new ArrayList<DockingLayout>();
  private DockingLayout activeLayout = null;

  private Properties settings = null;
  private MagellanDesktop desktop;

  /**
   * Creates new DockingFrameworkBuilder
   *
   * @param desktop
   */
  public DFBInstance(MagellanDesktop desktop) {
    this.desktop = desktop;
    componentsUsed = new LinkedList<Component>();
  }

  /**
   * Returns true for the keys of components that are no longer shown as docks.
   */
  private static boolean isDeprecated(String key) {
    return key.equals(MagellanDesktop.COMMANDS_IDENTIFIER)
        || key.equals(MagellanDesktop.NAME_IDENTIFIER)
        || key.equals(MagellanDesktop.DESCRIPTION_IDENTIFIER)
        || key.equals(MagellanDesktop.OVERVIEWHISTORY_IDENTIFIER);
  }

  /**
   * This method builds the desktop. This is the main component inside Magellan. It contains the
   * root docking panel with all docks.
   */
  public JComponent buildDesktop(Map<String, Component> components, File serializedView) {
    componentsUsed.clear();
    componentsUsed.addAll(components.values());

    Window mainWindow = desktop.getClientFrame();
    Docking.initialize(mainWindow);
    rootPanel = new RootDockingPanel(mainWindow);

    dockables.clear();
    for (String key : components.keySet()) {
      if (isDeprecated(key)) {
        continue;
      }
      MagellanDockable dockable =
          new MagellanDockable(key, components.get(key), Resources.get("dock." + key + ".title"),
              Resources.get("dock." + key + ".tooltip"));
      Docking.registerDockable(dockable);
      dockables.put(key, dockable);
    }
    Docking.addDockingListener(this::dockingChanged);

    showTabs =
        !PropertiesHelper.getBoolean(settings, PropertiesHelper.CLIENTPREFERENCES_DONT_SHOW_TABS,
            false);
    applyTabPreference();

    try {
      if (serializedView != null && serializedView.exists()) {
        read(serializedView);
      }
      if (activeLayout == null) {
        createDefault();
      }
    } catch (Throwable t) {
      DFBInstance.log.fatal(t.getMessage(), t);
      ErrorWindow errorWindow = new ErrorWindow(Client.INSTANCE, t);
      errorWindow.setVisible(true);
    }

    return rootPanel;
  }

  private DockingAPI docking() {
    return Docking.getSingleInstance();
  }

  /**
   * Keeps the desktop menu and the dock components informed about opened and closed docks.
   */
  private void dockingChanged(DockingEvent event) {
    if (!(event.getDockable() instanceof MagellanDockable))
      return;
    MagellanDockable dockable = (MagellanDockable) event.getDockable();
    switch (event.getID()) {
    case DOCKED:
      desktop.setActive(dockable.getPersistentID());
      dockable.fireShown();
      break;
    case UNDOCKED:
      if (!event.isTemporary()) {
        desktop.setInActive(dockable.getPersistentID());
        dockable.fireHidden();
      }
      break;
    case SHOWN:
      dockable.fireShown();
      break;
    case HIDDEN:
      dockable.fireHidden();
      break;
    default:
      break;
    }
  }

  /**
   * Shows the given layout in the docking framework.
   */
  private void open(DockingLayout layout) {
    layout.open(docking(), dockables.keySet(), showTabs);
  }

  /**
   * Stores the state of the docking framework in the active layout.
   */
  private void captureActiveLayout() {
    if (activeLayout != null && rootPanel != null) {
      activeLayout.capture(docking());
    }
  }

  /**
   * This method writes a docking configuration to the given file.
   */
  public void write(File serializedViewData) throws IOException {
    DFBInstance.log.info("Storing docking layout in " + serializedViewData);
    captureActiveLayout();

    StringBuffer buffer = new StringBuffer();
    buffer.append("<?xml version='1.0' encoding='" + Encoding.DEFAULT.toString() + "'?>\r\n");
    buffer.append("<dock version='1.0'>\r\n");
    for (DockingLayout layout : layouts) {
      layout.save(buffer);
    }
    buffer.append("</dock>\r\n");

    PrintWriter pw = new PrintWriter(serializedViewData, Encoding.DEFAULT.toString());
    pw.println(buffer.toString());
    pw.close();
  }

  /**
   * This method reads a docking configuration from the given file and opens the active layout.
   */
  protected synchronized void read(File serializedViewData) {
    DFBInstance.log.info("Loading Docking Layouts");

    List<DockingLayout> loaded = DockingLayout.load(serializedViewData);
    if (loaded == null || loaded.isEmpty()) {
      log.warn("Could not load any docking layouts.");
      return;
    }
    layouts = loaded;
    DFBInstance.log.info("Loaded " + layouts.size() + " Docking layouts.");

    activeLayout = null;
    for (DockingLayout layout : layouts) {
      if (layout.isActive()) {
        if (activeLayout == null) {
          activeLayout = layout;
        } else {
          layout.setActive(false);
        }
      }
    }
    if (activeLayout == null) {
      activeLayout = layouts.get(0);
    }
    activeLayout.setActive(true);
    try {
      open(activeLayout);
    } catch (RuntimeException e) {
      DFBInstance.log.error(e);
      ErrorWindow errorWindow = new ErrorWindow("Could not load docking layouts.", e);
      errorWindow.open();
    }
  }

  /**
   * Adds all docking layouts from the given file to the currently available layouts. all layouts
   * are disabled and layouts with the same name are renamed
   */
  public synchronized void addLayouts(File file) {
    List<DockingLayout> newLayouts = DockingLayout.load(file);
    if (newLayouts != null) {
      for (DockingLayout layout : newLayouts) {
        layout.setActive(false);
        layout.setName(findNewName(layouts, layout.getName(), layout.getName(), 0));
        layouts.add(layout);
      }
      updateLayoutMenu();
    }
  }

  /**
   * Searches for a layout with the given name. If it is not found, then the name is returned. If it
   * is found, then it returns the name "name (x)".
   */
  private synchronized String findNewName(List<DockingLayout> layouts, String originalName,
      String name, int suffix) {
    for (DockingLayout layout : layouts) {
      if (layout.getName().equalsIgnoreCase(name)) {
        String newName = originalName + " (" + (++suffix) + ")";
        return findNewName(layouts, originalName, newName, suffix);
      }
    }
    return name;
  }

  /**
   * Creates a default Docking layout and opens it.
   */
  protected synchronized void createDefault() {
    Element root;
    try {
      root = DockingLayout.createDefaultLayout("Standard", true);
    } catch (LayoutException e) {
      ErrorWindow errorWindow = new ErrorWindow(null, "Could not create default docking layout.", e);
      errorWindow.open();
      throw new RuntimeException(e);
    }
    DockingLayout defaultLayout = new DockingLayout("Standard", root);
    defaultLayout.setActive(true);
    layouts.add(defaultLayout);

    activeLayout = defaultLayout;
    open(activeLayout);
  }

  /**
   * This method creates the desktop menu. It contains the layout submenu and all dock components.
   */
  public JMenu createDesktopMenu(Map<String, Component> components, Properties settings,
      ActionListener listener) {
    JMenu desktopMenu = new JMenu(Resources.get("desktop.magellandesktop.menu.desktop.caption"));
    desktopMenu.setMnemonic(Resources.get("desktop.magellandesktop.menu.desktop.mnemonic")
        .charAt(0));

    layoutMenu =
        new JMenu(Resources.get("desktop.magellandesktop.menu.desktop.layout.caption"));

    ButtonGroup group = new ButtonGroup();
    for (DockingLayout layout : layouts) {
      LayoutCheckboxMenuItem item = new LayoutCheckboxMenuItem(layout, this);
      group.add(item);
      layoutMenu.add(item);
    }

    deleteMenu = new LayoutDeleteAction(this);
    deleteMenu.setEnabled(layouts.size() > 1);

    layoutMenu.addSeparator();
    layoutMenu.add(new LayoutExportAction(this));
    layoutMenu.add(new LayoutImportAction(this));
    layoutMenu.addSeparator();
    layoutMenu.add(new LayoutNewAction(this));
    layoutMenu.add(new LayoutSaveAction(this));
    layoutMenu.add(deleteMenu);

    desktopMenu.add(layoutMenu);

    hideTabs =
        new JCheckBoxMenuItem(Resources
            .get("desktop.magellandesktop.menu.desktop.hidetabs.caption"), PropertiesHelper
                .getBoolean(settings, PropertiesHelper.CLIENTPREFERENCES_DONT_SHOW_TABS, false));
    hideTabs.setActionCommand("hideTabs");
    desktopMenu.add(hideTabs);
    hideTabs.addActionListener(listener);

    desktopMenu.addSeparator();

    for (String key : components.keySet()) {
      if (isDeprecated(key)) {
        continue;
      }

      JCheckBoxMenuItem item =
          new JCheckBoxMenuItem(Resources.get("dock." + key + ".title"), false);
      item.setActionCommand("menu." + key);
      desktopMenu.add(item);
      item.addActionListener(listener);
    }

    return desktopMenu;
  }

  /**
   * Updates all Docking Layouts in the Desktop--Layout menu by removing them and recreate the list
   * of available docking layouts.
   */
  public void updateLayoutMenu() {
    if (layoutMenu == null)
      return;

    // remove all layout items from the menu
    for (int i = 0; i < layoutMenu.getItemCount(); i++) {
      if (layoutMenu.getItem(i) instanceof LayoutCheckboxMenuItem) {
        layoutMenu.remove(i);
        i--;
      }
    }

    // add all available items.
    int i = 0;
    ButtonGroup group = new ButtonGroup();
    for (DockingLayout layout : layouts) {
      LayoutCheckboxMenuItem item = new LayoutCheckboxMenuItem(layout, this);
      group.add(item);
      layoutMenu.insert(item, i++);
    }

    deleteMenu.setEnabled(layouts.size() > 1);
  }

  /**
   * Shows or hides the tab of docks that are alone in their area. The current arrangement is kept.
   */
  public void setTabVisibility(boolean show) {
    if (hideTabs != null) {
      hideTabs.setSelected(!show);
    }
    if (show == showTabs)
      return;
    showTabs = show;
    applyTabPreference();
    if (activeLayout != null && rootPanel != null) {
      captureActiveLayout();
      open(activeLayout);
    }
  }

  private void applyTabPreference() {
    Settings.setDefaultTabPreference(showTabs ? DockableTabPreference.TOP_ALWAYS
        : DockableTabPreference.TOP);
  }

  /**
   * Returns the docking layout with the given name or null, if there is no layout with this name.
   */
  public DockingLayout getLayout(String name) {
    for (DockingLayout layout : layouts) {
      if (layout.getName().equalsIgnoreCase(name))
        return layout;
    }
    return null;
  }

  /**
   * This method creates a new layout with the given name and the default settings.
   */
  public void createNewLayout(String name) {
    Element root;
    try {
      root = DockingLayout.createDefaultLayout(name, false);
    } catch (LayoutException e) {
      ErrorWindow errorWindow = new ErrorWindow("Could not create default docking layout.", e);
      errorWindow.open();
      throw new RuntimeException(e);
    }

    DockingLayout layout = new DockingLayout(name, root);
    layouts.add(layout);

    setActiveLayout(layout);

    updateLayoutMenu();
  }

  /**
   * Deletes the current layout and selects the first layout.
   */
  public void deleteCurrentLayout() {
    if (layouts.size() <= 1)
      return;
    if (activeLayout == null)
      return;

    DockingLayout deletableLayout = activeLayout;
    log.debug("Remove docking layout '" + deletableLayout.getName() + "'");
    // if the first layout is active, use the second layout.
    int index = layouts.indexOf(activeLayout) == 0 ? 1 : 0;
    setActiveLayout(layouts.get(index));

    layouts.remove(deletableLayout);

    updateLayoutMenu();
  }

  /**
   * Enables the specified docking layout.
   */
  public void setActiveLayout(DockingLayout layout) {
    DFBInstance.log.info("Set docking layout '" + layout.getName() + "'");
    if (activeLayout != null) {
      captureActiveLayout();
      activeLayout.setActive(false);
    }
    activeLayout = layout;
    activeLayout.setActive(true);
    open(activeLayout);
  }

  /**
   * Returns the list of used components
   */
  public List<Component> getComponentsUsed() {
    return componentsUsed;
  }

  /**
   *
   */
  public void setProperties(Properties settings) {
    this.settings = settings;
  }

  /**
   * Opens or closes a specific dock.
   */
  public void setVisible(String componentName, boolean setVisible) {
    MagellanDockable dockable = dockables.get(componentName);
    if (dockable == null)
      return;
    if (setVisible) {
      Docking.display(dockable);
    } else if (Docking.isDocked(dockable)) {
      Docking.undock(dockable);
    }
  }

  /**
   * Returns true if the dock is open.
   */
  public boolean isVisible(String componentName) {
    MagellanDockable dockable = dockables.get(componentName);
    return dockable != null && Docking.isDocked(dockable);
  }

  /**
   * Opens the dock if necessary and brings it to front.
   */
  public void restoreView(String componentName) {
    setVisible(componentName, true);
  }
}
