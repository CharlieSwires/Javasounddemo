#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
if [[ -n "${JAVA_HOME:-}" ]]; then runtime="$JAVA_HOME/bin/java"; else runtime=java; fi
exec "$runtime" -cp JavaSoundDemo.jar CapturePlayback "$@"
