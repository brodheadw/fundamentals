#!/bin/zsh
# A brand-new flat showcase world with the plant staged by the datapack, then the client into it.
# usage: tools/showcase/fresh.sh [repo]   (defaults to the repo this script is in)
# Kills any running showcase client of this repo first; never run it while someone is playing.
set -u
W=${1:-$(cd "$(dirname "$0")/../.." && pwd)}; cd "$W"
export JAVA_HOME=${JAVA_HOME:-$HOME/.local/jdk/jdk-21.0.12.1+1/Contents/Home}
LOGS=run/showcase-logs; mkdir -p $LOGS
pkill -f "$W/run" 2>/dev/null; for i in {1..30}; do pgrep -f "$W/run" >/dev/null || break; sleep 1; done; sleep 2
rm -rf run/world; echo 'eula=true' > run/eula.txt
printf 'server-port=25634\nonline-mode=false\nlevel-name=world\nlevel-type=minecraft:flat\nmax-tick-time=-1\n' > run/server.properties
FIFO=$(mktemp -u); mkfifo $FIFO; (./gradlew runServer < $FIFO > $LOGS/world.log 2>&1 &); exec 3> $FIFO
for i in {1..150}; do grep -qE 'Done \(|Failed to initialize|BUILD FAILED' $LOGS/world.log && break; sleep 2; done
# a server that failed to start has closed the pipe; writing to it would SIGPIPE this shell silently
if grep -q 'Done (' $LOGS/world.log; then echo stop >&3; sleep 10; else echo 'world server did not start; see run/showcase-logs/world.log'; exec 3>&-; rm -f $FIFO; exit 1; fi
exec 3>&-; rm -f $FIFO
rm -rf run/saves/showcase && mkdir -p run/saves && cp -R run/world run/saves/showcase && rm -f run/saves/showcase/session.lock
python3 - <<'PY'
import gzip
p = 'run/saves/showcase/level.dat'; d = gzip.open(p).read()
i = d.find(b'\x01\x00\x0dallowCommands'); d = d[:i+16] + b'\x01' + d[i+17:] if i >= 0 else d
j = d.find(b'\x03\x00\x08GameType'); d = d[:j+11] + b'\x00\x00\x00\x01' + d[j+15:] if j >= 0 else d
open(p, 'wb').write(gzip.compress(d)); print('creative with cheats:', i >= 0 and j >= 0)
PY
mkdir -p run/saves/showcase/datapacks && cp -R "$(dirname "$0")/datapack" run/saves/showcase/datapacks/showcase
(nohup ./gradlew :1.21.1-neoforge:runShowcase > $LOGS/client.log 2>&1 &)
sleep 30; for i in {1..50}; do grep -qE 'joined the game|BUILD FAILED' $LOGS/client.log 2>/dev/null && break; sleep 5; done
echo "joined=$(grep -c 'joined the game' $LOGS/client.log)"
