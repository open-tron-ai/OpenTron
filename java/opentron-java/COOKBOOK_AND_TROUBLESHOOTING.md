# OpenTron Java CLI - Command Cookbook & Troubleshooting

## 📖 Command Cookbook - Real-World Examples

### Setup & First-Time Use

```bash
# 1. Initialize OpenTron (5 minutes)
tron init
# Answers prompts, detects hardware, generates config

# 2. Verify installation
tron doctor
# Checks engines, models, connectivity, memory

# 3. Download a model
tron model pull qwen2.5:7b
# Downloads from Ollama hub (~4GB for this model)

# 4. Start chatting
tron chat
# Now you can ask questions!
```

### Interactive Chat Examples

```bash
# Start chat session
$ tron chat

You> What is the capital of France?
Assistant> The capital of France is Paris. It's located in the northern central part of the country along the Seine River.

You> /model
Model: qwen2.5:7b  Engine: ollama

You> /clear
History cleared.

You> /quit
Goodbye!
```

### One-Off Queries

```bash
# Simple question
tron ask "What is quantum computing?"

# With profiling
tron ask "Explain neural networks" --profile

# With specific model
tron ask "What is X?" -m mistral:7b

# With temperature setting
tron ask "Be creative" -t 0.9

# Research mode (knowledge base)
tron ask "Research recent AI breakthroughs" --research

# Save output to file
tron ask "Write a poem" > output.txt
```

### Vision Support (Images & Screenshots)

```bash
# Single image
tron ask "What's in this image?" --image photo.jpg

# Multiple images
tron ask "Compare these" --image img1.jpg --image img2.jpg

# Capture screen
tron ask "What do you see?" --screen

# Image + research
tron ask "Analyze diagram" --image diagram.png --profile
```

### Voice Mode Adventures

```bash
# Start voice chat
$ tron --voice

# System listens for speech...
[Listening for voice input...]

"What time is it?"  (you speak)
🔊 [AI responds audibly]
"The current time is..."

"Show me the weather"
🔊 [AI provides forecast]

"exit"  (or "goodbye")
🔊 [Goodbye!]
```

### Hardware & Configuration

```bash
# Show detected hardware
$ tron doctor

[1/5] Checking Configuration...
✓ Configuration file found
  Location: ~/.OpenTron/config.toml
  Default engine: ollama
  Default model: qwen2.5:7b

[2/5] Checking Engines...
✓ ollama is running on localhost:11434
⚠ No vLLM detected

[3/5] Checking Models...
✓ Models available: 2
  - qwen2.5:7b
  - mistral:7b

...

Status: All checks passed!
```

### Model Management

```bash
# List available models
$ tron model list

[Available Models]

Model                   Parameters    Context        VRAM
──────────────────────────────────────────────────────
qwen2.5:7b             7B           32K             4GB
mistral:7b             7B           32K             5GB
llama2:70b             70B          4K              40GB

# Get detailed model info
$ tron model info qwen2.5:7b

Model ID:        qwen2.5:7b
Name:            Qwen 2.5 7B
Parameters:      7B
Context:         32,768
Architecture:    Transformer
Quantization:    Q4_0
Min VRAM:        4GB
Provider:        Alibaba
License:         Apache 2.0

# Download model
$ tron model pull qwen3.5:14b
Pulling qwen3.5:14b...
  downloading: 20%
  downloading: 40%
  downloading: 60%
  downloading: 80%
  downloading: 100%
✓ Successfully pulled qwen3.5:14b
```

### Memory Management

```bash
# Store facts about yourself
$ tron memory add "User Name: Alex"
$ tron memory add "User Job: ML Engineer"
$ tron memory add "User Location: San Francisco"
$ tron memory add "Project: Building AI chatbot"

# Search for information
$ tron memory search "user job"

[Results]

Score: 0.987
User Job: ML Engineer

Score: 0.456
Project: Building AI chatbot

# Export memory backup
$ tron memory export
✓ Memory exported to memory_export_1234567890.json

# Clear problematic memory
$ tron memory clear
Are you sure? (y/N): y
✓ Memory cleared

# Restore from backup
$ tron memory import memory_export_1234567890.json
✓ Imported 42 memories
```

### Agent Management

