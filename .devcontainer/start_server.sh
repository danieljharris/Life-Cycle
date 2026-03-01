#!/bin/bash
# Hytale Server Launcher
# This script handles staged updates and starts the server with default arguments.
#
# CUSTOM JVM ARGUMENTS
# --------------------
# To customize JVM arguments (e.g., memory settings), create a file named
# "jvm.options" in the same directory as this script. One argument per line.
# Lines starting with # are comments.
#
# Example jvm.options:
#   -Xms2G
#   -Xmx4G
#   -XX:+UseG1GC

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"
WORKING_DIR="/workspace/hytale-downloader"
cd "$WORKING_DIR"

# Configuration
CLIENT_ID="hytale-server"
SCOPES="openid offline auth:server"
SERVER_DIR="/workspace/hytale-downloader/Server"
AUTH_FILE="$SERVER_DIR/hytale_auth_data.json"
ASSETS_ZIP="/workspace/hytale-downloader/Assets.zip"

URL_DEVICE_AUTH="https://oauth.accounts.hytale.com/oauth2/device/auth"
URL_TOKEN="https://oauth.accounts.hytale.com/oauth2/token"
URL_PROFILES="https://account-data.hytale.com/my-account/get-profiles"
URL_SESSION="https://sessions.hytale.com/game-session/new"

error_exit() {
    echo "Error: $1"
    exit 1
}

# Check for jq
if ! command -v jq &> /dev/null; then
    error_exit "jq is required but not installed. Please install jq."
fi

perform_device_auth() {
    echo "--- Initiating Device Authentication ---"
    
    # 1. Request Device Code
    RESPONSE=$(curl -s -X POST "$URL_DEVICE_AUTH" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "client_id=$CLIENT_ID" \
        -d "scope=$SCOPES")
    
    DEVICE_CODE=$(echo "$RESPONSE" | jq -r '.device_code')
    USER_CODE=$(echo "$RESPONSE" | jq -r '.user_code')
    VERIFICATION_URI=$(echo "$RESPONSE" | jq -r '.verification_uri_complete')
    INTERVAL=$(echo "$RESPONSE" | jq -r '.interval')
    
    if [ "$DEVICE_CODE" == "null" ]; then
        error_exit "Failed to get device code. Response: $RESPONSE"
    fi

    echo ""
    echo "=================================================================="
    echo " PLEASE AUTHENTICATE YOUR SERVER"
    echo " Visit the URL below in your browser to approve this server:"
    echo " URL:  $VERIFICATION_URI"
    echo " Code: $USER_CODE"
    echo "=================================================================="
    echo "Waiting for authorization..."

    while true; do
        sleep "${INTERVAL:-5}"
        
        TOKEN_RESPONSE=$(curl -s -X POST "$URL_TOKEN" \
            -H "Content-Type: application/x-www-form-urlencoded" \
            -d "client_id=$CLIENT_ID" \
            -d "grant_type=urn:ietf:params:oauth:grant-type:device_code" \
            -d "device_code=$DEVICE_CODE")
            
        ERROR=$(echo "$TOKEN_RESPONSE" | jq -r '.error')
        
        if [ "$ERROR" == "null" ]; then
            # Success!
            ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token')
            REFRESH_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.refresh_token')
            # Save refresh token for future reuse
            echo "{\"refresh_token\": \"$REFRESH_TOKEN\"}" > "$AUTH_FILE"
            echo "Authentication successful!"
            break
        elif [ "$ERROR" == "authorization_pending" ]; then
            continue
        elif [ "$ERROR" == "slow_down" ]; then
            sleep 5
        elif [ "$ERROR" == "expired_token" ]; then
            error_exit "Authentication timed out. Please run the script again."
        else
            error_exit "Auth failed: $ERROR"
        fi
    done
}

refresh_oauth_token() {
    local REFRESH_TOKEN=$1
    echo "Attempting to refresh existing token..."
    
    TOKEN_RESPONSE=$(curl -s -X POST "$URL_TOKEN" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "client_id=$CLIENT_ID" \
        -d "grant_type=refresh_token" \
        -d "refresh_token=$REFRESH_TOKEN")

    ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token')
    NEW_REFRESH_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.refresh_token')
    
    if [ "$ACCESS_TOKEN" != "null" ]; then
        # Update stored refresh token if a new one was sent
        if [ "$NEW_REFRESH_TOKEN" != "null" ]; then
             echo "{\"refresh_token\": \"$NEW_REFRESH_TOKEN\"}" > "$AUTH_FILE"
        fi
        return 0
    else
        echo "Token refresh failed. Re-authenticating..."
        return 1
    fi
}

if [ -f "$AUTH_FILE" ]; then
    STORED_REFRESH=$(jq -r '.refresh_token' "$AUTH_FILE")
    if [ "$STORED_REFRESH" != "null" ]; then
        refresh_oauth_token "$STORED_REFRESH"
        if [ $? -ne 0 ]; then
            perform_device_auth
        fi
    else
        perform_device_auth
    fi
else
    perform_device_auth
fi

echo "Fetching profile..."
PROFILE_RES=$(curl -s -X GET "$URL_PROFILES" -H "Authorization: Bearer $ACCESS_TOKEN")
# Select the first profile UUID available
PROFILE_UUID=$(echo "$PROFILE_RES" | jq -r '.profiles[0].uuid')

