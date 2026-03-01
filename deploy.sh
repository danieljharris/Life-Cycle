#!/bin/bash
# Define a function to handle CTRL+C
cleanup() {
    exit 1
}

# Trap SIGINT (CTRL+C) and call the cleanup function
trap cleanup SIGINT

###############################

# TODO: add select gui to pick pipeline
export PIPELINE=dev

# Build the dist target (explicitly include pipeline flag)
if ! bazel build //plugin:dist --//build_flags:pipeline="$PIPELINE"; then
    echo "Error: build failed"
    exit 1
fi

# Locate dist output (cquery prints the output path; make it absolute if necessary)
OUTPUT_PATH=$(bazel cquery //plugin:dist --output=files --//build_flags:pipeline="$PIPELINE" | tail -n1)
DIST_FILE="$OUTPUT_PATH"
if [ -n "$DIST_FILE" ] && [ ! -e "$DIST_FILE" ]; then
    EXECROOT=$(bazel info execution_root)
    if [ -n "$EXECROOT" ] && [ -e "$EXECROOT/$DIST_FILE" ]; then
        DIST_FILE="$EXECROOT/$DIST_FILE"
    fi
fi
SERVER_MODS_DIR="/workspace/hytale-downloader/Server/mods"

if [ -n "$DIST_FILE" ]; then
    echo "Moving $DIST_FILE to server mods..."
    mkdir -p "$SERVER_MODS_DIR"
    
    # Remove existing jar files to avoid permission issues
    rm -f "$SERVER_MODS_DIR"/*.jar

    cp "$DIST_FILE" "$SERVER_MODS_DIR/"

    # Set proper permissions on the copied file(s)
    chmod 644 "$SERVER_MODS_DIR"/*.jar
else
    echo "Error: Could not locate distribution file."
    exit 1
fi

echo "Deployed plugin to server."

# Check if Hytale server is running and reload plugin
COMMAND_PIPE="/tmp/hytale_commands.fifo"
if [ -f /tmp/hytale_java.pid ] && kill -0 $(cat /tmp/hytale_java.pid) 2>/dev/null; then
    echo "Detected running Hytale server. Reloading plugin..."
    
    if [ -p "$COMMAND_PIPE" ]; then
        echo "plugin reload DrDan:LifeCycle" > "$COMMAND_PIPE"
        echo "say Reloading DrDan:LifeCycle..." > "$COMMAND_PIPE"
        echo " ↺ Reload command sent to server"
    else
        echo "⚠ Server is running but command pipe not found."
        echo "Please manually run: /plugin reload DrDan:LifeCycle"
    fi
else
    echo "No running Hytale server detected. Plugin will load on next server start."
fi
