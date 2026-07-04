#!/bin/bash
# OpenTron Full Stack Integration - Start Script
# Starts PostgreSQL, Backend, and Frontend

set -e

echo ""
echo "╔════════════════════════════════════════════╗"
echo "║  OpenTron - PostgreSQL Integration Stack   ║"
echo "║  Backend + Frontend + Database              ║"
echo "╚════════════════════════════════════════════╝"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Set environment variables
export POSTGRES_URL="jdbc:postgresql://localhost:5432/opentron"
export POSTGRES_USER="opentron"
export POSTGRES_PASSWORD="opentron_secure_password"
export ENGINE_HOST="http://localhost:11434"

# Step 1: Check Docker
echo -e "${YELLOW}[1/4] Checking Docker...${NC}"
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Docker found${NC}"

# Step 2: Start PostgreSQL
echo -e "${YELLOW}[2/4] Starting PostgreSQL...${NC}"
if docker ps | grep -q "opentron-postgres"; then
    echo -e "${GREEN}✅ PostgreSQL already running${NC}"
else
    echo "Starting PostgreSQL container..."
    docker run -d --restart always --name opentron-postgres \
        -e POSTGRES_DB=opentron \
        -e POSTGRES_USER=opentron \
        -e POSTGRES_PASSWORD=opentron_secure_password \
        -p 5432:5432 \
        -v postgres_data:/var/lib/postgresql/data \
        postgres:16-alpine
    
    echo "Waiting for PostgreSQL to start..."
    sleep 5
    echo -e "${GREEN}✅ PostgreSQL started${NC}"
fi

# Step 3: Start Backend
echo -e "${YELLOW}[3/4] Starting Backend...${NC}"
cd java/opentron-java/backend

echo "Building backend..."
mvn clean package -DskipTests -q

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Backend built successfully${NC}"
    
    echo "Starting backend server..."
    mvn spring-boot:run &
    BACKEND_PID=$!
    
    echo "Waiting for backend to start..."
    sleep 10
    
    if curl -s http://localhost:8000/v1/agents/status > /dev/null; then
        echo -e "${GREEN}✅ Backend running on port 8000${NC}"
    else
        echo -e "${RED}⚠️ Backend may not be responding${NC}"
    fi
else
    echo -e "${RED}❌ Backend build failed${NC}"
    exit 1
fi

cd - > /dev/null

# Step 4: Start Frontend
echo -e "${YELLOW}[4/4] Starting Frontend...${NC}"
cd frontend

if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    npm install -q
fi

echo "Starting frontend..."
npm run tauri dev &
FRONTEND_PID=$!

echo -e "${GREEN}✅ Frontend starting${NC}"

echo ""
echo "╔════════════════════════════════════════════╗"
echo "║  🚀 OpenTron Stack Running                  ║"
echo "╠════════════════════════════════════════════╣"
echo "║  PostgreSQL:  localhost:5432               ║"
echo "║  Backend:     http://localhost:8000        ║"
echo "║  Frontend:    http://localhost:5173 (Tauri)║"
echo "╠════════════════════════════════════════════╣"
echo "║  Database:    opentron                     ║"
echo "║  User:        opentron                     ║"
echo "║  Password:    opentron_secure_password     ║"
echo "╠════════════════════════════════════════════╣"
echo "║  Open StorageDashboard to view real-time   ║"
echo "║  traces, memories, and statistics.         ║"
echo "╚════════════════════════════════════════════╝"
echo ""

# Keep script running
wait $BACKEND_PID $FRONTEND_PID
