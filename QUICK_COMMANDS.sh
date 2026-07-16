#!/bin/bash
# Quick Commands Reference for Virtual Threads & H2 Setup

# ===========================================
# BUILD COMMANDS
# ===========================================

# Clean build (all profiles support same JAR)
cd java/opentron-java/backend
mvn clean package -DskipTests

# Build with specific settings
mvn clean package -DskipTests -Dmaven.test.skip=true


# ===========================================
# RUN WITH EMBEDDED H2 (Zero External Dependencies)
# ===========================================

# Development: Maven Spring Boot
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=embedded"

# Production JAR
java -jar target/opentron-java-backend-0.1.0-exec.jar --spring.profiles.active=embedded

# With custom data directory (optional)
java -jar target/opentron-java-backend-0.1.0-exec.jar \
  --spring.profiles.active=embedded \
  --server.port=8000


# ===========================================
# RUN WITH POSTGRESQL (Production)
# ===========================================

# Development: Maven Spring Boot
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod"

# Production JAR with custom PostgreSQL
java -jar target/opentron-java-backend-0.1.0-exec.jar \
  --spring.profiles.active=prod \
  --spring.datasource.url=jdbc:postgresql://db.example.com:5432/opentron \
  --spring.datasource.username=opentron_user \
  --spring.datasource.password=secure_password

# Production JAR with environment variables
export SPRING_PROFILES_ACTIVE=prod
export POSTGRES_URL=jdbc:postgresql://localhost:5432/opentron
export POSTGRES_USER=opentron
export POSTGRES_PASSWORD=opentron_secure_password
java -jar target/opentron-java-backend-0.1.0-exec.jar


# ===========================================
# ENVIRONMENT VARIABLES
# ===========================================

# Embedded Profile (H2)
export SPRING_PROFILES_ACTIVE=embedded
export SERVER_PORT=8000

# Production Profile (PostgreSQL)
export SPRING_PROFILES_ACTIVE=prod
export POSTGRES_URL=jdbc:postgresql://localhost:5432/opentron
export POSTGRES_USER=opentron
export POSTGRES_PASSWORD=opentron_secure_password
export POSTGRES_DB=opentron


# ===========================================
# DATABASE MANAGEMENT
# ===========================================

# H2 Database Location
~/.opentron/opentron.mv.db

# Start H2 Web Console (requires running backend with embedded profile)
# Then access: http://localhost:8000/h2-console
# JDBC URL: jdbc:h2:file:~/.opentron/opentron
# User: sa
# Password: (empty)

# PostgreSQL Docker Commands
docker run -d --name opentron-postgres \
  -e POSTGRES_DB=opentron \
  -e POSTGRES_USER=opentron \
  -e POSTGRES_PASSWORD=opentron_secure_password \
  -p 5432:5432 \
  -v postgres_data:/var/lib/postgresql/data \
  postgres:16-alpine

docker stop opentron-postgres
docker rm opentron-postgres
docker logs opentron-postgres


# ===========================================
# TESTING PROFILES
# ===========================================

# Test embedded profile startup
java -jar target/opentron-java-backend-0.1.0-exec.jar \
  --spring.profiles.active=embedded \
  --debug 2>&1 | grep -i "datasource\|h2\|virtual"

# Test prod profile startup
java -jar target/opentron-java-backend-0.1.0-exec.jar \
  --spring.profiles.active=prod \
  --debug 2>&1 | grep -i "datasource\|postgres\|virtual"

# Check active profile in logs
# Look for: "Active profiles:" line


# ===========================================
# CONNECTIVITY TESTS
# ===========================================

# Health check
curl http://localhost:8000/actuator/health

# Metrics
curl http://localhost:8000/actuator/metrics

# API test
curl -X GET http://localhost:8000/v1/health

# H2 Console (embedded profile only)
# Browser: http://localhost:8000/h2-console


# ===========================================
# POWERSHELL COMMANDS (Windows)
# ===========================================

# Interactive profile selection
powershell -ExecutionPolicy Bypass -File start-stack-profiles.ps1

# Manual embedded startup
$env:SPRING_PROFILES_ACTIVE="embedded"
mvn spring-boot:run

# Manual PostgreSQL startup
$env:SPRING_PROFILES_ACTIVE="prod"
$env:POSTGRES_URL="jdbc:postgresql://localhost:5432/opentron"
$env:POSTGRES_USER="opentron"
$env:POSTGRES_PASSWORD="opentron_secure_password"
mvn spring-boot:run


# ===========================================
# DEBUGGING
# ===========================================

# Enable debug logging
java -jar target/opentron-java-backend-0.1.0-exec.jar \
  --spring.profiles.active=embedded \
  --logging.level.root=DEBUG

# Enable SQL logging (see actual queries)
java -jar target/opentron-java-backend-0.1.0-exec.jar \
  --spring.profiles.active=embedded \
  --logging.level.org.hibernate.SQL=DEBUG \
  --spring.jpa.show-sql=true

# Trace H2 Database file operations
java -jar target/opentron-java-backend-0.1.0-exec.jar \
  --spring.profiles.active=embedded \
  --logging.level.org.h2=DEBUG


# ===========================================
# PORT & SERVICE CHECKS
# ===========================================

# Linux/macOS: Check if port 8000 is in use
lsof -i :8000

# Windows PowerShell: Check if port 8000 is in use
Get-NetTCPConnection -LocalPort 8000 -ErrorAction SilentlyContinue

# Kill process on port 8000 (macOS/Linux)
kill $(lsof -t -i :8000)

# Kill process on port 8000 (Windows PowerShell)
Stop-Process -Id (Get-NetTCPConnection -LocalPort 8000).OwningProcess -Force


# ===========================================
# FLYWAY MIGRATIONS
# ===========================================

# Info about pending migrations
mvn flyway:info

# Validate migrations
mvn flyway:validate

# Baseline (first time setup with existing DB)
mvn flyway:baseline

# Migrate
mvn flyway:migrate


# ===========================================
# DESKTOP/TAURI INTEGRATION
# ===========================================

# Build JAR for embedding
mvn clean package -DskipTests -f java/opentron-java/backend/pom.xml

# Location of JAR for Tauri
# Source: java/opentron-java/backend/target/opentron-java-backend-0.1.0-exec.jar
# Destination: desktop/src-tauri/binaries/

# Embedded profile ensures H2 used (no external deps)
# Tauri command: java -jar ./opentron-backend.jar --spring.profiles.active=embedded
