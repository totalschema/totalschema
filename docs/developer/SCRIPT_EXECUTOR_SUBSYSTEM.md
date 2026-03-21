# Script Executor Subsystem - Developer Guide

## Overview

The Script Executor subsystem enables TotalSchema to execute various types of scripts (SQL, Groovy, etc.) against connectors during change deployment. It uses a pure IoC container pattern with qualifiers to map file extensions to executor implementations.

## Architecture

### High-Level Flow

```
Change File (e.g., 0001.create_table.apply.mydb.sql)
    ↓
JdbcConnector
    ↓ (extracts extension: "sql")
Container lookup: context.get(ScriptExecutor.class, "sql", name, config)
    ↓
SqlScriptExecutorFactory (qualifier="sql")
    ↓
SqlScriptExecutor (executes SQL against JdbcDatabase)
```

### Core Components

#### 1. ScriptExecutor Interface

**Location:** `totalschema-core/src/main/java/io/github/totalschema/spi/script/ScriptExecutor.java`

```java
public interface ScriptExecutor extends Closeable {
    /**
     * Executes the script content against the target system.
     *
     * @param scriptContent The script to execute
     * @param context Command context providing access to services
     * @throws InterruptedException if execution is interrupted
     */
    void execute(String scriptContent, CommandContext context) throws InterruptedException;
    
    @Override
    void close() throws IOException;
}
```

**Responsibilities:**
- Parse and execute script content
- Handle script-specific syntax and features
- Manage execution lifecycle
- Report errors with context

#### 2. ComponentFactory<ScriptExecutor>

Script executors are registered via standard `ComponentFactory` pattern with:
- **Qualifier:** File extension (e.g., "sql", "groovy", "py")
- **Arguments:** `name` (String), `configuration` (Configuration)

#### 3. Container-Based Lookup

```java
// JdbcConnector.execute() method
String extension = changeFile.getId().getExtension();
ScriptExecutor executor = context.get(
    ScriptExecutor.class,    // Type
    extension,               // Qualifier (file extension)
    name,                    // Connector name
    connectorConfiguration   // Connector configuration
);
executor.execute(fileContent, context);
```

## Built-in Script Executors

### SQL Script Executor

**Extension:** `.sql`  
**Factory:** `SqlScriptExecutorFactory`  
**Implementation:** `SqlScriptExecutor`

**Features:**
- Executes SQL statements against JDBC databases
- Configurable statement separator (default: `;`)
- Can disable separator with `no.statementSeparator=true`
- Supports all JDBC-compliant databases

**Configuration Example:**
```yaml
connectors:
  mydb:
    type: jdbc
    url: jdbc:postgresql://localhost:5432/mydb
    username: user
    password: ${secret:!SECRET;1.0;...}
    statementSeparator: ";"  # Optional, default is ";"
    # no.statementSeparator: true  # Uncomment to disable separator
```

**Script Example:**
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
**Factory:** `GroovyScriptExecutorFactory`  
**Implementation:** `GroovyScriptExecutor`  
**Module:** `totalschema-groovy-extensions`

**Features:**
- Executes Groovy scripts with database connectivity
- Injects `sql` (groovy.sql.Sql) for database operations
- Injects `configuration` for connector settings
- Injects `environment` (if available in context)
- Full Groovy language support

**Configuration Example:**
```yaml
connectors:
  mydb:
    type: jdbc
    url: jdbc:postgresql://localhost:5432/mydb
    username: user
    password: ${secret:!SECRET;1.0;...}
```

**Script Example:**
```groovy
// 0001.create_and_populate_users.apply.mydb.groovy
sql.execute("""
    CREATE TABLE users (
        id SERIAL PRIMARY KEY,
        username VARCHAR(50) NOT NULL,
        email VARCHAR(100) NOT NULL
    )
""")

// Populate with data
['alice', 'bob', 'charlie'].each { username ->
    sql.execute(
        "INSERT INTO users (username, email) VALUES (?, ?)",
        [username, "${username}@example.com"]
    )
}

println "Created and populated users table"
```

## Creating a Custom Script Executor

### Step-by-Step Guide

