#!/bin/bash
# Setup PostgreSQL Docker container for OpenTron
# Run this script to start PostgreSQL and configure the environment

set -e

echo "=========================================="
echo "OpenTron PostgreSQL Docker Setup"
echo "=========================================="
echo ""

# Check if Docker is running
echo "[1/5] Checking Docker daemon..."
if ! docker ps > /dev/null 2>&1; then
    echo "❌ Docker daemon is not running!"
    echo "   Please start Docker Desktop and try again."
    exit 1
fi
echo "✅ Docker daemon is running"
echo ""

# Check if container already exists
echo "[2/5] Checking for existing PostgreSQL container..."
if docker ps -a --format '{{.Names}}' | grep -q "^opentron-postgres$"; then
    echo "⚠️  Container 'opentron-postgres' already exists"
    if docker ps --format '{{.Names}}' | grep -q "^opentron-postgres$"; then
        echo "✅ Container is already running"
        CONTAINER_RUNNING=true
    else
        echo "   Starting existing container..."
        docker start opentron-postgres
        echo "✅ Container started"
        CONTAINER_RUNNING=true
    fi
else
    echo "   Creating new PostgreSQL container..."
    docker run -d \
        --name opentron-postgres \
        -e POSTGRES_DB=opentron \
        -e POSTGRES_USER=opentron \
        -e POSTGRES_PASSWORD=opentron_secure_password \
        -p 5432:5432 \
        -v postgres_data:/var/lib/postgresql/data \
        postgres:16-alpine
    
    echo "✅ PostgreSQL container created"
    CONTAINER_RUNNING=true
fi
echo ""

# Wait for PostgreSQL to be ready
echo "[3/5] Waiting for PostgreSQL to be ready..."
for i in {1..30}; do
    if docker exec opentron-postgres pg_isready -U opentron > /dev/null 2>&1; then
        echo "✅ PostgreSQL is ready"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "❌ PostgreSQL failed to start within 30 seconds"
        exit 1
    fi
    echo "   Waiting... ($i/30)"
    sleep 1
done
echo ""

# Verify connection
echo "[4/5] Verifying connection..."
if docker exec opentron-postgres psql -U opentron -d opentron -c "SELECT version();" > /dev/null 2>&1; then
    VERSION=$(docker exec opentron-postgres psql -U opentron -d opentron -c "SELECT version();" -t)
    echo "✅ Connection successful"
    echo "   PostgreSQL: $VERSION"
else
    echo "❌ Connection failed"
    exit 1
fi
echo ""

# Display connection information
echo "[5/5] Configuration Summary"
echo "=========================================="
echo "✅ PostgreSQL is ready!"
echo ""
echo "Connection Details:"
echo "  Host:     localhost"
echo "  Port:     5432"
echo "  Database: opentron"
echo "  User:     opentron"
echo "  Password: opentron_secure_password"
echo ""
echo "Docker Container:"
echo "  Name:     opentron-postgres"
echo "  Image:    postgres:16-alpine"
echo "  Status:   Running"
echo ""
echo "Environment Variables (set these for your Java backend):"
echo "  export POSTGRES_URL=jdbc:postgresql://localhost:5432/opentron"
echo "  export POSTGRES_USER=opentron"
echo "  export POSTGRES_PASSWORD=opentron_secure_password"
echo ""
echo "Useful Docker Commands:"
echo "  docker logs opentron-postgres          # View logs"
echo "  docker exec -it opentron-postgres psql -U opentron -d opentron  # Connect via psql"
echo "  docker stop opentron-postgres          # Stop container"
echo "  docker start opentron-postgres         # Start container"
echo "  docker rm opentron-postgres            # Remove container"
echo ""
echo "=========================================="
