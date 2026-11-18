#!/usr/bin/env bash
# Installs the project's Git hooks (.githooks/ folder) for this clone.
# Usage: scripts/install-hooks.sh
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

chmod +x .githooks/*
git config core.hooksPath .githooks

echo "Hooks installed (core.hooksPath = .githooks):"
for hook in .githooks/*; do
    echo "  - $(basename "$hook")"
done

if ! command -v gitleaks >/dev/null 2>&1; then
    echo "Tip: install gitleaks to scan for secrets before commit (macOS: brew install gitleaks)."
fi
