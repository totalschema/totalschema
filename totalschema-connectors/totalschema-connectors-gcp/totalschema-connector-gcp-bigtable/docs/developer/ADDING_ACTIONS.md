# Adding New Actions to BigTable Connector

This guide provides a complete, step-by-step walkthrough for adding new actions to the BigTable connector.

## Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Step-by-Step Guide](#step-by-step-guide)
- [Complete Example](#complete-example)
- [Testing Your Action](#testing-your-action)
- [Documentation](#documentation)
- [Checklist](#checklist)
- [Troubleshooting](#troubleshooting)

---

## Overview

Adding a new action requires creating:

1. **Action POJO** — The data model and execution logic
2. **Factory Class** — Registers the action with ServiceLoader
3. **ServiceLoader Entry** — Registers factory in `META-INF/services`
4. **Unit Tests** — Verifies deserialization and execution
5. **Example YAML** — Demonstrates usage
6. **Documentation** — Updates user manual and README

**Estimated Time:** 30-60 minutes for a simple action

---

## Prerequisites

### Required Knowledge

- Basic Java programming
- Understanding of Jackson data binding
- Familiarity with BigTable Admin API
- TestNG testing framework

### Tools

- Java 11+ JDK
- Maven 3.6+
- IDE (IntelliJ IDEA recommended)
- Git

### Setup

```bash
cd totalschema-connectors/totalschema-connectors-gcp/totalschema-connector-gcp-bigtable
mvn clean install
```

---

## Step-by-Step Guide

### Step 1: Design Your Action

Before coding, answer these questions:

1. **What does the action do?**
   - Example: "Grants read access to a BigTable table for a service account"

2. **What BigTable API does it call?**
   - Example: `BigtableTableAdminClient.setIamPolicy()`

3. **What parameters are needed?**
   - Example: `tableId`, `role`, `member`

4. **What validation is required?**
   - Example: `tableId` must not be empty, `role` must start with "roles/"

5. **What should the YAML look like?**
   ```yaml
   GrantTableAccess:
     tableId: users
     role: roles/bigtable.reader
     member: serviceAccount:analytics@project.iam.gserviceaccount.com
   ```

### Step 2: Create Package Structure

Create a sub-package for your action:

```bash
cd src/main/java/io/github/totalschema/connector/gcp/bigtable/actions
mkdir granttableaccess
cd granttableaccess
```

**Naming Convention:**
- Package name: lowercase, no underscores (e.g., `granttableaccess`)
- Action class: PascalCase matching package (e.g., `GrantTableAccess`)
- Factory class: Action name + "ActionFactory" (e.g., `GrantTableAccessActionFactory`)

### Step 3: Create Action POJO

Create `GrantTableAccess.java`:

```java
package io.github.totalschema.connector.gcp.bigtable.actions.granttableaccess;

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.iam.v1.Binding;
import com.google.iam.v1.Policy;
import io.github.totalschema.connector.gcp.bigtable.actions.table.TableAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Grants IAM permissions to a user, service account, or group for a BigTable table.
 *
 * <p>Example YAML:
 * <pre>
 * GrantTableAccess:
 *   tableId: users
 *   role: roles/bigtable.reader
 *   member: serviceAccount:analytics@project.iam.gserviceaccount.com
 * </pre>
 *
 * @see BigtableTableAdminClient#getIamPolicy(String)
 * @see BigtableTableAdminClient#setIamPolicy(String, Policy)
 */
public class GrantTableAccess implements TableAction {

    private static final Logger logger = LoggerFactory.getLogger(GrantTableAccess.class);

    private String tableId;
    private String role;
    private String member;

    @Override
    public void execute(BigtableTableAdminClient adminClient) {
        // Step 1: Validate inputs
        validateInputs();

        logger.info("Granting access: table={}, role={}, member={}", tableId, role, member);

        // Step 2: Get current policy
        Policy currentPolicy = adminClient.getIamPolicy(tableId);

        // Step 3: Modify policy
        Policy.Builder policyBuilder = currentPolicy.toBuilder();
        boolean roleExists = false;

        for (int i = 0; i < policyBuilder.getBindingsCount(); i++) {
            Binding binding = policyBuilder.getBindings(i);
            if (binding.getRole().equals(role)) {
                // Add member to existing role
                if (!binding.getMembersList().contains(member)) {
                    Binding updatedBinding = binding.toBuilder()
                            .addMembers(member)
                            .build();
                    policyBuilder.setBindings(i, updatedBinding);
                    logger.debug("Added member to existing role");
                } else {
                    logger.debug("Member already has this role");
                }
                roleExists = true;
                break;
            }
        }

        if (!roleExists) {
            // Create new role binding
            Binding newBinding = Binding.newBuilder()
                    .setRole(role)
                    .addMembers(member)
                    .build();
            policyBuilder.addBindings(newBinding);
            logger.debug("Created new role binding");
        }

        // Step 4: Set updated policy
        Policy updatedPolicy = policyBuilder.build();
        adminClient.setIamPolicy(tableId, updatedPolicy);

        logger.info("Access granted successfully");
    }

    private void validateInputs() {
        if (tableId == null || tableId.isEmpty()) {
            throw new IllegalArgumentException("tableId is required");
        }
        if (role == null || role.isEmpty()) {
            throw new IllegalArgumentException("role is required");
        }
        if (!role.startsWith("roles/")) {
            throw new IllegalArgumentException("role must start with 'roles/', got: " + role);
        }
        if (member == null || member.isEmpty()) {
            throw new IllegalArgumentException("member is required");
        }
    }

    // Jackson requires getters and setters
    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMember() {
        return member;
    }

    public void setMember(String member) {
        this.member = member;
    }

    @Override
    public String toString() {
        return "GrantTableAccess{" +
                "tableId='" + tableId + '\'' +
                ", role='" + role + '\'' +
                ", member='" + member + '\'' +
                '}';
    }
}
```

**Key Points:**

- Implement `TableAction` (or `InstanceAction` for instance operations)
- Validate inputs in `execute()` method
- Use SLF4J for logging
- Provide JavaDoc with example YAML
- Include getters/setters for Jackson
- Override `toString()` for debugging

### Step 4: Create Factory Class

Create `GrantTableAccessActionFactory.java`:

```java
package io.github.totalschema.connector.gcp.bigtable.actions.granttableaccess;

import io.github.totalschema.connector.gcp.bigtable.actions.ActionFactory;

/**
 * Factory for creating {@link GrantTableAccess} actions from YAML configuration.
 *
 * <p>Registered via ServiceLoader. Handles action type "GrantTableAccess".
 */
public class GrantTableAccessActionFactory extends ActionFactory<GrantTableAccess> {

    /**
     * Creates a new GrantTableAccessActionFactory.
     *
     * <p>The action type "GrantTableAccess" is derived from the class name passed to super().
     */
    public GrantTableAccessActionFactory() {
        super(GrantTableAccess.class);
    }
}
```

**Key Points:**

- Extend `ActionFactory<YourAction>`
- Pass action class to `super()`
- Action type is derived from class simple name
- No other methods needed (deserialization handled by base class)

### Step 5: Register with ServiceLoader

Edit `src/main/resources/META-INF/services/io.github.totalschema.connector.gcp.bigtable.actions.ActionFactory`:

Add one line with the fully-qualified factory class name:

```
io.github.totalschema.connector.gcp.bigtable.actions.createtable.CreateTableActionFactory
io.github.totalschema.connector.gcp.bigtable.actions.deletetable.DeleteTableActionFactory
... (existing entries)
io.github.totalschema.connector.gcp.bigtable.actions.granttableaccess.GrantTableAccessActionFactory
```

**Important:**
- Full package name required
- One entry per line
- No trailing whitespace
- File must end with newline

### Step 6: Build and Verify

```bash
# Compile
mvn clean compile

# Check for errors
mvn spotbugs:check

# Format code
mvn spotless:apply
```

If compilation fails, check:
- Package names match directory structure
- All imports are correct
- ServiceLoader file has correct class names

---

## Complete Example

Here's a complete minimal example for a simple action:

**Action (ClearTableData.java):**

```java
package io.github.totalschema.connector.gcp.bigtable.actions.cleartabledata;

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import io.github.totalschema.connector.gcp.bigtable.actions.table.TableAction;

/**
 * Clears all data from a BigTable table by dropping and recreating it.
 *
 * <p>⚠️ WARNING: This permanently deletes all data in the table.
 */
public class ClearTableData implements TableAction {

    private String tableId;

    @Override
    public void execute(BigtableTableAdminClient adminClient) {
        if (tableId == null || tableId.isEmpty()) {
            throw new IllegalArgumentException("tableId is required");
        }

        // Get current table definition
        var table = adminClient.getTable(tableId);

        // Delete table
        adminClient.deleteTable(tableId);

        // Recreate with same definition
        var request = com.google.cloud.bigtable.admin.v2.models.CreateTableRequest.of(tableId);
        for (var family : table.getColumnFamilies()) {
            request.addFamily(family);
        }
        adminClient.createTable(request);
    }

    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }
}
```

**Factory (ClearTableDataActionFactory.java):**

```java
package io.github.totalschema.connector.gcp.bigtable.actions.cleartabledata;

import io.github.totalschema.connector.gcp.bigtable.actions.ActionFactory;

public class ClearTableDataActionFactory extends ActionFactory<ClearTableData> {
    public ClearTableDataActionFactory() {
        super(ClearTableData.class);
    }
}
```

**ServiceLoader Entry:**

```
io.github.totalschema.connector.gcp.bigtable.actions.cleartabledata.ClearTableDataActionFactory
```

**Total Lines:** ~40 lines of code!

---

## Testing Your Action

### Step 7: Create Test Class

Create `src/test/java/.../actions/granttableaccess/GrantTableAccessTest.java`:

```java
package io.github.totalschema.connector.gcp.bigtable.actions.granttableaccess;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.iam.v1.Binding;
import com.google.iam.v1.Policy;
import io.github.totalschema.connector.gcp.bigtable.actions.Action;
import io.github.totalschema.connector.gcp.bigtable.actions.ActionFactory;
import io.github.totalschema.engine.api.Context;
import java.util.HashMap;
import java.util.Map;
import org.testng.annotations.Test;

public class GrantTableAccessTest {

    @Test
    public void testDeserialization() {
        // Given: YAML configuration
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> actionConfig = new HashMap<>();
        actionConfig.put("tableId", "test-table");
        actionConfig.put("role", "roles/bigtable.reader");
        actionConfig.put("member", "serviceAccount:test@project.iam.gserviceaccount.com");
        config.put("GrantTableAccess", actionConfig);

        Context context = createMockContext();

        // When: Deserialize action
        Action<?> action = ActionFactory.getAction(config, context);

        // Then: Verify correct type and properties
        assertTrue(action instanceof GrantTableAccess);
        GrantTableAccess grantAccess = (GrantTableAccess) action;
        assertEquals("test-table", grantAccess.getTableId());
        assertEquals("roles/bigtable.reader", grantAccess.getRole());
        assertEquals("serviceAccount:test@project.iam.gserviceaccount.com", grantAccess.getMember());
    }

    @Test
    public void testExecution() {
        // Given: Action with valid configuration
        GrantTableAccess action = new GrantTableAccess();
        action.setTableId("test-table");
        action.setRole("roles/bigtable.reader");
        action.setMember("serviceAccount:test@project.iam.gserviceaccount.com");

        // Mock admin client
        BigtableTableAdminClient adminClient = createMock(BigtableTableAdminClient.class);
        
        Policy currentPolicy = Policy.newBuilder().build();
        expect(adminClient.getIamPolicy("test-table")).andReturn(currentPolicy);
        
        // Capture the updated policy
        adminClient.setIamPolicy(eq("test-table"), anyObject(Policy.class));
        expectLastCall();
        
        replay(adminClient);

        // When: Execute action
        action.execute(adminClient);

        // Then: Verify admin client was called
        verify(adminClient);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = ".*tableId is required.*")
    public void testValidationTableIdRequired() {
        GrantTableAccess action = new GrantTableAccess();
        action.setRole("roles/bigtable.reader");
        action.setMember("serviceAccount:test@project.iam.gserviceaccount.com");
        
        BigtableTableAdminClient adminClient = createMock(BigtableTableAdminClient.class);
        action.execute(adminClient);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = ".*role must start with 'roles/'.*")
    public void testValidationInvalidRole() {
        GrantTableAccess action = new GrantTableAccess();
        action.setTableId("test-table");
        action.setRole("invalid-role");
        action.setMember("serviceAccount:test@project.iam.gserviceaccount.com");
        
        BigtableTableAdminClient adminClient = createMock(BigtableTableAdminClient.class);
        action.execute(adminClient);
    }

    private Context createMockContext() {
        Context context = createMock(Context.class);
        // Add necessary mocks for ExpressionEvaluator, VariableService
        // (See existing tests for complete example)
        replay(context);
        return context;
    }
}
```

### Run Tests

```bash
# Run your new test
mvn test -Dtest=GrantTableAccessTest

# Run all tests
mvn test

# Check coverage
mvn clean test jacoco:report
open target/site/jacoco/index.html
```

---

## Documentation

### Step 8: Create Example YAML

Create `examples/XX-grant-table-access.yml`:

```yaml
# Example: Grant read access to analytics service

GrantTableAccess:
  tableId: users
  role: roles/bigtable.reader
  member: serviceAccount:analytics@project.iam.gserviceaccount.com
```

### Step 9: Update User Manual

Edit `docs/user_manual/ACTIONS-REFERENCE.md`:

```markdown
### GrantTableAccess

Grants IAM permissions to a user, service account, or group.

```yaml
GrantTableAccess:
  tableId: users
  role: roles/bigtable.reader
  member: serviceAccount:analytics@project.iam.gserviceaccount.com
```

**Properties:**
- `tableId` (required) — table ID
- `role` (required) — IAM role (e.g., `roles/bigtable.reader`)
- `member` (required) — member identifier

**Common Roles:**
- `roles/bigtable.reader` — read-only access
- `roles/bigtable.user` — read and write access
- `roles/bigtable.admin` — full administration
```

### Step 10: Update README

Add your action to the main README's action list.

---

## Checklist

Use this checklist to ensure you've completed all steps:

### Code

- [ ] Created action POJO in correct package
- [ ] Implemented `Action<BigtableTableAdminClient>` (or marker interface)
- [ ] Added validation in `execute()` method
- [ ] Provided getters/setters for all properties
- [ ] Added JavaDoc with example YAML
- [ ] Created factory class extending `ActionFactory<T>`
- [ ] Registered factory in `META-INF/services`

### Testing

- [ ] Created test class
- [ ] Test deserialization from YAML
- [ ] Test execution with mock admin client
- [ ] Test validation (required fields, invalid values)
- [ ] Test edge cases
- [ ] All tests pass: `mvn test`

### Documentation

- [ ] Created example YAML in `examples/`
- [ ] Updated `docs/user_manual/ACTIONS-REFERENCE.md`
- [ ] Updated `docs/user_manual/README.md` (if major feature)
- [ ] Updated main `README.md` action count
- [ ] Updated `AGENTS.md` if architecture changed

### Quality

- [ ] Code compiles: `mvn compile`
- [ ] SpotBugs passes: `mvn spotbugs:check`
- [ ] Code formatted: `mvn spotless:apply`
- [ ] No TODO/FIXME comments without issue numbers
- [ ] Commit message follows conventions

---

## Troubleshooting

### ServiceLoader Not Finding Factory

**Symptom:** `Unknown action type: YourAction`

**Solutions:**

1. Check `META-INF/services` file exists
2. Verify fully-qualified class name is correct
3. Ensure file ends with newline
4. Rebuild: `mvn clean compile`
5. Check JAR contains the file: `jar tf target/*.jar | grep META-INF/services`

### Jackson Deserialization Error

**Symptom:** `Unrecognized field "fieldName"`

**Solutions:**

1. Ensure property has getter/setter
2. Check YAML field name matches Java property name (camelCase)
3. Add `@JsonProperty("yaml_name")` if names differ
4. Check for typos in YAML

### YAML Key Doesn't Match

**Symptom:** `Unknown action type: YourAction`

**Solutions:**

1. YAML key must match Action class simple name exactly
2. Case-sensitive: `YourAction` not `youraction`
3. Check factory passes correct class: `super(YourAction.class)`

### Tests Fail with NullPointerException

**Symptom:** `NullPointerException` in test

**Solutions:**

1. Mock Context and dependencies
2. Set up ExpressionEvaluator and VariableService mocks
3. Use `replay()` after setting expectations
4. Check test setup methods

### Validation Error Messages Not Clear

**Solution:** Follow pattern:

```java
throw new IllegalArgumentException(
    String.format("Invalid %s: expected %s, got: %s", 
        fieldName, expectedFormat, actualValue)
);
```

---

## Advanced Topics

### Complex Data Structures

If your action needs nested objects:

```java
public class ComplexAction implements TableAction {
    private String tableId;
    private List<Rule> rules;  // Nested class
    
    public static class Rule {
        private String name;
        private int value;
        
        // getters/setters
    }
    
    // getters/setters for tableId and rules
}
```

Jackson handles nested objects automatically.

### Optional Fields with Defaults

```java
private int maxRetries = 3;  // Default value

public int getMaxRetries() { return maxRetries; }
public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
```

If not specified in YAML, uses default.

### Custom Validation

Create a validation method:

```java
private void validate() {
    if (tableId == null || tableId.isEmpty()) {
        throw new IllegalArgumentException("tableId is required");
    }
    if (maxRetries < 1 || maxRetries > 10) {
        throw new IllegalArgumentException(
            "maxRetries must be between 1 and 10, got: " + maxRetries
        );
    }
}

@Override
public void execute(BigtableTableAdminClient adminClient) {
    validate();
    // ... implementation
}
```

---

**Last Updated:** July 31, 2026  
**Maintainers:** TotalSchema Development Team