#### Step 1: Create the Executor Implementation

```java
package com.example.totalschema.python;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.jdbc.JdbcDatabase;
import io.github.totalschema.spi.script.ScriptExecutor;
import java.io.IOException;

/**
 * Script executor for Python scripts with database connectivity.
 */
public final class PythonScriptExecutor implements ScriptExecutor {

    private final String connectorName;
    private final Configuration configuration;
    private final JdbcDatabase jdbcDatabase;

    /**
     * Constructor with dependencies injected by IoC container.
     *
     * @param connectorName Name of the connector
     * @param configuration Connector configuration
     * @param jdbcDatabase Database connection (injected by container)
     */
    public PythonScriptExecutor(
            String connectorName,
            Configuration configuration,
            JdbcDatabase jdbcDatabase) {
        this.connectorName = connectorName;
        this.configuration = configuration;
        this.jdbcDatabase = jdbcDatabase;
    }

    @Override
    public void execute(String scriptContent, CommandContext context) throws InterruptedException {
        try {
            // Your Python execution logic here
            // Example: Use Jython, GraalVM Python, or ProcessBuilder
            executePythonScript(scriptContent, context);
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to execute Python script for connector: " + connectorName, e);
        }
    }

    private void executePythonScript(String scriptContent, CommandContext context) {
        // Implementation details:
        // 1. Set up Python interpreter (Jython, GraalVM, etc.)
        // 2. Inject database connection
        // 3. Execute script
        // 4. Handle results and errors
    }

    @Override
    public void close() throws IOException {
        // Cleanup resources if needed
        // Note: JdbcDatabase is managed by container, don't close it here
    }

    @Override
    public String toString() {
        return "PythonScriptExecutor{connector=" + connectorName + "}";
    }
}
```

#### Step 2: Create the ComponentFactory

```java
package com.example.totalschema.python;

import static io.github.totalschema.spi.ArgumentSpecification.*;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.jdbc.JdbcDatabase;
import io.github.totalschema.spi.ArgumentSpecification;
import io.github.totalschema.spi.ComponentFactory;
import io.github.totalschema.spi.script.ScriptExecutor;
import java.util.List;

/**
 * ComponentFactory for creating Python script executors.
 *
 * <p>Registers with qualifier "python" to handle .py files.
 * Usage: context.get(ScriptExecutor.class, "python", connectorName, configuration)
 */
public final class PythonScriptExecutorFactory extends ComponentFactory<ScriptExecutor> {

    private static final ArgumentSpecification<String> NAME_ARGUMENT = string("name");
    private static final ArgumentSpecification<Configuration> CONFIGURATION_ARGUMENT =
            configuration("configuration");

    @Override
    public boolean isLazy() {
        return true; // Created on-demand when .py files are executed
    }

    @Override
    public Class<ScriptExecutor> getConstructedClass() {
        return ScriptExecutor.class; // Service interface, not implementation class
    }

    @Override
    public String getQualifier() {
        return "python"; // Maps .py file extension
    }

    @Override
    public List<Class<?>> getRequiredContextTypes() {
        // No dependencies required - container manages lifecycle automatically
        return List.of();
    }

    @Override
    public List<ArgumentSpecification<?>> getArgumentSpecifications() {
        // Arguments provided by JdbcConnector at runtime
        return List.of(NAME_ARGUMENT, CONFIGURATION_ARGUMENT);
    }

    @Override
    public ScriptExecutor newComponent(Context context, Object... arguments) {
        // Extract validated arguments
        String name = getArgument(NAME_ARGUMENT, arguments, 0);
        Configuration configuration = getArgument(CONFIGURATION_ARGUMENT, arguments, 1);

        // Get JdbcDatabase from IoC container
        // Container manages lifecycle (caching, closing)
        JdbcDatabase jdbcDatabase = context.get(JdbcDatabase.class, null, name, configuration);

        // Create executor with injected dependencies
        // Container will automatically close it when engine closes (implements Closeable)
        PythonScriptExecutor executor = new PythonScriptExecutor(name, configuration, jdbcDatabase);


        return executor;
    }
}
```

