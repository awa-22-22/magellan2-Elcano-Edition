# Issue #2 – Group C: patched / abandoned / unavailable libraries

Task description for a follow-up session (agent). Upstream issue:
https://github.com/awa-22-22/magellan2-Elcano-Edition/issues/2 ("Modernize the library dependencies",
labels Refactoring, internal). This file covers TODO step 3 of that issue: *replace the patched or
unavailable libraries, splitting the hard ones into their own issues.*

## Status (2026-11-22): done

All packages of this file are finished; the rest of the file is kept as history.

- **C3** docking: InfoNode replaced by Modern Docking 1.4.9 (branch `Issue-#2-New-Docks`).
- **C1** look & feel: old skins removed, FlatLaf 3.7.2 added (same branch).
- **C2** skill chart: JFreeChart 1.5.6 from Maven Central (branch `Issue-#2-C2-jfreechart`).
- **Cleanup** (branch `Issue-#2-cleanup`): langchain4j removed, Ant build (`build.xml`, `.classpath`,
  `etc/manifest.mf.template`, `lib/`) removed, fat jar replaced by a Magellan jar with
  `Class-Path` plus `lib/` (branch `Issue-#2-thin-jar`),
  `maven-enforcer-plugin` bans system scope, Dependabot configured, `mavenbuild.md`/`README.md`
  updated. The installers under `installer/` were kept on purpose.

Still open: exercising the step-2 changes against a real server (order upload, email, version
check, online help, `.bz2` reports) and updating the checklist of issue #2 on GitHub.

## State when this file was written

Steps 1 and 2 are done (not yet committed, see `git status`):

- All libraries that can come from Maven Central now do (commons-codec, commons-email, jakarta.mail,
  commons-logging, jackson via BOM, javahelp, multiline-label, commons-beanutils, commons-compress).
- Ant, commons-httpclient and jimi are gone; `HTTPClient`/`HTTPResult` now use `java.net.http`;
  bzip2 uses commons-compress; macify was unused and dropped.
- The Ant build is **no longer used**. `build.xml`, `.classpath`, `etc/manifest.mf.template` and
  `release/*.jar` are dead leftovers (not yet deleted, ask the user).
- Build: `mvn clean package` (runs the unit tests, builds the fat jar and the dist zip). Java 11
  target, JDK 21 installed.

What is left in `lib/` and as `magellan.local` system-scope dependencies in `pom.xml`:

| Jar | Used by | Problem |
|---|---|---|
| `lib/idw-gpl-1.6.1.jar` | docking framework, 7 files | InfoNode Docking Windows, abandoned |
| `lib/jfreechart.jar` | skill chart, 3 files | ancient `com.jrefinery.chart` API (JFreeChart 0.9.x) |
| `lib/skins/kunststoff-2_0_2.jar` | `etc/plaf.ini` only | abandoned, no Maven artifact |
| `lib/skins/liquidlnf-2.9.1.jar` | `etc/plaf.ini` only | abandoned, no Maven artifact |
| `lib/skins/xplookandfeel.jar` | `etc/plaf.ini` only | abandoned, no Maven artifact |
| `lib/skins/looks-1.3b1.jar` | `SimpleInternalFrame` (comment only), `plaf.ini` | old JGoodies Looks, package `com.jgoodies.plaf` |
| `lib/skins/skinsCollected.jar` | `RendererLoaderTest`, `plaf.ini` | hand-built bundle, see below |

Source/documentation archives for these are in `libsrc/` (`idw-gpl-1.6.1-src.zip`,
`kunststoff-2_0_2.zip`, `liquidlnf-2.9.1.zip`). Do not delete them before the matching library is
replaced or dropped.

## Pitfalls (read first)

1. **`grep` in this environment is a wrapper that silently skips files it considers binary.**
   The Java sources are ISO-8859-1 and contain non-UTF-8 bytes, so a plain `grep -r` misses
   files. In the previous session this produced two wrong "unused" conclusions (jfreechart, Ant's
   bzip2). Use `command grep -a ...` (or `rg -a`) for every usage check, and let the compiler
   confirm: remove the dependency, run `mvn -q compile`, read the errors.
2. **Source encoding is ISO-8859-1** (`project.build.sourceEncoding`). When editing existing Java
   files, preserve the encoding (e.g. edit via Python with `encoding='iso-8859-1'`); the Edit/Write
   tools write UTF-8 and would corrupt umlauts. New files should be ASCII-only.
3. **The fat jar is assembled by hand** in `pom.xml` (maven-antrun-plugin, `build-fat-jar`
   execution): every runtime jar needs its own `<zipfileset src="${group:artifact:jar}" .../>` line,
   including transitive dependencies. The `${group:artifact:jar}` properties come from the
   `maven-dependency-plugin` `properties` goal. Order matters: first occurrence of a duplicate
   entry wins. If a new Central dependency is not added there, the build is green but the app
   fails at runtime with `NoClassDefFoundError`. Consider replacing this with
   `maven-shade-plugin` once no `system`-scope dependencies remain (shade cannot merge system scope).
