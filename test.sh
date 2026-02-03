#!/bin/bash
# test.sh - Run AnimalsGrow integration tests
#
# This script:
# 1. Deploys the plugin
# 2. Sends "ag test" command to the server
# 3. Watches server logs for test results
# 4. Reports PASS/FAIL status

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
SERVER_DIR="./server"
LOG_DIR="$SERVER_DIR/logs"
COMMAND_FIFO="/tmp/hytale_commands.fifo"
TIMEOUT=30  # seconds to wait for test results

echo -e "${YELLOW}=== AnimalsGrow Test Runner ===${NC}"
echo ""

# Step 1: Build and deploy
echo -e "${YELLOW}[1/4] Building and deploying plugin...${NC}"
./deploy.sh
if [ $? -ne 0 ]; then
    echo -e "${RED}Deploy failed!${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Plugin deployed${NC}"
echo ""

# Give the server a moment to reload
sleep 2

# Step 2: Find the latest log file
echo -e "${YELLOW}[2/4] Finding server log...${NC}"
LATEST_LOG=$(ls -t "$LOG_DIR"/*.log 2>/dev/null | head -1)
if [ -z "$LATEST_LOG" ]; then
    # Try .log.lck files if no .log files exist
    LATEST_LOG=$(ls -t "$LOG_DIR"/*.log.lck 2>/dev/null | head -1)
fi

if [ -z "$LATEST_LOG" ]; then
    echo -e "${RED}No server log found in $LOG_DIR${NC}"
    echo "Make sure the server is running"
    exit 1
fi
echo -e "${GREEN}✓ Using log: $LATEST_LOG${NC}"
echo ""

# Step 3: Record current log position and send test command
echo -e "${YELLOW}[3/4] Running tests...${NC}"

# Get current line count to only watch new output
LOG_LINES_BEFORE=$(wc -l < "$LATEST_LOG" 2>/dev/null || echo "0")

# Send test command to server via FIFO
if [ -p "$COMMAND_FIFO" ]; then
    echo "agtest" > "$COMMAND_FIFO"
    echo "Sent 'agtest' command to server"
else
    echo -e "${RED}Command FIFO not found at $COMMAND_FIFO${NC}"
    echo "Make sure the server is running with the FIFO input"
    exit 1
fi

# Step 4: Watch for test results
echo -e "${YELLOW}[4/4] Waiting for test results (timeout: ${TIMEOUT}s)...${NC}"
echo ""

START_TIME=$(date +%s)
TEST_STARTED=false
TEST_ENDED=false
PASSED=0
TOTAL=0

while true; do
    CURRENT_TIME=$(date +%s)
    ELAPSED=$((CURRENT_TIME - START_TIME))
    
    if [ $ELAPSED -ge $TIMEOUT ]; then
        echo -e "${RED}Timeout waiting for test results!${NC}"
        exit 1
    fi
    
    # Read new log lines
    NEW_LINES=$(tail -n +$((LOG_LINES_BEFORE + 1)) "$LATEST_LOG" 2>/dev/null)

    # Check for test start
    if echo "$NEW_LINES" | grep -q "\[AG_TEST:START\]"; then
        if [ "$TEST_STARTED" = false ]; then
            TEST_STARTED=true
            echo "Tests started..."
        fi
    fi

    # Check for individual test results and commands
    while IFS= read -r line; do
        if echo "$line" | grep -q "\[AG_TEST:.*:PASS\]"; then
            TEST_NAME=$(echo "$line" | sed -n 's/.*\[AG_TEST:\([^:]*\):PASS\].*/\1/p')
            echo -e "  ${GREEN}✓ PASS${NC}: $TEST_NAME"
        elif echo "$line" | grep -q "\[AG_TEST:.*:FAIL:"; then
            TEST_NAME=$(echo "$line" | sed -n 's/.*\[AG_TEST:\([^:]*\):FAIL:.*/\1/p')
            REASON=$(echo "$line" | sed -n 's/.*\[AG_TEST:[^:]*:FAIL:\([^]]*\)\].*/\1/p')
            echo -e "  ${RED}✗ FAIL${NC}: $TEST_NAME - $REASON"
        elif echo "$line" | grep -q "\[AG_TEST:COMMAND:"; then
            # Extract command and send to server FIFO
            COMMAND=$(echo "$line" | sed -n 's/.*\[AG_TEST:COMMAND:\([^]]*\)\].*/\1/p')
            if [ -n "$COMMAND" ] && [ -p "$COMMAND_FIFO" ]; then
                echo "$COMMAND" > "$COMMAND_FIFO"
                echo -e "  ${YELLOW}↪ Sent command to server:${NC} $COMMAND"
            fi
        fi
    done <<< "$NEW_LINES"

    # Check for test end
    if echo "$NEW_LINES" | grep -q "\[AG_TEST:END:"; then
        RESULT=$(echo "$NEW_LINES" | grep -o "\[AG_TEST:END:[^]]*\]" | tail -1)
        PASSED=$(echo "$RESULT" | sed -n 's/.*\[AG_TEST:END:\([0-9]*\)\/.*/\1/p')
        TOTAL=$(echo "$RESULT" | sed -n 's/.*\[AG_TEST:END:[0-9]*\/[0-9]*\)\].*/\1/p')
        TEST_ENDED=true
        break
    fi

    sleep 0.5

echo ""
echo "================================"
if [ "$PASSED" = "$TOTAL" ] && [ "$TOTAL" != "0" ]; then
    echo -e "${GREEN}All tests passed! ($PASSED/$TOTAL)${NC}"
    exit 0
else
    FAILED=$((TOTAL - PASSED))
    echo -e "${RED}$FAILED test(s) failed ($PASSED/$TOTAL passed)${NC}"
    exit 1
fi