#### Step 3: Register via ServiceLoader

Create file: `src/main/resources/META-INF/services/io.github.totalschema.spi.ComponentFactory`

```
com.example.totalschema.python.PythonScriptExecutorFactory
```

#### Step 4: Add Dependencies (if needed)

If your executor needs external libraries:

```xml
<!-- pom.xml -->
<dependencies>
    <!-- Example: Jython for Python support -->
    <dependency>
        <groupId>org.python</groupId>
        <artifactId>jython-standalone</artifactId>
        <version>2.7.3</version>
        <scope>provided</scope> <!-- User installs in user_libs/ -->
    </dependency>
    
    <!-- TotalSchema core (provided by classpath) -->
    <dependency>
        <groupId>io.github.totalschema</groupId>
        <artifactId>totalschema-core</artifactId>
        <version>${project.version}</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

**Important:** External libraries should be `provided` scope. Users install them in `user_libs/` directory.

#### Step 5: Package as Extension Module

```
totalschema-python-extension/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/totalschema/python/
│   │   │       ├── PythonScriptExecutor.java
│   │   │       └── PythonScriptExecutorFactory.java
│   │   └── resources/
│   │       └── META-INF/
│   │           └── services/
│   │               └── io.github.totalschema.spi.ComponentFactory
│   └── test/
│       └── java/
│           └── com/example/totalschema/python/
│               └── PythonScriptExecutorTest.java
└── README.md
```

## Best Practices

**⚠️ Important:** The IoC container automatically manages the lifecycle of all `Closeable` components. If your `ScriptExecutor` implements `Closeable`, you do NOT need to:
- Subscribe to `ChangeEngineCloseEvent`
- Manually register with `EventDispatcher`
- Track cleanup yourself

The container will automatically call `close()` on all `Closeable` components in reverse creation order when `engine.close()` is called.

### 1. Always Use Constructor Injection

✅ **Correct:**
```java
public MyScriptExecutor(String name, Configuration config, JdbcDatabase database) {
    this.database = database; // Injected by factory
}
```

❌ **Wrong:**
```java
public MyScriptExecutor(String name, Configuration config) {
    JdbcDatabaseFactory factory = JdbcDatabaseFactory.getInstance(); // Deprecated!
    this.database = factory.getJdbcDatabase(name, config);
}
```

### 2. Let Container Manage Lifecycle

The container automatically manages all `Closeable` components:

✅ **Correct:**
```java
@Override
public ScriptExecutor newComponent(Context context, Object... arguments) {
    // Get JdbcDatabase from container - container manages lifecycle
    JdbcDatabase database = context.get(JdbcDatabase.class, null, name, config);
    
    // Create executor - container will automatically close it (Closeable)
    return new MyScriptExecutor(name, config, database);
}
```

❌ **Wrong:**
```java
@Override
public ScriptExecutor newComponent(Context context, Object... arguments) {
    // Don't create JdbcDatabase manually - container won't know about it
    JdbcDatabase database = DefaultJdbcDatabase.newInstance(name, config);
    return new MyScriptExecutor(name, config, database);
}
```

**Important:** If your executor implements `Closeable`, the container will:
- Track it automatically when created
- Call `close()` in reverse order when the engine closes
- Handle exceptions during cleanup

No manual event subscription needed!

### 3. Use Qualifier for File Extension

```java
@Override
public String getQualifier() {
    return "myext"; // Lowercase file extension without dot
}
```

This allows scripts with `.myext` extension to be executed by your executor.

### 4. Handle Errors Gracefully

```java
@Override
public void execute(String scriptContent, CommandContext context) throws InterruptedException {
    try {
        executeScript(scriptContent);
    } catch (Exception e) {
        throw new RuntimeException(
            "Failed to execute script for connector '" + connectorName + "': " + e.getMessage(),
            e);
    }
}
```

### 5. Make Executors Thread-Safe

If your executor maintains state, ensure it's thread-safe:

```java
public final class MyScriptExecutor implements ScriptExecutor {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    
    @Override
    public void close() throws IOException {
        if (!isClosed.compareAndSet(false, true)) {
            return; // Already closed
        }
        // Cleanup resources
    }
}
```

## Testing Script Executors

### Unit Testing the Executor

```java
@Test
public void testExecutorExecutesScript() throws Exception {
    // Mock dependencies
    Configuration mockConfig = createMock(Configuration.class);
    JdbcDatabase mockDatabase = createMock(JdbcDatabase.class);
    CommandContext mockContext = createMock(CommandContext.class);
    
    // Setup expectations
    mockDatabase.execute("SELECT 1");
    expectLastCall().once();
    replay(mockConfig, mockDatabase, mockContext);
    
    // Create executor
    MyScriptExecutor executor = new MyScriptExecutor("test", mockConfig, mockDatabase);
    
    // Execute script
    executor.execute("SELECT 1", mockContext);
    
    // Verify
    verify(mockDatabase);
}
```

### Integration Testing with Container

```java
@Test
public void testFactoryCreatesExecutor() {
    Configuration config = new MapBackedConfiguration(Map.of(
        "url", "jdbc:h2:mem:test"
    ));
    
    ComponentContainer container = ComponentContainer.builder()
        .withComponent(Configuration.class, config)
        .withFactory(new JdbcDatabaseComponentFactory())
        .withFactory(new MyScriptExecutorFactory())
        .build();
    
    try {
        // Get executor from container
        ScriptExecutor executor = container.get(
            ScriptExecutor.class,
            "myext",  // Qualifier
            "testdb", // Name
            config    // Configuration
        );
        
        assertNotNull(executor);
        assertTrue(executor instanceof MyScriptExecutor);
    } finally {
        container.close();
    }
}
```

### Testing Script Execution End-to-End

```java
@Test
public void testScriptExecutionViaConnector() throws Exception {
    // Setup workspace with test script
    Path workspace = Files.createTempDirectory("test-workspace");
    Path changesDir = workspace.resolve("totalschema/changes");
    Files.createDirectories(changesDir);
    
    // Create test script
    Path scriptFile = changesDir.resolve("0001.test.apply.mydb.myext");
    Files.writeString(scriptFile, "test script content");
    
    // Execute via TotalSchema
    ChangeEngine engine = ChangeEngineFactory.getInstance()
        .createChangeEngine(workspace, null);
    
    try {
        engine.getChangeManager().applyChanges();
        // Verify script was executed
    } finally {
        engine.close();
    }
}
```

## Advanced Features

### Accessing Context Services

Your executor can access any service from the CommandContext:

```java
@Override
public void execute(String scriptContent, CommandContext context) throws InterruptedException {
    // Get environment (if available)
    if (context.has(Environment.class)) {
        Environment env = context.get(Environment.class);
        String envName = env.getName(); // DEV, PROD, etc.
    }
    
    // Get configuration
    Configuration config = context.get(Configuration.class);
    
    // Get secrets manager
    SecretsManager secretsManager = context.get(SecretsManager.class);
    
    // Execute script with context
    executeWithContext(scriptContent, context);
}
```

### Supporting Multiple Extensions

If your executor handles multiple file extensions, create separate factories:

```java
// PythonScriptExecutorFactory - qualifier "python"
// Jython2ScriptExecutorFactory - qualifier "jython2"
// Jython3ScriptExecutorFactory - qualifier "jython3"

