#!/bin/bash

# PostgreSQL Integration Setup Script for OpenTron (macOS/Linux)
# This script sets up all environment variables and starts the backend

echo ""
echo "============================================================"
echo "  OpenTron PostgreSQL Integration Setup"
echo "============================================================"
echo ""

# Set Java Home (adjust path as needed)
export JAVA_HOME="/usr/libexec/java_home -v 21"  # macOS
# For Linux, adjust to your JDK path
export PATH="$JAVA_HOME/bin:$PATH"

# Set Maven Home (adjust path as needed)
export MAVEN_HOME="/opt/homebrew/opt/maven/libexec"  # macOS
# For Linux: /usr/share/maven or wherever Maven is installed
export PATH="$MAVEN_HOME/bin:$PATH"

# Set PostgreSQL Configuration
export POSTGRES_URL="jdbc:postgresql://localhost:5432/opentron"
export POSTGRES_USER="opentron"
export POSTGRES_PASSWORD="opentron_secure_password"
export ENGINE_HOST="http://localhost:11434"

echo "[1/5] Java and Maven configuration set"
echo ""

# Verify Docker is running
echo "[2/5] Verifying Docker and PostgreSQL setup..."
if ! docker ps | grep -q "opentron-postgres"; then
    echo ""
    echo "WARNING: PostgreSQL container not found!"
    echo "Run this command first to start PostgreSQL:"
    echo ""
    echo "docker run -d --restart always --name opentron-postgres \\"
    echo "  -e POSTGRES_DB=opentron \\"
    echo "  -e POSTGRES_USER=opentron \\"
    echo "  -e POSTGRES_PASSWORD=opentron_secure_password \\"
    echo "  -p 5432:5432 \\"
    echo "  -v postgres_data:/var/lib/postgresql/data \\"
    echo "  postgres:16-alpine"
    echo ""
    echo "Waiting 30 seconds before continuing..."
    sleep 30
else
    echo "PostgreSQL container is running"
fi

echo ""

# Navigate to backend
cd java/opentron-java/backend

echo "[3/5] Building backend with Maven..."
echo ""
mvn clean package -DskipTests -q
if [ $? -ne 0 ]; then
    echo ""
    echo "ERROR: Maven build failed!"
    echo ""
    exit 1
fi

echo ""
echo "[4/5] Backend build completed successfully"
echo ""

# Show startup information
echo "============================================================"
echo "[5/5] Starting OpenTron Backend"
echo "============================================================"
echo ""
echo "Configuration:"
echo "  Java Home: $JAVA_HOME"
echo "  Maven Home: $MAVEN_HOME"
echo "  PostgreSQL URL: $POSTGRES_URL"
echo "  PostgreSQL User: $POSTGRES_USER"
echo "  Ollama Engine: $ENGINE_HOST"
echo ""
echo "Starting backend..."
echo ""

# Start the backend
mvn spring-boot:run

# If backend exits, show message
echo ""
echo "Backend has stopped."
exit 0
