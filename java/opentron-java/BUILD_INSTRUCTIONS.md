# Build Instructions for OpenTron Java Conversion

## Prerequisites

### 1. Install Java 17 or Later
- Download from: https://www.oracle.com/java/technologies/downloads/
- Or use: `winget install Oracle.JDK.17` (Windows)
- Verify: `java -version`

### 2. Install Apache Maven
- Download from: https://maven.apache.org/download.cgi
- Extract to a folder (e.g., `C:\apache-maven-3.9.x`)
- Add Maven `bin` folder to system PATH
- Verify: `mvn -version`

### 3. Ensure Python 3.9+ is in PATH
- The Java wrappers delegate CLI commands to the Python OpenTron CLI
- Verify: `python3 --version` or `python --version`

## Building the Project

### From the Java Project Root

```powershell
# Build without tests
mvn -q -DskipTests package

# Build with tests (once converted)
mvn package

# Clean and rebuild
mvn clean package

# Compile only (faster for iteration)
mvn compile
```

### Expected Build Artifacts
- JAR: `target/OpenTron-java.jar`
- Classes: `target/classes/`

## Known Limitations & Conversion Status

### Fully Converted Packages
- **core**: `Config`, `Paths`, `Credentials`, `Events`, `Registry`, `Types`, `Utils`
- **cli**: All 50+ CLI commands wired to Python CLI forwarding via `Utils.runPythonCli()`

### Partially Converted Packages
- **server**: Stubs exist but not yet implemented
- **agents**: Stubs exist but not yet implemented
- **tools**: Stubs exist but not yet implemented
- **connectors**: Stubs exist but not yet implemented

### Helper/Utility Classes (Remain as Stubs)
- Banner, BgState, ChatBanner, ChatNotifications, Dashboard, FirstRun, Hints, InstallDetect, LogConfig, Screen, ToolNames, VersionCheck

## Architecture

### CLI Delegation Pattern
All CLI commands follow a simple forwarding pattern:
```java
String[] cmd = new String[args.length + 1];
cmd[0] = "command-name";
System.arraycopy(args, 0, cmd, 1, args.length);
int code = Utils.runPythonCli(cmd);  // Forwards to: python -m OpenTron.cli <command>
System.exit(code);
```

This preserves full functionality while Java implementations are completed incrementally.

### Entry Point
- **Main.java**: Delegates entire CLI to Python
- **Individual Cmd classes**: Each command has its own wrapper (Ask, Serve, Config, etc.)

## Common Build Errors & Fixes

### Error: "Cannot find symbol: class Utils"
- Ensure `src/main/java/io/OpenTron/core/Utils.java` exists
- Check package declarations match directory structure

### Error: "The method ... is not found"
- This indicates a stub class that needs implementation
- Stubs throw `UnsupportedOperationException("Auto-generated stub")`
- Implement or replace with a working version

### Error: Maven command not found
- Add Maven bin folder to Windows PATH environment variable
- Restart terminal after updating PATH
- Verify: `mvn -version`

## Next Steps

1. **Run the build locally**: Follow "Building the Project" section above
2. **Report any compilation errors**: Share error output for targeted fixes
3. **Iterate on high-priority modules**: Convert core packages to full Java as needed
4. **Test individual commands**: `java -cp target/classes io.OpenTron.cli.Ask "test query"`

## File Structure

```
java/OpenTron-java/
├── pom.xml                           # Maven project descriptor
├── src/
│   ├── main/java/io/OpenTron/
│   │   ├── cli/                      # 50+ CLI command wrappers
│   │   ├── core/                     # Core types and utilities
│   │   └── ...                       # Other packages (stubs)
│   └── test/java/
│       └── io/OpenTron/...         # Test stubs (to be converted)
├── tools/
│   └── generate_stubs.py             # Stub generator script
├── README.md                         # Quick start guide
├── CONVERSION_NOTES.md               # Detailed conversion notes
└── BUILD_INSTRUCTIONS.md             # This file
```

## Questions?

Refer to:
- [Maven documentation](https://maven.apache.org/guides/)
- [Java 17 features](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
- Original Python code in `src/OpenTron/` for implementation details

