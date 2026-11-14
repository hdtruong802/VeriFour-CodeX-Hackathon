#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
echo "Starting VeriFour Hackathon Local Server on port ${PORT:-8787}..."
exec python3 "$DIR/backend/local_server.py"
