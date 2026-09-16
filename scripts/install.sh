#!/bin/bash
# Self-installer for Little Log Peep (desktop wrapper for local_log_search).
#
# Follows the same pattern as scripts/install.sh in
# claude-session-analyzer / history_viewer / code_graph_search: run
# in the repo root, produce a self-contained .app under ~/Applications,
# and drop a `little-log-peep` launcher symlink under ~/.local/bin.
#
# Prerequisites: JDK >= 21 with jpackage on PATH (any modern JDK 21+ works;
# `brew install --cask temurin@21` is easiest on macOS). Maven wrapper OR
# system `mvn` — checks in order.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

BIN_DIR="${HOME}/.local/bin"
APPS_DIR="${HOME}/Applications"

command -v java >/dev/null || { echo "❌ java not found on PATH"; exit 1; }
command -v jpackage >/dev/null || { echo "❌ jpackage not found — install JDK 21+"; exit 1; }

MVN="./mvnw"
if [ ! -x "$MVN" ]; then MVN="mvn"; fi
command -v "$MVN" >/dev/null 2>&1 || { echo "❌ neither ./mvnw nor mvn found"; exit 1; }

echo "==> Building service + desktop JARs"
"$MVN" -q -pl desktop -am -DskipTests package

echo "==> Building self-contained .app via jpackage"
make -C desktop app

echo "==> Installing to $APPS_DIR"
make -C desktop install

mkdir -p "$BIN_DIR"
LAUNCH_SH="$BIN_DIR/little-log-peep"
cat > "$LAUNCH_SH" <<'EOF'
#!/bin/bash
# Launcher stub for Little Log Peep. Prefers the installed .app; falls
# back to `open` if the bundle path drifted.
APP="$HOME/Applications/Little Log Peep.app"
if [ -d "$APP" ]; then
    exec open -a "$APP" --args "$@"
fi
echo "❌ $APP not found — re-run scripts/install.sh from local_log_search checkout" >&2
exit 1
EOF
chmod +x "$LAUNCH_SH"

echo ""
echo "✅ Installed:"
echo "   .app        → $APPS_DIR/Little Log Peep.app"
echo "   launcher    → $LAUNCH_SH"
echo ""
echo "Usage:"
echo "   open '$APPS_DIR/Little Log Peep.app'    # launch from Finder"
echo "   little-log-peep                          # launch from CLI"
