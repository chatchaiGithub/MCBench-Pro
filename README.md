# MCBench Pro

Professional-grade controlled server benchmark plugin for Paper/Pufferfish Minecraft servers.

## Overview

MCBench Pro performs comprehensive server benchmarking by intentionally generating CPU-heavy loads and measuring recovery time based on real server TPS/MSPT values. It provides detailed performance analysis, chunk/entity insights, and tuning recommendations.

## Requirements

- **Java**: 21 or newer
- **Minecraft**: 1.21 or newer  
- **Server**: Paper, PurpurMC, or Pufferfish (Paper-based forks)
- **Permission**: Console access (commands are console-only for security)

## Features

- **Real TPS/MSPT Measurement**: Integrates with Paper APIs for accurate performance metrics
- **CPU-Intensive Workloads**: Deterministic math operations, prime checking, matrix multiplication
- **Multiple Profiles**: minimum, normal, extreme - fully configurable
- **Safe Mode**: Kicks players and clears inventories before benchmark
- **Emergency Abort**: Automatic stops if MSPT exceeds thresholds
- **Recovery Monitoring**: Measures time for TPS/MSPT to return to normal
- **Comprehensive Analysis**: Chunk counts, entity analysis, performance hotspots
- **Tuning Recommendations**: Actionable suggestions based on results
- **CineBench-style Scoring**: Benchmark Point calculation for easy comparison
- **Export Reports**: TXT, JSON, YAML formats with timestamps

## Installation

1. Ensure your server meets the requirements above
2. Download `mcbenchpro-1.0.0.jar`
3. Place in your server's `plugins/` directory
4. Restart the server
5. Configure settings in `plugins/MCBench Pro/config.yml` if needed

## Usage

### Basic Commands

All commands require console execution for security:

```bash
# Start benchmark with confirmation flow
/mcbench start <profile> [safe]

# Available profiles: minimum, normal, extreme
/mcbench start normal          # Normal mode
/mcbench start extreme safe    # Safe mode (kicks players)

# Confirm pending benchmark (must be done within 60 seconds)
/mcbench confirm

# Cancel pending benchmark
/mcbench cancel

# Stop running benchmark
/mcbench stop

# Show help
/mcbench help
```

### Benchmark Profiles

- **minimum**: 1-minute workload, 50% intensity (small VPS, ≤2 cores)
- **normal**: 2-minute workload, 100% intensity (mid-range servers)  
- **extreme**: 3-minute workload, 200% intensity (high-end servers)

All profiles are configurable in `config.yml`.

### Safe Mode

When using the `safe` argument:
- All online players are kicked with a configurable message
- Player inventories can be cleared (configurable)
- Dropped items are removed from worlds (configurable)
- Reduces risk of player data corruption during stress testing

## Example Console Output

```
[MCBench Pro] WARNING: Running MCBench Pro may cause temporary server instability
[MCBench Pro] The plugin authors accept NO RESPONSIBILITY for damage to game files
[MCBench Pro] Profile: normal | Mode: Safe Mode
[MCBench Pro] Run '/mcbench confirm' within 60 seconds to proceed

=== Server Status ===
OS: Windows 11 10.0 (amd64)
CPU: amd64 (amd64)
RAM: 2048/8192
Server loader: Paper
Version: 1.21.1-R0.1-SNAPSHOT
ViaVersion: False
Confirmation expires in: 45 seconds

[MCBench Pro] Benchmark confirmed. Starting in 3 seconds...
[MCBench Pro] Safe mode: Kicked 5 players
[MCBench Pro] Workload phase started. Profile: normal
[MCBench Pro] In load: TPS 15.2 | MSPT 65.4ms | RAM 78% | CPU 85%
[MCBench Pro] Workload phase completed. Beginning recovery measurement...
[MCBench Pro] Recovery completed. Generating final report...

=== MCBench Pro Results ===
Profile: normal | Mode: Safe Mode | Duration: 120 seconds
--- Baseline Metrics ---
TPS: 20.0 | MSPT: 45.2ms | CPU: 25% | RAM: 60%
--- After Load Metrics ---  
TPS: 18.5 | MSPT: 52.1ms | CPU: 30% | RAM: 65%
--- Recovery & Scoring ---
Recovery time: 0:35 | Score: 3428.57 | Benchmark Point: 0.000700
--- System Information ---
Java: 21.0.1 | Minecraft: 1.21.1 | JVM RAM: 2048MB/8192MB
Hardware: amd64 / 16.0 GB
--- Tuning Recommendations ---
• Server performance is optimal, no recommendations needed
```

## Configuration

### config.yml

Key settings you can modify:

```yaml
settings:
  globalTimeoutSeconds: 300     # Max benchmark duration
  baseScore: 1000.0            # Base score for calculation
  
profiles:
  normal:
    durationSeconds: 120       # Workload duration
    intensityMultiplier: 1.0   # CPU load multiplier
    loopCountPerTick: 1000000  # Operations per tick
    
safeMode:
  kickMessage: "MCBench Pro — server benchmarking in progress"
  clearInventories: true
  clearDroppedItems: true
```

### Performance Tuning

MCBench Pro generates specific recommendations:
- View distance optimization
- Entity cleanup suggestions  
- Memory allocation advice
- Plugin performance hints
- JVM flags recommendations

## Build Instructions

```bash
# Clone the repository
git clone <repository-url>
cd mcbench-pro

# Build with Maven
mvn clean package

# The JAR will be in target/mcbenchpro-1.0.0.jar
```

## Benchmark Methodology

1. **Baseline Collection**: Records TPS, MSPT, CPU, RAM before workload
2. **Workload Execution**: CPU-intensive tasks via Bukkit scheduler
3. **Recovery Monitoring**: Waits for TPS ≥ 20 and MSPT ≤ 25ms
4. **Score Calculation**: `score = baseScore * (workloadSeconds / recoverySeconds)`
5. **Benchmark Point**: `point = recoverySeconds / 50000` (CineBench style)

## Safety Features

- **Console-only commands** prevent accidental execution
- **60-second confirmation** with server status display
- **Emergency abort** if MSPT > 1000ms for 10+ seconds
- **Global timeout** prevents infinite runs (5 minutes default)
- **Safe mode** protects player data during testing
- **Exception handling** prevents server crashes

## Compatibility

Tested with:
- Paper 1.21.1+
- PurpurMC 1.21.1+  
- Pufferfish 1.21.1+
- Java 21+

**Not compatible with**: Vanilla, CraftBukkit, Spigot (requires Paper APIs)

## License

This project is provided as-is for educational and testing purposes. The authors accept no responsibility for any damage to server files, worlds, or player data. Use at your own risk.

## Support

For issues, feature requests, or questions:
1. Check server console for detailed error messages
2. Verify Java 21+ and Paper 1.21+ requirements
3. Review configuration settings
4. Test with minimal plugins first

## Version History

- **1.0.0**: Initial release with full benchmark suite, safe mode, and comprehensive reporting
