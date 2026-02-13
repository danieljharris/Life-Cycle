#!/bin/bash

# Lock file to prevent concurrent runs
LOCK="/tmp/.deploy.lock"

# Optional: prevent concurrent runs
if [ -f "$LOCK" ]; then
    echo "Deployment setup already in progress..."
    exit 1
fi
touch "$LOCK"

# If server jar already exists, assume setup completed and exit
if [ -f "/workspace/hytale-downloader/Server/HytaleServer.jar" ]; then
    echo "Server already set up (HytaleServer.jar found)."
    rm -f "$LOCK"
    exit 0
fi

echo "Setting up deployment environment..."

# Ensure working server directory exists
OUTPUT_DIR="/workspace/hytale-downloader"
mkdir -p "$OUTPUT_DIR"

# Only use the downloader zip at the project root. Do not create any
# /workspace/hytale-downloader directory. Inflate the zip into the
# `$OUTPUT_DIR` so its contents are available for artifact detection.
DOWNLOADER_ZIP="/workspace/hytale-downloader.zip"

if [ -f "$DOWNLOADER_ZIP" ]; then
    echo "Using existing $DOWNLOADER_ZIP"
else
    echo "Downloading hytale-downloader.zip..."
    wget -O "$DOWNLOADER_ZIP" https://downloader.hytale.com/hytale-downloader.zip
fi

echo "Inflating $DOWNLOADER_ZIP into $OUTPUT_DIR"
unzip -o "$DOWNLOADER_ZIP" -d "$OUTPUT_DIR" || true

    # Look for server artifacts in the output directory
    SERVER_DIR=""
    ASSETS_ZIP_FOUND=""
    # Prefer Server/ subfolder
    if [ -d "$OUTPUT_DIR/Server" ]; then
        SERVER_DIR="$OUTPUT_DIR/Server"
    fi
    if [ -f "$OUTPUT_DIR/Assets.zip" ]; then
        ASSETS_ZIP_FOUND="$OUTPUT_DIR/Assets.zip"
    fi

    # Fallback: search for Server/HytaleServer.jar or Assets.zip anywhere under output
    if [ -z "$SERVER_DIR" ]; then
        SERVER_JAR_PATH=$(find "$OUTPUT_DIR" -type f -path '*/server/HytaleServer.jar' | head -n1 || true)
        if [ -n "$SERVER_JAR_PATH" ]; then
            SERVER_DIR=$(dirname "$SERVER_JAR_PATH")
        fi
    fi
    if [ -z "$ASSETS_ZIP_FOUND" ]; then
        ASSETS_ZIP_FOUND=$(find "$OUTPUT_DIR" -type f -name 'Assets.zip' | head -n1 || true)
    fi

echo "Deployment setup complete!"

# Remove lock
rm -f "$LOCK"
