# BigTable Connector - Code Patterns

This document describes common code patterns, conventions, and best practices used in the BigTable connector codebase.

## Table of Contents

- [Naming Conventions](#naming-conventions)
- [Package Structure](#package-structure)
- [Action Patterns](#action-patterns)
- [Validation Patterns](#validation-patterns)
- [Error Handling](#error-handling)
- [Logging Patterns](#logging-patterns)
- [Jackson Binding](#jackson-binding)
- [Builder Patterns](#builder-patterns)
- [Immutability](#immutability)
- [Documentation](#documentation)
- [Code Style](#code-style)

---

## Naming Conventions

### Classes

- **Action Classes:** PascalCase, descriptive verb+noun (e.g., `CreateTable`, `ModifyColumnFamily`)
- **Factory Classes:** Action name + `ActionFactory` (e.g., `CreateTableActionFactory`)
- **Helper Classes:** Descriptive nouns (e.g., `ColumnFamily`, `GcRule`)
- **Test Classes:** Class name + `Test` (e.g., `CreateTableTest`)

### Methods

- **Actions:** `execute(BigtableTableAdminClient adminClient)`
- **Validation:** `validate()`, `validateXyz()`
- **Builders:** `buildGcRule()`, `buildRequest()`
- **Getters/Setters:** Standard JavaBeans convention

### Variables

- **Local variables:** camelCase, descriptive (e.g., `adminClient`, `createTableRequest`)
- **Instance fields:** camelCase, descriptive (e.g., `tableId`, `columnFamilies`)
- **Constants:** UPPER_SNAKE_CASE (e.g., `MAX_RETRIES`, `DEFAULT_TIMEOUT`)

### Packages

- **Package names:** lowercase, no underscores (e.g., `createtable`, `modifycolumnfamily`)
- **Base package:** `io.github.totalschema.connector.gcp.bigtable.actions`
- **Action packages:** `actions.<actionname>` (e.g., `actions.createtable`)

---

## Package Structure

### Per-Action Sub-Package Pattern

Each action lives in its own package:

```
actions/
├── createtable/
│   ├── CreateTableActionFactory.java
│   ├── CreateTable.java
│   ├── ColumnFamily.java
│   └── GcRule.java
│
├── deletetable/
│   ├── DeleteTableActionFactory.java
│   └── DeleteTable.java
│
└── createcolumnfamily/
    ├── CreateColumnFamilyActionFactory.java
    └── CreateColumnFamily.java
```

**Benefits:**
- Clear ownership
- Easy navigation
- Self-contained modules
- Scales well

**When to Share Classes:**

If multiple actions use the same helper class, move to parent package:

```
actions/
├── ColumnFamily.java          # Shared by multiple actions
├── GcRule.java                # Shared by multiple actions
│
├── createtable/
│   ├── CreateTableActionFactory.java
│   └── CreateTable.java       # Uses shared ColumnFamily
│
└── createcolumnfamily/
    ├── CreateColumnFamilyActionFactory.java
    └── CreateColumnFamily.java  # Uses shared ColumnFamily
```

---

## Action Patterns

### Standard Action Template

```java
package io.github.totalschema.connector.gcp.bigtable.actions.youraction;

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import io.github.totalschema.connector.gcp.bigtable.actions.table.TableAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Brief description of what this action does.
 *
 * <p>More detailed explanation if needed.
 *
 * <p>Example YAML:
 * <pre>
 * YourAction:
 *   tableId: users
 *   otherParam: value
 * </pre>
 *
 * @see BigtableTableAdminClient#relevantMethod(String)
 */
public class YourAction implements TableAction {

    private static final Logger logger = LoggerFactory.getLogger(YourAction.class);

    // Instance fields (configured from YAML)
    private String tableId;
    private String otherParam;

    @Override
    public void execute(BigtableTableAdminClient adminClient) {
        // 1. Validate inputs
        validate();

        // 2. Log intent
        logger.info("Executing action: tableId={}, otherParam={}", tableId, otherParam);

        // 3. Build request
        var request = buildRequest();

        // 4. Execute API call
        try {
            adminClient.someMethod(request);
            logger.debug("Action completed successfully");
        } catch (Exception e) {
            logger.error("Action failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to execute action", e);
        }
    }

    private void validate() {
        if (tableId == null || tableId.isEmpty()) {
            throw new IllegalArgumentException("tableId is required");
        }
        // Additional validation...
    }

    private SomeRequest buildRequest() {
        return SomeRequest.newBuilder()
                .setTableId(tableId)
                .setOtherParam(otherParam)
                .build();
    }

    // Jackson requires getters and setters
    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public String getOtherParam() {
        return otherParam;
    }

    public void setOtherParam(String otherParam) {
        this.otherParam = otherParam;
    }

    @Override
    public String toString() {
        return "YourAction{" +
                "tableId='" + tableId + '\'' +
                ", otherParam='" + otherParam + '\'' +
                '}';
    }
}
```

### Factory Template

```java
package io.github.totalschema.connector.gcp.bigtable.actions.youraction;

import io.github.totalschema.connector.gcp.bigtable.actions.ActionFactory;

/**
 * Factory for creating {@link YourAction} instances from YAML configuration.
 *
 * <p>Registered via ServiceLoader. Handles action type "YourAction".
 */
public class YourActionFactory extends ActionFactory<YourAction> {

    /**
     * Creates a new YourActionFactory.
     *
     * <p>The action type "YourAction" is derived from the class name
     * passed to the superclass constructor.
     */
    public YourActionFactory() {
        super(YourAction.class);
    }
}
```

**Key Points:**
- Minimal code (~7 lines)
- Clear JavaDoc
- Correct class passed to super

---

## Validation Patterns

### Basic Validation

```java
private void validate() {
    if (tableId == null || tableId.isEmpty()) {
        throw new IllegalArgumentException("tableId is required");
    }
}
```

### Multiple Field Validation

```java
private void validate() {
    List<String> errors = new ArrayList<>();
    
    if (tableId == null || tableId.isEmpty()) {
        errors.add("tableId is required");
    }
    
    if (role == null || !role.startsWith("roles/")) {
        errors.add("role must start with 'roles/', got: " + role);
    }
    
    if (member == null || member.isEmpty()) {
        errors.add("member is required");
    }
    
    if (!errors.isEmpty()) {
        throw new IllegalArgumentException(
            "Validation failed: " + String.join(", ", errors)
        );
    }
}
```

### Range Validation

```java
private void validate() {
    if (tableId == null || tableId.isEmpty()) {
        throw new IllegalArgumentException("tableId is required");
    }
    
    if (minNodes < 1 || minNodes > maxNodes) {
        throw new IllegalArgumentException(
            String.format("minNodes must be >= 1 and <= maxNodes, got: %d", minNodes)
        );
    }
    
    if (cpuTargetPercent < 1 || cpuTargetPercent > 99) {
        throw new IllegalArgumentException(
            String.format("cpuTargetPercent must be 1-99, got: %d", cpuTargetPercent)
        );
    }
}
```

### Conditional Validation

```java
private void validate() {
    if (tableId == null || tableId.isEmpty()) {
        throw new IllegalArgumentException("tableId is required");
    }
    
    // Either backupId OR sourceTableId must be specified
    if ((backupId == null || backupId.isEmpty()) && 
        (sourceTableId == null || sourceTableId.isEmpty())) {
        throw new IllegalArgumentException(
            "Either backupId or sourceTableId must be specified"
        );
    }
    
    // Cannot specify both
    if (backupId != null && sourceTableId != null) {
        throw new IllegalArgumentException(
            "Cannot specify both backupId and sourceTableId"
        );
    }
}
```

---

## Error Handling

### Action Execution Errors

```java
@Override
public void execute(BigtableTableAdminClient adminClient) {
    validate();
    
    logger.info("Creating table: {}", tableId);
    
    try {
        var request = buildCreateTableRequest();
        adminClient.createTable(request);
        logger.info("Table created successfully: {}", tableId);
        
    } catch (AlreadyExistsException e) {
        logger.warn("Table already exists: {}", tableId);
        throw new IllegalStateException(
            "Table '" + tableId + "' already exists", e
        );
        
    } catch (Exception e) {
        logger.error("Failed to create table: {}", tableId, e);
        throw new RuntimeException(
            "Failed to create table '" + tableId + "': " + e.getMessage(), e
        );
    }
}
```

### Validation Errors

```java
private void validate() {
    if (tableId == null || tableId.isEmpty()) {
        throw new IllegalArgumentException(
            "tableId is required. Example: 'users'"
        );
    }
    
    if (!tableId.matches("[a-z0-9_-]+")) {
        throw new IllegalArgumentException(
            "tableId must contain only lowercase letters, numbers, underscores, " +
            "and hyphens. Got: '" + tableId + "'"
        );
    }
}
```

### Contextual Error Messages

```java
private void validateGcRules(ColumnFamily family) {
    if (family.getUnion() != null && family.getIntersection() != null) {
        throw new IllegalArgumentException(
            String.format(
                "Cannot specify both 'union' and 'intersection' for family: %s. " +
                "Choose one rule type.",
                family.getName()
            )
        );
    }
    
    if (family.getUnion() != null && family.getUnion().isEmpty()) {
        throw new IllegalArgumentException(
            String.format(
                "Union rule must have at least one rule for family: %s. " +
                "Add maxVersions or maxAge rules.",
                family.getName()
            )
        );
    }
}
```

---

## Logging Patterns

### Logger Declaration

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YourAction implements TableAction {
    private static final Logger logger = LoggerFactory.getLogger(YourAction.class);
    // ...
}
```

### Log Levels

**INFO:** User-facing operations
```java
logger.info("Creating table: {}", tableId);
logger.info("Table created successfully: {}", tableId);
```

**DEBUG:** Implementation details
```java
logger.debug("Built create table request: {}", request);
logger.debug("GC rule configuration: maxVersions={}, maxAge={}", maxVersions, maxAge);
```

**WARN:** Recoverable issues
```java
logger.warn("Table already exists, skipping creation: {}", tableId);
logger.warn("No GC rules specified, using BigTable defaults");
```

**ERROR:** Failures
```java
logger.error("Failed to create table: {}", tableId, exception);
logger.error("BigTable API call failed: {}", exception.getMessage(), exception);
```

### Structured Logging

```java
// Good: Structured parameters
logger.info("Processing action: table={}, families={}, gcRules={}", 
    tableId, familyCount, hasGcRules);

// Bad: String concatenation
logger.info("Processing action for table " + tableId + " with " + familyCount + " families");
```

### Performance Considerations

```java
// Expensive operation - guard with isDebugEnabled()
if (logger.isDebugEnabled()) {
    logger.debug("Request details: {}", buildDetailedRequestString());
}

// Simple operation - no guard needed
logger.debug("Processing table: {}", tableId);
```

---

## Jackson Binding

### Simple Properties

```java
private String tableId;
private int maxVersions;
private boolean enabled;

// Jackson requires getters and setters
public String getTableId() { return tableId; }
public void setTableId(String tableId) { this.tableId = tableId; }

public int getMaxVersions() { return maxVersions; }
public void setMaxVersions(int maxVersions) { this.maxVersions = maxVersions; }

public boolean isEnabled() { return enabled; }  // Note: "is" for boolean
public void setEnabled(boolean enabled) { this.enabled = enabled; }
```

### Collections

```java
private List<String> members;
private List<ColumnFamily> families;

public List<String> getMembers() { return members; }
public void setMembers(List<String> members) { this.members = members; }

public List<ColumnFamily> getFamilies() { return families; }
public void setFamilies(List<ColumnFamily> families) { this.families = families; }
```

### Nested Objects

```java
public class CreateTable {
    private String tableId;
    private List<ColumnFamily> families;
    
    // getters/setters
}

public class ColumnFamily {
    private String name;
    private Integer maxVersions;
    private String maxAge;
    
    // getters/setters
}
```

Jackson handles nesting automatically.

### Enums

```java
public enum ConsistencyMode {
    EVENTUAL,
    STRONG
}

public class EnableTableReplication {
    private String tableId;
    private ConsistencyMode consistencyMode;
    
    // getters/setters
}
```

YAML:
```yaml
EnableTableReplication:
  tableId: users
  consistencyMode: EVENTUAL  # String in YAML, enum in Java
```

### Optional Fields with Defaults

```java
private int maxRetries = 3;  // Default value
private boolean overwrite = false;  // Default value

public int getMaxRetries() { return maxRetries; }
public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

public boolean isOverwrite() { return overwrite; }
public void setOverwrite(boolean overwrite) { this.overwrite = overwrite; }
```

If not in YAML, defaults are used.

### Custom Property Names

If YAML name differs from Java property:

```java
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonProperty("table_id")  // YAML uses snake_case
private String tableId;     // Java uses camelCase

public String getTableId() { return tableId; }
public void setTableId(String tableId) { this.tableId = tableId; }
```

---

## Builder Patterns

### Request Building

```java
private CreateTableRequest buildCreateTableRequest() {
    CreateTableRequest request = CreateTableRequest.of(tableId);
    
    if (families != null) {
        for (ColumnFamily family : families) {
            if (family.hasGcRules()) {
                GCRules.GCRule gcRule = family.buildGcRule();
                request.addFamily(family.getName(), gcRule);
            } else {
                request.addFamily(family.getName());
            }
        }
    }
    
    return request;
}
```

### GC Rule Building

```java
public GCRules.GCRule buildGcRule() {
    // Union rule
    if (union != null && !union.isEmpty()) {
        GCRules.UnionRule.Builder builder = GCRules.GCRULES.union();
        for (GcRule rule : union) {
            builder.rule(rule.buildGcRule());
        }
        return builder.build();
    }
    
    // Intersection rule (explicit)
    if (intersection != null && !intersection.isEmpty()) {
        GCRules.IntersectionRule.Builder builder = GCRules.GCRULES.intersection();
        for (GcRule rule : intersection) {
            builder.rule(rule.buildGcRule());
        }
        return builder.build();
    }
    
    // Implicit intersection (both maxVersions and maxAge)
    if (maxVersions != null && maxAge != null) {
        return GCRules.GCRULES.intersection()
            .rule(GCRules.GCRULES.maxVersions(maxVersions))
            .rule(GCRules.GCRULES.maxAge(parseDuration(maxAge)))
            .build();
    }
    
    // Single rules
    if (maxVersions != null) {
        return GCRules.GCRULES.maxVersions(maxVersions);
    }
    
    if (maxAge != null) {
        return GCRules.GCRULES.maxAge(parseDuration(maxAge));
    }
    
    return null;  // No GC rules
}
```

### IAM Policy Building

```java
private Policy buildUpdatedPolicy(Policy currentPolicy) {
    Policy.Builder builder = currentPolicy.toBuilder();
    
    for (IamBinding binding : bindings) {
        boolean roleExists = false;
        
        // Find existing role
        for (int i = 0; i < builder.getBindingsCount(); i++) {
            Binding existingBinding = builder.getBindings(i);
            if (existingBinding.getRole().equals(binding.getRole())) {
                // Add new members to existing role
                Binding.Builder bindingBuilder = existingBinding.toBuilder();
                for (String member : binding.getMembers()) {
                    if (!existingBinding.getMembersList().contains(member)) {
                        bindingBuilder.addMembers(member);
                    }
                }
                builder.setBindings(i, bindingBuilder.build());
                roleExists = true;
                break;
            }
        }
        
        // Create new role binding
        if (!roleExists) {
            Binding newBinding = Binding.newBuilder()
                .setRole(binding.getRole())
                .addAllMembers(binding.getMembers())
                .build();
            builder.addBindings(newBinding);
        }
    }
    
    return builder.build();
}
```

---

## Immutability

### Immutable Configuration

```java
public class YourAction implements TableAction {
    // Fields are set once during deserialization, then never modified
    private String tableId;
    private List<String> members;
    
    @Override
    public void execute(BigtableTableAdminClient adminClient) {
        // Never modify fields here
        // Build new objects instead
        var request = buildRequest();  // New object
        adminClient.doSomething(request);
    }
    
    // Getters return fields as-is (they're only set once)
    public String getTableId() {
        return tableId;
    }
    
    // Setter used by Jackson during deserialization only
    public void setTableId(String tableId) {
        this.tableId = tableId;
    }
}
```

### Defensive Copies for Collections

```java
private List<String> members;

public List<String> getMembers() {
    // Return unmodifiable view to prevent external modification
    return members == null ? null : Collections.unmodifiableList(members);
}

public void setMembers(List<String> members) {
    // Store defensive copy
    this.members = members == null ? null : new ArrayList<>(members);
}
```

**Note:** This is optional and only needed if immutability is critical.

---

## Documentation

### JavaDoc Template

```java
/**
 * Brief one-line description of what this class does.
 *
 * <p>More detailed explanation paragraph. Explain the purpose, when to use it,
 * and any important constraints or behaviors.
 *
 * <p>Example YAML configuration:
 * <pre>
 * YourAction:
 *   tableId: users
 *   parameter: value
 * </pre>
 *
 * <p>Additional notes about behavior, limitations, or warnings.
 *
 * @see RelatedClass
 * @see BigtableTableAdminClient#relevantMethod(String)
 */
public class YourAction implements TableAction {
    // ...
}
```

### Method Documentation

```java
/**
 * Validates all input parameters.
 *
 * @throws IllegalArgumentException if any parameter is invalid
 */
private void validate() {
    // ...
}

/**
 * Builds a CreateTableRequest from this action's configuration.
 *
 * @return the configured request, never null
 */
private CreateTableRequest buildRequest() {
    // ...
}
```

### Field Documentation

```java
/** The BigTable table ID. Required. */
private String tableId;

/** List of column families to create. Optional. */
private List<ColumnFamily> families;

/** Maximum number of cell versions to retain. Optional, defaults to 1. */
private int maxVersions = 1;
```

---

## Code Style

### Formatting

Use TotalSchema's Spotless configuration:

```bash
# Check formatting
mvn spotless:check

# Auto-format
mvn spotless:apply
```

### Line Length

- **Prefer:** 100 characters
- **Maximum:** 120 characters
- **Break long lines** at logical points

### Imports

- **Organize imports** by groups:
  1. `java.*`
  2. `javax.*`
  3. Third-party libraries
  4. `io.github.totalschema.*`
- **No wildcard imports** (`import java.util.*`)
- **Static imports** last

### Braces

```java
// Always use braces, even for single-line blocks
if (condition) {
    doSomething();
}

// Not:
if (condition)
    doSomething();
```

### Naming

- **Constants:** `UPPER_SNAKE_CASE`
- **Variables:** `camelCase`
- **Classes:** `PascalCase`
- **Methods:** `camelCase`

### Comments

```java
// Single-line comments use //

/*
 * Multi-line comments use this format.
 * Each line starts with *.
 */

/**
 * JavaDoc comments use this format.
 * For classes, methods, and public APIs.
 */
```

---

**Last Updated:** July 31, 2026  
**Maintainers:** TotalSchema Development Team

