# Issue #2 / C3 - InfoNode Docking Windows: analysis and replacement plan

Follow-up to `TODO-issue2-group-C.md`, package C3. Written 2026-11-15. No code has been changed.

## 1. Current behaviour

### Where InfoNode is used (`lib/idw-gpl-1.6.1.jar`)

| File | Lines | Role |
|---|---|---|
| `src-client/.../desktop/DockingFrameworkBuilder.java` | 54 | Interface, but leaks `RootWindow`, `View`, `StringViewMap` |
| `src-client/.../desktop/DFBInstance.java` | 533 | Only implementation: creates views, root window, theme, layout menu, layout list |
| `src-client/.../desktop/DockingLayout.java` | 851 | One named layout: XML <-> InfoNode window tree |
| `src-client/.../desktop/MagellanPopupMenuFactory.java` | 408 | Right-click menu on tabs/windows |
| `src-client/.../desktop/MagellanDesktop.java` | 1562 | Holds the `RootWindow`, desktop menu checkboxes, listener (`DockingWindowListener`) |
| `src-plugins/.../extendedcommands/ExtendedCommandsDock.java` | 567 | Uses InfoNode `TabbedPanel`/`TitledTab` *inside* its dock + `DockingWindowListener` |
| `src-plugins/.../extendedcommands/ExtendedCommandsDocument.java` | 465 | Holds a reference to its `TitledTab` |

(`.kilo/worktrees/emphasized-sponge/` contains stale copies of the same files, ignore them.)

Not affected: the plugin API. `MagellanPlugIn.getDocks()` returns `Map<String, Component>`,
`BattleDock` only calls `MagellanDesktop.restoreView(String)`, `DesktopPreferences` only calls
`DockingFrameworkBuilder.setTabVisibility(boolean)`. The other plugins (GroupEditor, MapEdit,
BattleAnalyzer) need no change.

### Startup / views
- `MagellanDesktop.initSplitSet()` -> `DFBInstance.buildDesktop(components, <settingsDir>/dock-default.xml)`.
- Every component (core docks + plugin docks) becomes a `View` named by its key (`OVERVIEW`, `MAP`,
  `MINIMAP`, `DETAILS`, `NAME&DESCRIPTION`, `ORDERS`, `MESSAGES`, `TASKS`, `HISTORY`, `BOOKMARKS`,
  plugin IDs, ...). Title/tooltip from `Resources` `dock.<key>.title/.tooltip`. Four deprecated keys
  are skipped. If the component implements `DockingWindowListener` it is registered on its view.
- Theme: `ShapedGradientDockingTheme` with most borders/backgrounds nulled, so it mostly follows the LaF.
  Bottom window bar enabled.

### Layout persistence (the important part)
- The file `dock-default.xml` is **Magellan's own XML format**, written and parsed by `DockingLayout`
  by hand. It does **not** use InfoNode's serialization. So the file format can be kept unchanged
  with any docking library.
- Structure: `<dock version='1.0'>` with several `<rootwindow name=.. isActive=..>` (named layouts);
  inside: `windowbar direction=Up|Down|Left|Right`, `floatingwindow x y width height isVisible`,
  `splitwindow divider=<float 0..1> horizontal=<bool>` with exactly two `<split>`,
  `tabwindow` with `<tab isActive=..>`, `view title=<key>`.
- Real files on this machine (`~/.local/share/Magellan-EE/default/dock-default.xml`) have one layout,
  an empty bottom window bar, no floating windows.
- Multiple named layouts: Desktop > Layout menu with switch, new (= default tree), save, delete,
  export, import (imported names get " (n)" suffixes). Switching = save tree of the old layout into
  its DOM element, remove all views, build the new tree.

### Runtime features
- Desktop menu: one checkbox per dock (open = `view.restore(); view.makeVisible()`, close =
  `window.close()`), kept in sync via `windowAdded`/`windowRemoved`/`windowClosed`.
- "Hide tabs" option: `TabAreaVisiblePolicy.MORE_THAN_ONE_TAB` vs `ALWAYS` (also in preferences).
- Popup menu: undock (float), dock, restore, minimize (to window bar), maximize, close,
  close others, move to window bar (4 directions), tab area orientation (4), tab text direction (3),
  split ratio 25/50/75 %, flip split orientation, mirror split, "show view" (all views).
