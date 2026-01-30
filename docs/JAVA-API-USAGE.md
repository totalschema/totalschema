# TotalSchema Java API Usage Guide

This guide covers using TotalSchema programmatically through its Java API for custom integrations, embedded usage, and advanced automation scenarios.

## Overview

The Java API provides full programmatic control over TotalSchema operations, allowing you to:

- Embed TotalSchema in your applications
- Build custom deployment orchestration tools
- Create web dashboards for database change management
- Implement complex multi-environment deployment logic
- Perform programmatic state inspection and reporting

## Maven Dependency

Add TotalSchema Core to your project:

```xml
<dependency>
    <groupId>io.github.totalschema</groupId>
    <artifactId>totalschema-core</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>

<!-- JDBC Drivers -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.3</version>
</dependency>
```

## Basic Usage Example

```java
import io.github.totalschema.engine.api.ChangeEngine;
import io.github.totalschema.engine.api.ChangeEngineFactory;
import io.github.totalschema.spi.config.ConfigurationSupplier;
import io.github.totalschema.spi.secrets.SecretsManager;
import io.github.totalschema.model.ApplyFile;
import io.github.totalschema.model.Environment;

import java.nio.file.Paths;
import java.util.List;

public class TotalSchemaExample {
    
    public static void main(String[] args) {
        // 1. Create configuration supplier (reads totalschema.yml)
        ConfigurationSupplier configSupplier = ConfigurationSupplier.fromFile(
            Paths.get("totalschema.yml")
        );
        
        // 2. Create secrets manager (for encrypted values)
        SecretsManager secretsManager = SecretsManager.withPassword("MyPassword123");
        // Or: SecretsManager secretsManager = SecretsManager.withPasswordFile("/path/to/key");
        
        // 3. Create ChangeEngine for specific environment
        ChangeEngineFactory factory = ChangeEngineFactory.getInstance();
        ChangeEngine engine = factory.getChangeEngine(
            configSupplier,
            secretsManager,
            "DEV"  // environment name
        );
        
        // 4. Execute operations
        
        // Apply all pending changes
        engine.executePendingApplies(null);
        
        // Or get pending changes first
        List<ApplyFile> allChanges = engine.getAllApplyFiles(null);
        List<ApplyFile> pendingChanges = engine.getPendingApplyFiles(allChanges);
        System.out.println("Pending changes: " + pendingChanges.size());
        
        // Apply them
        for (ApplyFile change : pendingChanges) {
            engine.execute(change);
        }
        
        // Validate changes
        List<Exception> validationErrors = engine.validateChangeFiles(null);
        if (!validationErrors.isEmpty()) {
            System.err.println("Validation failed!");
            validationErrors.forEach(Throwable::printStackTrace);
        }
        
        // Execute reverts
        engine.executeReverts(null);
    }
}
```

## Advanced Usage: Custom Integration

```java
import io.github.totalschema.engine.api.ChangeEngine;
import io.github.totalschema.engine.api.ChangeEngineFactory;
import io.github.totalschema.model.StateRecord;
import io.github.totalschema.model.Environment;

import java.util.List;
import java.util.Map;

public class CustomDeploymentOrchestrator {
    
    private final ChangeEngine changeEngine;
    
    public CustomDeploymentOrchestrator(String environment) {
        // Initialize with custom configuration
        ConfigurationSupplier config = ConfigurationSupplier.fromFile(
            Paths.get("totalschema.yml")
        );
        SecretsManager secrets = SecretsManager.withPasswordFile("/etc/secrets/key");
        
        ChangeEngineFactory factory = ChangeEngineFactory.getInstance();
        this.changeEngine = factory.getChangeEngine(config, secrets, environment);
    }
    
    public DeploymentReport deploy(String filter) {
        DeploymentReport report = new DeploymentReport();
        
        try {
            // Check current state
            List<StateRecord> currentState = changeEngine.getStateRecords();
            report.setPreviousState(currentState);
            
            // Get pending changes
            List<ApplyFile> allChanges = changeEngine.getAllApplyFiles(filter);
            List<ApplyFile> pendingChanges = changeEngine.getPendingApplyFiles(allChanges);
            report.setPendingChanges(pendingChanges);
            
            if (pendingChanges.isEmpty()) {
                report.setStatus("NO_CHANGES");
                return report;
            }
            
            // Pre-deployment validation
            List<Exception> validationErrors = changeEngine.validateChangeFiles(filter);
            if (!validationErrors.isEmpty()) {
                report.setStatus("VALIDATION_FAILED");
                report.setErrors(validationErrors);
                return report;
            }
            
            // Execute changes
            changeEngine.executePendingApplies(filter);
            
            // Post-deployment state
            List<StateRecord> newState = changeEngine.getStateRecords();
            report.setNewState(newState);
            report.setStatus("SUCCESS");
            
        } catch (Exception e) {
            report.setStatus("FAILED");
            report.setException(e);
        }
        
        return report;
    }
    
    public void rollback(String filter) {
        changeEngine.executeReverts(filter);
    }
    
    public Map<String, String> getEnvironmentVariables() {
        // Get current environment's variables
        return changeEngine.getVariables(changeEngine.getEnvironment().getName());
    }
    
    public List<Environment> listEnvironments() {
        // Get all available environments
        return changeEngine.getEnvironments();
    }
}
```

