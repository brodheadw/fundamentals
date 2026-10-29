#!/bin/zsh
# Runs every gametest headless and prints the failures; the server quits as soon as the last test ends. usage: tools/gametest.sh [repo] [log]
set -u
W=${1:-$(cd "$(dirname "$0")/.." && pwd)}; OUT=${2:-$W/run/gametest.log}
export JAVA_HOME=${JAVA_HOME:-$HOME/.local/jdk/jdk-21.0.12.1+1/Contents/Home}
mkdir -p "$W/run"
echo "eula=true" > "$W/run/eula.txt"
rm -rf "$W/run/gametestworld"
cd "$W"
./gradlew runGameTestServer > "$OUT" 2>&1; echo "GRADLE_EXIT=$?" >> "$OUT"
grep -nE 'Running [0-9]+ tests|failed at|required tests|GRADLE_EXIT|Parsing error|error:' "$OUT" | grep -v 'block entity' | cut -c1-260 | tail -20
