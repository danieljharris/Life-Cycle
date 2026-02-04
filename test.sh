#!/bin/bash
# test.sh - Run AnimalsGrow integration tests
#
# This script:
# 1. Deploys the plugin
# 2. Sends "ag test" command to the server
# 3. Watches server logs for test results
# 4. Reports PASS/FAIL status

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color



# Configuration
SERVER_DIR="./server"
TIMEOUT=30  # seconds to wait for test results

# FIFO used to send commands to server
COMMAND_FIFO="/tmp/hytale_commands.fifo"

# Locate latest server log if not already set
LOG_DIR="$SERVER_DIR/logs"

echo -e "${YELLOW}=== AnimalsGrow Test Runner ===${NC}"
echo ""

# Step 1: Build and deploy
echo -e "${YELLOW}[1/4] Building and deploying plugin...${NC}"
if ! ./deploy.sh; then
    echo -e "${RED}Deploy failed! Aborting tests.${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Plugin deployed${NC}"
echo ""

# Give the server a moment to reload
sleep 2

LATEST_LOG=$(ls -t "$SERVER_DIR"/logs/*.log 2>/dev/null | head -1)
if [ -z "$LATEST_LOG" ]; then
    LATEST_LOG=$(ls -t "$SERVER_DIR"/logs/*.log.lck 2>/dev/null | head -1)
fi
if [ -z "$LATEST_LOG" ]; then
    echo -e "${RED}No server log found in $SERVER_DIR/logs${NC}"
    echo "Make sure the server is running"
    exit 1
fi
LOG_LINES_BEFORE=$(wc -l < "$LATEST_LOG" 2>/dev/null || echo "0")
echo "Using log: $LATEST_LOG (starting at line $LOG_LINES_BEFORE)"

echo -e "${YELLOW}[3/4] Running tests...${NC}"

# Send test command to server via FIFO
if [ -p "$COMMAND_FIFO" ]; then
    echo "agtest" > "$COMMAND_FIFO"
    echo "Sent 'agtest' command to server"
else
    echo -e "${RED}Command FIFO not found at $COMMAND_FIFO${NC}"
    echo "Make sure the server is running with the FIFO input"
    exit 1
fi

echo -e "${YELLOW}[4/4] Waiting for test results (timeout: ${TIMEOUT}s)...${NC}"
echo ""

START_TIME=$(date +%s)
TEST_STARTED=false
declare -A TEST_RESULTS
TOTAL_TESTS=0
PASSED=0
FAILED=0

while true; do
    CURRENT_TIME=$(date +%s)
    ELAPSED=$((CURRENT_TIME - START_TIME))
    if [ $ELAPSED -ge $TIMEOUT ]; then
        echo -e "${RED}Timeout waiting for test results!${NC}"
        exit 1
    fi

    # Read new log lines
    NEW_LINES=$(tail -n +$((LOG_LINES_BEFORE + 1)) "$LATEST_LOG" 2>/dev/null)
    LOG_LINES_BEFORE=$(wc -l < "$LATEST_LOG" 2>/dev/null || echo "0")

    # Debug: show newly-read log lines for troubleshooting
    if [ -n "$NEW_LINES" ]; then
        echo "DEBUG: New log lines from $LATEST_LOG:"
        echo "$NEW_LINES"
    fi

    # Check for test start
    if echo "$NEW_LINES" | grep -q "\[AG_TEST:START\]"; then
        if [ "$TEST_STARTED" = false ]; then
            TEST_STARTED=true
            echo "Tests started..."
        fi
    fi

    # Check for individual test results and commands
    IFS=$'\n'
    for line in $NEW_LINES; do
        if echo "$line" | grep -q "\[AG_TEST:\([^:]*\):PASS\]"; then
            TEST_NAME=$(echo "$line" | sed -n 's/.*\[AG_TEST:\([^:]*\):PASS\].*/\1/p')
            if [ -z "${TEST_RESULTS[$TEST_NAME]:-}" ]; then
                TEST_RESULTS[$TEST_NAME]="PASS"
                echo -e "  ${GREEN}✓ PASS${NC}: $TEST_NAME"
            fi
        elif echo "$line" | grep -q "\[AG_TEST:END:[^:]*:PASS\]"; then
            TEST_NAME=$(echo "$line" | sed -n 's/.*\[AG_TEST:END:\([^:]*\):PASS\].*/\1/p')
            if [ -z "${TEST_RESULTS[$TEST_NAME]:-}" ]; then
                TEST_RESULTS[$TEST_NAME]="PASS"
                echo -e "  ${GREEN}✓ PASS${NC}: $TEST_NAME"
            fi
        elif echo "$line" | grep -q "\[AG_TEST:END:\([^:]*\):FAIL:"; then
            TEST_NAME=$(echo "$line" | sed -n 's/.*\[AG_TEST:END:\([^:]*\):FAIL:.*/\1/p')
            REASON=$(echo "$line" | sed -n 's/.*\[AG_TEST:END:[^:]*:FAIL:\([^]]*\)\].*/\1/p')
            if [ -z "${TEST_RESULTS[$TEST_NAME]:-}" ]; then
                TEST_RESULTS[$TEST_NAME]="FAIL"
                echo -e "  ${RED}✗ FAIL${NC}: $TEST_NAME - $REASON"
            fi
        elif echo "$line" | grep -q "\[AG_TEST:END:\([^:]*\):FAIL:Exception:"; then
            TEST_NAME=$(echo "$line" | sed -n 's/.*\[AG_TEST:END:\([^:]*\):FAIL:Exception:.*/\1/p')
            REASON=$(echo "$line" | sed -n 's/.*\[AG_TEST:END:[^:]*:FAIL:Exception: \([^]]*\)\].*/\1/p')
            if [ -z "${TEST_RESULTS[$TEST_NAME]:-}" ]; then
                TEST_RESULTS[$TEST_NAME]="FAIL"
                echo -e "  ${RED}✗ FAIL${NC}: $TEST_NAME - Exception: $REASON"
            fi
        elif echo "$line" | grep -q "\[AG_TEST:COMMAND:"; then
            COMMAND=$(echo "$line" | sed -n 's/.*\[AG_TEST:COMMAND:\([^]]*\)\].*/\1/p')
            if [ -n "$COMMAND" ] && [ -p "$COMMAND_FIFO" ]; then
                echo "$COMMAND" > "$COMMAND_FIFO"
                echo -e "  ${YELLOW}↪ Sent command to server:${NC} $COMMAND"
            fi
        fi
    done

    # End test if all results are in (count PASS and END:FAIL lines)
    TOTAL_TESTS=0
    PASSED=0
    FAILED=0
    for key in "${!TEST_RESULTS[@]}"; do
        TOTAL_TESTS=$((TOTAL_TESTS+1))
        if [ "${TEST_RESULTS[$key]}" = "PASS" ]; then
            PASSED=$((PASSED+1))
        else
            FAILED=$((FAILED+1))
        fi
    done

    # If at least one result and all are accounted for, break
    if [ "$TEST_STARTED" = true ] && [ $TOTAL_TESTS -gt 0 ]; then
        # If no new lines for a while, assume done
        if [ $((PASSED+FAILED)) -eq $TOTAL_TESTS ]; then
            break
        fi
    fi

    sleep 0.5
done

echo ""
echo "================================"
if [ $FAILED -eq 0 ] && [ $TOTAL_TESTS -gt 0 ]; then
    echo -e "${GREEN}All tests passed! ($PASSED/$TOTAL_TESTS)${NC}"
    exit 0
else
    echo -e "${RED}$FAILED test(s) failed ($PASSED/$TOTAL_TESTS passed)${NC}"
    exit 1
fi

