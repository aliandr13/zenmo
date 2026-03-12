#!/bin/bash
APP_NAME="zenmo.jar"
PID=$(ps aux | grep $APP_NAME | grep -v grep | awk '{print $2}')

if [ -z "$PID" ]; then
    echo "No process found for $APP_NAME"
else
    echo "Stopping $APP_NAME (PID $PID)..."
    kill $PID
fi