```bash
# Create a research agent
$ tron agent create --name "Research Bot"
✓ Created agent: agent-abc123 (Research Bot)

# List all agents
$ tron agent list

[Managed Agents]

ID          Name          Type              Status
─────────────────────────────────────────────────
agent-abc   Research Bot  monitor_operative idle
agent-def   Writer        simple            idle

# Run agent immediately (testing)
$ tron agent run agent-abc123
Running tick for "Research Bot"...
  ✓ Searching: "latest AI trends"
  ✓ Found 12 results
  ✓ Analyzing results
  ✓ Generating summary
✓ Tick complete. Status: idle, runs: 3

[Findings]
# AI Trends for This Week
- Diffusion models improving...
- Transformer efficiency advances...
- New benchmarks released...

# Ask agent a question (immediate mode)
$ tron agent ask agent-abc123 "What's trending in ML?"

Agent: Based on recent papers and discussions, the hottest topics are...

# Watch agent activity live
$ tron agent watch
agent-research      TICK_START
agent-research      TOOL_CALL: web_search
agent-research      TOOL_CALL: summarize
agent-research      TICK_END

# Pause and resume
$ tron agent pause agent-abc123
$ tron agent resume agent-abc123
```

### Learning & Insights

```bash
# View learning history
$ tron learning agent-abc123

[Learning History for Agent: agent-abc123]

Timestamp               Event               Impact Score
───────────────────────────────────────────────────────
2024-01-15 10:30:45    tool_call_success   0.852
2024-01-15 10:25:12    pattern_recognized  0.921
2024-01-15 10:20:03    error_recovered     0.234

# Trigger manual learning cycle
$ tron learning agent-abc123 --trigger
✓ Learning triggered
  Session ID: sess-xyz789

# View learning insights
$ tron learning agent-abc123 --insights

[Learning Insights]

Top Learning Patterns:
  • Recursive search improves results (confidence: 92.1%)
  • Multi-step verification prevents errors (confidence: 87.5%)
  • Semantic search beats keyword (confidence: 84.3%)

Areas for Improvement:
  • Tool selection logic (priority: high)
  • Error handling edge cases (priority: medium)
  • Response time optimization (priority: low)

Overall Learning Score: 7.8 / 10.00
```

### Execution Traces

```bash
# List recent traces
$ tron traces

[Recent Execution Traces]

Trace ID      Timestamp             Agent       Status    Duration
──────────────────────────────────────────────────────────────────
trace-123     2024-01-15 10:30:45   agent-abc   success   2340ms
trace-122     2024-01-15 10:25:12   agent-abc   success   1890ms
trace-121     2024-01-15 09:50:03   agent-def   success   5670ms

# View agent traces
$ tron traces agent-abc123

# See detailed trace
$ tron traces trace-123 --detail

[Execution Trace: trace-123]

Metadata:
  Status: success
  Duration: 2340ms
  Timestamp: 2024-01-15 10:30:45

Execution Steps:

  Step 1: search_query
    Type: tool_call
    Status: success
    Duration: 1200ms
    Input: {"query": "latest AI trends"}
    Output: {"results": 12, "top_score": 0.95}

  Step 2: analyze_results
    Type: inference
    Status: success
    Duration: 890ms
    Input: {"results": 12}
    Output: {"insights": [... ]}

  Step 3: generate_summary
    Type: inference
    Status: success
    Duration: 250ms

Summary:
Successfully completed research cycle on AI trends. Found 12 relevant sources with high relevance scores.
```

### Workflow Composition

```bash
# Create workflow
$ tron workflow create --name "Research Pipeline"
✓ Workflow created
  ID: wf-789
  Name: Research Pipeline

# Add steps to workflow
$ tron workflow add-step wf-789 "Search" --type tool --config '{"tool":"web_search","max_results":10}'
✓ Step added: Search

$ tron workflow add-step wf-789 "Analyze" --type inference
✓ Step added: Analyze

$ tron workflow add-step wf-789 "Summarize" --type tool --config '{"tool":"summarize"}'
✓ Step added: Summarize

# Show workflow
$ tron workflow info wf-789

[Workflow: Research Pipeline]

Description: Research pipeline workflow
Status: draft
Created: 2024-01-15 10:00:00

Steps:
  1. Search
     Type: tool
     web_search with max_results=10

  2. Analyze
     Type: inference
     Analyze results

  3. Summarize
     Type: tool
     Summarize findings

# Execute workflow
$ tron workflow run wf-789 '{"topic":"quantum computing"}'

[Execution Result]

Output:
{"summary": "Quantum computing is..."}

Steps executed: 3
Execution time: 3.45s
Status: ✓ success
```

