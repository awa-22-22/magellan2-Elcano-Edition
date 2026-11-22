# Building Magellan EE with Maven

The project is built with Maven. The build produces a single executable
**fat jar** that contains all compiled classes, all libraries and the help, and
a runnable distribution zip. All libraries come from Maven Central. (The former
Ant build, `build.xml`, has been removed, see the end of this file.)

## Requirements

- JDK 11 or newer (the build targets Java 11)
- Maven 3.6+
- On the first build, internet access to Maven Central is required (for the
  build plugins and the test dependencies).

## Building

```bash
mvn package
```

This compiles all sources, runs the unit tests, and creates both

```
target/magellan-ee-1.0.0-SNAPSHOT.jar          # the fat jar alone
target/magellan-ee-1.0.0-SNAPSHOT-dist.zip     # a runnable distribution (see below)
```

To skip the tests for a quick build:

```bash
mvn package -DskipTests
```

## Running

Magellan reads its configuration and data files (`etc/`) from the
working directory at runtime — only the classpath and the help (see below)
are bundled into the jar.
The jar alone is therefore **not standalone**: running
`java -jar target/magellan-ee-1.0.0-SNAPSHOT.jar` only works if an `etc/` directory
happens to be reachable from the current directory (e.g. the project root,
since `Resources.initialize` also checks one directory up), and otherwise
fails with `RuntimeException: Could NOT find location Magellan`.

For a copy that works from anywhere, unpack the distribution zip and run the
jar from inside it (or use the provided start script):

```bash
unzip target/magellan-ee-1.0.0-SNAPSHOT-dist.zip -d /somewhere
cd /somewhere/magellan-ee-1.0.0-SNAPSHOT
./magellan.sh          # or magellan.bat on Windows, or: java -jar magellan-ee-1.0.0-SNAPSHOT.jar
```

Alternatively, point the jar at the project root explicitly with `-d`
(the directory must exist and be writable, or the option is silently
ignored):

```bash
java -jar target/magellan-ee-1.0.0-SNAPSHOT.jar -d /path/to/magellan2
```

## How the build works

- **Source folders**: the Eclipse source roots `src-library`, `src-client`
  and `src-plugins` are registered as compile source roots, `src-test` as the
  test source root (via `build-helper-maven-plugin`). Compilation uses
  release 11 and ISO-8859-1 encoding.
- **Dependencies**: all libraries are regular dependencies from Maven Central
  (issue #2). Local jars are not allowed: `maven-enforcer-plugin` rejects
  `system`-scoped dependencies and also checks the minimum Java (11) and Maven
  (3.6.3) versions. Dependabot (`.github/dependabot.yml`) proposes updates.
- **Tests**: JUnit 4.13 and Hamcrest 1.3. `E3CommandParserTest` is excluded,
  and the tests run with `-Djava.awt.headless=true`.
- **Help**: the JavaHelp sets (`help/de`, `help/en`) are bundled inside the
  jar under the `help/` prefix, where `Help.java` finds them via the
  classloader. An `index-help` antrun execution (`prepare-package`) copies
  `help/` into `target/classes/help` and runs the JavaHelp `Indexer` there for
  both languages, so the gitignored `JavaHelpSearch/` indexes are generated
  during the build and the source tree stays untouched.
- **Fat jar**: `maven-shade-plugin` merges the jar of `maven-jar-plugin`
  (classes, resources, help) with all compile and runtime dependencies into
  one executable jar (`Main-Class: magellan.client.Client`), which replaces
  the thin jar (kept as `target/original-*.jar`). On duplicate entries the
  first dependency in the order of `pom.xml` wins; `META-INF/services` files
  are merged. Signature files (`META-INF/*.SF`, `*.RSA`, `*.DSA`) and module
  descriptors are excluded, because they would be invalid in a merged jar.
  A new dependency needs no further configuration to end up in the jar.
- **Distribution zip**: `maven-assembly-plugin` runs after the shade plugin
  in the `package` phase (so it picks up the finished jar). It uses the
  descriptor `src/assembly/dist.xml` to lay out a runnable copy: the fat jar,
  the runtime data Magellan reads from the working directory
  (`etc/*.properties`, `etc/*.ini`, `etc/images`, `etc/rules`, `etc/plugins`),
  the generated `etc/VERSION`, `doc/`, the top-level readme/license files, the
  Magellan 2 plugin compatibility jars, an empty `plugins/` folder for user
  plugins, and the start scripts `src/main/scripts/magellan.sh` / `.bat`.
  Not included: `etc/names` (used by the name generator, but not part of the
  classic release layout either).

## Version

The version is defined once in `pom.xml` as `<revision>` (plus `<changelist>`,
`-SNAPSHOT` by default; the CI test-build workflow appends the push time).
Maven filters it into `src/main/resources/magellan-version.properties`, which
is packaged into the jar. At runtime `VersionInfo.getVersion` reads that
classpath resource first and only falls back to the `etc/VERSION` file
if the resource
is missing, e.g. in a plain IDE build. `etc/VERSION` is generated from
`etc/VERSION.template` during `prepare-package` and written into the dist zip.

## Settings directory

The settings directory is chosen in `MagellanFinder.findSettingsDirectory`: the
`-s` option, else the application data folder `Magellan-EE` (see
`MagellanFinder.getAppDataDirectory`), else the program or current folder. The
name differs on purpose from the original's `Magellan` / `.magellan`, so a
test installation never touches the settings of Magellan 2. The user's home
folder itself is not searched for a `magellan.ini` either. See "Settings and
running next to Magellan 2" in the README.

## Former Ant build

Until issue #2 the project also had an Ant build (`build.xml`) with the
libraries in `lib/`. It has been removed together with `.classpath`,
`etc/manifest.mf.template` and `lib/`. The following parts of it were
intentionally not migrated:

- **Installers** (IzPack, install4j, macOS `.app` bundle). The distribution
  zip is the supported way to obtain a runnable copy. The installer
  configuration under `installer/` is still in the repository.
- **Javadoc and source jar**: there are no downstream projects that would use
  them.
- **Build number**: Ant composed the version from `VERSION.MAJOR/MINOR/SUB`
  plus an incremented build number; Maven uses `<revision>` and
  `<changelist>` (see "Version").
- **Four jars plus lib folder**: Ant shipped `magellan-library.jar`,
  `magellan-client.jar`, `magellan-plugins.jar` and `magellan-help.jar` with
  `lib/*.jar`; Maven builds one jar. Splitting the project into Maven modules
  would be needed to get that layout back.

## Notes

- `mvn clean` removes the `target/` directory. `target/` is listed in
  `.gitignore`.
- The Maven build does not create installers (IzPack / install4j), Javadoc,
  or a source jar (see "Former Ant build" above). It does create a runnable
  distribution zip (see "Running" above).
