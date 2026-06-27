Opentron Java backend

This is a minimal Spring Boot application that implements a small subset of
the OpenTron OpenAI-compatible API used by the frontend. It exposes:

- `GET /v1/models` — returns available models
- `POST /v1/chat/completions` — returns a simple completion (echo of last user message)

Build and run:

```bash
cd java/opentron-java/backend
mvn package
java -jar target/opentron-java-backend-0.1.0.jar
```

The server listens on port `8000` by default so it is a drop-in replacement for
the existing Python backend used by the frontend.

Windows packaging (optional)

You can create a native Windows installer or executable using `jpackage` (JDK
17+). Example:

```bash
cd java/opentron-java/backend/target
jpackage --type exe --input . --name OpentronBackend \
	--main-jar opentron-java-backend-0.1.0.jar --app-version 0.1.0 \
	--icon path/to/icon.ico
```

Alternatively use Launch4j or Inno Setup for more advanced installers.