- Drag & drop of tabs/views (InfoNode built-in).
- `ExtendedCommandsDock`: InfoNode `TabbedPanel` with reorderable tabs and a close button per tab,
  one tab per unit/container script. Listener only uses `windowShown`/`windowHidden` to set `visible`.

### Existing bug noticed
`MagellanDesktop.windowRemoved` calls `setInActive(removedFromWindow.getName())` - should almost
certainly be `removedWindow`. Keep in mind when re-implementing the menu sync.

## 2. Options

| | Option | Assessment |
|---|---|---|
| a | Keep `idw-gpl-1.6.1.jar` as documented exception (system scope or project-local Maven repo) | Zero risk, zero effort. Blocks "no system scope" and the switch to `maven-shade-plugin`. Library unmaintained since 2009. |
| b | Maintained fork with same `net.infonode` API | **No fork on Maven Central** (checked `repo1.maven.org/maven2/net/infonode/` -> 404). Best fork: `ebourg/infonode` branch `contrib` (MIT-licensed fixes, `net.infonode:infonode-docking-windows:1.6.1`, last commit 2025-02-21). Would have to be built and vendored ourselves, so still a local jar - only marginally better than (a). |
| c1 | **Modern Docking** `io.github.andrewauclair:modern-docking-api` + `modern-docking-single-app` **1.4.9** (2026-06-10) | On Central, actively maintained, MIT, Java 11 bytecode (matches our target), api has no dependencies (`-ui` module adds FlatLaf). Covers splits, tabs, floating windows, maximize, auto-hide toolbars, events, layout API. **Recommended.** |
| c2 | DockingFrames `org.dockingframes:docking-frames-common` 1.1.2p6a | Last release 2013. Same staleness problem as InfoNode. Rejected. |

Note: search.maven.org's index is stale for Modern Docking (shows 1.1.2); `maven-metadata.xml` shows 1.4.9.

## 3. Feature mapping InfoNode -> Modern Docking

| Magellan / InfoNode | Modern Docking | Gap |
|---|---|---|
| `View` wrapping a component | Class `MagellanDockable extends JPanel implements Dockable` wrapping the component; `getPersistentID()` = key | none |
| `RootWindow` | `Docking.initialize(clientFrame)` + `RootDockingPanel` as workspace content | none |
| `SplitWindow(horizontal, divider)` | `DockingSplitPanelNode(left, right, orientation, proportion)`; InfoNode `horizontal=true` = side by side = `JSplitPane.HORIZONTAL_SPLIT` | verify orientation mapping |
| `TabWindow` + selected tab | `DockingTabPanelNode` + selected ID | none |
| `windowbar` Down/Left/Right | auto-hide toolbars SOUTH/WEST/EAST (`WindowLayout.set*AutoHideToolbarIDs`) | **no UP/north toolbar** - map Up to West or drop |
| `floatingwindow x y w h` | floating `WindowLayout` in `ApplicationLayout.addFrame(...)` | none |
| restore + makeVisible | `Docking.display(id)` (or `dock` if not docked) | none |
| close | `Docking.undock(id)` | none |
| maximize / minimize | `maximize` / `autoHideDockable` | none |
| `DockingWindowListener` | `DockingListener` events DOCKED/UNDOCKED/SHOWN/HIDDEN/AUTO_HIDE_* | none for our usage |
| Hide tabs (`MORE_THAN_ONE_TAB`/`ALWAYS`) | `Settings.setDefaultTabPreference(TOP / TOP_ALWAYS)` | check if it applies at runtime or only to new tab panels |
| Popup menu | Built-in header menu (close, float, auto-hide, maximize) + `Dockable.addMoreOptions(JPopupMenu)` | **lost**: tab orientation, tab text direction, split 25/50/75 %, flip, mirror, "move to window bar" direction choice |
| `ShapedGradientDockingTheme` | Follows the Swing LaF; FlatLaf recommended (ties in with C1) | visual change |
| Multiple named layouts, import/export | Keep Magellan's own `DockingLayout` list; each converts to/from `ApplicationLayout` | none - keep our code |

Layout file decision: **keep writing our own `dock-default.xml` format** and convert in code
(our XML tree <-> `WindowLayout` node tree). Users' saved layouts and exported layout files keep
working in both directions, no migration step needed. Do not switch to Modern Docking's own XML.