// All can delegate to the same executor implementation
```

Or use a base factory:

```java
abstract class AbstractPythonScriptExecutorFactory extends ComponentFactory<ScriptExecutor> {
    private final String qualifier;
    
    protected AbstractPythonScriptExecutorFactory(String qualifier) {
        this.qualifier = qualifier;
    }
    
    @Override
    public String getQualifier() {
        return qualifier;
    }
    
    // ... common implementation
}

public class Python2Factory extends AbstractPythonScriptExecutorFactory {
    public Python2Factory() { super("py2"); }
}

public class Python3Factory extends AbstractPythonScriptExecutorFactory {
    public Python3Factory() { super("py3"); }
}
```

### Configuration Options

Your executor can read connector-specific configuration:

```java
@Override
public void execute(String scriptContent, CommandContext context) throws InterruptedException {
    // Read custom configuration
    boolean strictMode = configuration.getBoolean("python.strictMode").orElse(false);
    int timeout = configuration.getInteger("python.timeout").orElse(300);
    String pythonPath = configuration.getString("python.path").orElse("/usr/bin/python3");
    
    // Use configuration
    executeWithOptions(scriptContent, strictMode, timeout, pythonPath);
}
```

User configuration:
```yaml
connectors:
  mydb:
    type: jdbc
    url: jdbc:postgresql://localhost:5432/mydb
    python.strictMode: true
    python.timeout: 600
    python.path: /opt/python3.9/bin/python3