if [ "$PROFILE_UUID" == "null" ]; then
    error_exit "Could not retrieve profile UUID. Response: $PROFILE_RES"
fi
echo "Using Profile UUID: $PROFILE_UUID"

echo "Creating game session..."
SESSION_RES=$(curl -s -X POST "$URL_SESSION" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"uuid\": \"$PROFILE_UUID\"}")

SESSION_TOKEN=$(echo "$SESSION_RES" | jq -r '.sessionToken')
IDENTITY_TOKEN=$(echo "$SESSION_RES" | jq -r '.identityToken')

if [ "$SESSION_TOKEN" == "null" ]; then
    error_exit "Failed to create game session. Response: $SESSION_RES"
fi

while true; do
    APPLIED_UPDATE=false

    # Apply staged update if present
    if [ -f "updater/staging/Server/HytaleServer.jar" ]; then
        echo "[Launcher] Applying staged update..."
        # Only replace update files, preserve config/saves/mods
        cp -f updater/staging/Server/HytaleServer.jar Server/
        [ -d "updater/staging/Server/Licenses" ] && rm -rf Server/Licenses && cp -r updater/staging/Server/Licenses Server/
        [ -f "updater/staging/Assets.zip" ] && cp -f updater/staging/Assets.zip ./
        [ -f "updater/staging/start.sh" ] && cp -f updater/staging/start.sh ./
        [ -f "updater/staging/start.bat" ] && cp -f updater/staging/start.bat ./
        rm -rf updater/staging
        APPLIED_UPDATE=true
    fi

    # Run server from inside Server/ folder so config/backups/etc. are generated there
    cd Server

    # Load custom JVM arguments from jvm.options if it exists (uses JVM's @-file syntax)
    JVM_OPTS=""
    [ -f "../jvm.options" ] && JVM_OPTS="@../jvm.options"

    # Default server arguments
    # --assets: Assets.zip is in parent directory
    # --backup: Enable periodic backups like singleplayer
    DEFAULT_ARGS="--assets ../Assets.zip --backup --backup-dir backups --backup-frequency 30 --session-token $SESSION_TOKEN --identity-token $IDENTITY_TOKEN --bind 0.0.0.0:5520 --allow-op"

    # Create a named pipe for command input if it doesn't exist
    COMMAND_PIPE="/tmp/hytale_commands.fifo"
    if [ ! -p "$COMMAND_PIPE" ]; then
        mkfifo "$COMMAND_PIPE"
    fi

    # Store the server PID
    echo $$ > /tmp/hytale_server.pid

    # Cleanup function for Ctrl+C
    cleanup_server() {
        echo ""
        echo "Shutting down server..."
        if [ -n "$TAIL_PID" ] && kill -0 "$TAIL_PID" 2>/dev/null; then
            kill "$TAIL_PID" 2>/dev/null
        fi
        if [ -n "$JAVA_PID" ] && kill -0 "$JAVA_PID" 2>/dev/null; then
            kill "$JAVA_PID" 2>/dev/null
            wait "$JAVA_PID" 2>/dev/null
        fi
        rm -f "$COMMAND_PIPE" /tmp/hytale_server.pid /tmp/hytale_java.pid
        exit 0
    }

    trap cleanup_server SIGINT SIGTERM

    echo "--- Starting Hytale Server ---"
    # Start server and track time
    START_TIME=$(date +%s)
    # Run tail in background and capture its PID
    tail -f "$COMMAND_PIPE" | java $JVM_OPTS -jar HytaleServer.jar $DEFAULT_ARGS "$@" &
    
    # Store the java process PID
    JAVA_PID=$!
    echo $JAVA_PID > /tmp/hytale_java.pid

    # Find the tail process PID (parent of java in the pipeline)
    TAIL_PID=$(ps -o pid= --ppid $$ | grep -v $JAVA_PID | head -n1 | xargs)

    # Wait for the Java process
    wait $JAVA_PID
    EXIT_CODE=$?
    ELAPSED=$(( $(date +%s) - START_TIME ))

    # Cleanup
    rm -f "$COMMAND_PIPE" /tmp/hytale_server.pid /tmp/hytale_java.pid

    # Return to script dir for next iteration
    cd "$WORKING_DIR"

    # Exit code 8 = restart for update
    if [ $EXIT_CODE -eq 8 ]; then
        echo "[Launcher] Restarting to apply update..."
        continue
    fi

    # Warn on crash shortly after update
    if [ $EXIT_CODE -ne 0 ] && [ "$APPLIED_UPDATE" = true ] && [ $ELAPSED -lt 30 ]; then
        echo ""
        echo "[Launcher] ERROR: Server exited with code $EXIT_CODE within ${ELAPSED}s of starting."
        echo "[Launcher] This may indicate the update failed to start correctly."
        echo "[Launcher]"
        echo "[Launcher] Your previous files are in the updater/backup/ folder."
        echo "[Launcher] To rollback: delete Server/ and Assets.zip, then move from updater/backup/"
        echo ""
        # Only prompt if running interactively (has terminal)
        if [ -t 0 ]; then
            read -p "Press Enter to exit..."
        fi
    fi

    exit $EXIT_CODE
done