4. **Look & feels are loaded dynamically** from `etc/plaf.ini` (by class name) and shown only if
   the class is on the classpath. Removing a skin jar therefore compiles fine and only drops the
   entry from the Look & Feel list at runtime. There is no test for this.
5. The unit tests do not cover the GUI. Anything touching docking, charts or look & feel has to be
   checked by actually starting the client (`mvn package`, then `java -jar target/magellan-ee-*.jar`
   or `Client.launch`) and clicking through, or by asking the user to do it. Say clearly what was
   and was not run.
6. Unrelated modified files in `git status` (`.project`, `EresseaMovementEvaluator.java`,
   `test/FileUtils/*`) are not part of this issue. Do not include them in commits.

## Work packages

Each package should become its own GitHub issue (they are independent), referencing #2. Suggested
order: skins first (cheapest, mostly removal), then jfreechart, then idw-gpl.

### C1 – Look & feel skin cluster (kunststoff, liquidlnf, xplookandfeel, looks, skinsCollected)

Facts:
- `etc/plaf.ini` lists ~17 look & feels by class name; most of the old ones (TinyLaF, Metouia,
  WinLAF, PGs, Squareness, SH Farr) have no jar in the repository at all, so they never show up.
  Only entries whose class is on the classpath appear.
- Directly imported by code: none of kunststoff, liquidlnf, xplookandfeel. JGoodies is only
  mentioned in a comment in `SimpleInternalFrame.java` and in `RendererLoaderTest.java`.
- `skinsCollected.jar` has no manifest. It contains: a "Classic(built-in)" theme (SkinLF-style
  `.colors.xml`/`.files.xml`/icons), `com.jgoodies.looks.*` 2.x classes (plastic, windows, common),
  `com.incors.plaf.*` (Kunststoff) and `com.pagosoft.plaf.*` (PGS LaF).
- `pom.xml` comment: skinsCollected bundles a newer JGoodies Looks 2.x whose
  `WindowsMenuItemRenderer` conflicts with the older `looks-1.3b1`; the fat jar excludes
  `com/jgoodies/looks/windows/WindowsMenuItemRenderer*.class` from skinsCollected.
  `RendererLoaderTest` references that class.

Proposed approach (confirm with the user first, it is a product decision):
1. Decide which look & feels still matter. Modern JDKs ship Nimbus, Metal and (platform) Windows /
   GTK / Aqua. The abandoned skins (Kunststoff, Liquid, XP, Plastic from 2005) are very likely not
   worth keeping.
2. For anything worth keeping, use the maintained Central artifacts instead of the checked-in jars:
   JGoodies Looks (`com.jgoodies:jgoodies-looks`, 2.x; package `com.jgoodies.looks`), possibly
   FlatLaf (`com.formdev:flatlaf`) as a modern replacement. **Verify coordinates and versions on
   Maven Central before using them.**
3. Remove the rest from `pom.xml`, the fat jar, `lib/skins/` and the matching `plaf.ini` entries
   (keep `plaf.ini` readable: it is user-facing configuration, check the German/English docs under
   `doc/` and `help/` for mentions).
4. Fix or remove `RendererLoaderTest` dependence on `skinsCollected.jar`.
5. Check what else in `skinsCollected.jar` is used at runtime (the "Classic(built-in)" theme may be
   loaded by a skin library by name; search resources and `plaf.ini` with `command grep -a`).

Acceptance: no `lib/skins/` directory; look & feel dialog still offers the chosen set; client
starts and switches between them; tests green.

### C2 – JFreeChart (`com.jrefinery.chart`) in the skill chart

Facts:
- Used in `src-client/magellan/client/skillchart/`: `SkillChartPanel.java`,
  `SkillChartJFreeChartPanel.java`, `VerticalBarPlot.java`.
- Imported types (all from the pre-1.0 API): `JFreeChart`, `JFreeChartPanel`, `Plot`, `BarPlot`,
  `Bar`, `Axis`, `CategoryAxis`, `HorizontalCategoryAxis`, `VerticalNumberAxis`, `NumberAxis`,
  `HorizontalAxis`, `VerticalAxis`, `VerticalValuePlot`, `DataSource`, `DataSources`,
  `CategoryDataSource`, `DefaultCategoryDataSource`, `AxisNotCompatibleException`.
- `lib/jfreechart.jar` is a very old build (package `com.jrefinery.chart`, ~60 classes). The
  current library is `org.jfree:jfreechart` 1.5.x with a completely different API (`JFreeChart`,
  `ChartPanel`, `CategoryPlot`, `DefaultCategoryDataset`, ...). **Verify the current version on
  Maven Central.**

