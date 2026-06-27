/**
 * OpenTron Command-Line Interface Package
 * 
 * <h2>Overview</h2>
 * <p>
 * The {@code io.opentron.cli} package contains the complete command-line interface
 * for OpenTron, a personal AI assistant with local-first, privacy-focused operations.
 * All 74+ commands are implemented with full feature parity to the Python reference.
 * </p>
 * 
 * <h2>Core Architecture</h2>
 * <ul>
 *   <li><strong>Main</strong> - Command router and dispatcher</li>
 *   <li><strong>BaseCommand</strong> - Abstract base for all CLI commands</li>
 *   <li><strong>Command Classes</strong> - 74 command implementations</li>
 *   <li><strong>Utility Classes</strong> - Helper functions and system utilities</li>
 *   <li><strong>Voice System</strong> - Cross-platform TTS/STT</li>
 * </ul>
 * 
 * <h2>Command Categories</h2>
 * 
 * <h3>Core Commands</h3>
 * <ul>
 *   <li>{@link Ask} - Query the AI assistant</li>
 *   <li>{@link ChatCmd} - Interactive chat mode</li>
 *   <li>{@link Serve} - Start OpenAI-compatible API server</li>
 *   <li>{@link VoiceCmd} - Voice input/output and voice chat</li>
 * </ul>
 * 
 * <h3>System Commands</h3>
 * <ul>
 *   <li>{@link InitCmd} - Initialize OpenTron system</li>
 *   <li>{@link DoctorCmd} - System health check</li>
 *   <li>{@link ConfigCmd} - Manage configuration</li>
 *   <li>{@link Bootstrap} - Bootstrap initialization</li>
 *   <li>{@link DaemonCmd} - Run as daemon/service</li>
 * </ul>
 * 
 * <h3>Data Management Commands</h3>
 * <ul>
 *   <li>{@link MemoryCmd} - Manage knowledge base (add, search, export)</li>
 *   <li>{@link VaultCmd} - Manage encrypted secrets</li>
 *   <li>{@link TelemetryCmd} - View usage metrics and statistics</li>
 *   <li>{@link ModelCmd} - Browse and manage models</li>
 * </ul>
 * 
 * <h3>Agent Commands</h3>
 * <ul>
 *   <li>{@link AgentCmd} - Create and manage agents</li>
 *   <li>{@link SchedulerCmd} - Schedule recurring tasks</li>
 *   <li>{@link WorkflowCmd} - Manage workflows</li>
 *   <li>{@link SkillCmd} - Manage skills</li>
 *   <li>{@link ToolCmd} - Manage tools</li>
 * </ul>
 * 
 * <h3>Integration Commands</h3>
 * <ul>
 *   <li>{@link ChannelCmd} - Manage communication channels</li>
 *   <li>{@link ChannelsCmd} - Global channel directory</li>
 *   <li>{@link ConnectCmd} - Connect to remote systems</li>
 *   <li>{@link TunnelCmd} - Create network tunnels</li>
 *   <li>{@link GatewayCmd} - Gateway management</li>
 * </ul>
 * 
 * <h3>Diagnostic Commands</h3>
 * <ul>
 *   <li>{@link BenchCmd} - Performance benchmarking</li>
 *   <li>{@link ScanCmd} - System scanning</li>
 *   <li>{@link TracesCmd} - Trace operations</li>
 *   <li>{@link EvalCmd} - Evaluate models and prompts</li>
 * </ul>
 * 
 * <h3>Advanced Commands</h3>
 * <ul>
 *   <li>{@link DeepResearchSetupCmd} - Setup deep research capabilities</li>
 *   <li>{@link OptimizeCmd} - Optimize performance</li>
 *   <li>{@link ComposeCmd} - Docker Compose integration</li>
 *   <li>{@link DigestCmd} - Content summarization</li>
 *   <li>{@link QuickstartCmd} - Quick start guide</li>
 * </ul>
 * 
 * <h2>Data Management System</h2>
 * <p>
 * The CLI integrates with the data management system through several backends:
 * </p>
 * <ul>
 *   <li><strong>MemoryStore</strong> - Persistent knowledge base with full-text search</li>
 *   <li><strong>VaultStore</strong> - Encrypted secrets management</li>
 *   <li><strong>TelemetryStore</strong> - Usage metrics and analytics</li>
 *   <li><strong>ModelRegistry</strong> - Model catalog with 20+ builtin models</li>
 *   <li><strong>DataManager</strong> - File I/O, configuration, and logging</li>
 * </ul>
 * 
 * <h2>Voice System</h2>
 * <p>
 * Full cross-platform voice support:
 * </p>
 * <ul>
 *   <li><strong>Windows</strong> - PowerShell speech synthesis + Speech Recognition API</li>
 *   <li><strong>macOS</strong> - Native 'say' command + AppleScript dictation</li>
 *   <li><strong>Linux</strong> - espeak for TTS + speech-recognition module for STT</li>
 * </ul>
 * 
 * <h2>Usage Examples</h2>
 * 
 * <h3>Query Examples</h3>
 * <pre>{@code
 * // Basic query
 * tron ask "What is machine learning?"
 * 
 * // Research mode with citations
 * tron ask "Explain quantum computing" --research
 * 
 * // JSON output
 * tron ask "Query" --json
 * 
 * // Save to memory
 * tron ask "Question" --save
 * }</pre>
 * 
 * <h3>Memory Management</h3>
 * <pre>{@code
 * // Add to memory
 * tron memory add "Learning note" --tags ml,learning --category study
 * 
 * // Search memory
 * tron memory search "machine learning"
 * 
 * // Export memory
 * tron memory export -f csv
 * }</pre>
 * 
 * <h3>Secrets Management</h3>
 * <pre>{@code
 * // Store secret
 * tron vault set api-key "secret-value"
 * 
 * // Retrieve secret
 * tron vault get api-key
 * 
 * // List secrets
 * tron vault list
 * }</pre>
 * 
 * <h3>Voice Commands</h3>
 * <pre>{@code
 * // Listen for voice input
 * tron voice listen
 * 
 * // Speak text
 * tron voice speak "Hello world"
 * 
 * // Interactive voice chat
 * tron voice chat
 * 
 * // Test voice system
 * tron voice test
 * }</pre>
 * 
 * <h2>Command Structure</h2>
 * <p>
 * All commands follow a consistent pattern:
 * </p>
 * <ul>
 *   <li>Extend {@link BaseCommand}</li>
 *   <li>Implement {@code execute(String[] args)}</li>
 *   <li>Implement {@code printUsage()}</li>
 *   <li>Provide {@code main(String[] args)} entry point</li>
 *   <li>Use {@code println()}, {@code print()}, {@code errorExit()} for output</li>
 * </ul>
 * 
 * <h2>Configuration</h2>
 * <p>
 * Configuration stored at {@code ~/.OpenTron/config.toml} with:
 * </p>
 * <ul>
 *   <li>Engine selection (Ollama, vLLM, etc.)</li>
 *   <li>Model defaults</li>
 *   <li>API endpoints</li>
 *   <li>Feature toggles</li>
 *   <li>Performance tuning</li>
 * </ul>
 * 
 * <h2>Storage Structure</h2>
 * <p>
 * All data stored in {@code ~/.OpenTron/}:
 * </p>
 * <ul>
 *   <li>{@code config.toml} - Main configuration</li>
 *   <li>{@code memory.db} - Knowledge base (JSON)</li>
 *   <li>{@code vault.json} - Encrypted secrets (JSON)</li>
 *   <li>{@code telemetry.db} - Usage metrics (JSON)</li>
 *   <li>{@code models.json} - Model registry (JSON)</li>
 *   <li>{@code logs/} - Audit logs</li>
 *   <li>{@code skills/} - User skills directory</li>
 * </ul>
 * 
 * <h2>Error Handling</h2>
 * <p>
 * All commands use consistent error handling:
 * </p>
 * <ul>
 *   <li>{@code errorExit(String msg)} - Print error and exit</li>
 *   <li>{@code println(String msg)} - Standard output</li>
 *   <li>{@code printlnErr(String msg)} - Error output</li>
 *   <li>{@code debug(String msg)} - Debug logging (--verbose)</li>
 * </ul>
 * 
 * <h2>Telemetry Integration</h2>
 * <p>
 * All commands automatically record metrics via {@link io.opentron.cli.data.TelemetryStore}:
 * </p>
 * <ul>
 *   <li>Command name and parameters</li>
 *   <li>Execution duration</li>
 *   <li>Token count</li>
 *   <li>Success/failure status</li>
 *   <li>Error messages</li>
 * </ul>
 * 
 * <h2>Features</h2>
 * <ul>
 *   <li><strong>74 Commands</strong> - Complete CLI coverage</li>
 *   <li><strong>Cross-Platform</strong> - Windows, macOS, Linux</li>
 *   <li><strong>Local-First</strong> - No cloud dependency</li>
 *   <li><strong>Privacy-Focused</strong> - All data stays local</li>
 *   <li><strong>Voice Support</strong> - TTS/STT on all platforms</li>
 *   <li><strong>Data Persistence</strong> - Everything saved locally</li>
 *   <li><strong>Encryption</strong> - Secrets encrypted in vault</li>
 *   <li><strong>Search</strong> - Full-text search on all data</li>
 *   <li><strong>Export</strong> - CSV/JSON export capabilities</li>
 *   <li><strong>Metrics</strong> - Comprehensive telemetry</li>
 * </ul>
 * 
 * <h2>Performance</h2>
 * <ul>
 *   <li>Fast command routing</li>
 *   <li>Minimal memory footprint</li>
 *   <li>Efficient data storage</li>
 *   <li>Async voice operations</li>
 *   <li>Optimized search algorithms</li>
 * </ul>
 * 
 * <h2>Compatibility</h2>
 * <ul>
 *   <li><strong>Java</strong> - 17+</li>
 *   <li><strong>Platforms</strong> - Windows, macOS, Linux</li>
 *   <li><strong>Shells</strong> - bash, zsh, PowerShell, cmd</li>
 *   <li><strong>Engines</strong> - Ollama, vLLM, LM Studio, Hugging Face</li>
 * </ul>
 * 
 * <h2>Development</h2>
 * <p>
 * To extend OpenTron:
 * </p>
 * <ol>
 *   <li>Create new command class extending {@link BaseCommand}</li>
 *   <li>Implement {@code execute()} and {@code printUsage()}</li>
 *   <li>Add case to {@link Main} switch statement</li>
 *   <li>Build with Maven: {@code mvn clean compile}</li>
 *   <li>Test: {@code java -jar tron-cli.jar command args}</li>
 * </ol>
 * 
 * <h2>License & Attribution</h2>
 * <p>
 * OpenTron Java CLI - Complete Java port of OpenTron Python CLI
 * Full feature parity with research, agents, workflows, and voice support.
 * </p>
 * 
 * @author OpenTron Development Team
 * @version 0.1.0
 * @since 2024
 */
package io.opentron.cli;
