# Script Executor Subsystem - Developer Guide

## Overview

The Script Executor subsystem lets TotalSchema execute different scripting languages (SQL, Groovy,
…) against a JDBC connector. The `JdbcConnector` is language-agnostic: it looks up a
`ScriptExecutor` keyed by the file extension, so adding a new language requires no changes to
`totalschema-core`.

## Architecture

### High-Level Flow

```
Change File (e.g., 0001.create_table.apply.mydb.sql)
    ↓
JdbcConnector
    ↓  acquires JdbcDatabase, places it in a child CommandContext
    ↓  looks up executor by extension: context.get(ScriptExecutor.class, "sql", config)
SqlScriptExecutorComponentFactory  (qualifier = "sql")
    ↓  createExecutor(config) → new SqlScriptExecutor(config)
SqlScriptExecutor.execute(fileContent, context)
    ↓  reads JdbcDatabase from context, optionally substitutes variables, executes SQL
```

### Core Components

#### 1. `ScriptExecutor` Interface

**Location:** `totalschema-core/.../spi/script/ScriptExecutor.java`

```java
public interface ScriptExecutor {
    /**
     * Executes the given script content.
     *
     * When invoked from JdbcConnector, the context always contains a JdbcDatabase instance
     * that can be retrieved with context.get(JdbcDatabase.class).
     */
    void execute(String script, Context context) throws InterruptedException;
}
```

#### 2. `AbstractScriptExecutorComponentFactory`

**Location:** `totalschema-core/.../spi/script/AbstractScriptExecutorComponentFactory.java`

Base class for all script-executor factories. It owns the shared boilerplate:
- Accepts a single `Configuration` argument (the connector configuration)
- Qualifier = file-extension string passed to the constructor
- All lifecycle methods (`isLazy`, `getComponentType`, `getDependencies`, …) are `final`

Subclasses only override one method:

```java
protected abstract ScriptExecutor createExecutor(Configuration configuration);
```

#### 3. `JdbcConnector` — dispatcher

**Location:** `totalschema-core/.../connector/jdbc/JdbcConnector.java`

```java
// JdbcConnector.execute() — simplified
JdbcDatabase jdbcDatabase = context.get(JdbcDatabase.class, null, name, connectorConfiguration);

CommandContext executorContext = new CommandContext(context);
executorContext.setValue(JdbcDatabase.class, jdbcDatabase);

ScriptExecutor executor = executorContext.get(ScriptExecutor.class, extension, connectorConfiguration);
executor.execute(fileContent, executorContext);
```

`JdbcConnector` owns the `JdbcDatabase` lifecycle for each execution. Executors retrieve it
from the context; they do not create or close the connection.

---

## Built-in Script Executors

### SQL Script Executor

**Extension:** `.sql`  
**Factory:** `SqlScriptExecutorComponentFactory`  
**Implementation:** `SqlScriptExecutor`  
**Module:** `totalschema-core`

**Features:**
- Optional variable substitution (opt-in via `scriptExecutors.sql.variableSubstitution: true`)
- Splits the script on a configurable separator (default: `;`)
- Separator can be disabled with `disableStatementSeparator: true`

**Configuration:**

```yaml
connectors:
  mydb:
    type: jdbc
    # ...connection settings...
    statementSeparator: ";"          # optional, default is ";"
    # disableStatementSeparator: true   # disable splitting entirely
    scriptExecutors:
      sql:
        variableSubstitution: true   # opt-in: enable ${varName} substitution for .sql files
```

**Example script:**

```sql
-- 0001.create_users_table.apply.mydb.sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL
);

CREATE INDEX idx_users_email ON users(email);
```

### Groovy Script Executor

**Extension:** `.groovy`  
**Factory:** `GroovyScriptExecutorComponentFactory`  
**Implementation:** `GroovyScriptExecutor`  
**Module:** `totalschema-groovy-extensions`

**Bindings injected into every Groovy script:**

| Name | Type | Description |
|---|---|---|
| `sql` | `groovy.sql.Sql` | Open database connection |
| `configuration` | `Configuration` | Connector configuration |
| `environment` | `Environment` | Current environment (if present in context) |

**No variable substitution** is applied to Groovy scripts — Groovy's own `${...}` GString
syntax handles dynamic values.

**Example script:**

```groovy
// 0001.create_and_populate_users.apply.mydb.groovy
sql.execute("""
    CREATE TABLE users (
        id SERIAL PRIMARY KEY,
        username VARCHAR(50) NOT NULL
    )
""")

['alice', 'bob'].each { name ->
    sql.execute("INSERT INTO users (username) VALUES (?)", [name])
}
```

### JSR-223 Script Executor

