#!/bin/zsh
# usage: tools/showcase/reopen.sh [repo]
set -u
W=${1:-$(cd "$(dirname "$0")/../.." && pwd)}; cd "$W"
export JAVA_HOME=${JAVA_HOME:-$HOME/.local/jdk/jdk-21.0.12.1+1/Contents/Home}
LOGS=run/showcase-logs; mkdir -p $LOGS
for i in {1..450}; do pgrep -f "$W/versions/[^ ]*/argFiles/run" >/dev/null || break; sleep 2; done
rm -f run/saves/showcase/session.lock
(nohup ./gradlew :1.21.1-neoforge:runShowcase > $LOGS/client.log 2>&1 &)
sleep 30; for i in {1..50}; do grep -qE 'joined the game|BUILD FAILED' $LOGS/client.log 2>/dev/null && break; sleep 5; done
echo "joined=$(grep -c 'joined the game' $LOGS/client.log)"
