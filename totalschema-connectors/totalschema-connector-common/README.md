# TotalSchema Connector Common

## Purpose

This module provides **shared infrastructure** for TotalSchema connector implementations. It contains base classes and utilities that are used by multiple connector types but are not needed in `totalschema-core`.

## Contents

### AbstractTerminalSession
Base class for terminal session implementations providing:
- Stream reading utilities with async executors
- Standard output/error handling
- Common terminal session lifecycle management

**Used by:**
- SSH connectors (`MinaSshdConnection`)
- Shell connectors (`DefaultShellScriptSession`)

### ExternalProcessTerminalSession
Concrete implementation for local process execution:
- Manages Java `Process` lifecycle
- Handles stdout/stderr streaming
- Exit code validation
- Command execution with configurable shell

**Used by:**
- Shell connectors for local script execution

## Architecture

```
┌─────────────────────┐
│  totalschema-core   │ ← Defines TerminalSession interface (SPI)
│  - TerminalSession  │
│  - Connector API    │
└─────────────────────┘
          ↑
          │ depends on
          │
┌─────────────────────────────────────┐
│  totalschema-connector-common       │ ← Implements session base classes
│  - AbstractTerminalSession          │
│  - ExternalProcessTerminalSession   │
└─────────────────────────────────────┘
          ↑                    ↑
          │                    │
          │ depends on         │ depends on
          │                    │
┌─────────────────┐   ┌────────────────────┐
│  connector-ssh  │   │  connector-shell   │ ← Concrete connectors
└─────────────────┘   └────────────────────┘
```

## Dependency Chain

1. **totalschema-core** provides:
   - `TerminalSession<C>` interface (SPI contract)
   - `AbstractTerminalConnector<C>` base class
   - Connector API and factories

2. **totalschema-connector-common** provides:
   - `AbstractTerminalSession<C>` implementation
   - `ExternalProcessTerminalSession` for local processes

3. **Connector modules** provide:
   - Concrete connector implementations
   - Connector-specific session implementations
   - ServiceLoader factory registrations

## Why Not in Core?

These classes are:
1. **Only used by connectors**, not by the core engine
2. **Implementation details**, not part of the public API
3. **Optional dependencies**, not required for database-only deployments

By separating them into `connector-common`:
- Maven plugin users can exclude all connectors without pulling in unused infrastructure
- Core remains minimal with only essential APIs
- Connector modules share infrastructure without duplication

## Maven Usage

Most users don't need to explicitly depend on this module. It's pulled in transitively:

```xml
<!-- If you use SSH connectors -->
<dependency>
    <groupId>io.github.totalschema</groupId>
    <artifactId>totalschema-connector-ssh</artifactId>
    <version>1.3.0-SNAPSHOT</version>
    <!-- Transitively includes totalschema-connector-common -->
</dependency>

<!-- If you use shell connectors -->
<dependency>
    <groupId>io.github.totalschema</groupId>
    <artifactId>totalschema-connector-shell</artifactId>
    <version>1.3.0-SNAPSHOT</version>
    <!-- Transitively includes totalschema-connector-common -->
</dependency>
```

## Class Responsibilities

### AbstractTerminalSession
- **Purpose:** Base implementation for terminal sessions
- **Provides:** Stream reading with ExecutorService, error handling
- **Abstract:** No concrete execute() implementation
- **Used by:** Both SSH and shell session implementations

### ExternalProcessTerminalSession  
- **Purpose:** Local process execution via Java ProcessBuilder
- **Provides:** Process lifecycle, stdout/stderr capture, exit code validation
- **Extends:** AbstractTerminalSession
- **Used by:** DefaultShellScriptSession for local shell scripts

## Thread Safety

- `AbstractTerminalSession` uses a shared cached thread pool for stream reading
- Stream readers are submitted as independent tasks
- Each session instance manages its own connection/process
- Synchronization is handled by concrete implementations if needed

## Design Principles

1. **Minimal surface area** - Only essential shared code
2. **No connector-specific logic** - Generic terminal abstractions only
3. **Depends only on core** - No external connector dependencies
4. **Stateless utilities** - Session state managed by concrete implementations

## Future Additions

Potential future additions to this module:
- `ScriptTemplateEngine` - Template processing for scripts
- `CommandValidator` - Pre-execution validation
- `SessionPool` - Connection pooling abstractions
- `RetryPolicy` - Retry logic for transient failures

Keep this module focused on **shared terminal/session infrastructure only**.