```

### Custom Error Handling

```java
public class ScriptExecutionException extends RuntimeException {
    private final String connectorName;
    private final String scriptContent;
    private final int lineNumber;
    
    public ScriptExecutionException(
            String message,
            String connectorName,
            String scriptContent,
            int lineNumber,
            Throwable cause) {
        super(formatMessage(message, connectorName, lineNumber), cause);
        this.connectorName = connectorName;
        this.scriptContent = scriptContent;
        this.lineNumber = lineNumber;
    }
    
    private static String formatMessage(String message, String connector, int line) {
        return String.format(
            "Script execution failed for connector '%s' at line %d: %s",
            connector, line, message);
    }
}
```

## Troubleshooting

### Common Issues

#### 1. Executor Not Found

**Error:**
```
IllegalStateException: No component factory found for: ScriptExecutor with qualifier 'myext'
```

**Solution:**
- Verify factory is in `META-INF/services/io.github.totalschema.spi.ComponentFactory`
- Check qualifier matches file extension (lowercase)
- Ensure extension module is on classpath

#### 2. Constructor Arguments Mismatch

**Error:**
```
IllegalArgumentException: Factory expects 2 arguments but received 3
```

**Solution:**
- Verify `getArgumentSpecifications()` returns correct argument types
- Check `JdbcConnector` passes: `name` (String), `configuration` (Configuration)
- Don't add extra arguments without updating connector

#### 3. Dependency Not Available

**Error:**
```
IllegalStateException: Required dependency Configuration not available in container
```

**Solution:**
- Verify dependency is available in the container
- Most common services (Configuration, EventDispatcher) are automatically registered
- Add dependency to `getRequiredContextTypes()` only if truly required
- Use `ConditionalComponentFactory` for optional dependencies

#### 4. ClassNotFoundException

**Error:**
```
ClassNotFoundException: com.example.MyScriptLibrary
```

**Solution:**
- Add library to `user_libs/` directory
- Set `TOTALSCHEMA_USER_LIBS` environment variable
- Mark dependencies as `provided` scope in pom.xml

## Examples Repository

For complete working examples, see:
- **SQL Executor:** `totalschema-core/src/main/java/io/github/totalschema/engine/internal/script/`
- **Groovy Executor:** `totalschema-extensions/totalschema-groovy-extensions/src/main/java/`

## Related Documentation

- **IoC Container Architecture:** `docs/developer/IOC_CONTAINER_ARCHITECTURE.md`
- **Script Executor Refactoring:** `docs/developer/SCRIPT_EXECUTOR_IOC_REFACTORING.md`
- **Extension Development:** `AGENTS.md` (Extension Points section)
- **User Manual:** `totalschema-cli/user_manual/README.md`

## Contributing

When adding new script executors to TotalSchema:

1. **Create module** under `totalschema-extensions/`
2. **Follow naming convention:** `totalschema-{language}-extensions`
3. **Add documentation** in module README.md
4. **Include tests** with >80% coverage
5. **Mark external dependencies** as `provided` scope
6. **Submit PR** with examples and usage instructions

## Summary

The Script Executor subsystem provides a clean, extensible way to add support for new scripting languages:

1. **Implement** `ScriptExecutor` interface
2. **Extend** `ComponentFactory<ScriptExecutor>` with qualifier
3. **Register** via `META-INF/services/io.github.totalschema.spi.ComponentFactory`
4. **Deploy** as extension module or user_libs JAR

The IoC container handles discovery, lifecycle, and dependency injection automatically.

