# BigTable Connector - Developer Guide

Welcome to the BigTable connector developer documentation. This guide covers architecture, development patterns, testing, and contribution guidelines.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Development Setup](#development-setup)
- [Adding New Actions](#adding-new-actions)
- [Testing](#testing)
- [Code Patterns](#code-patterns)
- [Contributing](#contributing)
- [Resources](#resources)

---

## Overview

The BigTable connector is a production-ready TotalSchema connector that manages Google Cloud Bigtable infrastructure through declarative YAML and Groovy scripts. It uses a **reflection-free architecture** based on ServiceLoader and Jackson for optimal performance.

### Key Design Principles

1. **Zero Reflection** — ServiceLoader-based plugin discovery, Jackson for data binding
2. **Type Safety** — Compile-time type checking, no runtime surprises
3. **Extensibility** — Add new actions without modifying existing code
4. **Simplicity** — Each action is a simple POJO with one execute method
5. **Testability** — Clean separation of concerns, easy to mock and test

### Technology Stack

- **Java 11+** — Base language
- **ServiceLoader** — Plugin discovery mechanism
- **Jackson 2.22.1** — YAML/JSON data binding
- **Google Cloud Bigtable SDK** — BigTable Admin API
- **TestNG** — Testing framework
- **EasyMock** — Mocking framework

---

## Architecture

### High-Level Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     TotalSchema Core                        │
│  ┌───────────────────────────────────────────────────────┐  │
│  │            ChangeExecutorConnector                    │  │
│  └───────────────────┬───────────────────────────────────┘  │
└────────────────────┬─┴──────────────────────────────────────┘
                     │
┌────────────────────▼───────────────────────────────────────┐
│               BigTableConnector                            │
│  • Manages connections                                     │
│  • Routes change files to executors                        │
│  • Provides admin client bindings                          │
└────────────────────┬───────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        │                         │
┌───────▼──────────┐    ┌────────▼──────────────┐
│ YML Executor     │    │ Groovy Executor       │
│ (.yml files)     │    │ (.groovy files)       │
└───────┬──────────┘    └───────────────────────┘
        │
┌───────▼─────────────────────────────────────────────────────┐
│                    ActionFactory Registry                   │
│  Map<String, ActionFactory<?>>                              │
│  • "CreateTable" → CreateTableActionFactory                 │
│  • "DeleteTable" → DeleteTableActionFactory                 │
│  • ... (19 total action types)                              │
└───────┬─────────────────────────────────────────────────────┘
        │
┌───────▼─────────────────────────────────────────────────────┐
│                   Concrete Action Classes                   │
│  CreateTable, DeleteTable, CreateColumnFamily, ...          │
│  • Execute against BigtableTableAdminClient                 │
│  • Build GC rules, IAM policies, etc.                       │
└─────────────────────────────────────────────────────────────┘
```

### Action Discovery Flow

The connector uses a sophisticated but simple discovery mechanism:

1. **JVM Class Loading** — When `ActionFactory` class is loaded, static initializer runs
2. **ServiceLoader Discovery** — `ServiceLoader.load(ActionFactory.class)` finds all registered factories
3. **Factory Instantiation** — Each factory is instantiated (e.g., `new CreateTableActionFactory()`)
4. **Action Type Registration** — Factory constructor calls `super(CreateTable.class)`, base class extracts simple name: `"CreateTable"`
5. **Registry Population** — Map is populated: `{"CreateTable" → factory, "DeleteTable" → factory, ...}`
6. **Runtime Lookup** — When YAML is parsed, top-level key (e.g., `CreateTable:`) is used to lookup factory
7. **Deserialization** — Factory deserializes YAML content into Action POJO using Jackson
8. **Execution** — Action's `execute()` method is called with admin client

**Critical Insight:** The YAML key **must match the Action class simple name exactly**. The factory class name is just a convention for clarity.

### Package Structure

```
io.github.totalschema.connector.gcp.bigtable/
├── BigTableConnector.java                    # Main connector class
├── BigTableConnectorComponentFactory.java    # IoC factory
├── BigTableConnectorException.java           # Custom exception
├── YmlBigTableChangeExecutor.java            # YAML executor
│
└── actions/                                   # All actions live here
    ├── Action.java                            # Base interface
    ├── ActionFactory.java                     # Abstract factory (ServiceLoader)
    ├── TableAction.java                       # Marker for table actions
    ├── InstanceAction.java                    # Marker for instance actions
    │
    ├── createtable/                           # Per-action sub-package
    │   ├── CreateTableActionFactory.java      # Factory (7 lines)
    │   ├── CreateTable.java                   # Action POJO
    │   ├── ColumnFamily.java                  # Helper class
    │   └── GcRule.java                        # Helper class
    │
    ├── deletetable/
    │   ├── DeleteTableActionFactory.java
    │   └── DeleteTable.java
    │
    ├── createcolumnfamily/
    │   ├── CreateColumnFamilyActionFactory.java
    │   └── CreateColumnFamily.java
    │
    └── ... (19 total action types)
```

### Data Flow for YAML Execution

```
1. Change File Discovery
   └─> 0001.create_users.DEV.apply.mybigtable.yml

2. YAML Parsing (SnakeYAML)
   └─> Map<String, Object> { "CreateTable" → {...} }

3. Variable Substitution (ExpressionEvaluator)
   └─> Recursively replace ${VAR} in all strings

4. Action Factory Lookup
   └─> ActionFactory.getAction("CreateTable", config, context)

5. Jackson Deserialization
   └─> OBJECT_MAPPER.convertValue(config, CreateTable.class)

6. Action Execution
   └─> createTable.execute(bigtableTableAdminClient)

7. BigTable API Call
   └─> adminClient.createTable(createTableRequest)
```

---

## Development Setup

### Prerequisites

- **Java 11+** (Java 17 recommended)
- **Maven 3.6+**
- **IntelliJ IDEA** (recommended) or any Java IDE
- **Google Cloud SDK** (for testing)
- **Git**

### Clone and Build

```bash
# Clone repository
git clone https://github.com/totalschema/totalschema.git
cd totalschema

# Build entire project
mvn clean install

# Build only BigTable connector
cd totalschema-connectors/totalschema-connectors-gcp/totalschema-connector-gcp-bigtable
mvn clean install

# Skip SpotBugs for faster iteration
mvn clean install -Dspotbugs.skip=true

# Run tests only
mvn test

# Run specific test
mvn test -Dtest=ActionFactoryTest
```

### IDE Setup

#### IntelliJ IDEA

1. **Import Project** — File → Open → Select `pom.xml`
2. **Configure SDK** — File → Project Structure → Project SDK: Java 11+
3. **Enable Annotation Processing** — Settings → Build → Compiler → Annotation Processors → Enable
4. **Code Style** — Import `configuration/intellij-code-style.xml` (if available)
5. **Run Configuration** — Create TestNG run configuration for tests

#### Eclipse

1. **Import Maven Project** — File → Import → Maven → Existing Maven Projects
2. **Configure JDK** — Project → Properties → Java Build Path → JRE System Library: Java 11+
3. **TestNG Plugin** — Help → Eclipse Marketplace → Install TestNG

### Running Tests Locally

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=ActionFactoryTest

# Specific test method
mvn test -Dtest=ActionFactoryTest#testCreateTableDeserialization

# With debugging
mvn test -Dmaven.surefire.debug

# Generate test report
mvn surefire-report:report
open target/site/surefire-report.html
```

---

## Adding New Actions

See [ADDING_ACTIONS.md](ADDING_ACTIONS.md) for a complete step-by-step guide.

### Quick Overview

1. **Create Action POJO** — Implements `Action<BigtableTableAdminClient>`
2. **Create Factory** — Extends `ActionFactory<YourAction>`
3. **Register Factory** — Add to `META-INF/services`
4. **Write Tests** — Unit tests for deserialization and execution
5. **Add Example** — Create example YAML in `examples/`
6. **Update Documentation** — Add to user manual and README

### Minimal Example

**Action POJO:**
```java
package io.github.totalschema.connector.gcp.bigtable.actions.youraction;

public class YourAction implements Action<BigtableTableAdminClient> {
    private String tableId;
    
    @Override
    public void execute(BigtableTableAdminClient adminClient) {
        // Implementation
    }
    
    // Getters/setters for Jackson
    public String getTableId() { return tableId; }
    public void setTableId(String tableId) { this.tableId = tableId; }
}
```

**Factory:**
```java
package io.github.totalschema.connector.gcp.bigtable.actions.youraction;

public class YourActionFactory extends ActionFactory<YourAction> {
    public YourActionFactory() {
        super(YourAction.class);
    }
}
```

**ServiceLoader Registration:**
```
# META-INF/services/io.github.totalschema.connector.gcp.bigtable.actions.ActionFactory
io.github.totalschema.connector.gcp.bigtable.actions.youraction.YourActionFactory
```

---

## Testing

See [TESTING_GUIDE.md](TESTING_GUIDE.md) for comprehensive testing guidelines.

### Test Categories

1. **Unit Tests** — Action factories, data binding, validation
2. **Integration Tests** — End-to-end workflows with mocked BigTable
3. **Service Tests** — BigTable API interactions (mocked)

### Test Structure

```java
@Test
public void testYourActionDeserialization() {
    // Given: YAML configuration
    Map<String, Object> config = new HashMap<>();
    Map<String, Object> actionConfig = new HashMap<>();
    actionConfig.put("tableId", "test-table");
    config.put("YourAction", actionConfig);
    
    Context context = createMockContext();
    
    // When: Deserialize action
    Action<?> action = ActionFactory.getAction(config, context);
    
    // Then: Verify correct type and properties
    assertTrue(action instanceof YourAction);
    YourAction yourAction = (YourAction) action;
    assertEquals("test-table", yourAction.getTableId());
}
```

### Running Tests

```bash
# All tests
mvn test

# With coverage report
mvn clean test jacoco:report
open target/site/jacoco/index.html

# Integration tests only
mvn test -Dgroups=integration

# Unit tests only
mvn test -Dgroups=unit
```

---

## Code Patterns

See [CODE_PATTERNS.md](CODE_PATTERNS.md) for detailed patterns and conventions.

### Common Patterns

#### 1. Action Interface Implementation

All actions implement the `Action<T>` interface:

```java
public interface Action<T> {
    void execute(T client);
}
```

For table operations, use `TableAction` marker:
```java
public class CreateTable implements TableAction {
    // ...
}
```

For instance operations, use `InstanceAction` marker:
```java
public class CreateInstance implements InstanceAction {
    // ...
}
```

#### 2. Jackson Data Binding

Always provide getters and setters for Jackson:

```java
private String tableId;
private List<String> columnFamilies;

public String getTableId() { return tableId; }
public void setTableId(String tableId) { this.tableId = tableId; }

public List<String> getColumnFamilies() { return columnFamilies; }
public void setColumnFamilies(List<String> columnFamilies) { 
    this.columnFamilies = columnFamilies; 
}
```

#### 3. Validation

Validate in the `execute()` method, not constructor:

```java
@Override
public void execute(BigtableTableAdminClient adminClient) {
    if (tableId == null || tableId.isEmpty()) {
        throw new IllegalArgumentException("tableId is required");
    }
    
    // ... implementation
}
```

#### 4. Error Messages

Provide clear, actionable error messages:

```java
throw new IllegalArgumentException(
    "Cannot specify both 'union' and 'intersection' for family: " + familyName
);
```

#### 5. Logging

Use SLF4J for logging:

```java
private static final Logger logger = LoggerFactory.getLogger(YourAction.class);

@Override
public void execute(BigtableTableAdminClient adminClient) {
    logger.info("Creating table: {}", tableId);
    // ...
    logger.debug("Table created successfully: {}", tableId);
}
```

---

## Contributing

### Contribution Workflow

1. **Fork Repository** — Fork on GitHub
2. **Create Branch** — `git checkout -b feature/your-feature`
3. **Make Changes** — Follow code patterns and conventions
4. **Write Tests** — Add unit and integration tests
5. **Run Tests** — `mvn test`
6. **Format Code** — `mvn spotless:apply`
7. **Commit Changes** — Use conventional commits
8. **Push Branch** — `git push origin feature/your-feature`
9. **Create PR** — Submit pull request on GitHub

### Code Quality Standards

- **Code Coverage** — Aim for 80%+ coverage for new code
- **SpotBugs** — Must pass with no violations (run `mvn spotbugs:check`)
- **Spotless** — Code must be formatted (run `mvn spotless:check`)
- **JavaDoc** — Public APIs must have JavaDoc comments
- **Tests** — All new actions must have tests

### Commit Message Convention

Use conventional commits format:

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:** `feat`, `fix`, `docs`, `test`, `refactor`, `chore`

**Examples:**
```
feat(bigtable): add SetTableSplits action

Implements pre-split configuration for tables to optimize
initial data distribution.

Closes #123
```

```
fix(bigtable): correct GC rule union deserialization

Union rules were incorrectly deserializing when maxAge
was specified without maxVersions.
```

### Pull Request Guidelines

- **Title** — Clear, concise description of change
- **Description** — Explain what, why, and how
- **Tests** — Include test coverage report
- **Documentation** — Update relevant docs
- **Examples** — Add example YAML if applicable
- **Breaking Changes** — Clearly document any breaking changes

---

## Resources

### Documentation

- **[User Manual](../user_manual/README.md)** — Complete user guide
- **[AGENTS.md](../../AGENTS.md)** — AI agent guide
- **[ARCHITECTURE.md](ARCHITECTURE.md)** — Detailed architecture
- **[ADDING_ACTIONS.md](ADDING_ACTIONS.md)** — Step-by-step action guide
- **[TESTING_GUIDE.md](TESTING_GUIDE.md)** — Testing best practices
- **[CODE_PATTERNS.md](CODE_PATTERNS.md)** — Code patterns and conventions

### External Resources

- **[TotalSchema Documentation](../../../../docs/)** — Core documentation
- **[BigTable Documentation](https://cloud.google.com/bigtable/docs)** — GCP BigTable docs
- **[Jackson Documentation](https://github.com/FasterXML/jackson-docs)** — Jackson data binding
- **[ServiceLoader Guide](https://docs.oracle.com/javase/11/docs/api/java.base/java/util/ServiceLoader.html)** — Java ServiceLoader

### Getting Help

- **GitHub Issues** — https://github.com/totalschema/totalschema/issues
- **Discussions** — https://github.com/totalschema/totalschema/discussions
- **Stack Overflow** — Tag: `totalschema`

---

## Quick Reference

### Build Commands

```bash
mvn clean install              # Full build
mvn clean install -DskipTests  # Build without tests
mvn test                       # Run tests
mvn spotless:apply             # Format code
mvn spotbugs:check             # Run SpotBugs
```

### Test Commands

```bash
mvn test -Dtest=ActionFactoryTest                    # Specific test class
mvn test -Dtest=ActionFactoryTest#testMethodName     # Specific test method
mvn test -Dgroups=integration                        # Integration tests only
```

### Code Quality

```bash
mvn spotless:check             # Check formatting
mvn spotless:apply             # Auto-format code
mvn spotbugs:check             # Static analysis
mvn spotbugs:gui               # View SpotBugs report (GUI)
```

---

**Last Updated:** July 31, 2026  
**Maintainers:** TotalSchema Development Team