## 4. Re-implementation plan (lean variant)

No separate refactoring pass with InfoNode still in place, and no separate Swing-free layout model:
the XML DOM is converted directly to and from Modern Docking's layout objects. Cost: one larger
step 3 that can only be verified as a whole by starting the client.

1. **ExtendedCommandsDock: replace InfoNode `TabbedPanel` with `JTabbedPane`** and a tab component
   with a close button (`setTabComponentAt`). Tab reordering by drag is lost unless implemented
   (small) or provided by FlatLaf. Independent of the rest, can be done and committed first.
2. **Replace `DockingWindowListener`** on dock components with a small Magellan interface, e.g.
   `magellan.client.desktop.DockVisibilityListener { dockShown(); dockHidden(); }`
   (`ExtendedCommandsDock` only uses shown/hidden). `MagellanDesktop.addDockingWindowListener` has
   no callers and can go.
3. **Switch the desktop to Modern Docking in one step:**
   - `DockingFrameworkBuilder`: drop `View`, `RootWindow`, `StringViewMap` from the signatures
     (`buildDesktop` returns `JComponent`; `setVisible(String, boolean)`, `restoreView(String)`).
   - Wrapper `MagellanDockable extends JPanel implements Dockable` per dock component.
   - `DockingLayout`: replace the InfoNode tree code with XML DOM <-> `WindowLayout` /
     `ApplicationLayout` conversion (both directions); keep the file format, the default layout and
     the named-layout handling. Handle views missing from the layout (plugin removed) and views
     missing from the file (new plugin) like today.
   - `DFBInstance`: `Docking.initialize`, `RootDockingPanel`, register dockables, switch layouts via
     `restoreApplicationLayout`, menu sync via `DockingListener`, hide tabs via
     `Settings.setDefaultTabPreference`. Fix the `windowRemoved` bug on the way.
   - Delete `MagellanPopupMenuFactory` and the InfoNode listener stubs in `MagellanDesktop`.
   - `pom.xml`: remove the `idw-gpl` system dependency and its fat-jar zipfileset, add
     `modern-docking-api` and `modern-docking-single-app` (and their zipfilesets!); delete
     `lib/idw-gpl-1.6.1.jar` and `libsrc/idw-gpl-1.6.1-src.zip`.
4. **Unit tests:** XML -> layout -> XML round trip for the default layout and a copy of a real
   `dock-default.xml` (incl. a floating window and a bottom window bar). The only part testable
   without the GUI.
5. **Manual GUI test (user):** start with no `dock-default.xml`, with an existing one, with a
   layout containing a floating window and a bottom window bar; switch/create/delete/import/export
   layouts; Desktop menu checkboxes; hide tabs; drag, float, maximize, auto-hide; extended commands
   dock tabs; battle analyzer `restoreView`; restart and check the layout was saved.

### Estimated size

About 1,000 deleted lines, mostly mechanical:
- `MagellanPopupMenuFactory` (408 lines, replaced by Modern Docking's built-in menu),
- no-op `DockingWindowListener` methods (165 lines in `MagellanDesktop`, 122 in `ExtendedCommandsDock`),
- InfoNode tree building/reading in `DockingLayout` (~280 lines).

About 500 new lines:

| Part | Approx. lines |
|---|---|
| XML DOM <-> `WindowLayout`/`ApplicationLayout` conversion | 200 |
| `DFBInstance` adaptation (dockables, layout switch, menu sync, hide tabs) | 120 |
| `MagellanDockable` wrapper | 40 |
| `ExtendedCommandsDock` with `JTabbedPane` + close button | 60 |
| `DockVisibilityListener` | 10 |
| Round-trip unit tests | 100 |

Step 3 is the real work and the only risky part.

## 5. Decisions (user, 2026-11-15)

1. Library: **Modern Docking** (option c1).
2. Losing the popup-menu extras (tab orientation/direction, split ratios, flip, mirror) and the top
   ("Up") window bar is **accepted**. Map a saved `Up` window bar to West.
3. The LaF-driven look instead of the InfoNode gradient theme is **accepted**.
4. **No GitHub operations** (issues, comments, pushes) without asking the user first.
