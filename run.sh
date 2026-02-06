#!/bin/bash

set -e

echo "=========================================="
echo "Setting Up Server"
echo "=========================================="
rm -f /tmp/.deploy_finished /tmp/.deploy.lock
./.devcontainer/setup_server.sh

echo ""
echo "=========================================="
echo "Building Hytale Plugin"
echo "=========================================="
./build.sh

echo ""
echo "=========================================="
echo "Deploying to Server"
echo "=========================================="
./deploy.sh

echo ""
echo "=========================================="
echo "Starting command forwarder"
echo "=========================================="
# Start command forwarder in background before starting the server so it can follow logs
bash ./commandForwarder.sh >/tmp/commandForwarder.log 2>&1 &
CF_PID=$!
echo "Started commandForwarder (pid $CF_PID, log: /tmp/commandForwarder.log)"

# Ensure commandForwarder is killed when this script exits (e.g. Ctrl+C)
trap 'echo "Killing commandForwarder (pid $CF_PID)"; kill $CF_PID 2>/dev/null || true' EXIT

echo ""
echo "=========================================="
echo "Starting Hytale Server"
echo "=========================================="
./.devcontainer/start_server.sh

# When the server process exits, stop the forwarder
echo "Server stopped; shutting down commandForwarder (pid $CF_PID)"
kill $CF_PID 2>/dev/null || true
wait $CF_PID 2>/dev/null || true
trap - EXIT
