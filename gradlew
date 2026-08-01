#!/usr/bin/env sh
set -eu
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi
echo "gradle not found. Install Gradle or open this project in AndroidIDE." >&2
exit 127