## Query Operations (Read-Only)

```java
// List all environments
ChangeEngine engine = factory.getChangeEngine(configSupplier, secretsManager);
List<Environment> environments = engine.getEnvironments();
for (Environment env : environments) {
    System.out.println("Environment: " + env.getName());
}

// Get variables for specific environment
Map<String, String> variables = engine.getVariables("PROD");
variables.forEach((key, value) -> 
    System.out.println(key + " = " + value)
);

// Get current state
List<StateRecord> state = engine.getStateRecords();
for (StateRecord record : state) {
    System.out.println("Applied: " + record.getChangeFileId() + 
                       " at " + record.getApplyTimestamp());
}

// Get all available changes
List<ApplyFile> allChanges = engine.getAllApplyFiles(null);
List<ApplyFile> pending = engine.getPendingApplyFiles(allChanges);
System.out.println("Total changes: " + allChanges.size());
System.out.println("Pending: " + pending.size());
```

## Advantages

✅ Full programmatic control  
✅ Custom error handling and reporting  
✅ Integration into existing Java applications  
✅ Conditional execution logic  
✅ Custom deployment workflows  
✅ Embedded in application startup  
✅ Advanced automation scenarios  

## Use Cases

- Custom deployment orchestration tools
- Integration with existing Java applications
- Application startup database initialization
- Custom CI/CD tooling in Java
- Complex multi-environment deployment logic
- Programmatic state inspection and reporting
- Custom web dashboards for database change management

## API Components

### ChangeEngine

The main interface for interacting with TotalSchema. Key methods include:

- `executePendingApplies(String filter)` - Apply all pending changes
- `getAllApplyFiles(String filter)` - Get all available changes
- `getPendingApplyFiles(List<ApplyFile> allChanges)` - Filter for pending changes
- `execute(ApplyFile change)` - Execute a specific change
- `validateChangeFiles(String filter)` - Validate change integrity
- `executeReverts(String filter)` - Execute revert scripts
- `getStateRecords()` - Get current deployment state
- `getEnvironments()` - List all environments
- `getVariables(String environment)` - Get environment variables
- `getEnvironment()` - Get current environment

### ConfigurationSupplier

Reads and provides configuration from `totalschema.yml`:

```java
ConfigurationSupplier configSupplier = ConfigurationSupplier.fromFile(
    Paths.get("totalschema.yml")
);
```

### SecretsManager

Manages encrypted values and passwords:

```java
// With password string
SecretsManager secretsManager = SecretsManager.withPassword("MyPassword123");

// With password file
SecretsManager secretsManager = SecretsManager.withPasswordFile("/path/to/key");
```

### ChangeEngineFactory

Factory for creating ChangeEngine instances:

```java
ChangeEngineFactory factory = ChangeEngineFactory.getInstance();

// Create engine for specific environment
ChangeEngine engine = factory.getChangeEngine(
    configSupplier,
    secretsManager,
    "DEV"
);

// Create engine without environment (for query operations)
ChangeEngine engine = factory.getChangeEngine(
    configSupplier,
    secretsManager
);
```

## When to Use the Java API

**Choose the Java API if you need:**

- Full programmatic control over TotalSchema operations
- Custom deployment orchestration tools
- Web dashboards for database change management
- Integration into existing Java applications
- Application startup database initialization
- Complex multi-environment deployment logic
- Custom error handling and reporting
- Conditional execution based on application state

**Consider CLI or Maven Plugin if:**

- You need standalone deployment tool → Use [CLI](totalschema-cli/CLI-USAGE.md)
- You have a Java/Maven project with standard workflows → Use [Maven Plugin](MAVEN-USAGE.md)

## See Also

- [Main README](../README.md) - Architecture, concepts, and configuration
- [CLI Usage Guide](totalschema-cli/CLI-USAGE.md) - Command-line interface usage
- [Maven Plugin Guide](MAVEN-USAGE.md) - Maven integration