Proposed approach:
1. Read the three files and `SkillChartPanel`'s callers to understand what is drawn (bar chart of
   skill levels per unit/person, custom `VerticalBarPlot`).
2. Re-implement with `org.jfree:jfreechart` 1.5.x (`ChartFactory.createBarChart`, custom renderer
   if the special bar look is needed). Keep the public behaviour of the skill chart dialog.
3. Alternative if too costly: keep the jar (document it) or drop the chart feature (user decision).
4. Add the new jar and its transitive dependencies to the fat jar list in `pom.xml`.

Acceptance: skill chart opens and renders for a loaded report; no `com.jrefinery` imports remain;
`lib/jfreechart.jar` removed.

### C3 – InfoNode Docking Windows (`idw-gpl-1.6.1.jar`)

Facts:
- Used (`net.infonode.docking.*`, `net.infonode.gui`, `net.infonode.tabbedpanel`) in:
  `DockingFrameworkBuilder`, `MagellanPopupMenuFactory`, `DFBInstance`, `MagellanDesktop`,
  `DockingLayout` (all `src-client/magellan/client/desktop/`), and
  `src-plugins/magellan/plugin/extendedcommands/ExtendedCommandsDock.java` / `ExtendedCommandsDocument.java`.
- Also uses internal API (`net.infonode.docking.internalutil.InternalDockingUtil`) and themes
  (`ShapedGradientDockingTheme`). `DockingLayout` serializes the layout as XML; saved layouts of
  users must keep working or be migrated.
- InfoNode is abandoned (last release ~2012). The jar is the GPL edition 1.6.1. Check whether a
  maintained fork exists on Maven Central; I am not sure of the coordinates, **do not guess,
  search and verify**. Candidates to evaluate: a published fork of InfoNode IDW, **Modern Docking**
  (`io.github.andrewauclair`), **DockingFrames** (`org.dockingframes`), JIDE (commercial, not
  an option).
- This is the largest item: the docking framework is central to the desktop and the layout files.

Proposed approach:
1. Spike first: decide between (a) keeping idw-gpl as the one remaining local jar, e.g. installed
   into a project-local Maven repository or `system` scope documented as a known exception,
   (b) switching to a maintained fork with the same `net.infonode` API (smallest code change),
   (c) migrating to another docking library (large change, needs its own plan).
2. Write the findings and a recommendation to the issue before making code changes; this needs a
   user decision.
3. Whatever is chosen: keep existing saved layouts loadable (`DockingLayout` XML), and keep the
   extendedcommands plugin docks working.

Acceptance: documented decision; if replaced, the desktop starts with default and previously saved
layouts, docking/undocking/floating/maximizing works, plugin docks work.

## Cross-cutting cleanup (after C1-C3, or whenever the user agrees)

- Delete the dead Ant build: `build.xml`, `.classpath`, `etc/manifest.mf.template`, checked-in
  `release/*.jar`, `plugins-compatibility-resources/*.jar`, `magellan-src.jar`,
  `installer/` Ant/IzPack parts (ask first; installers via install4j may still be wanted).
- Remove `lib/README.txt`, empty `lib/`, and the `magellan.local` / `system`-scope block in `pom.xml`
  once nothing is left; then replace the hand-written fat-jar zipfilesets with `maven-shade-plugin`.
- Update `mavenbuild.md` and `README.md` (they still describe Ant and `lib/`).
- Issue step "set up a process to keep them updated": add Dependabot or Renovate configuration
  (`.github/dependabot.yml`, ecosystem `maven`) and consider `versions-maven-plugin` /
  `maven-enforcer-plugin` (e.g. ban `system` scope, dependency convergence). The CI workflow is
  `.github/workflows/build_mvn.yml`.
- Unused declared dependencies: `langchain4j` and `langchain4j-open-ai` are declared in `pom.xml` but
  no Java file uses them and they are not bundled into the fat jar. Ask the user whether they are
  planned for a feature, otherwise remove.
- Known behaviour changes from step 2 that should still be exercised in the real app (never run
  against a real server so far): order upload to the Eressea server (`OrderWriterDialog` ->
  `HTTPClient.postFile`; Basic auth only, hostname verification now active, single self-signed
  certs still accepted), sending orders by email (commons-email 1.6.0), JSON version info
  (Jackson 2.21), online help (javahelp), reading/writing `.bz2` reports (commons-compress).

## How to work

- Create one branch per package; commit with the project's usual style. Only commit when the user
  asks; do not push.
- After each change: `mvn -q clean package` and report test results honestly. Then list what was
  checked in the running client and what was not.
- Check Maven Central coordinates and versions online (repo1.maven.org `maven-metadata.xml`) before
  adding any dependency; pick a release that is at least two weeks old.
- Update the checklist in issue #2 (steps 3-5: replace, review, test) as packages complete.
