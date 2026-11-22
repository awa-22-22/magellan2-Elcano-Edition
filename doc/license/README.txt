Licenses of the libraries in lib/

The distribution contains, for every library in lib/ that ships them, the
license and notice files taken from the library jar itself, in a folder per
library (e.g. commons-io-2.20.0/). They are collected automatically during the
build, so they always match the bundled versions.

The files directly in this folder cover libraries whose jar contains no
license file, and code that is part of the Magellan sources:

  javahelp.LICENSE.txt         JavaHelp (GPL v2 with Classpath Exception)
  jfreechart.LICENSE.txt       JFreeChart (GNU LGPL 2.1)
  modern-docking.LICENSE.txt   Modern Docking, api and single-app (MIT)
  multiline-label.LICENSE.txt  multiline-label (Apache License 2.0)
  RadixTree/                   RadixTree, included as source in src-library