**Extensions:** any extension advertised by a JSR-223 `ScriptEngineFactory` on the classpath
(e.g. `.js`, `.py`, `.rb`, `.kts`)  
**Factory:** `JSR223ScriptExecutorFactory` *(static factory, not an IoC component)*  
**Implementation:** `JSR223ScriptExecutor`  
**Module:** `totalschema-core`

The JSR-223 executor provides **automatic support for any scripting language** whose
`javax.script.ScriptEngineFactory` implementation is present on the classpath. No custom factory
or `ServiceLoader` registration is required — TotalSchema discovers all engines via
`ScriptEngineManager.getEngineFactories()` and registers one executor per advertised file
extension at startup.

#### Opting in

JSR-223 support is **disabled by default** and must be enabled per connector:

```yaml
connectors:
  mydb:
    type: jdbc
    # ...connection settings...
    scripting:
      jsr223:
        enabled: true
```

#### Bindings injected at runtime

The following objects are bound into the script's `Bindings` when present in the execution context:

| Binding name | Type | Description |
|---|---|---|
| `configuration` | `Configuration` | Connector configuration |
| `connection` | `java.sql.Connection` | Active JDBC connection |
| `environment` | `Environment` | Current environment (if present) |

#### Example — JavaScript (GraalVM / Nashorn)

Place the engine JAR (e.g. GraalVM Polyglot) in `user_libs/`, then enable JSR-223 on the
connector:

```javascript
// 0001.create_users.apply.mydb.js
var stmt = connection.createStatement();
stmt.execute(
  "CREATE TABLE users (id INT PRIMARY KEY, username VARCHAR(50))"
);
stmt.close();
```

#### Classpath discovery

Engines are discovered once at startup via:

```java
new ScriptEngineManager(Thread.currentThread().getContextClassLoader())
    .getEngineFactories();
```

Each factory that reports at least one file extension gets a corresponding `JSR223ScriptExecutor`.
Factories reporting a `null` or empty extension list are silently skipped.

#### Relationship to custom executors

JSR-223 is the **zero-friction path**: drop in a JAR, enable the flag, write scripts. For
production use-cases that need richer bindings (e.g. a `groovy.sql.Sql` wrapper), performance
tuning, or connector-specific helpers, implement a dedicated `ScriptExecutor` instead — see
*Creating a Custom Script Executor* below.

---

## Creating a Custom Script Executor

### Step 1 — Implement `ScriptExecutor`

```java
package com.example.totalschema.python;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.jdbc.JdbcDatabase;
import io.github.totalschema.spi.script.ScriptExecutor;

public final class PythonScriptExecutor implements ScriptExecutor {

    private final Configuration configuration;

    public PythonScriptExecutor(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void execute(String scriptContent, Context context) throws InterruptedException {
        // JdbcDatabase is placed in the context by JdbcConnector before this is called.
        JdbcDatabase jdbcDatabase = context.get(JdbcDatabase.class);

        try {
            executePythonScript(scriptContent, jdbcDatabase);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute Python script", e);
        }
    }

    private void executePythonScript(String scriptContent, JdbcDatabase jdbcDatabase) {
        // Set up interpreter (Jython, GraalVM Python, etc.)
        // Inject jdbcDatabase.withConnection(...) if needed
    }
}
```

### Step 2 — Extend `AbstractScriptExecutorComponentFactory`

```java
package com.example.totalschema.python;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.spi.script.AbstractScriptExecutorComponentFactory;
import io.github.totalschema.spi.script.ScriptExecutor;

public final class PythonScriptExecutorComponentFactory
        extends AbstractScriptExecutorComponentFactory {

    public PythonScriptExecutorComponentFactory() {
        super("python");   // handles .python files; use "py" for .py files
    }

    @Override
    protected ScriptExecutor createExecutor(Configuration configuration) {
        return new PythonScriptExecutor(configuration);
    }
}
```

That is all. `AbstractScriptExecutorComponentFactory` handles `isLazy()`, `getComponentType()`,
`getQualifier()`, `getDependencies()`, `getArgumentSpecifications()`, and `createComponent()`.

### Step 3 — Register via ServiceLoader

Create `src/main/resources/META-INF/services/io.github.totalschema.spi.factory.ComponentFactory`:

```
com.example.totalschema.python.PythonScriptExecutorFactory
```

### Step 4 — Add Dependencies

Mark any external libraries as `provided` scope — users install them in `user_libs/`:

```xml
<dependency>
    <groupId>io.github.totalschema</groupId>
    <artifactId>totalschema-core</artifactId>
    <version>${project.version}</version>
    <scope>provided</scope>
</dependency>
```

---

## Best Practices

### 1. Retrieve `JdbcDatabase` from context, never create it

```java
// ✅ Correct — JdbcConnector places it here before dispatch
JdbcDatabase jdbcDatabase = context.get(JdbcDatabase.class);

// ❌ Wrong — bypasses IoC lifecycle
JdbcDatabase database = DefaultJdbcDatabase.newInstance(name, config);
```

