#!/usr/bin/env bash
# Tron-uninstall.sh — clean removal of OpenTron from $HOME.
#
# Removes:
#   ~/.OpenTron/
#   ~/.local/bin/Tron
#   ~/.local/bin/Tron-uninstall
#
# Does NOT remove: ollama, uv, or the Rust toolchain.

set -euo pipefail

OpenTron_HOME="${OpenTron_HOME:-$HOME/.OpenTron}"

if [[ -f "$OpenTron_HOME/.state/bg.pid" ]]; then
    pid=$(cat "$OpenTron_HOME/.state/bg.pid" 2>/dev/null || echo "")
    if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
        echo "Stopping background work (pid=$pid)..."
        kill "$pid" 2>/dev/null || true
    fi
fi

if command -v ollama >/dev/null 2>&1; then
    ollama stop >/dev/null 2>&1 || true
fi

if [[ -d "$OpenTron_HOME" ]]; then
    rm -rf "$OpenTron_HOME"
    echo "Removed $OpenTron_HOME"
fi

for f in "$HOME/.local/bin/Tron" "$HOME/.local/bin/Tron-uninstall"; do
    if [[ -L "$f" ]] || [[ -f "$f" ]]; then
        rm -f "$f"
        echo "Removed $f"
    fi
done

cat <<EOF

OpenTron removed.

Left intact (may be used by other tools):
  - Ollama       (uninstall: brew uninstall ollama  /  rm -f /usr/local/bin/ollama)
  - uv           (uninstall: rm -rf ~/.local/share/uv ~/.cargo/bin/uv)
  - Rust toolchain (uninstall: rustup self uninstall)
EOF

