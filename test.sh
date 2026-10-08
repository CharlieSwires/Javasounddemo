#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
bash ./build.sh
if [[ -n "${JAVA_HOME:-}" ]]; then runtime="$JAVA_HOME/bin/java"; else runtime=java; fi
"$runtime" com.sun.tools.javac.Main --release 17 -encoding UTF-8 -cp build -d build tests/SampleAudioTest.java
"$runtime" -Djava.awt.headless=true -cp build SampleAudioTest