### 2. Use `AbstractScriptExecutorComponentFactory`, not `ComponentFactory` directly

```java
// ✅ Correct
public final class MyFactory extends AbstractScriptExecutorComponentFactory {
    public MyFactory() { super("myext"); }

    @Override
    protected ScriptExecutor createExecutor(Configuration configuration) {
        return new MyScriptExecutor(configuration);
    }
}

// ❌ Unnecessarily verbose — re-implements boilerplate already in the base class
public final class MyFactory extends ComponentFactory<ScriptExecutor> {
    // ...40 lines of isLazy, getComponentType, getDependencies, ...
}
```

### 3. Multiple extensions — one factory per extension

```java
// PythonScriptExecutorComponentFactory  — super("python")
// PyScriptExecutorComponentFactory      — super("py")
// Both delegate to the same executor implementation:
@Override
protected ScriptExecutor createExecutor(Configuration configuration) {
    return new PythonScriptExecutor(configuration);
}
```

### 4. Thread-safety for `Closeable` executors

If your executor holds resources (e.g. a persistent interpreter), implement `Closeable` and use
`AtomicBoolean` for idempotent close. The IoC container calls `close()` automatically in reverse
creation order when the engine shuts down — no manual event subscription needed.

```java
public final class MyScriptExecutor implements ScriptExecutor, Closeable {
    private final AtomicBoolean closed = new AtomicBoolean(false);

    @Override
    public void close() throws IOException {
        if (!closed.compareAndSet(false, true)) return;
        // release resources
    }
}
```

### 5. Reading connector configuration

The `Configuration` passed to your constructor is the **connector's** configuration block,
so all custom keys live there:

```yaml
connectors:
  mydb:
    type: jdbc
    python.timeout: 600
    python.path: /opt/python3.9/bin/python3
```

```java
int timeout = configuration.getInteger("python.timeout").orElse(300);
String pythonPath = configuration.getString("python.path").orElse("/usr/bin/python3");
```

---

## Testing

### Unit testing the executor

Wire `JdbcDatabase` directly into the `CommandContext` — this is what `JdbcConnector` does at
runtime.

```java
@Test
public void testExecutorRunsScript() throws Exception {
    JdbcDatabase mockDatabase = createMock(JdbcDatabase.class);
    mockDatabase.execute("SELECT 1");
    expectLastCall().once();
    replay(mockDatabase);

    Configuration config = Configuration.builder().build();
    CommandContext context = new CommandContext();
    context.setValue(JdbcDatabase.class, mockDatabase);

    new SqlScriptExecutor(config).execute("SELECT 1", context);

    verify(mockDatabase);
}
```

### Integration testing with the IoC container

```java
@Test
public void testFactoryCreatesExecutor() {
    Configuration config = Configuration.builder()
        .set("url", "jdbc:h2:mem:test")
        .build();

    ComponentContainer container = ComponentContainer.builder()
        .withFactory(new JdbcDatabaseComponentFactory())
        .withFactory(new MyScriptExecutorFactory())
        .build();

    try {
        ScriptExecutor executor = container.get(
            ScriptExecutor.class,
            "myext",    // qualifier (extension)
            config      // connector configuration
        );
        assertNotNull(executor);
    } finally {
        container.close();
    }
}
```

---

## Troubleshooting

### Executor not found

```
IllegalStateException: No component factory found for: ScriptExecutor with qualifier 'myext'
```

- Verify the factory is listed in `META-INF/services/io.github.totalschema.spi.factory.ComponentFactory`.
- Check that the qualifier passed to `super(...)` matches the file extension (lowercase, no dot).
- Ensure the extension module JAR is on the classpath / in `user_libs/`.

### Wrong argument count

```
IllegalArgumentException: AbstractScriptExecutorComponentFactory expects 1 argument(s) …
```

The factory expects exactly one argument: the `Configuration`. The call site is
`context.get(ScriptExecutor.class, extension, configuration)`. No connector name is passed.

### ClassNotFoundException for script library

Add the library JAR to `user_libs/` (CLI) or mark it `provided` scope in your extension POM and
instruct users to supply it.

---

## Examples

- **SQL Executor:** `totalschema-core/src/main/java/io/github/totalschema/engine/internal/script/`
- **Groovy Executor:** `totalschema-extensions/totalschema-groovy-extensions/src/main/java/`
- **SQL Executor tests:** `totalschema-core/src/test/java/…/engine/internal/script/SqlScriptExecutorTest.java`

## Related Documentation

- `docs/developer/CONNECTOR_AND_SCRIPT_EXECUTOR_OVERVIEW.md` — concept overview
- `docs/developer/CONNECTOR_ARCHITECTURE.md` — full JDBC connector execution flow
- `docs/developer/IOC_CONTAINER_ARCHITECTURE.md` — how context.get() resolves factories