### Tool & Skill Management

```bash
# List available tools
$ tron tool list

[Available Tools]

Name                Description
─────────────────────────────────────
web_search          Search the web
calculate           Math calculations
summarize           Summarize text
translate           Translate languages

# Run a tool
$ tron tool run calculate a=10 b=5 operation=multiply
[Tool Execution Result]
Output: 50
Status: ✓ Success
Execution time: 0.12s

# Manage skills
$ tron skill list

[Agent Skills]

Skill             Description                Status
─────────────────────────────────────────────
research          Web research & analysis    ✓ enabled
writing           Content generation        ✓ enabled
coding            Code generation & review  ✗ disabled
math              Mathematical operations   ✓ enabled

# Enable/disable skills
$ tron skill enable coding
✓ Skill 'coding' enabled

$ tron skill disable math
✓ Skill 'math' disabled
```

### Channel Integration (Slack/Telegram/Discord)

```bash
# Setup Slack
$ tron channels add slack

# Store Slack credentials
$ tron vault add slack bot_token "xoxb-YOUR-TOKEN"
$ tron vault add slack webhook_url "https://hooks.slack.com/..."

# Bind agent to Slack channel
$ tron channels bind channel-slack agent-research

# Test connectivity
$ tron channels test channel-slack
✓ Test successful
  Latency: 245ms

# Now your agent posts to Slack automatically!
```

---

## 🔧 Troubleshooting Guide

### Problem: "No inference engine available"

**Symptoms**: 
```
✗ No inference engine available
Make sure an engine is running: ollama serve
```

**Solutions**:
```bash
# 1. Check if Ollama is running
curl http://localhost:11434/api/tags

# 2. Start Ollama
ollama serve &

# 3. Download a model
ollama pull qwen2.5:7b

# 4. Test
tron ask "Hello"
```

### Problem: "No models available"

**Symptoms**:
```
✗ No models available
Run: tron model pull <model-name>
```

**Solutions**:
```bash
# 1. List available models
tron model list

# 2. If empty, pull a model
tron model pull qwen2.5:7b

# 3. Wait for download
# (7B model is ~4GB, takes 5-10 minutes)

# 4. Verify
tron model list
```

### Problem: Voice not working on macOS

**Symptoms**:
```
Voice recognition failed
Error: Permission denied
```

**Solutions**:
```bash
# 1. Enable accessibility permissions
System Preferences > Security & Privacy > Accessibility
→ Add Terminal to the list

# 2. Enable Siri/Dictation
System Preferences > Keyboard > Dictation
→ Enable Enhanced Dictation

# 3. Test voice
tron --voice

# 4. If still fails, try explicit mode
tron chat  # Then in chat, say "/voice"
```

### Problem: Voice not working on Windows

**Symptoms**:
```
Voice recognition unavailable
Error: WinRT API not accessible
```

**Solutions**:
```bash
# 1. Check Speech Recognition is enabled
Settings > Time & Language > Speech
→ Make sure "Speech Recognition" is on

# 2. Check audio device
Settings > Sound > Input
→ Microphone should be the default device

# 3. Test microphone
Settings > Sound > Volume > Input
→ Speak to test microphone level

# 4. Restart service
Restart-Service SpeechRecognitionService  # PowerShell

# 5. Try tron again
tron --voice
```

### Problem: Voice not working on Linux

**Symptoms**:
```
Voice recognition failed
No speech_recognition module
```

**Solutions**:
```bash
# 1. Install dependencies
sudo apt install espeak portaudio19-dev
pip install SpeechRecognition pydub

# 2. Test microphone
arecord --list-devices
# or
pactl list sources

# 3. Test voice
tron --voice

# 4. Check permissions
groups $USER
# If 'audio' group not listed:
sudo usermod -a -G audio $USER
# Then log out and back in
```

### Problem: "Configuration not found"

**Symptoms**:
```
✗ Configuration not found
Location: ~/.OpenTron/config.toml
Fix: Run 'tron init' to generate configuration
```

**Solutions**:
```bash
# 1. Run initialization
tron init

# 2. Follow the wizard
# - Select engine
# - Choose model
# - Optionally download model

# 3. Verify
tron doctor
tron config show
```

### Problem: API server not responding

**Symptoms**:
```
✗ API server not responding
Make sure the backend is running
```

