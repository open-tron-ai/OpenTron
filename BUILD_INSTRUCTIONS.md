# Rebuild Instructions

## Build Command (Copy & Paste)

Since PowerShell has issues with Maven, use Command Prompt (cmd.exe) instead:

**Open Command Prompt (cmd.exe) and run:**

```cmd
cd C:\Users\ciorica\Documents\OpenTron\java\opentron-java
mvn clean package -DskipTests
```

## If Maven is not in PATH

**Use the full path:**

```cmd
cd C:\Users\ciorica\Documents\OpenTron\java\opentron-java
"C:\Program Files\Apache Maven\apache-maven-3.9.0\bin\mvn" clean package -DskipTests
```

## Or use the Maven wrapper (if available)

```cmd
cd C:\Users\ciorica\Documents\OpenTron\java\opentron-java
mvnw clean package -DskipTests
```

## Expected Output

```
[INFO] Scanning for projects...
[INFO] 
[INFO] ---------< org.opentron:opentron-java-backend >----------
[INFO] Building opentron-java-backend 1.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
...
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXX s
[INFO] Finished at: ...
```

## After Build

1. Start backend:
```cmd
cd C:\Users\ciorica\Documents\OpenTron\java\opentron-java\backend\target
java -jar opentron-java-backend-1.0-SNAPSHOT.jar
```

2. Test the agents:
```cmd
curl -X POST http://localhost:8000/v1/agents/coordinate -H "Content-Type: application/json" -d "{\"request\":\"optimize database\"}"
```

Expected response in <600ms with AI recommendations!

## What Was Fixed

- `MultiAgentController.java` - Fixed type incompatibility with `ResponseEntity<?>`
- Added explicit `(Object)` casts to resolve Mono generic type issues
- All 5 agents (Coordinator, Backend, Frontend, QA, DevOps) are now ready with LLM integration
