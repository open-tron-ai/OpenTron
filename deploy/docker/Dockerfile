# Stage 1: Build frontend SPA
FROM dhi.io/node:22-alpine-dev AS frontend

WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm ci --ignore-scripts 2>/dev/null || npm install
COPY frontend/ .
RUN npm run build

# Stage 2: Build Java backend
FROM dhi.io/maven:3.9-jdk17-alpine-dev AS java-builder

WORKDIR /app
COPY java/opentron-java/backend/pom.xml .
COPY java/opentron-java/backend/src ./src

RUN mvn clean package -DskipTests -q

# Stage 3: Runtime
FROM dhi.io/eclipse-temurin:17-alpine

WORKDIR /app

# Copy built JAR from builder
COPY --from=java-builder /app/target/opentron-java-backend-*.jar app.jar

EXPOSE 8000

ENTRYPOINT exec java -Dengine.host="${ENGINE_HOST:-http://127.0.0.1:8000}" -Dengine.apiKey="${ENGINE_APIKEY}" -jar app.jar
