# BigTable Connector - Testing Guide

Comprehensive guide for testing the BigTable connector, covering unit tests, integration tests, mocking strategies, and best practices.

## Table of Contents

- [Overview](#overview)
- [Testing Framework](#testing-framework)
- [Test Categories](#test-categories)
- [Unit Testing](#unit-testing)
- [Integration Testing](#integration-testing)
- [Mocking Strategies](#mocking-strategies)
- [Test Data Management](#test-data-management)
- [Coverage Goals](#coverage-goals)
- [Best Practices](#best-practices)
- [Common Patterns](#common-patterns)
- [Troubleshooting](#troubleshooting)

---

## Overview

The BigTable connector uses a layered testing strategy:

1. **Unit Tests** — Test individual components in isolation
2. **Integration Tests** — Test component interactions
3. **Service Tests** — Test BigTable API interactions (mocked)

**Testing Philosophy:**

- **Fast** — Tests run in milliseconds
- **Isolated** — No external dependencies
- **Deterministic** — Same input always produces same output
- **Readable** — Clear test names and assertions

---

## Testing Framework

### Stack

- **TestNG** — Testing framework (not JUnit)
- **EasyMock** — Mocking framework
- **Mockito** — Additional mocking (for specific cases)
- **AssertJ** — Fluent assertions (optional)

### Why TestNG?

TotalSchema uses TestNG for:
- Flexible test configuration
- Better parallel execution
- Data providers
- Dependency injection support
- Consistent with core project

---

## Test Categories

### Unit Tests

**Purpose:** Test single class in isolation

**Characteristics:**
- No external dependencies
- Use mocks for dependencies
- Fast (< 10ms per test)
- High coverage (aim for 90%+)

**Example:**
```java
@Test
public void testCreateTableDeserialization() {
    // Test ActionFactory deserializes YAML correctly
}
```

### Integration Tests

**Purpose:** Test multiple components working together

**Characteristics:**
- Multiple classes interact
- May use in-memory databases
- Medium speed (10-100ms per test)
- Moderate coverage (60%+)

**Example:**
```java
@Test
public void testYmlExecutorWithVariableSubstitution() {
    // Test executor + action factory + variable evaluation
}
```

### Service Tests

**Purpose:** Test BigTable API interactions

**Characteristics:**
- Mock BigTable admin clients
- Verify correct API calls
- Test error handling
- Medium speed (10-50ms per test)

**Example:**
```java
@Test
public void testCreateTableApiCall() {
    // Test CreateTable calls adminClient.createTable() with correct params
}
```

---

## Unit Testing

### Testing Action Deserialization

**Pattern:**

```java
@Test
public void testActionDeserialization() {
    // Given: YAML configuration as Map
    Map<String, Object> config = new HashMap<>();
    Map<String, Object> actionConfig = new HashMap<>();
    actionConfig.put("tableId", "test-table");
    actionConfig.put("families", createFamilies());
    config.put("CreateTable", actionConfig);
    
    Context context = createMockContext();
    
    // When: Deserialize action
    Action<?> action = ActionFactory.getAction(config, context);
    
    // Then: Verify correct type and properties
    assertTrue(action instanceof CreateTable);
    CreateTable createTable = (CreateTable) action;
    assertEquals("test-table", createTable.getTableId());
    assertNotNull(createTable.getFamilies());
}

private Context createMockContext() {
    Context context = createMock(Context.class);
    ExpressionEvaluator evaluator = createMock(ExpressionEvaluator.class);
    VariableService variableService = createMock(VariableService.class);
    
    expect(context.get(ExpressionEvaluator.class))
        .andReturn(evaluator).anyTimes();
    expect(context.get(VariableService.class))
        .andReturn(variableService).anyTimes();
    expect(variableService.getEffectiveVariables(context))
        .andReturn(new HashMap<>()).anyTimes();
    expect(evaluator.evaluate(anyString(), anyObject()))
        .andAnswer(() -> getCurrentArguments()[0])  // Return input unchanged
        .anyTimes();
    
    replay(context, evaluator, variableService);
    return context;
}
```

### Testing Action Execution

**Pattern:**

```java
@Test
public void testActionExecution() {
    // Given: Action with valid configuration
    CreateTable action = new CreateTable();
    action.setTableId("test-table");
    
    // Mock admin client
    BigtableTableAdminClient adminClient = createMock(BigtableTableAdminClient.class);
    
    // Expect specific method call
    CreateTableRequest expectedRequest = CreateTableRequest.of("test-table");
    adminClient.createTable(expectedRequest);
    expectLastCall();
    
    replay(adminClient);
    
    // When: Execute action
    action.execute(adminClient);
    
    // Then: Verify admin client was called
    verify(adminClient);
}
```

### Testing Validation

**Pattern:**

```java
@Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*tableId is required.*")
public void testValidationTableIdRequired() {
    CreateTable action = new CreateTable();
    // Don't set tableId
    
    BigtableTableAdminClient adminClient = createMock(BigtableTableAdminClient.class);
    
    // Should throw before calling admin client
    action.execute(adminClient);
}

@Test(expectedExceptions = IllegalArgumentException.class)
public void testValidationEmptyTableId() {
    CreateTable action = new CreateTable();
    action.setTableId("");  // Empty string
    
    BigtableTableAdminClient adminClient = createMock(BigtableTableAdminClient.class);
    action.execute(adminClient);
}
```

### Testing Complex Objects

**Pattern:**

```java
@Test
public void testGcRuleDeserialization() {
    // Given: YAML with nested GC rules
    Map<String, Object> config = new HashMap<>();
    Map<String, Object> actionConfig = new HashMap<>();
    actionConfig.put("tableId", "test-table");
    
    List<Map<String, Object>> families = new ArrayList<>();
    Map<String, Object> family = new HashMap<>();
    family.put("name", "profile");
    family.put("maxVersions", 3);
    family.put("maxAge", "7d");
    families.add(family);
    
    actionConfig.put("families", families);
    config.put("CreateTable", actionConfig);
    
    Context context = createMockContext();
    
    // When: Deserialize
    Action<?> action = ActionFactory.getAction(config, context);
    
    // Then: Verify nested structure
    CreateTable createTable = (CreateTable) action;
    assertEquals(1, createTable.getFamilies().size());
    
    ColumnFamily cf = createTable.getFamilies().get(0);
    assertEquals("profile", cf.getName());
    assertEquals(Integer.valueOf(3), cf.getMaxVersions());
    assertEquals("7d", cf.getMaxAge());
}
```

---

## Integration Testing

### Testing End-to-End Workflows

**Pattern:**

```java
@Test
public void testYmlExecutorEndToEnd() throws IOException {
    // Given: Temp YAML file
    Path tempFile = Files.createTempFile("test", ".yml");
    String yaml = "CreateTable:\n" +
                  "  tableId: test-table\n" +
                  "  families:\n" +
                  "    - name: cf1\n";
    Files.writeString(tempFile, yaml);
    
    // Create real executor with mocked admin client
    Configuration config = createConfiguration();
    Context context = createRealContext();
    
    BigtableTableAdminClient adminClient = createMock(BigtableTableAdminClient.class);
    adminClient.createTable(anyObject(CreateTableRequest.class));
    expectLastCall();
    replay(adminClient);
    
    context.setValue(BigtableTableAdminClient.class, adminClient);
    
    YmlBigTableChangeExecutor executor = new YmlBigTableChangeExecutor(
        "test", config, context
    );
    
    ChangeFile changeFile = createChangeFile(tempFile);
    
    // When: Execute
    executor.execute(changeFile, context);
    
    // Then: Verify execution
    verify(adminClient);
    
    // Cleanup
    Files.delete(tempFile);
}
```

### Testing Variable Substitution

**Pattern:**

```java
@Test
public void testVariableSubstitutionInYaml() {
    // Given: YAML with variables
    Map<String, Object> config = new HashMap<>();
    Map<String, Object> actionConfig = new HashMap<>();
    actionConfig.put("tableId", "${TABLE_NAME}");
    config.put("CreateTable", actionConfig);
    
    // Context with variables
    Context context = createMock(Context.class);
    ExpressionEvaluator evaluator = createMock(ExpressionEvaluator.class);
    VariableService variableService = createMock(VariableService.class);
    
    Map<String, String> variables = new HashMap<>();
    variables.put("TABLE_NAME", "resolved-table");
    
    expect(context.get(ExpressionEvaluator.class)).andReturn(evaluator).anyTimes();
    expect(context.get(VariableService.class)).andReturn(variableService).anyTimes();
    expect(variableService.getEffectiveVariables(context)).andReturn(variables);
    expect(evaluator.evaluate("${TABLE_NAME}", variables)).andReturn("resolved-table");
    
    replay(context, evaluator, variableService);
    
    // When: Deserialize with variable substitution
    Action<?> action = ActionFactory.getAction(config, context);
    
    // Then: Variable should be substituted
    CreateTable createTable = (CreateTable) action;
    assertEquals("resolved-table", createTable.getTableId());
    
    verify(context, evaluator, variableService);
}
```

---

## Mocking Strategies

### EasyMock Basics

```java
// Create mock
BigtableTableAdminClient adminClient = createMock(BigtableTableAdminClient.class);

// Set expectations
expect(adminClient.listTables()).andReturn(Arrays.asList("table1", "table2"));

// For void methods
adminClient.createTable(anyObject(CreateTableRequest.class));
expectLastCall();

// Replay (switch to replay mode)
replay(adminClient);

// Execute code under test
// ...

// Verify expectations were met
verify(adminClient);
```

### Matching Arguments

```java
// Exact match
expect(adminClient.getTable("users")).andReturn(table);

// Any value
expect(adminClient.getTable(anyString())).andReturn(table);

// Specific type
adminClient.createTable(anyObject(CreateTableRequest.class));

// Custom matcher
adminClient.createTable(matches(request -> request.getTableId().equals("users")));

// Capture argument
Capture<CreateTableRequest> capture = newCapture();
adminClient.createTable(capture(capture));
replay(adminClient);
// ... execute
assertEquals("users", capture.getValue().getTableId());
```

### Mock Return Values

```java
// Simple value
expect(adminClient.getTable("users")).andReturn(table);

// Throw exception
expect(adminClient.getTable("missing"))
    .andThrow(new NotFoundException("Table not found"));

// Answer with custom logic
expect(adminClient.getTable(anyString()))
    .andAnswer(() -> {
        String tableId = (String) getCurrentArguments()[0];
        return createTable(tableId);
    });

// Return different values on successive calls
expect(adminClient.getTable("users"))
    .andReturn(table1).times(1)
    .andReturn(table2).times(1);
```

### Mock Setup Helpers

```java
private Context createMockContext() {
    Context context = createMock(Context.class);
    ExpressionEvaluator evaluator = createMock(ExpressionEvaluator.class);
    VariableService variableService = createMock(VariableService.class);
    
    expect(context.get(ExpressionEvaluator.class))
        .andReturn(evaluator).anyTimes();
    expect(context.get(VariableService.class))
        .andReturn(variableService).anyTimes();
    expect(variableService.getEffectiveVariables(context))
        .andReturn(Collections.emptyMap()).anyTimes();
    expect(evaluator.evaluate(anyString(), anyObject()))
        .andAnswer(() -> getCurrentArguments()[0])
        .anyTimes();
    
    replay(context, evaluator, variableService);
    return context;
}

private BigtableTableAdminClient createMockAdminClient() {
    BigtableTableAdminClient adminClient = createMock(BigtableTableAdminClient.class);
    // Common setup
    expect(adminClient.exists(anyString())).andReturn(true).anyTimes();
    replay(adminClient);
    return adminClient;
}
```

---

## Test Data Management

### Test Data Builders

```java
public class TestDataBuilder {
    
    public static Map<String, Object> createTableConfig(String tableId) {
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("tableId", tableId);
        config.put("CreateTable", actionConfig);
        return config;
    }
    
    public static Map<String, Object> createTableWithFamilies(
            String tableId, String... familyNames) {
        Map<String, Object> config = createTableConfig(tableId);
        Map<String, Object> actionConfig = 
            (Map<String, Object>) config.get("CreateTable");
        
        List<Map<String, Object>> families = new ArrayList<>();
        for (String name : familyNames) {
            Map<String, Object> family = new HashMap<>();
            family.put("name", name);
            families.add(family);
        }
        actionConfig.put("families", families);
        
        return config;
    }
    
    public static ColumnFamily createColumnFamily(String name, int maxVersions) {
        ColumnFamily family = new ColumnFamily();
        family.setName(name);
        family.setMaxVersions(maxVersions);
        return family;
    }
}
```

### Test Data Files

For complex test data, use resource files:

```java
@Test
public void testComplexYaml() throws IOException {
    // Load YAML from test resources
    InputStream yamlStream = getClass()
        .getResourceAsStream("/test-data/complex-table.yml");
    
    Yaml yaml = new Yaml();
    Map<String, Object> config = yaml.load(yamlStream);
    
    Context context = createMockContext();
    Action<?> action = ActionFactory.getAction(config, context);
    
    // Assertions...
}
```

Put YAML files in `src/test/resources/test-data/`.

---

## Coverage Goals

### Target Coverage

- **Unit Tests:** 90%+ line coverage
- **Integration Tests:** 70%+ line coverage
- **Overall:** 80%+ line coverage

### Running Coverage Reports

```bash
# Generate coverage report
mvn clean test jacoco:report

# View report
open target/site/jacoco/index.html
```

### Coverage Exclusions

Some code doesn't need coverage:

- **toString()** methods
- **Getters/setters** (unless they have logic)
- **Main methods**
- **Exception constructors**

Mark with comment:
```java
// Coverage: excluded - simple getter
public String getTableId() {
    return tableId;
}
```

---

## Best Practices

### Test Naming

Use descriptive names:

**Good:**
```java
@Test
public void testCreateTableWithMaxVersionsGcRule()

@Test
public void testValidationThrowsWhenTableIdIsNull()

@Test
public void testUnionRuleWithTwoConditions()
```

**Bad:**
```java
@Test
public void test1()

@Test
public void testCreateTable()

@Test
public void testGcRule()
```

### Test Organization

```java
public class CreateTableTest {
    
    // Deserialization tests
    @Test
    public void testDeserializationSimpleTable() { }
    
    @Test
    public void testDeserializationWithFamilies() { }
    
    @Test
    public void testDeserializationWithGcRules() { }
    
    // Execution tests
    @Test
    public void testExecutionCallsApiCorrectly() { }
    
    @Test
    public void testExecutionWithFamilies() { }
    
    // Validation tests
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testValidationTableIdRequired() { }
    
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testValidationEmptyTableId() { }
    
    // Edge cases
    @Test
    public void testEmptyFamiliesList() { }
    
    @Test
    public void testNullFamiliesList() { }
}
```

### Assertions

Use clear assertions:

**Good:**
```java
assertEquals("test-table", createTable.getTableId());
assertNotNull(createTable.getFamilies());
assertEquals(2, createTable.getFamilies().size());
assertTrue(createTable.getFamilies().get(0).hasGcRules());
```

**Bad:**
```java
assertTrue(createTable.getTableId().equals("test-table"));
assertTrue(createTable.getFamilies() != null);
```

### Test Independence

Each test should be independent:

```java
// Bad: tests depend on execution order
private CreateTable action;

@BeforeClass
public void setup() {
    action = new CreateTable();
    action.setTableId("test");
}

@Test
public void test1() {
    // Modifies action
}

@Test
public void test2() {
    // Depends on test1 modifications
}

// Good: each test has own instance
@Test
public void test1() {
    CreateTable action = new CreateTable();
    action.setTableId("test");
    // ...
}

@Test
public void test2() {
    CreateTable action = new CreateTable();
    action.setTableId("test");
    // ...
}
```

---

## Common Patterns

### Testing Lists

```java
@Test
public void testMultipleFamilies() {
    // Given: Config with 3 families
    Map<String, Object> config = createTableWithFamilies("users", "cf1", "cf2", "cf3");
    Context context = createMockContext();
    
    // When
    Action<?> action = ActionFactory.getAction(config, context);
    
    // Then
    CreateTable createTable = (CreateTable) action;
    List<ColumnFamily> families = createTable.getFamilies();
    
    assertNotNull(families);
    assertEquals(3, families.size());
    assertEquals("cf1", families.get(0).getName());
    assertEquals("cf2", families.get(1).getName());
    assertEquals("cf3", families.get(2).getName());
}
```

### Testing Enums

```java
@Test
public void testEnumDeserialization() {
    Map<String, Object> config = new HashMap<>();
    Map<String, Object> actionConfig = new HashMap<>();
    actionConfig.put("tableId", "users");
    actionConfig.put("consistencyMode", "EVENTUAL");  // String in YAML
    config.put("EnableTableReplication", actionConfig);
    
    Context context = createMockContext();
    Action<?> action = ActionFactory.getAction(config, context);
    
    EnableTableReplication enable = (EnableTableReplication) action;
    assertEquals(ConsistencyMode.EVENTUAL, enable.getConsistencyMode());
}
```

### Testing Error Messages

```java
@Test
public void testErrorMessageIsHelpful() {
    try {
        CreateTable action = new CreateTable();
        action.execute(createMock(BigtableTableAdminClient.class));
        fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
        String message = e.getMessage();
        assertTrue(message.contains("tableId"));
        assertTrue(message.contains("required"));
    }
}
```

---

## Troubleshooting

### Mock Not Replayed

**Symptom:** `IllegalStateException: missing behavior definition for the preceding method call`

**Solution:** Call `replay(mock)` before executing code under test.

### Unexpected Method Call

**Symptom:** `Unexpected method call: methodName(...)`

**Solution:** Add expectation for the method:
```java
expect(mock.methodName(anyString())).andReturn(value).anyTimes();
```

### Verification Failed

**Symptom:** `Expectation failure on verify: methodName(...): expected: 1, actual: 0`

**Solution:** Ensure method is actually called in code under test.

### EasyMock vs Mockito Confusion

**Issue:** Mixing EasyMock and Mockito syntax

**Solution:** Stick with EasyMock (preferred), or Mockito for all mocks in a test.

### Tests Pass Individually, Fail Together

**Issue:** Shared state between tests

**Solution:** 
- Avoid static mutable state
- Reset mocks between tests
- Use `@BeforeMethod` for setup, not `@BeforeClass`

---

**Last Updated:** July 31, 2026  
**Maintainers:** TotalSchema Development Team

