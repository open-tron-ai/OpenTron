#!/usr/bin/env bash
# Tron-wrapper.sh — symlinked to ~/.local/bin/Tron.
# Activates the managed venv and execs the real Tron CLI.

OpenTron_HOME="${OpenTron_HOME:-$HOME/.OpenTron}"
VENV="$OpenTron_HOME/.venv"

if [[ ! -d "$VENV" ]]; then
    echo "Tron: venv not found at $VENV" >&2
    echo "Re-run the installer: curl -fsSL https://open-Tron.github.io/OpenTron/install.sh | bash" >&2
    exit 1
fi

exec "$VENV/bin/Tron" "$@"

