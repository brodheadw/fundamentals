#!/bin/zsh
# Runs every gametest headless and prints the failures. usage: tools/gametest.sh [repo] [port] [log]
# A dedicated server comes up on a fifo, `test runall` runs the lot, and because passes are only announced to
# players, `test runfailed` is asked afterwards: "No tests found" means everything passed.
set -u
W=${1:-$(cd "$(dirname "$0")/.." && pwd)}; PORT=${2:-25599}; OUT=${3:-$W/run/gametest.log}
export JAVA_HOME=${JAVA_HOME:-$HOME/.local/jdk/jdk-21.0.12.1+1/Contents/Home}
mkdir -p "$W/run"
echo "eula=true" > "$W/run/eula.txt"
printf 'server-port=%s\nonline-mode=false\nlevel-name=world\nmax-tick-time=-1\n' $PORT > "$W/run/server.properties"
rm -rf "$W/run/world"
FIFO=$(mktemp -u); mkfifo $FIFO
cd "$W"
(./gradlew runServer < $FIFO > "$OUT" 2>&1; echo "GRADLE_EXIT=$?" >> "$OUT") &
exec 3> $FIFO
for i in {1..300}; do grep -q 'Done (' "$OUT" && break; grep -q 'GRADLE_EXIT' "$OUT" && break; sleep 2; done
echo "test runall" >&3
sleep 85   # the longest test fires a bloomery for 1200 ticks
echo "test runfailed" >&3
sleep 4
echo "stop" >&3
exec 3>&-
wait
rm -f $FIFO
grep -nE 'Running [0-9]+ tests|failed at|No tests found|GRADLE_EXIT|Parsing error' "$OUT" | cut -c1-260 | tail -20
