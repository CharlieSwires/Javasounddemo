#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
# JAVA_HOME overrides PATH; Git Bash accepts Windows JDK paths with spaces.
if [[ -n "${JAVA_HOME:-}" ]]; then
  compiler=("$JAVA_HOME/bin/java" com.sun.tools.javac.Main)
  jar_tool=("$JAVA_HOME/bin/java" sun.tools.jar.Main)
else
  compiler=(java com.sun.tools.javac.Main)
  jar_tool=(java sun.tools.jar.Main)
fi
# Always clean output so removed legacy classes cannot enter the new JAR.
rm -rf -- build
mkdir -p build
"${compiler[@]}" --release 17 -encoding UTF-8 -Xlint:deprecation -d build src/*.java
"${jar_tool[@]}" --create --file JavaSoundDemo.jar --main-class JavaSound -C build .
echo 'Built JavaSoundDemo.jar for Java 17.'
