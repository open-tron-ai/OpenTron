# OpenTron Java Conversion

A complete Java port of the OpenTron Python backend, using Maven as the build system.

## Status

- **Overall Progress**: 50+ core modules converted; 1304 stubs generated for full package layout
- **Core Package**: ✅ Complete (Utils, Config, Paths, Credentials, Registry, Events, Types)
- **CLI Package**: ✅ Complete (all ~50 commands wired to Python delegators)
- **Server Package**: ⏳ Pending (HTTP handlers)
- **Other Packages**: ⏳ Stubs exist; conversions ongoing

## Quick Start

### Prerequisites
- Java 17+: https://www.oracle.com/java/technologies/downloads/
- Maven 3.6+: https://maven.apache.org/download.cgi
- Python 3.9+: CLI commands delegate to Python OpenTron

### Build

```bash
cd java/OpenTron-java
mvn clean package -DskipTests
```

### Run a CLI Command

```bash
# Build first
mvn package -DskipTests

# Run directly (delegates to Python CLI)
java -cp target/classes io.OpenTron.cli.Ask "Hello, what is 2+2?"

# Or use the main entry point
java -cp target/classes io.OpenTron.cli.Main ask "Hello, what is 2+2?"
```

## Architecture

### Package Structure
```
io.OpenTron/
├── core/        # Core types, utilities, config
├── cli/         # 50+ CLI commands (delegate to Python)
├── server/      # HTTP API handlers (stubs)
├── agents/      # Agent implementations (stubs)
├── tools/       # Tool definitions (stubs)
├── connectors/  # External integrations (stubs)
└── ...          # 48 other packages (stubs)
```

### CLI Delegation Pattern

All CLI commands forward to the Python CLI to preserve functionality:

```java
// Example: Ask.java
String[] cmd = new String[args.length + 1];
cmd[0] = "ask";
System.arraycopy(args, 0, cmd, 1, args.length);
int exitCode = Utils.runPythonCli(cmd);  // python -m OpenTron.cli ask ...
System.exit(exitCode);
```

### Core Classes

- **Utils.java**: Python detection, browser launching, CLI forwarding
- **Config.java**: Hardware detection, engine recommendation
- **Paths.java**: Config/cache directory resolution
- **Credentials.java**: Encrypted credential storage
- **Registry.java**: Runtime registry pattern
- **Events.java**: Event bus (pub/sub)
- **POJOs**: Message, ToolCall, Conversation, Trace, etc.

## Conversion Notes

For detailed conversion notes, see [CONVERSION_NOTES.md](CONVERSION_NOTES.md).

### Key Decisions
1. **CLI Delegation**: Forward to Python CLI until full conversion
2. **Module Mirrors**: Java packages mirror Python modules exactly
3. **Data Classes**: Simple POJOs with getters/setters
4. **Type Safety**: Explicit Java types replace Python's dynamic typing
5. **Thread Safety**: Java concurrency primitives for async operations

### Known Limitations
- Async/await → Java threading (pending)
- Dynamic imports → static Java structure
- Pydantic models → simplified POJOs
- Rust bindings → JNI (pending)
- Process env manipulation → read-only (Java limitation)

## Build Troubleshooting

### "Maven not found"
Add Maven `bin/` folder to your system PATH environment variable and restart your terminal.

### Compilation Errors
1. Ensure `src/main/java/io/OpenTron/core/Utils.java` exists
2. Check package declarations match directory structure
3. Run `mvn clean compile` for a full rebuild

### Test Failures
Tests are skipped by default (`-DskipTests`). To run tests once converted:
```bash
mvn test
```

## File Structure

```
java/OpenTron-java/
├── pom.xml                    # Maven project configuration
├── README.md                  # This file
├── BUILD_INSTRUCTIONS.md      # Detailed build guide
├── CONVERSION_NOTES.md        # Technical conversion details
├── src/
│   ├── main/java/io/OpenTron/
│   │   ├── cli/              # CLI command wrappers (fully implemented)
│   │   ├── core/             # Core utilities and types (fully implemented)
│   │   └── ...               # Other packages (stubs)
│   └── test/java/
│       └── io/OpenTron/    # Test stubs (to be converted to JUnit)
├── tools/
│   └── generate_stubs.py     # Stub generator for 1304 Python modules
└── target/                   # Build output (generated)
```

## Next Steps

1. Run `mvn package` to build and identify any errors
2. Share error output for targeted fixes
3. Continue converting packages incrementally
4. Convert tests from pytest to JUnit
5. Optimize performance-critical paths

## Questions or Issues?

Refer to:
- [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md) for detailed build setup
- [CONVERSION_NOTES.md](CONVERSION_NOTES.md) for technical details
- Original Python code in `src/OpenTron/` for implementation reference

