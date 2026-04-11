# Connector Architecture - Developer Guide

## Overview

Connectors are the execution layer of TotalSchema. They are responsible for taking a **change
script file** and deploying it against a specific target system — a database, a remote server, or
the local machine. Every change script filename contains a **connector name** that must match a
key in the `connectors:` section of `totalschema.yml`; this is how TotalSchema routes each file to
the right execution backend.

Because connectors are registered via Java `ServiceLoader`, adding a new connector type requires
no changes to `totalschema-core`. You ship a JAR, drop it on the classpath, and it is
automatically discovered.

---

## Table of Contents

1. [Core Concepts](#1-core-concepts)
2. [Architecture Diagram](#2-architecture-diagram)
3. [Core API Classes (totalschema-core)](#3-core-api-classes-totalschema-core)
4. [Shared Infrastructure (totalschema-connector-common)](#4-shared-infrastructure-totalschema-connector-common)
5. [Built-in Connector Types](#5-built-in-connector-types)
   - [JDBC (`jdbc`)](#51-jdbc-connector)
   - [SSH Script (`ssh-script`)](#52-ssh-script-connector)
   - [SSH Command List (`ssh-commands`)](#53-ssh-command-list-connector)
   - [Shell (`shell`)](#54-shell-connector)
6. [Module Structure & Dependencies](#6-module-structure--dependencies)
7. [Configuration Reference](#7-configuration-reference)
8. [Execution Flow](#8-execution-flow)
9. [Creating a Custom Connector](#9-creating-a-custom-connector)
10. [SPI Extension Points](#10-spi-extension-points)

---

## 1. Core Concepts

| Concept | Description |
|---|---|
| **Connector** | An abstraction over a target system (database, server, shell). Executes a change file. |
| **Connector type** | The string identifier used in `type:` in `totalschema.yml` (e.g. `jdbc`, `ssh-script`). Matches a factory's qualifier. |
| **Connector name** | The user-defined key under `connectors:` (e.g. `mydb`, `myserver`). Also appears in every change script filename. |
| **ConnectorManager** | Orchestrates discovery, configuration merging, and caching of connector instances. |
| **AbstractConnectorComponentFactory** | IoC base factory — handles config resolution; subclasses only implement `createConnector()`. |

### Connector Name in Filenames

Change script filenames embed the connector name directly:

```
<order>.<description>[.<env>].<changetype>.<connectorName>.<ext>

0001.create_users.DEV.apply.mydb.sql        → routed to connector "mydb"
0002.deploy_config.PROD.apply.myserver.sh   → routed to connector "myserver"
0003.backup.apply.localshell.sh             → routed to connector "localshell"
```

The `<connectorName>` segment must exactly match a key in `connectors:` in `totalschema.yml`.

---

## 2. Architecture Diagram

```
totalschema.yml
  connectors:
    mydb:      type: jdbc       ──────────────────────────────┐
    myserver:  type: ssh-script ───────────────────────┐      │
    local:     type: shell      ──────────┐            │      │
                                          │            │      │
Change file:  0001.init.apply.mydb.sql ───┼────────────┼──────┘
                                          │            │
┌─────────────────────────────────────────┼────────────┼───────────────────┐
│  DefaultConnectorManager                │            │                   │
│  (reads config, resolves env overrides, │            │                   │
│   calls context.get(Connector.class,    │            │                   │
│   <type>, <name>, <config>))            │            │                   │
└─────────────────────────────────────────┼────────────┼───────────────────┘
                                          │            │
       ┌──────────────────────────────────┘            │
       │             ┌─────────────────────────────────┘
       ▼             ▼                    ▼
ShellScriptConnectorFactory   SshScriptConnectorFactory   JdbcConnectorFactory
  (qualifier="shell")           (qualifier="ssh-script")    (qualifier="jdbc")
       │                               │                         │
       ▼                               ▼                         ▼
ShellScriptConnector         SshScriptConnector           JdbcConnector
  (local process)              (SCP + SSH exec)            (delegates to ScriptExecutor)
       │                               │                         │
       ▼                               ▼                         ▼
DefaultShellScriptRunner       MinaSshdConnection         SqlScriptExecutor / GroovyExecutor
  (ExternalProcess)              (Apache MINA SSHD)          (via file extension lookup)
```

---

## 3. Core API Classes (`totalschema-core`)

### `Connector` — Base class

**Package:** `io.github.totalschema.connector`

The root abstract class for all connectors. Every connector must implement:

```java
public abstract class Connector {
    // Human-readable description of this connector instance
    public abstract String toString();

    // Execute a single change file against the target system
    public abstract void execute(ChangeFile changeFile, CommandContext context)
            throws InterruptedException;
}
```

---

### `AbstractTerminalConnector<C>` — Terminal session base

**Package:** `io.github.totalschema.connector`

Used by all shell/SSH connectors. Wraps a `TerminalSession<C>`, delegates
`execute(ChangeFile, …)` → `execute(Path, …)` and manages session lifecycle (`Closeable`).

```java
public abstract class AbstractTerminalConnector<C> extends Connector implements Closeable {
    protected final TerminalSession<C> session;

    @Override
    public void execute(ChangeFile changeFile, CommandContext context) throws InterruptedException {
        execute(changeFile.getFile(), context);   // unwraps to Path
    }

    protected abstract void execute(Path scriptFile, CommandContext context)
            throws InterruptedException;

    @Override
    public void close() throws IOException { session.close(); }
}
```

---

### `TerminalSession<C>` — Session interface

**Package:** `io.github.totalschema.engine.internal.shell.direct`

Single-method contract for executing one command of type `C` against a session:

```java
public interface TerminalSession<C> extends AutoCloseable {
    void execute(C command) throws InterruptedException;
    void close();
}
```

- SSH connectors use `TerminalSession<String>` (command = shell string).
- Shell connector uses `TerminalSession<List<String>>` (command = argv list).

---

### `ConnectorManager` — Connector lifecycle manager

**Package:** `io.github.totalschema.connector`

```java
public interface ConnectorManager {
    static ConnectorManager getInstance() { ... }  // via ServiceLoader

    Connector getConnectorByName(String name, Context context);
}
```

The default implementation `DefaultConnectorManager`:
1. Reads `connectors.<name>` from the global configuration.
2. Merges `environments.<ENV>.connectors.<name>` overrides on top.
3. Extracts the `type` field and calls `context.get(Connector.class, type, name, config)`.
4. This triggers the matching `AbstractConnectorComponentFactory` via the IoC container.

---

### `AbstractConnectorComponentFactory` — Factory base

**Package:** `io.github.totalschema.connector`

All connector factories extend this class. It handles:

- **Argument validation** — requires exactly two arguments: `name` (String) and `configuration` (Configuration).
- **Type guard** — verifies that the configured `type` matches the factory's qualifier.
- **Logging** — debug/info on connector creation.
- **Lazy instantiation** — `isLazy()` returns `true`; connectors are always created on-demand.

Subclasses only need to implement `createConnector(String name, Configuration config)`:

```java
public final class JdbcConnectorFactory extends AbstractConnectorComponentFactory {
    @Override
    public Optional<String> getQualifier() { return Optional.of("jdbc"); }

    @Override
    protected Connector createConnector(String name, Configuration config) {
        return new JdbcConnector(name, config);
    }
}
```

---

## 4. Shared Infrastructure (`totalschema-connector-common`)

This module provides reusable session infrastructure shared by the SSH and shell connectors. It sits between `totalschema-core` (API only) and the concrete connector modules.

### `AbstractTerminalSession<C>`

**Package:** `io.github.totalschema.engine.internal.shell`

Base for all session implementations. Provides a shared `ExecutorService` and a
`submitReaderTask()` helper that reads stdout/stderr from an `InputStream` asynchronously,
line-by-line, feeding each line into a `Consumer<String>`.

```java
public abstract class AbstractTerminalSession<C> implements TerminalSession<C> {
    protected static final ExecutorService executorService = Executors.newCachedThreadPool();

    protected Future<?> submitReaderTask(InputStream inputStream, Consumer<String> consumer) { ... }
}
```

### `ExternalProcessTerminalSession`

**Package:** `io.github.totalschema.engine.internal.shell`

Extends `AbstractTerminalSession<List<String>>`. Launches OS processes via `ProcessBuilder`,
streams stdout and stderr to the console, and checks the exit code. Used by the shell connector.

```java
public abstract class ExternalProcessTerminalSession extends AbstractTerminalSession<List<String>> {
    @Override
    public void execute(List<String> command) {
        // starts process, streams output, checks exit code != 0
    }
    protected Process startProcess(List<String> command) throws IOException { ... }
}
```

---

## 5. Built-in Connector Types

### 5.1 JDBC Connector

| Property | Value |
|---|---|
| **Type string** | `jdbc` |
| **Module** | `totalschema-core` |
| **Class** | `io.github.totalschema.connector.jdbc.JdbcConnector` |
| **Factory** | `io.github.totalschema.connector.jdbc.JdbcConnectorFactory` |

**Purpose:** Execute database scripts against any JDBC-compliant database (PostgreSQL, MySQL,
Oracle, H2, BigQuery, SQL Server, …).

**Execution logic:** The connector reads the change file content and delegates to a
`ScriptExecutor` looked up by the file's extension:

```java
String extension = changeFile.getId().getExtension();   // e.g. "sql", "groovy"
ScriptExecutor executor = context.get(ScriptExecutor.class, extension, name, config);
executor.execute(fileContent, context);
```

This means JDBC connectors are agnostic to script language — the `ScriptExecutor` SPI handles
the actual execution. See `docs/developer/SCRIPT_EXECUTOR_SUBSYSTEM.md`.

**Minimal configuration:**

```yaml
connectors:
  mydb:
    type: jdbc
    driver:
      class: org.postgresql.Driver
    jdbc:
      url: jdbc:postgresql://localhost:5432/${dbName}
    username: ${dbUser}
    password: ${dbPassword}
    db:
      type: generic
```

**Full configuration options:**

```yaml
connectors:
  mydb:
    type: jdbc
    driver:
      class: org.postgresql.Driver        # Required: JDBC driver class
    jdbc:
      url: jdbc:postgresql://host/db      # Required: JDBC connection URL
    username: user                        # Required: DB username
    password: ${dbPassword}              # Required: DB password
    db:
      type: generic                       # DB dialect hint (generic, h2, bigquery, …)
    transaction:
      isolation: READ_COMMITTED           # Optional: transaction isolation level
    connection:
      properties:
        file: /path/to/props.properties   # Optional: extra connection properties
    logSql: false                         # Optional: log SQL before execution
```

---

### 5.2 SSH Script Connector

| Property | Value |
|---|---|
| **Type string** | `ssh-script` |
| **Module** | `totalschema-connector-ssh` |
| **Class** | `io.github.totalschema.connector.ssh.SshScriptConnector` |
| **Factory** | `io.github.totalschema.connector.ssh.SshScriptConnectorComponentFactory` |

**Purpose:** Execute a complete shell script on a remote host via SSH. The entire file runs as one
shell session, so variables, functions, `cd`, and other stateful operations persist across lines.

**Execution sequence:**
1. Generate a unique temporary filename (`totalschema-script-<UUID>.sh`).
2. Upload the local script to `remote.temp.dir` on the remote host via **SCP** (Apache MINA SSHD).
3. `chmod +x <remote_path>`.
4. Execute: `<shell> <remote_path>`.
5. `rm -f <remote_path>` (in `finally`, so cleanup happens even on failure).

**When to use:** Any script that uses variables, functions, loops, or changes directory between
commands.

**Configuration:**

```yaml
connectors:
  myserver:
    type: ssh-script
    host: server.example.com               # Required
    port: 22                               # Optional, default: 22
    user: deployuser                       # Required
    password: ${serverPassword}           # Either password or privateKey required
    privateKey:
      path: /home/user/.ssh/id_rsa        # Alternative: key-based auth
      passphrase: ${keyPassphrase}         # Optional: only if key is encrypted
    remote:
      temp:
        dir: /tmp                          # Optional, default: /tmp
    shell: /bin/bash                       # Optional, default: /bin/bash
    lock:
      timeout: 30                          # Optional connection/lock timeout (seconds)
      timeoutUnit: SECONDS                 # Optional, default: SECONDS
    command:
      timeoutMs: 300000                    # Optional per-command timeout (ms), default: 5 min
    ssh:
      properties:                          # Optional: raw Apache MINA SSHD client properties
        StrictHostKeyChecking: "no"
```

**Example script** (`0001.deploy_app.PROD.apply.myserver.sh`):

```bash
#!/bin/bash
APP_DIR="/opt/myapp"
LOG_FILE="/var/log/deploy.log"

deploy_app() {
  echo "Deploying to $APP_DIR..." | tee -a $LOG_FILE
  cd $APP_DIR           # directory change persists within the script
  ./install.sh
}

deploy_app
echo "Deployment complete at $(date)" >> $LOG_FILE
```

---

### 5.3 SSH Command List Connector

| Property | Value |
|---|---|
| **Type string** | `ssh-commands` |
| **Module** | `totalschema-connector-ssh` |
| **Class** | `io.github.totalschema.connector.ssh.SshCommandListConnector` |
| **Factory** | `io.github.totalschema.connector.ssh.SshCommandListConnectorComponentFactory` |

**Purpose:** Execute each non-blank line in a file as an independent SSH command. Lines do **not**
share shell context — each command is a fresh `exec` channel.

**Execution sequence:**
```java
for (String line : Files.readAllLines(commandListFile)) {
    if (!line.isBlank()) {
        session.execute(line);   // opens a new SSH exec channel per line
    }
}
```

**When to use:** Simple, independent administrative commands where stateful context is not needed.

> ⚠️ **Key difference from `ssh-script`:**  
> A `cd /some/dir` in line 3 will **not** affect line 4 — each line is an independent SSH
> exec call.

**Configuration:** Same fields as `ssh-script` (no `remote.temp.dir` or `shell` needed):

```yaml
connectors:
  myserver:
    type: ssh-commands
    host: server.example.com
    port: 22
    user: deployuser
    password: ${serverPassword}
```

**Example command list** (`0002.restart_services.PROD.apply.myserver.txt`):

```bash
systemctl restart nginx
systemctl restart myapp
echo "Services restarted"
# Note: each line runs independently; there is no shared environment between them
```

---

### SSH Internals — `MinaSshdConnection`

Both SSH connectors use `MinaSshdConnection` (via the `SshConnectionFactory` SPI) as their
underlying session.

**Key behaviours:**
- Lazy connect: establishes the TCP/SSH session on first `execute()` call, reconnects if the
  session is closed.
- Concurrent access is serialised with a `ReentrantLock` (same `LockTemplate` pattern used by the
  lock service).
- Exit code != 0 raises a `RuntimeException` (fails the deployment).
- Stdout/stderr are streamed asynchronously to `System.out`/`System.err` with an `[SSH]` prefix.
- Per-command timeout defaults to 5 minutes (`command.timeoutMs`).

**`SshConnectionFactory`** caches connections by `(name, config)` key using `ConcurrentHashMap`,
so multiple connectors sharing the same logical server reuse a single SSH session:

```java
public SshConnection getSshConnection(String name, Configuration configuration) {
    return connectionCache.computeIfAbsent(
        new NamedConfigKey(name, configuration),
        key -> new MinaSshdConnection(name, configuration));
}
```

---

### 5.4 Shell Connector

| Property | Value |
|---|---|
| **Type string** | `shell` |
| **Module** | `totalschema-connector-shell` |
| **Class** | `io.github.totalschema.connector.shell.ShellScriptConnector` |
| **Factory** | `io.github.totalschema.connector.shell.ShellScriptConnectorComponentFactory` |

**Purpose:** Execute a shell script on the **local machine** (the machine running TotalSchema).
The script path is passed as an argument to the system shell.

**Execution logic:**
```java
session.execute(Collections.singletonList(scriptFile.toAbsolutePath().toString()));
```

`DefaultShellScriptRunner` auto-detects the interpreter per execution based on the script's file
extension:

| Script extension | OS | Effective command |
|---|---|---|
| `.ps1` | any (`pwsh` on PATH) | `pwsh -ExecutionPolicy Bypass -File <script_path>` |
| `.ps1` | any (no `pwsh`) | `powershell.exe -ExecutionPolicy Bypass -File <script_path>` |
| other | Windows | `cmd.exe /c <script_path>` |
| other | Unix/macOS | `sh <script_path>` |

A custom start command can override auto-detection for all scripts in the connector
(comma-separated in YAML):

```yaml
connectors:
  localshell:
    type: shell
    start:
      command: /bin/bash
```

**Example** (`0003.backup.apply.localshell.sh`):

```bash
#!/bin/bash
pg_dump mydb > /backups/backup_$(date +%Y%m%d_%H%M%S).sql
echo "Backup complete"
```

---

## 6. Module Structure & Dependencies

```
totalschema-core
  ├── Connector (abstract base class)
  ├── AbstractTerminalConnector (terminal base)
  ├── TerminalSession (interface)
  ├── ConnectorManager / DefaultConnectorManager
  ├── AbstractConnectorComponentFactory
  ├── JdbcConnector + JdbcConnectorFactory          ← ALWAYS bundled
  └── (ServiceLoader: JdbcConnectorFactory)

totalschema-connector-common           ← shared by ssh + shell
  ├── AbstractTerminalSession
  └── ExternalProcessTerminalSession

totalschema-connector-ssh              ← OPTIONAL (included in CLI/release by default)
  ├── SshScriptConnector + Factory
  ├── SshCommandListConnector + Factory
  ├── SshConnection (SPI interface)
  ├── SshConnectionFactory (SPI interface, caching)
  ├── MinaSshdConnection (Apache MINA SSHD impl)
  ├── DefaultSshConnectionFactory
  └── (ServiceLoader: SshScriptConnectorFactory, SshCommandListConnectorFactory)

totalschema-connector-shell            ← OPTIONAL (included in CLI/release by default)
  ├── ShellScriptConnector + Factory
  ├── ShellScriptRunner (SPI interface)
  ├── ShellScriptRunnerFactory (SPI interface)
  ├── DefaultShellScriptRunnerFactory
  ├── DefaultShellScriptRunner (per-execution interpreter auto-detection)
  └── (ServiceLoader: ShellScriptConnectorFactory)
```

**Dependency rules:**
- `totalschema-connector-common` depends on `totalschema-core`.
- `totalschema-connector-ssh` and `totalschema-connector-shell` each depend on
  `totalschema-connector-common`.
- `totalschema-core` has **no** dependency on any connector module — connectors are pure plugins.

**Maven dependency for Maven plugin users** (select only what you need):

```xml
<!-- Core is always required -->
<dependency>
    <groupId>io.github.totalschema</groupId>
    <artifactId>totalschema-core</artifactId>
    <version>1.2.0-SNAPSHOT</version>
</dependency>

<!-- SSH connectors (optional) -->
<dependency>
    <groupId>io.github.totalschema</groupId>
    <artifactId>totalschema-connector-ssh</artifactId>
    <version>1.2.0-SNAPSHOT</version>
</dependency>

<!-- Shell connector (optional) -->
<dependency>
    <groupId>io.github.totalschema</groupId>
    <artifactId>totalschema-connector-shell</artifactId>
    <version>1.2.0-SNAPSHOT</version>
</dependency>
```

CLI and `totalschema-release` users get all connectors automatically — no action needed.

---

## 7. Configuration Reference

### Global vs. Environment-Specific Overrides

Connector configuration is merged in this order (later entries win):

1. `connectors.<name>` — global connector config
2. `environments.<ENV>.connectors.<name>` — environment-specific overrides

This allows you to share most of a connector's configuration while varying only the host or
credentials per environment:

```yaml
connectors:
  mydb:
    type: jdbc
    driver:
      class: org.postgresql.Driver
    username: app_user           # global default

environments:
  DEV:
    connectors:
      mydb:
        jdbc:
          url: jdbc:postgresql://dev-host/devdb
        password: devpass
  PROD:
    connectors:
      mydb:
        jdbc:
          url: jdbc:postgresql://prod-host/proddb
        password: ${secret:!SECRET;1.0;...}
```

### Variable Substitution

All connector configuration values support `${varName}` substitution. Variables are resolved
from the `variables` section (with environment overrides applied first):

```yaml
variables:
  dbHost: localhost

environments:
  PROD:
    variables:
      dbHost: prod-db.internal

connectors:
  mydb:
    type: jdbc
    jdbc:
      url: jdbc:postgresql://${dbHost}/mydb
```

---

## 8. Execution Flow

The following sequence describes how a single change file is dispatched from the engine to a
connector.

```
1. Engine selects a pending ChangeFile (e.g. 0001.init.DEV.apply.mydb.sql)
   │
2. Extracts connector name from ChangeFile.Id.getConnector() → "mydb"
   │
3. DefaultConnectorManager.getConnectorByName("mydb", context)
   │   a. Reads global `connectors.mydb` config
   │   b. Merges `environments.DEV.connectors.mydb` overrides
   │   c. Reads `type` → "jdbc"
   │
4. context.get(Connector.class, "jdbc", "mydb", mergedConfig)
   │   IoC container finds JdbcConnectorFactory (qualifier="jdbc")
   │
5. JdbcConnectorFactory.createComponent()
   │   → validates type field matches qualifier
   │   → calls createConnector("mydb", mergedConfig)
   │   → returns new JdbcConnector("mydb", mergedConfig)
   │
6. JdbcConnector.execute(changeFile, context)
   │   a. Reads file content from disk
   │   b. Extracts extension → "sql"
   │   c. context.get(ScriptExecutor.class, "sql", "mydb", config)
   │       → SqlScriptExecutorFactory creates SqlScriptExecutor
   │   d. SqlScriptExecutor.execute(fileContent, context)
   │       → splits on ";", executes each statement via JDBC
   │
7. Engine records the change as applied in the state table
```

---

## 9. Creating a Custom Connector

Follow these steps to add a new connector type (e.g. a hypothetical `ftp` connector).

### Step 1 — Create the Connector class

```java
package com.example.connector.ftp;

import io.github.totalschema.connector.Connector;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.model.ChangeFile;

public class FtpConnector extends Connector {

    public static final String CONNECTOR_TYPE = "ftp";

    private final String name;
    private final String host;
    private final String user;

    public FtpConnector(String name, Configuration config) {
        this.name = name;
        this.host = config.getString("host").orElseThrow();
        this.user = config.getString("user").orElseThrow();
    }

    @Override
    public void execute(ChangeFile changeFile, CommandContext context) throws InterruptedException {
        // Upload or execute changeFile.getFile() against the FTP target
    }

    @Override
    public String toString() {
        return "FTP Connector '" + name + "' → " + user + "@" + host;
    }
}
```

### Step 2 — Create the Factory

```java
package com.example.connector.ftp;

import io.github.totalschema.connector.AbstractConnectorComponentFactory;
import io.github.totalschema.connector.Connector;
import io.github.totalschema.config.Configuration;
import java.util.Optional;

public final class FtpConnectorFactory extends AbstractConnectorComponentFactory {

    @Override
    public Optional<String> getQualifier() {
        return Optional.of(FtpConnector.CONNECTOR_TYPE);   // "ftp"
    }

    @Override
    protected Connector createConnector(String name, Configuration config) {
        return new FtpConnector(name, config);
    }
}
```

### Step 3 — Register via ServiceLoader

Create the file:

```
src/main/resources/META-INF/services/io.github.totalschema.spi.factory.ComponentFactory
```

With content:

```
com.example.connector.ftp.FtpConnectorFactory
```

### Step 4 — Configure in `totalschema.yml`

```yaml
connectors:
  myftpserver:
    type: ftp
    host: ftp.example.com
    user: deployuser
    password: ${ftpPassword}
```

### Step 5 — Name your change scripts

```
0001.upload_data.apply.myftpserver.csv
```

### Step 6 — Add to classpath

Drop your JAR in `user_libs/` for CLI distributions, or add as a Maven dependency for the
Maven plugin.

---

## 10. SPI Extension Points

| SPI interface | Service file | Purpose |
|---|---|---|
| `ComponentFactory<Connector>` | `META-INF/services/io.github.totalschema.spi.factory.ComponentFactory` | Register new connector types (and other IoC components) |
| `SshConnectionFactory` | `META-INF/services/io.github.totalschema.engine.internal.shell.direct.ssh.spi.SshConnectionFactory` | Replace the SSH connection implementation (e.g. custom auth, keep-alive logic) |
| `ShellScriptRunnerFactory` | `META-INF/services/io.github.totalschema.connector.shell.spi.ShellScriptRunnerFactory` | Replace the local shell runner implementation |
| `ConnectorManager` | `META-INF/services/io.github.totalschema.connector.ConnectorManager` | Replace connector lifecycle management entirely |

All SPI lookups use `ServiceLoaderFactory.getSingleService()` or `getAllServices()` from
`totalschema-core`, which wraps Java's standard `ServiceLoader` with sensible error handling.

---

## Related Documentation

- **`docs/developer/IOC_CONTAINER_ARCHITECTURE.md`** — How the IoC container resolves factories
  by type and qualifier; required for understanding `context.get(Connector.class, "jdbc", …)`.
- **`docs/developer/SCRIPT_EXECUTOR_SUBSYSTEM.md`** — How JDBC connectors dispatch to
  `ScriptExecutor` implementations by file extension.
- **`docs/developer/CONNECTOR-EXTRACTION-SUMMARY.md`** — Refactoring history: why SSH/shell
  connectors were extracted from `totalschema-core` into separate modules.
- **`docs/developer/MODULE-CREATION-SUMMARY.md`** — Details on the `totalschema-connector-common`
  shared infrastructure module.
- **`sample/totalschema.yml`** — Reference configuration showing a JDBC connector in practice.