**Solutions**:
```bash
# 1. Start the backend server
tron serve &

# 2. Verify it's running
curl http://localhost:8000/v1/health

# 3. Check port is available
netstat -tulpn | grep 8000

# 4. If port in use
lsof -i :8000  # Find process
kill -9 PID     # Kill it
tron serve &    # Restart
```

### Problem: "Agent not found"

**Symptoms**:
```
Agent not found: agent-123
```

**Solutions**:
```bash
# 1. List all agents
tron agent list

# 2. Copy exact ID from list
tron agent info <EXACT_ID>

# 3. Create new agent if needed
tron agent create --name "New Agent"
```

### Problem: Memory/Knowledge store issues

**Symptoms**:
```
Failed to search memories
Memory backend not responding
```

**Solutions**:
```bash
# 1. Clear corrupted memory
tron memory clear

# 2. Backup first (if important)
tron memory export

# 3. Check memory files
ls -la ~/.OpenTron/*.db

# 4. Restart memory service
tron serve --restart-memory

# 5. Re-import if you have backup
tron memory import memory_export_1234567890.json
```

### Problem: Channel connection fails

**Symptoms**:
```
Failed to connect to Slack
Error: Invalid token
```

**Solutions**:
```bash
# 1. Verify credentials
tron vault list | grep slack

# 2. Regenerate token from Slack app settings
# https://api.slack.com/apps → Create New App

# 3. Update vault
tron vault delete slack bot_token
tron vault add slack bot_token "xoxb-NEW-TOKEN"

# 4. Test again
tron channels test channel-slack

# 5. Re-bind agent if needed
tron channels bind channel-slack agent-id
```

### Problem: High memory usage

**Symptoms**:
```
Java process using 1GB+ RAM
System slowdown
```

**Solutions**:
```bash
# 1. Check memory usage
ps aux | grep java
jps -l -m  # More details

# 2. Limit JVM heap
export _JAVA_OPTIONS="-Xmx512m"
tron chat

# 3. Or modify startup script
#!/bin/bash
java -Xmx512m -jar tron-cli-jar-with-dependencies.jar "$@"

# 4. Clear unused agent data
tron agent delete old-agent-id
tron memory clear  # If needed

# 5. Restart service
killall java
tron serve &
```

### Problem: Slow model inference

**Symptoms**:
```
tron ask "Hello"  # Takes 30+ seconds
```

**Solutions**:
```bash
# 1. Check if using CPU-only
tron doctor | grep GPU

# 2. If no GPU, consider:
# - Use smaller model: tron model pull mistral:7b
# - Reduce context: tron config set intelligence.max_tokens 1024
# - Use faster engine: tron config set engine.default vllm

# 3. Monitor system resources
top -p $(pgrep java)  # While running

# 4. Profile the inference
tron ask "Hello" --profile

# 5. Verify engine is running efficiently
curl http://localhost:11434/api/tags | jq .

# 6. Increase GPU memory if available
ollama set OLLAMA_KEEP_ALIVE 5m
```

### Problem: Build fails with Maven

**Symptoms**:
```
[ERROR] BUILD FAILURE
Compilation failed
```

**Solutions**:
```bash
# 1. Clear cache and rebuild
mvn clean package -DskipTests

# 2. Check Java version
java -version  # Should be 17+

# 3. Install Java 17 if needed
# macOS: brew install openjdk@17
# Ubuntu: sudo apt install openjdk-17-jdk
# Windows: Download from adoptopenjdk.net

# 4. Set JAVA_HOME
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# 5. Rebuild
mvn clean package -DskipTests
```

---

## 🎯 Performance Tips

```bash
# Use faster inference settings
tron config set intelligence.max_tokens 1024      # Reduce for speed
tron config set intelligence.temperature 0.5      # More deterministic
tron config set agent.max_turns 3                # Shorter conversations

# Use smaller models
tron model pull mistral:7b                       # Faster than 70B
tron ask "Hello" -m mistral:7b

# Profile and optimize
tron ask "Query" --profile | grep throughput

# Enable GPU
# Most engines auto-detect GPU
# Check: tron doctor | grep GPU

# Pre-load models
ollama pull mistral:7b  # Keep warm in memory
ollama set OLLAMA_KEEP_ALIVE 10m
```

---

## 📞 Support & Resources

- **Documentation**: `/docs` endpoint on running server
- **Issues**: Check GitHub repository
- **Forums**: OpenTron community discussions
- **Chat**: Use `tron ask` for quick help on CLI usage

---

**Last Updated**: 2024
**Status**: Production Ready ✅
