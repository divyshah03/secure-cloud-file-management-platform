#!/bin/bash

# Start script for Cloud File Manager
# Starts both backend and frontend in development mode

set -e

echo "🚀 Starting Cloud File Manager..."

# Create logs directory if it doesn't exist
mkdir -p logs

# Start backend in background
echo "Starting backend..."
cd backend
mvn spring-boot:run > ../logs/backend.log 2>&1 &
BACKEND_PID=$!
echo "Backend started (PID: $BACKEND_PID)"
cd ..

# Wait for backend to start
echo "Waiting for backend to be ready..."
sleep 10

# Start frontend
echo "Starting frontend..."
cd frontend
npm run dev &
FRONTEND_PID=$!
echo "Frontend started (PID: $FRONTEND_PID)"
cd ..

echo ""
echo "✅ Services started!"
echo "Backend: http://localhost:8080"
echo "Frontend: http://localhost:5173"
echo ""
echo "To stop services, run: ./scripts/stop.sh"
echo "Or kill processes: kill $BACKEND_PID $FRONTEND_PID"

# Save PIDs to file
echo "$BACKEND_PID $FRONTEND_PID" > logs/pids.txt
