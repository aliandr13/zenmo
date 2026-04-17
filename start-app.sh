#!/bin/bash
# -------------------------------------------------------------
# Start Script for Java Spring Boot App (1 CPU / 512MB RAM)
# -------------------------------------------------------------

# Path to your JAR file
APP_DIR="./zenmo-app/target"
JAR_FILE="${APP_DIR}/zenmo.jar"

# JVM options optimized for low RAM
JAVA_OPTS="
-server
-Xms128m
-Xmx256m
-XX:MaxMetaspaceSize=128m
-XX:ReservedCodeCacheSize=64m
-Xss256k
-XX:+UseSerialGC
-XX:+ExitOnOutOfMemoryError
-XX:+UseStringDeduplication
-Dspring.main.banner-mode=off
-Dspring.main.lazy-initialization=true
"

# Spring Boot / Jetty tuning for low RAM
SPRING_OPTS="
-Dserver.jetty.threads.max=20
-Dserver.jetty.threads.max-queue-capacity=40
-Dserver.jetty.max-connections=200
-Dserver.jetty.connection-idle-timeout=10s
"

# Log file
LOG_FILE="${APP_DIR}/logs/app.log"
mkdir -p "${APP_DIR}/logs"

# Run the app in background
nohup java $JAVA_OPTS $SPRING_OPTS -jar $JAR_FILE > "$LOG_FILE" 2>&1 &
APP_PID=$!

echo "App started with PID $APP_PID"
echo "Logs: $LOG_FILE"
