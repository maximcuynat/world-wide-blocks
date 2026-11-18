#!/usr/bin/env bash
# Installe les hooks Git du projet (dossier .githooks/) pour ce clone.
# Usage : scripts/install-hooks.sh
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

chmod +x .githooks/*
git config core.hooksPath .githooks

echo "Hooks installés (core.hooksPath = .githooks) :"
for hook in .githooks/*; do
    echo "  - $(basename "$hook")"
done

if ! command -v gitleaks >/dev/null 2>&1; then
    echo "Conseil : installez gitleaks pour la recherche de secrets avant commit (macOS : brew install gitleaks)."
fi
