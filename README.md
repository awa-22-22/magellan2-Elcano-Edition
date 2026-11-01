# Magellan Elcano Edition (EE)

**Magellan Elcano Edition (EE)** is a client tool for the play-by-e-mail game [Eressea](https://www.eressea.de/). It displays a map of your part of the Eressea world, supports you in writing orders and performs many complex calculations for you.

It is a hard fork of [Magellan 2](https://github.com/magellan2/magellan2). Magellan EE continues on its own: it has its own version numbers, its own releases and its own issue tracker, and it does not follow the original project any more. It is **not an official Magellan release**, and the original developers are not responsible for it.

Magellan EE is only one among many client programs for Eressea and is in no way related to the developers of Eressea.

## Support

Please report problems and ideas for Magellan EE in the [issue tracker](https://github.com/awa-22-22/magellan2-Elcano-Edition/issues) of this repository, not to the original Magellan project.

The program does not contact any server on its own. In particular, there is no check for newer versions; new versions are published under [Releases](https://github.com/awa-22-22/magellan2-Elcano-Edition/releases).

## How To Build

You need JDK 11 or newer and Maven 3.6+ (see https://maven.apache.org/). On the first build, internet access to Maven Central is required for the build plugins and the test dependencies.

Check out this repository, open a terminal in its root directory and run:

    mvn package

This compiles the sources, runs the unit tests and packages everything into a single executable fat jar plus a runnable distribution zip. To skip the tests for a quick build:

    mvn package -DskipTests

See [mavenbuild.md](mavenbuild.md) for details on how the build works, what it produces (`target/magellan-ee-<version>.jar` and `target/magellan-ee-<version>-dist.zip`) and which parts of the old Ant build (`build.xml`) it replaces. The Ant build is a leftover of the original project; it is not maintained and may be removed.

The project can also be imported as a Maven project into an IDE (Eclipse, IntelliJ, VS Code). The main class is `magellan.client.Client`. Native installers (IzPack / Install4J), Javadoc and a source jar are not part of the Maven build.

## How To Run

Magellan reads its configuration and data files from the working directory at runtime, so unpack the distribution zip and run the jar from inside it (or use the provided start script):

    unzip target/magellan-ee-<version>-dist.zip -d /somewhere
    cd /somewhere/magellan-ee-<version>
    ./magellan.sh          # or magellan.bat on Windows, or: java -jar magellan-ee-<version>.jar

See [mavenbuild.md](mavenbuild.md) for other ways to run the jar, such as pointing it at the project root explicitly with `-d`.

## Versions

The version is set in one place: `<revision>` in `pom.xml`. It is written into the jar at build time, so the program (title bar, About dialog) always shows the version it was built from.

- Local builds and the builds of the CI workflow are test versions: `<revision>-SNAPSHOT`, and in CI additionally the UTC time of the push, e.g. `1.0.0-SNAPSHOT.20261024-1530`.
- A release version has no suffix (`mvn package -Dchangelist=`).

## Contributing

Pull requests are welcome. Please keep changes focused, make sure `mvn package` passes, and describe user-visible changes in `CHANGELOG.txt` and `RELEASENOTES.txt`.

## Origin and License(s)

Magellan EE is derived from Magellan 2 and is a modified version of it. All original copyright and license notices are kept; the credits of the original authors are shown in the About dialog.

Unless otherwise indicated, all files in this distribution are provided to you under the terms and conditions of the GNU GPL License Version 2.0. A copy of this license is provided in COPYING.txt. Modifications made in Magellan EE are provided under the same license.

Exceptions from the GNU GPL are notified in the source files itself or written down in a separate file (e.g. `src/release/lib/jflap.LICENSE.txt`).

If you are not satisfied that EVERYTHING is under the GNU GPL License you should not use Magellan.
