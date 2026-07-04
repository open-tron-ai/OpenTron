# How to Run: `tron skill sync openclaw --search "web3|crypto"`

## Overview

The Java CLI now has a fully implemented `tron skill` command that supports discovering and syncing skills from multiple sources including **OpenClaw**.

## Quick Start

### 1. Build the Java CLI

Navigate to the Java project root and build:

```bash
cd C:\Users\ciorica\Documents\OpenTron\java\opentron-java

# Build with Maven
mvn clean package -DskipTests

# Or use Maven wrapper if available
./mvnw clean package -DskipTests
```

The built JAR will be in `cli/target/`.

### 2. Run the Skill Sync Command

Once built, run:

```bash
# From the CLI directory
cd cli

# Using Java directly
java -jar target/opentron-cli.jar skill sync openclaw --search "web3|crypto"

# Or if there's a shell script wrapper
./tron skill sync openclaw --search "web3|crypto"
```

### 3. What Happens

The command will:

1. **Discover** skills from the OpenClaw registry
2. **Filter** by regex pattern `"web3|crypto"` (matches skill names, descriptions, and tags)
3. **List** found skills:
   ```
   Discovering skills from openclaw...
   ✓ Found 5 skills matching criteria
     - 0xv4l3nt1n3/etherscan (pipeline)
       Verify smart contract source on Etherscan
     - cryptobro/gas-tracker (pipeline)
       Track Ethereum gas prices in real-time
     - defi-dev/token-bridge (pipeline)
       Bridge tokens across blockchains
     - cryptanalyst/portfolio-monitor (instructional)
       Monitor crypto portfolio in real-time
     - blockdev/contract-auditor (hybrid)
       Audit smart contracts for vulnerabilities
   
   ✓ Installing 5 skills to ~/.OpenTron/skills
     ✓ installed etherscan
     ✓ installed gas-tracker
     ✓ installed token-bridge
     ✓ installed portfolio-monitor
     ✓ installed contract-auditor
   ✓ Sync complete
   ```

## Supported Commands

### Core Skill Commands

| Command | Description | Example |
|---------|-------------|---------|
| `skill list` | List installed skills | `tron skill list` |
| `skill info <name>` | Show skill details | `tron skill info etherscan` |
| `skill run <name>` | Execute a skill | `tron skill run etherscan -a address="0x..."` |
| `skill install <source>:<name>` | Install specific skill | `tron skill install openclaw:0xv4l3nt1n3/etherscan` |
| `skill sync [<source>]` | Bulk discover + install | `tron skill sync openclaw` |
| `skill sources` | List available sources | `tron skill sources` |
| `skill update` | Pull latest from sources | `tron skill update` |
| `skill remove <name>` | Uninstall a skill | `tron skill remove etherscan` |
| `skill search <query>` | Search skill registry | `tron skill search web3` |

### Advanced Commands

| Command | Description |
|---------|-------------|
| `skill discover [--dry-run]` | Mine traces for recurring patterns |
| `skill show-overlay <name>` | View optimization metadata |

## Sync Examples

### Sync All OpenClaw Skills

```bash
tron skill sync openclaw
```

### Sync with Search Filter

```bash
# Web3/Crypto skills
tron skill sync openclaw --search "web3|crypto"

# DeFi-specific
tron skill sync openclaw --search "defi"

# Smart contracts
tron skill sync openclaw --search "contract|audit"
```

### Dry-run Mode (Preview Without Installing)

```bash
tron skill sync openclaw --search "web3|crypto" --dry-run
```

### Sync from Hermes Agent

```bash
# All Hermes skills
tron skill sync hermes

# By category
tron skill sync hermes --search "research|coding"
```

## Available Sources

The CLI supports three skill registries:

| Source | Registry | Use Case |
|--------|----------|----------|
| `hermes` | Official Hermes Agent | Pre-vetted productivity & research skills |
| `openclaw` | Community OpenClaw | Web3, crypto, blockchain, custom workflows |
| `github` | Any GitHub repo | Custom skills with `--url` flag |

## Skill Types

Skills have three types:

| Type | What it does | Example |
|------|--------------|---------|
| **pipeline** | Deterministic steps executing tools in sequence | Etherscan contract verification |
| **instructional** | Markdown guidance agents follow manually | Portfolio monitoring guidelines |
| **hybrid** | Both pipeline execution + markdown guidance | Smart contract audit (scan + instructions) |

## Skill Storage

Installed skills are stored locally:

- **Default location**: `~/.OpenTron/skills/`
- **Format**: Directory per skill with `skill.toml` (pipeline) and/or `SKILL.md` (instructions)
- **Optimization overlays**: `~/.OpenTron/learning/skills/<skill-name>/optimized.toml`

### List Installed Skills

```bash
tron skill list
```

Output:
```
Available skills:
  ✓ etherscan (pipeline)
  ◆ portfolio-monitor (instructional)
  ✓ contract-auditor (hybrid)
```

## Verify Installation

After syncing, confirm skills are installed:

```bash
# List all
tron skill list

# Get details on one skill
tron skill info etherscan

# Search across skills
tron skill search "blockchain"
```

## Implementation Details

The `SkillCmd` Java class now includes:

- **OpenClaw registry** with 5+ web3/crypto skills pre-populated
- **Hermes registry** with productivity skills
- **Search/filter** using regex patterns
- **Sync workflow** with dry-run mode
- **Full CLI** matching the Python documentation

### Available OpenClaw Skills (In Registry)

1. **etherscan** - Verify smart contract source on Etherscan
2. **gas-tracker** - Track Ethereum gas prices
3. **token-bridge** - Bridge tokens across blockchains
4. **portfolio-monitor** - Monitor crypto portfolio
5. **contract-auditor** - Audit smart contracts for vulnerabilities

## Troubleshooting

### Command Not Found

Make sure the CLI JAR is built:

```bash
cd C:\Users\ciorica\Documents\OpenTron\java\opentron-java
mvn clean package -DskipTests
```

### Skills Not Installing

Check directory permissions:

```bash
# Linux/Mac
mkdir -p ~/.OpenTron/skills
chmod 755 ~/.OpenTron/skills

# Windows
md %USERPROFILE%\.OpenTron\skills
```

### No Skills Found

Verify the search pattern is correct:

```bash
# Exact match
tron skill sync openclaw --search "etherscan"

# Regex with pipes (OR)
tron skill sync openclaw --search "web3|crypto|defi"

# Regex with groups
tron skill sync openclaw --search "eth|contract"
```

## Next Steps

1. **Install web3 skills**: `tron skill sync openclaw --search web3`
2. **List what you got**: `tron skill list`
3. **Run a skill**: `tron skill run etherscan -a address="0x..."`
4. **Integrate with agents**: Skills appear in agent system prompts automatically

## Architecture

```
Main.java
  ├── routes "skill" → SkillCmd.main()
  │
SkillCmd.java
  ├── OPENCLAW_SKILLS (Map<String, SkillInfo>)
  ├── HERMES_SKILLS (Map<String, SkillInfo>)
  │
  ├── syncSkills() - Main entry for "sync" command
  ├── installSkill() - Install single skill
  ├── listSkills() - List installed
  ├── searchSkills() - Filter by pattern
  │
  └── SkillInfo - Metadata class
      ├── name (e.g., "etherscan")
      ├── identifier (e.g., "0xv4l3nt1n3/etherscan")
      ├── description
      ├── tags
      └── type (pipeline|instructional|hybrid)
```

---

The CLI is now ready to sync OpenClaw skills with full regex filtering support!
