#!/bin/bash
set -euo pipefail

# Forwards [AG_TEST:COMMAND:...] lines from server logs into the server command FIFO
COMMAND_FIFO="/tmp/hytale_commands.fifo"
LOG_DIR="./server/logs"

if [ ! -d "$LOG_DIR" ]; then
  echo "Log dir $LOG_DIR not found" >&2
  exit 1
fi

echo "Command forwarder starting, waiting for FIFO $COMMAND_FIFO"
while [ ! -p "$COMMAND_FIFO" ]; do
  sleep 0.5
done
echo "Found FIFO $COMMAND_FIFO, starting to follow logs"

# Wait for at least one log file to appear before starting tail.
echo "Waiting for log files in $LOG_DIR"
while ! ls "$LOG_DIR"/*.log* 1> /dev/null 2>&1; do
  sleep 0.5
done
echo "Found log files, starting to follow logs"

# Follow all log files (including .log.lck). tail -F will follow rotated files.
tail -n0 -F "$LOG_DIR"/*.log* 2>/dev/null | while IFS= read -r line; do
  if echo "$line" | grep -q "\[AG_TEST:COMMAND:"; then
    COMMAND=$(echo "$line" | sed -n 's/.*\[AG_TEST:COMMAND:\([^]]*\)\].*/\1/p')
    # Debug info
    echo "[commandForwarder] matched line: $line" >&2
    echo "[commandForwarder] extracted command: '$COMMAND'" >&2
    if [ -e "$COMMAND_FIFO" ]; then
      if command -v stat >/dev/null 2>&1; then
        stat -c 'perms=%A owner=%U group=%G file=%n' "$COMMAND_FIFO" >&2 || true
      else
        ls -l "$COMMAND_FIFO" >&2 || true
      fi
    else
      echo "[commandForwarder] FIFO $COMMAND_FIFO missing" >&2
    fi

    if [ -n "$COMMAND" ] && [ -p "$COMMAND_FIFO" ]; then
      if command -v timeout >/dev/null 2>&1; then
        if timeout 2s bash -c "echo \"$COMMAND\" > \"$COMMAND_FIFO\""; then
          echo "Forwarded command to server: $COMMAND"
        else
          echo "[commandForwarder] Failed to write to FIFO (timeout) for command: $COMMAND" >&2
        fi
      else
        # Fallback: background the write so forwarder doesn't block indefinitely
        (echo "$COMMAND" > "$COMMAND_FIFO") &
        echo "Forwarded command to server (bg): $COMMAND"
      fi
    fi
  fi
done
