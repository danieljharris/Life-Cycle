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
echo "Starting Hytale Server"
echo "=========================================="
./.devcontainer/start_server.sh
