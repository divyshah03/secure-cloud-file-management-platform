#!/bin/bash

# Stop script for Cloud File Manager
# Stops backend and frontend services

echo "🛑 Stopping Cloud File Manager..."

# Try to read PIDs from file
if [ -f "logs/pids.txt" ]; then
    PIDS=$(cat logs/pids.txt)
    for PID in $PIDS; do
        if kill -0 $PID 2>/dev/null; then
            kill $PID
            echo "Stopped process (PID: $PID)"
        fi
    done
    rm logs/pids.txt
fi

# Also try to kill by name
pkill -f "spring-boot:run" 2>/dev/null && echo "Stopped Spring Boot application"
pkill -f "vite" 2>/dev/null && echo "Stopped Vite server"

echo "✅ Services stopped"
