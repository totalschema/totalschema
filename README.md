# TotalSchema

**TotalSchema** is a flexible deployment orchestration tool designed specifically for complex 
enterprise data engineering projects. 
Unlike traditional database change tools that excel at managing a single application database, 
TotalSchema is built for Data Warehouse (DWH), Hadoop and multi-system data engineering 
deployment scenarios where you need to manage:

- Multiple databases simultaneously
- Different database technologies (Oracle, PostgreSQL, H2, BigQuery, etc.)
- Non-database assets (Linux/Unix servers, cloud infrastructure, configuration files)
- Complex deployment orchestration across heterogeneous systems

## Why TotalSchema?

Traditional change tools work perfectly when you have one application managing one database. However, they fall short in scenarios where:

- **Multiple systems need synchronized changes** - In DWH and cloud environments, a single logical deployment might require updates to an operational database, a staging database, a data warehouse, and cloud infrastructure (e.g., spinning up a GCP cluster)
- **Non-database resources are involved** - You need to deploy configuration files, restart services, provision infrastructure, or execute shell commands as part of your deployment
- **Complex environment management is required** - Different environments (DEV, QA, PROD) may have completely different asset configurations

TotalSchema addresses these issues by treating databases as just one type of "asset" among many, allowing you to orchestrate complex multi-system deployments through a single, version-controlled set of change scripts.

## Key Features

- **Multi-Asset Management**: Manage unlimited databases and non-database assets within a single project
- **State Tracking**: Persistent state table (stored in a database) tracks which changes have been applied, with optional file content hashing for validation
- **Multiple Script Types**: Native support for SQL, Shell scripts, and Groovy scripts (with injected `sql` object for database operations)
- **Asset Types**: JDBC databases, SSH remote execution, local shell commands
- **Environment Management**: Define separate configurations, variables, and asset sets for each environment
- **Script Execution Modes**: Support for one-time execution, always-run scripts, change-triggered scripts, and rollback/revert operations
- **Locking**: Optional database-based locking to prevent concurrent executions
- **Variable Substitution**: Powerful variable system with environment overrides and expression evaluation
- **Maven Integration**: First-class Maven plugin support for CI/CD pipelines
- **CLI Interface**: Comprehensive command-line interface for all operations
- **Validation**: Built-in validation to detect modified change scripts after deployment

## 📚 Documentation

> **Using the Command-Line Interface?** See the **[CLI Usage Guide](totalschema-cli/user_manual/README.md)** for complete installation instructions, command reference, and usage examples.

> **Using Maven?** See the **[Maven Plugin Guide](docs/MAVEN-USAGE.md)** for plugin configuration, lifecycle integration, and build examples.

> **Using the Java API?** See the **[Java API Usage Guide](docs/JAVA-API-USAGE.md)** for programmatic usage, API reference, and integration examples.

This README covers architecture, concepts, and configuration. For usage-specific documentation:
- **CLI users** → [CLI-USAGE.md](totalschema-cli/user_manual/README.md)
- **Maven users** → [MAVEN-USAGE.md](docs/MAVEN-USAGE.md)
- **Java API users** → [JAVA-API-USAGE.md](docs/JAVA-API-USAGE.md)

## Architecture Overview

TotalSchema is built around several core concepts:

1. **Change Scripts**: Version-controlled scripts organized in a hierarchical directory structure
2. **Assets**: Target systems (databases, servers, cloud infrastructure) where scripts are executed
3. **Environments**: Separate configurations for DEV, QA, PROD, etc.
4. **State Table**: Tracks which changes have been applied (stored in a database of your choice)
5. **Script Execution Modes**: Control how and when scripts are executed
6. **Variables**: Environment-specific configuration with substitution support


## Usage Approaches

TotalSchema can be used in **three different ways**, each suited to different use cases and workflows. Choose the approach that best fits your deployment strategy:

### 📦 Approach 1: CLI Distribution (Recommended for Most Users)

**Best for:** Operations teams, standalone deployments, CI/CD pipelines, manual execution

Download and extract the pre-built CLI distribution for a complete, ready-to-use TotalSchema installation.

**📖 [Complete CLI Usage Guide](totalschema-cli/user_manual/README.md)** - Detailed documentation for installation, commands, and examples.

#### Quick Start

```bash
# Download the latest release
wget https://github.com/totalschema/totalschema/releases/download/v1.0/totalschema-1.0-SNAPSHOT.tar.gz

# Extract
tar -xzf totalschema-1.0-SNAPSHOT.tar.gz
cd totalschema-1.0-SNAPSHOT

# Add JDBC drivers to lib/jdbc/
cp postgresql-42.7.3.jar lib/jdbc/

# Apply changes to DEV environment
bin/totalschema.sh apply -e DEV
```

#### Advantages

✅ No build tool required  
✅ Simple installation and setup  
✅ Ideal for operations and deployment teams  
✅ Easy to integrate into any CI/CD system  
✅ Full control over JDBC driver versions  
✅ Portable across environments  

#### Use Cases

- Production deployments by operations teams
- CI/CD pipeline integration (Jenkins, GitLab CI, GitHub Actions)
- Manual database migrations
- Scheduled automated deployments
- Docker container deployments

**For complete installation instructions, command reference, and examples, see the [CLI Usage Guide](totalschema-cli/user_manual/README.md).**

---

### 🔧 Approach 2: Maven Plugin (Best for Java Projects)

**Best for:** Java/Maven projects, developers, build-time integration

Use the Maven plugin to integrate TotalSchema directly into your Maven build lifecycle.

**📖 [Complete Maven Plugin Guide](docs/MAVEN-USAGE.md)** - Detailed documentation for configuration, lifecycle integration, and examples.

#### Quick Start

Add the plugin to your `pom.xml`:

```xml
<plugin>
    <groupId>io.github.totalschema</groupId>
    <artifactId>totalschema-maven-plugin</artifactId>
    <version>1.0-SNAPSHOT</version>
    <dependencies>
        <dependency>
            <groupId>io.github.totalschema</groupId>
            <artifactId>totalschema-core</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>42.7.3</version>
        </dependency>
    </dependencies>
    <configuration>
        <arguments>
            <argument>apply</argument>
            <argument>-e</argument>
            <argument>${environment}</argument>
        </arguments>
    </configuration>
</plugin>
```

Run with:

```bash
mvn totalschema:run -Denvironment=DEV
```

#### Advantages

✅ Integrated into Maven build lifecycle  
✅ JDBC drivers managed via Maven dependencies  
✅ Consistent with Java project tooling  
✅ Automatic execution during build phases  
✅ Inherits Maven project properties and profiles  
✅ Works with Maven release plugin  

#### Use Cases

- Java application development with database changes
- CI/CD builds for Java/Maven projects
- Automated testing with database setup
- Application versioning aligned with database versioning
- Multi-module Maven projects

**For complete configuration, lifecycle integration, profiles, and examples, see the [Maven Plugin Guide](docs/MAVEN-USAGE.md).**

---

### 💻 Approach 3: Java API (Programmatic Usage)

**Best for:** Custom integrations, embedded usage, advanced automation

Use the Java API directly in your code for maximum flexibility and control.

**📖 [Complete Java API Usage Guide](docs/JAVA-API-USAGE.md)** - Detailed documentation for programmatic usage, API reference, and examples.

#### Quick Start

Add the Maven dependency:

```xml
<dependency>
    <groupId>io.github.totalschema</groupId>
    <artifactId>totalschema-core</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

Basic usage:

```java
import io.github.totalschema.engine.api.ChangeEngine;
import io.github.totalschema.engine.api.ChangeEngineFactory;

// Create ChangeEngine
ConfigurationSupplier config = ConfigurationSupplier.fromFile(
    Paths.get("totalschema.yml")
);
SecretsManager secrets = SecretsManager.withPassword("MyPassword123");
ChangeEngineFactory factory = ChangeEngineFactory.getInstance();
ChangeEngine engine = factory.getChangeEngine(config, secrets, "DEV");

// Apply pending changes
engine.executePendingApplies(null);
```

#### Advantages

✅ Full programmatic control  
✅ Custom error handling and reporting  
✅ Integration into existing Java applications  
✅ Conditional execution logic  
✅ Custom deployment workflows  
✅ Embedded in application startup  
✅ Advanced automation scenarios  

#### Use Cases

- Custom deployment orchestration tools
- Integration with existing Java applications
- Application startup database initialization
- Custom CI/CD tooling in Java
- Complex multi-environment deployment logic
- Programmatic state inspection and reporting
- Custom web dashboards for database change management

**For complete API reference, advanced examples, and integration patterns, see the [Java API Usage Guide](docs/JAVA-API-USAGE.md).**

---

## Comparison: Which Approach to Use?

| Feature | CLI Distribution | Maven Plugin | Java API |
|---------|-----------------|--------------|----------|
| **Ease of Setup** | ⭐⭐⭐ Very Easy | ⭐⭐ Moderate | ⭐ Advanced |
| **Build Tool Required** | ❌ No | ✅ Maven | ✅ Maven/Gradle |
| **JDBC Driver Management** | Manual (copy to lib/) | Maven dependencies | Maven dependencies |
| **Programmatic Control** | ❌ No | ❌ No | ✅ Full control |
| **CI/CD Integration** | ⭐⭐⭐ Excellent | ⭐⭐⭐ Excellent | ⭐⭐ Custom |
| **Operations Team Friendly** | ⭐⭐⭐ Yes | ⭐ No | ⭐ No |
| **Developer Friendly** | ⭐⭐ Good | ⭐⭐⭐ Excellent | ⭐⭐⭐ Excellent |
| **Custom Integrations** | ❌ Limited | ❌ Limited | ⭐⭐⭐ Unlimited |
| **Standalone Usage** | ⭐⭐⭐ Yes | ❌ No | ❌ No |

### Decision Guide

**Choose CLI Distribution if:**
- You need standalone deployment tool
- Operations team will run deployments
- Using non-Java CI/CD pipelines
- Want simple, portable installation
- Don't use Maven/Gradle

👉 **[See CLI Usage Guide](totalschema-cli/user_manual/README.md)** for complete installation and usage instructions.

**Choose Maven Plugin if:**
- You have a Java/Maven project
- Want build-time database migrations
- Prefer Maven dependency management
- Need lifecycle integration
- Team is familiar with Maven

👉 **[See Maven Plugin Guide](docs/MAVEN-USAGE.md)** for complete configuration and usage instructions.

**Choose Java API if:**
- Building custom deployment tools
- Need programmatic control
- Creating web dashboards
- Embedding in applications
- Complex automation requirements
- Custom workflow orchestration

👉 **[See Java API Usage Guide](docs/JAVA-API-USAGE.md)** for complete API reference and integration examples.

---

## Getting Started

### Step 1: Create Project Structure

Create the following directory structure in your project:

```
my-project/
├── totalschema.yml
└── changes/
    ├── 1.X/
    │   └── 1.0.0/
    ├── 2.X/
    │   └── 2.0.0/
    └── 3+.X/
```

### Step 2: Configure totalschema.yml

Create a `totalschema.yml` file at the root of your project:

```yaml
version: 1

# State table database connection (where to store the state tracking table)
stateRepository:
  type: database
  database:
    username: ${systemDatabaseSchema}
    password: ${dbPassword}
    jdbc:
      url: ${systemDatabaseJdbcUrl}
    table:
      catalog: ${systemDatabaseSchema}
      beforeCreate:
        sql: CREATE SCHEMA IF NOT EXISTS ${systemDatabaseSchema}

# Validation configuration
validation:
  type: contentHash

# Lock configuration (optional - prevents concurrent executions)
lock:
  type: none
  # database:
  #   username: ${systemDatabaseSchema}
  #   password: ${dbPassword}
  #   jdbc:
  #     url: ${systemDatabaseJdbcUrl}

# Global variables
variables:
  systemDatabaseJdbcUrl: jdbc:h2:file:./totalschema/state/db/h2/${systemDatabaseSchema}
  systemDatabaseSchema: ${dbName}_totalschema_system
  dbSchema: ${dbName}

# Define environments with environment-specific variables
environments:
  DEV:
    variables:
      dbPassword: devpassword
      dbName: dev_db
  QA:
    variables:
      dbPassword: qapassword
      dbName: qa_db
  PROD:
    variables:
      dbPassword: prodpassword
      dbName: prod_db

# Define assets (target systems)
assets:
  mydb:
    type: jdbc
    driver:
      class: org.h2.Driver
    jdbc:
      url: jdbc:h2:file:./data/db/${dbName}
    db:
      type: generic
      username: sa
      password: ${dbPassword}
  myserver:
    type: ssh-script  # or ssh-commands for command lists
    host: server.example.com
    user: deployuser
    password: ${serverPassword}
```

### Step 3: Understand the State Table

TotalSchema tracks which change scripts have been executed in a **state table**. You configure a database connection (called the "state database") to tell TotalSchema where to create and maintain this table. On first run, TotalSchema will automatically create the state table with the following structure:

```sql
CREATE TABLE totalschema_state (
    change_file_id VARCHAR(255) PRIMARY KEY,
    file_hash VARCHAR(64),
    apply_timestamp TIMESTAMP,
    applied_by VARCHAR(255)
)
```

**Important Notes:**
- The state table is created automatically on first execution
- Each change script has a unique ID based on its path and name
- File hashes (SHA-256) are stored when `validation.type=contentHash` is configured
- You can customize the table name, column types, and other properties via configuration

### Step 4: Create Your First Change Script

Create a change script file following the naming convention:

`changes/1.X/1.0.0/0001.create_users_table.DEV.apply.mydb.sql`

```sql
CREATE TABLE users (
    id INT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

Create the corresponding revert file:

`changes/1.X/1.0.0/0001.create_users_table.DEV.revert.mydb.sql`

```sql
DROP TABLE users;
```

### Step 5: Apply Changes

Run TotalSchema to apply pending changes:

```bash
# Using CLI Distribution
bin/totalschema.sh apply -e DEV

# Using Maven Plugin
mvn totalschema:run -Denvironment=DEV
```

TotalSchema will:
1. Connect to the state database and ensure the state table exists (creating it if needed)
2. Acquire a lock (if configured)
3. Scan for change script files
4. Determine which changes are pending (by checking the state table)
5. Execute pending changes in order
6. Record each successful change in the state table
7. Release the lock



## Change Script File Naming Convention

Change script files must follow a strict naming convention to be recognized by TotalSchema:

### Pattern

```
<order>.<description>.<environment>.<changetype>.<asset>.<extension>
```

or (for non-environment-specific changes):

```
<order>.<description>.<changetype>.<asset>.<extension>
```

### Components

1. **Order** (`<order>`): A numeric value (e.g., `0001`, `0002`) that determines execution order
2. **Description** (`<description>`): A human-readable description (e.g., `create_users_table`)
3. **Environment** (`<environment>`): Optional. The target environment (e.g., `DEV`, `QA`, `PROD`)
4. **Change Type** (`<changetype>`): One of:
   - `apply` - Execute once, tracked in state
   - `apply_always` - Execute on every run, NOT tracked in state
   - `apply_on_change` - Execute when file content changes (requires hashing)
   - `revert` - Rollback script for an apply change
5. **Asset** (`<asset>`): Target system name as defined in configuration (e.g., `mydb`, `myserver`)
6. **Extension** (`<extension>`): File type (e.g., `sql`, `groovy`, `sh`)

### Examples

```
0001.create_users_table.DEV.apply.mydb.sql
0002.insert_initial_data.apply.mydb.sql
0003.deploy_config.PROD.apply.myserver.sh
0001.create_users_table.DEV.revert.mydb.sql
0010.refresh_materialized_views.apply_always.dwh.sql
0020.update_schema_on_change.apply_on_change.mydb.sql
```

### Directory Structure

Change scripts are organized in a hierarchical directory structure:

```
changes/
├── 1.X/                           # Major version 1
│   ├── 1.0.0/                     # Version 1.0.0
│   │   ├── 0001.create_foo.DEV.apply.mydb.sql
│   │   ├── 0001.create_foo.DEV.revert.mydb.sql
│   │   └── 0002.create_bar.DEV.apply.mydb.sql
│   ├── 1.1.0/                     # Version 1.1.0
│   │   └── 0001.add_column.DEV.apply.mydb.sql
│   └── 99/                        # Always-last directory for version 1.X
│       └── 0001.final_cleanup.DEV.apply.mydb.sql
├── 2.X/                           # Major version 2
│   └── 2.0.0/
│       └── 0001.migrate_data.DEV.apply.mydb.sql
└── 3+.X/                          # Version 3 and beyond
    └── V10.0.0/
        └── 0001.major_refactor.DEV.apply.mydb.sql
```

**Ordering Rules:**
- Directories are processed in numeric order (1.X before 2.X before 3+.X)
- Within a version, subdirectories are processed in numeric order
- Directory `99` is always processed last within its parent
- Files within a directory are processed by their order number

## Script Execution Modes

TotalSchema supports two fundamental operations for managing changes:

### Apply Operations - Deploying Changes

Apply scripts deploy changes to your target systems (databases, servers, cloud resources). They support three execution modes:

#### `apply` - One-Time Execution

Execute once and track in state database. This is the most common mode.

```sql
-- 0001.create_table.DEV.apply.mydb.sql
CREATE TABLE products (
    id INT PRIMARY KEY,
    name VARCHAR(100)
);
```

- Executes only if not already applied
- Recorded in state database
- Cannot be re-executed unless state is manually cleared
- Use for: CREATE TABLE, ALTER TABLE, infrastructure provisioning, one-time deployments

#### `apply_always` - Repeating Execution

Execute on every run, never tracked in state.

```sql
-- 0010.refresh_views.apply_always.mydb.sql
REFRESH MATERIALIZED VIEW sales_summary;
REFRESH MATERIALIZED VIEW inventory_totals;
```

- Executes on every deployment
- NOT recorded in state database
- Useful for idempotent operations (views, procedures, reports, health checks)
- Use for: REFRESH VIEW, restart services, sync configurations

#### `apply_on_change` - Conditional Execution

Execute only when file content changes. Requires `validation.type=contentHash`.

```sql
-- 0020.create_or_replace_procedure.apply_on_change.mydb.sql
CREATE OR REPLACE PROCEDURE calculate_totals() AS
BEGIN
    -- procedure logic
END;
```

- First execution: runs and stores file hash
- Subsequent runs: compares current hash with stored hash
- Re-executes only if hash differs
- Use for: CREATE OR REPLACE procedures/views, update configurations, deploy applications

### Revert Operations - Rolling Back Changes

#### `revert` - Rollback Execution

Reverses the effects of a corresponding `apply` script.

```sql
-- 0001.create_table.DEV.revert.mydb.sql
DROP TABLE products;
```

- Must match the order and description of corresponding apply script
- Executed with `revert execute` command (executes revert scripts)
- Processed in reverse order (latest first)
- Use for: DROP TABLE, remove configurations, tear down infrastructure, rollback deployments

### Choosing the Right Execution Mode

| What You're Deploying | Use This Mode | Why |
|----------------------|---------------|-----|
| CREATE TABLE, ALTER TABLE | `apply` | Structural changes should run once |
| Provision GCP cluster, deploy infrastructure | `apply` | One-time resource creation |
| CREATE OR REPLACE VIEW | `apply_on_change` | Re-run when view logic changes |
| CREATE OR REPLACE PROCEDURE | `apply_on_change` | Re-run when procedure changes |
| REFRESH MATERIALIZED VIEW | `apply_always` | Run on every deployment |
| Deploy application version | `apply_on_change` | Re-deploy when version changes |
| Restart service, sync configs | `apply_always` | Run on every deployment |
| Rollback any apply script | `revert` | Reverse previous changes |

## Connector Types

TotalSchema supports multiple connector types for executing change scripts across different environments. Each connector type has a specific purpose:

| Connector Type | Description | Use Case |
|------------|-------------|----------|
| `jdbc` | JDBC database connections | Execute SQL scripts on any JDBC-compliant database |
| `ssh-script` | SSH shell script execution | Run complete shell scripts on remote servers with shared context |
| `ssh-commands` | SSH command list execution | Execute independent commands line-by-line on remote servers |
| `shell` | Local shell script execution | Run shell scripts on the local machine |
| Custom | User-defined connector types | Extend TotalSchema with custom connector implementations |

### JDBC Database Connectors

Connect to any JDBC-compliant database:

```yaml
assets:
  mydb:
    type: jdbc
    driver:
      class: org.postgresql.Driver
    jdbc:
      url: jdbc:postgresql://localhost:5432/${dbName}
    db:
      type: generic
      username: postgres
      password: ${dbPassword}
    transaction:
      isolation: READ_COMMITTED  # Optional
    connection:
      properties:
        file: /path/to/connection.properties  # Optional
```

**Supported databases** include PostgreSQL, MySQL, Oracle, H2, SQL Server, BigQuery, and any JDBC driver.

### SSH Remote Execution Connectors

TotalSchema provides two SSH connector types for remote execution, each with different behavior:

#### SSH Script Connectors (`ssh-script`)

Executes complete shell scripts on remote servers. The script is transferred to the remote host, executed as a single shell session (preserving context), and then cleaned up.

**Use this when:** You need variables, functions, or state to be shared across lines in your script.

```yaml
assets:
  myserver:
    type: ssh-script
    host: server.example.com
    port: 22  # Optional, defaults to 22
    user: deployuser
    password: ${serverPassword}
    # Optional configuration:
    remote:
      temp:
        dir: /tmp  # Where to upload scripts temporarily
    shell: /bin/bash  # Shell to use for execution
```

Example script with shared context:

```bash
# 0001.deploy_config.PROD.apply.myserver.sh
#!/bin/bash
# Variables and functions work across lines
APP_DIR="/opt/myapp"
LOG_FILE="/var/log/deploy.log"

deploy_app() {
  echo "Deploying to $APP_DIR..." | tee -a $LOG_FILE
  cd $APP_DIR  # Directory change persists
  ./install.sh
}

deploy_app
echo "Deployment complete" >> $LOG_FILE
```

#### SSH Command List Connectors (`ssh-commands`)

Executes commands from a file line-by-line, where **each line is an independent SSH command**. Commands do NOT share shell context.

**Use this when:** You need to execute a series of independent commands that don't depend on each other.

```yaml
assets:
  myserver:
    type: ssh-commands
    host: server.example.com
    port: 22  # Optional, defaults to 22
    user: deployuser
    password: ${serverPassword}
```

Example command list (no shared context):

```bash
# 0001.simple_tasks.PROD.apply.myserver.txt
# Each line is executed independently
mkdir -p /opt/myapp
systemctl restart nginx
echo "Task complete"
# Note: Variables from one line won't be available in the next line!
```

⚠️ **Important Difference:**
- `ssh-script`: Entire file runs as one script → Variables/functions/cd commands persist
- `ssh-commands`: Each line is separate → No shared state between lines

#### Authentication Options

Both SSH connector types support multiple authentication methods:

```yaml
# Password authentication
assets:
  myserver:
    type: ssh-script  # or ssh-commands
    host: server.example.com
    user: deployuser
    password: ${serverPassword}

# Key-based authentication (recommended)
assets:
  myserver:
    type: ssh-script
    host: server.example.com
    user: deployuser
    jsch:
      properties:
        StrictHostKeyChecking: "no"  # Optional
    # Key file path in jsch configuration
```

### Shell Script Connectors

Execute scripts on the local machine:

```yaml
assets:
  localshell:
    type: shell
```

```bash
# 0001.backup_data.apply.localshell.sh
#!/bin/bash
pg_dump mydb > backup_$(date +%Y%m%d).sql
```

## Locking Mechanism

Locking prevents concurrent TotalSchema executions from interfering with each other. This is crucial in CI/CD environments where multiple builds might run simultaneously.

### Understanding the Lock Table

The **lock table** is separate from the **state table** and serves a different purpose:
- **State Table**: Records which change scripts have been applied (persistent history). Configured via `stateRepository.database` in YML.
- **Lock Table**: Coordinates concurrent access (temporary coordination). Configured via `lock.database` in YML.

Both tables can be in the same database or different databases - they are independent.

### Enable Database Locking

```yaml
lock:
  type: database
  database:
    jdbc:
      url: jdbc:h2:file:./totalschema/lock/db
    username: totalschema
    password: ${lockPassword}
    # Optional: customize lock behavior
    timeout:
      seconds: 300
```

### Lock Table Creation

Like the state table, **the lock table is created automatically** on first execution when `lock.type=database` is configured. TotalSchema will create a table with the following structure:

```sql
CREATE TABLE totalschema_lock (
    lock_id VARCHAR(255) PRIMARY KEY,
    locked_at TIMESTAMP,
    locked_by VARCHAR(255),
    lock_owner VARCHAR(255),
    host_name VARCHAR(255),
    process_id VARCHAR(255)
);
```

**Important Notes:**
- Lock table is created automatically on first run with locking enabled
- The table is created in the database specified by `lock.database.jdbc.url`
- You can use the same database as the state table or a separate database
- Lock records are temporary and cleaned up after execution

### Customizing the Lock Table

Similar to the state table, you can customize the lock table structure:

```yaml
lock:
  type: database
  database:
    jdbc:
      url: jdbc:h2:file:./totalschema/lock/db
    username: totalschema
    password: ${lockPassword}
    table:
      catalog: my_schema
      schema: my_schema
      beforeCreate:
        sql: CREATE SCHEMA IF NOT EXISTS my_schema
      column:
        lock_id:
          type: VARCHAR(500)
        locked_at:
          type: DATETIME
        locked_by:
          type: VARCHAR(255)
        lock_owner:
          type: VARCHAR(255)
        host_name:
          type: VARCHAR(255)
        process_id:
          type: VARCHAR(255)
      primaryKeyClause:
        omit: true
        # Or custom primary key definition:
        # definition: CONSTRAINT pk_lock PRIMARY KEY(lock_id)
```

### How It Works

1. **Before Execution**: TotalSchema attempts to acquire a lock by inserting a record into the lock table
2. **Lock Held**: If another process holds the lock, waits up to `lock.timeout.seconds` (default: 300 seconds)
3. **Lock Acquired**: Once acquired, proceeds with change script execution
4. **After Completion**: Releases lock by deleting the record (even if errors occur)
5. **Timeout**: If timeout expires, throws an exception and does not execute changes

### What the Lock Table Stores

Each active lock record contains:

| Column | Description | Example |
|--------|-------------|---------|
| `lock_id` | Unique identifier for the lock | `totalschema_main_lock` |
| `locked_at` | Timestamp when lock was acquired | `2026-01-13 10:30:00` |
| `locked_by` | User who initiated the process | `john.doe` or `CI_SERVICE_ACCOUNT` |
| `lock_owner` | Process/application identifier | `totalschema-maven-plugin` |
| `host_name` | Machine hostname | `build-server-01.example.com` |
| `process_id` | Operating system process ID | `12345` |

This information helps identify which process is holding the lock if troubleshooting is needed.

### Lock Table vs State Table Location

You have three options for organizing your lock and state tables:

**Option 1: Same Database, Same Schema** (Simplest)
```yaml
# State table location (database connection)
stateRepository:
  type: database
  database:
    jdbc:
      url: jdbc:h2:file:./totalschema/db
    username: totalschema
    password: ${tsPassword}

# Lock table location (same database)
lock:
  type: database
  database:
    jdbc:
      url: jdbc:h2:file:./totalschema/db
    username: totalschema
    password: ${tsPassword}
```

**Option 2: Same Database, Different Schemas** (Organized)
```yaml
# State table location (in 'totalschema_state' schema)
stateRepository:
  type: database
  database:
    jdbc:
      url: jdbc:postgresql://localhost/totalschema_db
    username: totalschema
    table:
      catalog: totalschema_state
      beforeCreate:
        sql: CREATE SCHEMA IF NOT EXISTS totalschema_state

# Lock table location (in 'totalschema_locks' schema)
lock:
  type: database
  database:
    jdbc:
      url: jdbc:postgresql://localhost/totalschema_db
    username: totalschema
    table:
      catalog: totalschema_locks
      beforeCreate:
        sql: CREATE SCHEMA IF NOT EXISTS totalschema_locks
```

**Option 3: Separate Databases** (Maximum Isolation)
```yaml
# State table location (dedicated database server)
stateRepository:
  type: database
  database:
    jdbc:
      url: jdbc:postgresql://state-db:5432/state
    username: state_user

# Lock table location (separate server/database)
lock:
  type: database
  database:
    jdbc:
      url: jdbc:postgresql://lock-db:5432/locks
    username: lock_user
```

### Viewing Active Locks

You can query the lock table directly to see active locks:

```sql
SELECT * FROM totalschema_lock;
```

If TotalSchema is waiting for a lock, you'll see output like:
```
Waiting for lock... (attempt 1 of 60)
Lock is currently held by process 12345 on host build-server-01.example.com
```

### Manually Releasing a Stuck Lock

If a TotalSchema process is killed or crashes while holding a lock, you may need to manually release it:

```sql
-- View current locks
SELECT * FROM totalschema_lock;

-- Release the lock (use with caution!)
DELETE FROM totalschema_lock WHERE lock_id = 'totalschema_main_lock';
```

**Warning**: Only do this if you're certain no TotalSchema process is actually running. Releasing a lock while a process is running could lead to concurrent execution issues.

### Disable Locking (Single Environment)

```yaml
lock:
  type: none
```

Use this for:
- Local development environments
- Single-user scenarios
- When you're certain concurrent execution won't occur

**Not recommended for**:
- CI/CD pipelines
- Shared development/test environments
- Production environments

## State Table Management

### Overview

The **state table** is where TotalSchema records which change scripts have been applied. You configure a database connection (the "state database") to specify where this table should be stored. The state table can live in:
- A dedicated database (isolated tracking)
- Your application database (consolidated storage)
- Any JDBC-accessible database

### State Table Structure

TotalSchema creates and manages the state table automatically:

```sql
CREATE TABLE totalschema_state (
    change_file_id VARCHAR(255) PRIMARY KEY,
    file_hash VARCHAR(64),
    apply_timestamp TIMESTAMP,
    applied_by VARCHAR(255)
);
```

### Alternative: CSV File-Based State Storage

As an alternative to database state storage, TotalSchema supports storing state in CSV files. This approach stores the migration state in CSV files which can be committed to the same source code repository as the change files themselves.

#### CSV State Configuration

```yaml
stateRepository:
  type: csv
  file:
    path:
      pattern: totalschema/state/${environment}/state-${environment}.csv
```

#### Benefits of CSV State Storage

✅ **Version Control Integration**: State is versioned alongside change files in source control  
✅ **Human-Readable Format**: CSV format can be inspected and manually edited if needed  
✅ **No Database Dependency**: No external database required for state tracking  
✅ **Simple Backup**: Easy to backup and restore as part of repository backups  
✅ **Transparency**: Team can see state changes in pull requests and code reviews  

#### Critical Limitations

⚠️ **This approach has significant limitations that must be understood:**

**Manual State Management**
- Relies on developers to properly commit and push the state file after migrations
- Forgotten commits lead to state inconsistencies across team members
- No automatic synchronization between environments

**No Concurrent Execution Protection**
- Does not provide any solution against concurrent migration execution
- If multiple developers or CI/CD pipelines run migrations simultaneously, state corruption can occur
- File system locks only protect against concurrent access on the same machine, not across distributed systems

**Merge Conflicts**
- In distributed teams, the state file can experience merge conflicts when multiple branches apply different migrations
- Resolving conflicts requires manual intervention and understanding of migration history

**Distributed Development Challenges**
- State file may become out of sync across different developer machines
- No centralized coordination of what has been applied where

#### CSV State File Format

The CSV file contains the following columns:

```csv
CHANGE_FILE_ID,FILE_HASH,APPLY_TIMESTAMP,APPLIED_BY
1.X/1.0.0/0001.create_users.DEV.apply.mydb.sql,a3f5e9...,2026-01-13T10:30:00Z,john.doe
1.X/1.0.0/0002.create_orders.DEV.apply.mydb.sql,b7c2d1...,2026-01-13T10:30:05Z,john.doe
```

#### When to Use CSV State Storage

**✅ Suitable for:**
- Single developer projects
- Very small teams (2-3 developers) with disciplined workflow
- Sequential migration execution (one deployment at a time)
- Proof-of-concept or prototype projects
- Environments where database access is restricted or complicated

**❌ NOT Recommended for:**
- Larger teams (4+ developers)
- Distributed development environments
- CI/CD environments with parallel execution
- Production environments requiring high reliability
- Scenarios where multiple developers/pipelines might deploy simultaneously

**🔄 Migration Recommendation:**

For larger teams or production environments, **migrate to database-backed state storage** which provides:
- Proper transaction support
- Distributed locking mechanisms
- Centralized state coordination
- Protection against concurrent execution

#### Example: Small Team Workflow with CSV State

```yaml
# totalschema.yml - CSV state configuration
stateRepository:
  type: csv
  file:
    path:
      pattern: totalschema/state/${environment}/state-${environment}.csv

# Workflow:
# 1. Developer runs: bin/totalschema.sh apply -e DEV
# 2. TotalSchema updates: totalschema/state/DEV/state-DEV.csv
# 3. Developer commits: git add totalschema/state/DEV/state-DEV.csv
# 4. Developer pushes: git push
# 5. Team pulls latest state: git pull
```

⚠️ **Critical**: Developers must remember to commit and push the state file after every deployment, or team members will have inconsistent state.

#### Migrating from CSV to Database State

If you outgrow CSV state storage, you can migrate to database state:

1. **Export current CSV state** to preserve history
2. **Update configuration** to use database state repository:

```yaml
stateRepository:
  type: database
  database:
    username: ${systemDatabaseSchema}
    password: ${dbPassword}
    jdbc:
      url: ${systemDatabaseJdbcUrl}
    table:
      catalog: ${systemDatabaseSchema}
```

3. **Import CSV state** into database (manual SQL inserts based on CSV content)
4. **Commit configuration change** and remove CSV state files from repository

### Customizing the State Table

Similar to the state table, you can customize the lock table structure:

```yaml
stateRepository:
  type: database
  database:
    username: ${systemDatabaseSchema}
    password: ${dbPassword}
    jdbc:
      url: ${systemDatabaseJdbcUrl}
    table:
      # Table location
      catalog: my_schema
      schema: my_schema
      # Execute SQL before table creation
      beforeCreate:
        sql: CREATE SCHEMA IF NOT EXISTS my_schema
      # Customize column types
      column:
        change_file_id:
          type: VARCHAR(500)
        file_hash:
          type: CHAR(64)
        apply_timestamp:
          type: DATETIME
        applied_by:
          type: VARCHAR(100)
      # Primary key configuration
      primaryKeyClause:
        omit: true
        # Or custom primary key definition:
        # definition: CONSTRAINT pk_state PRIMARY KEY(change_file_id)
```

## Variables and Configuration

### Variable Declaration

Variables can be defined globally or per-environment:

```yaml
# Global variables
variables:
  appName: MyApplication
  dbPort: 5432
  defaultSchema: public

# Environment-specific variables (override global)
environments:
  DEV:
    variables:
      dbHost: localhost
      dbPassword: devpass
  PROD:
    variables:
      dbHost: prod-db.example.com
      dbPassword: securepass
```

### Variable Substitution

Variables are substituted using `${variableName}` syntax:

```yaml
assets:
  mydb:
    type: jdbc
    jdbc:
      url: jdbc:postgresql://${dbHost}:${dbPort}/${dbName}
    db:
      password: ${dbPassword}
```

### Using Variables in Scripts

Variables are resolved before script execution:

```sql
-- Variables in SQL
CREATE SCHEMA ${defaultSchema};
CREATE USER ${appName}_user IDENTIFIED BY '${dbPassword}';
```

### System Property Variables

System properties are automatically available:

```bash
java -DdbHost=localhost -jar totalschema-core.jar changes apply -e DEV
```

System properties override file-based variables.

### Listing Variables

```bash
# List all variables for an environment
bin/totalschema.sh variables list -e DEV
```

## Secret Management

TotalSchema provides built-in secret management capabilities to securely handle sensitive information like database passwords, API keys, and SSH credentials. Instead of storing plain-text secrets in configuration files, you can encrypt them and let TotalSchema decrypt them at runtime.

### Why Use Secret Management?

- **Security**: Keeps sensitive credentials encrypted in version control
- **Compliance**: Meets security requirements for password storage
- **Flexibility**: Supports file-based and custom secret management
- **Integration**: Works seamlessly with existing configuration

### Secret Encryption

TotalSchema can encrypt sensitive values for secure storage:

```bash
# Encrypt a file containing secrets (provide password inline)
bin/totalschema.sh secrets encrypt-string --clearTextValue FOOBAR --password "YourSecurePassword123"

# Or read password from a file
bin/totalschema.sh secrets encrypt-string --clearTextValue FOOBAR --passwordFile /secure/path/encryption.key
```

The value output then can be used in the configuration, which will then be decrypted using the password.


Encryption of sensitive files (e.g. private keys):

```bash
# Encrypt a file containing secrets (provide password inline)
bin/totalschema.sh secrets encrypt-file --clearTextFile secrets.txt --encryptedFile secrets.txt.secret --password "YourSecurePassword123"

# Or read password from a file
bin/totalschema.sh secrets encrypt-file --clearTextFile secrets.txt --encryptedFile secrets.txt.secret --passwordFile /secure/path/encryption.key
```

**Important**: The `--password` or `--passwordFile` parameter is required. TotalSchema does not use environment variables or interactive prompts for encryption/decryption.


### Using Encrypted Secrets in Configuration

TotalSchema uses **expression lookup syntax** to securely reference and decrypt secrets in your configuration files. Encrypted secrets are referenced using the special `${prefix:argument}` syntax, which tells TotalSchema how to handle the secure value.

#### Expression Lookup Syntax for Secrets

The expression lookup syntax uses the format `${lookupPrefix:argument}` and is built on Apache Commons Text's StringSubstitutor with custom security-focused lookup functions.

**Available Secret Lookups:**

| Lookup Prefix       | Purpose                                                               | Use Case                                             |
|---------------------|-----------------------------------------------------------------------|------------------------------------------------------|
| `secret`            | Decrypt an encrypted secret string inline                             | Database passwords, API keys, tokens                 |
| `secretFileContent` | Decrypt an encrypted file and return its content                      | SSH private keys, certificates, multi-line secrets   |
| `decodedFilePath`   | Decrypt an encrypted file and return path to temporary decrypted file | Files needed by external tools (e.g., SSH key files) |

**Syntax Rules:**

- **Regular variables:** `${variableName}` - looks up variables from your configuration
- **Secret lookups:** `${lookupPrefix:argument}` - decrypts the secret using the specified lookup function
- The part after the colon (`:`) is the encrypted value or file path
- Nested substitution is supported: `${secret:${encryptedPasswordVar}}`

**Important:** When using encrypted string values (output from `secrets encrypt-string`), you must wrap the entire `!SECRET;1.0;...` output in the `${secret:...}` lookup expression.

#### Basic Secret Configuration Examples

```yaml
variables:
  # Store encrypted password as variable
  dbPassword: '${secret:!SECRET;1.0;0F0E0000000898660FA93EB1FAA500000010A230E4F7CCC4BCB1808C5C35F643861B0000001001D4A16E28AD33D31C8671753E926495}'

# Environment-specific secrets
environments:
  PROD:
    variables:
      dbPassword: '${secret:!SECRET;1.0;...}'
      apiKey: '${secret:!SECRET;1.0;...}'

# Secrets in asset configuration
assets:
  myserver:
    type: ssh-script  # or ssh-commands
    host: server.example.com
    user: deployuser
    password: '${secret:!SECRET;1.0;...}'
```

#### Advanced Secret Handling Examples

```yaml
variables:
  # Regular configuration values
  dbHost: localhost
  dbPort: 5432
  
  # Encrypted secret stored as variable
  encryptedPassword: '${secret:!SECRET;1.0;0F0E0000000898660FA93EB1FAA500000010A230E4F7CCC4BCB1808C5C35F643861B0000001001D4A16E28AD33D31C8671753E926495}'
  
  # Path to encrypted SSH key file
  sshKeyPath: /secure/keys/id_rsa.secret

assets:
  myDatabase:
    type: jdbc
    connection:
      # Regular variable interpolation
      url: jdbc:postgresql://${dbHost}:${dbPort}/mydb
      
      # Option 1: Decrypt secret inline
      password: ${secret:!SECRET;1.0;0F0E...}
      
      # Option 2: Reference encrypted variable (already wrapped with ${secret:...})
      password: ${encryptedPassword}
  
  myServer:
    type: ssh
    hostname: server.example.com
    user:
      name: deployuser
      
      # Decrypt password from an encrypted file and use its content
      password: ${secretFileContent:/secure/password.txt.secret}
      
      # Decrypt SSH private key file and get path to temporary decrypted file
      privateKeyFile: ${decodedFilePath:${sshKeyPath}}
```

**How Secret Lookups Work:**

1. TotalSchema parses your configuration and identifies `${prefix:argument}` expressions
2. For secret lookups (e.g., `secret`, `secretFileContent`, `decodedFilePath`), it uses the SecretsManager
3. The SecretsManager decrypts the value using the password provided via `--password` or `--passwordFile`
4. The decrypted value is substituted into the configuration at runtime
5. For `decodedFilePath`, a temporary file is created with the decrypted content and automatically cleaned up

#### Runtime Decryption

When TotalSchema decrypts secrets during change execution, it uses the same `--password` or `--passwordFile` parameters:

```bash
# Decrypt secrets at runtime using password file
bin/totalschema.sh apply -e DEV --passwordFile /secure/path/encryption.key

# Or inline password
bin/totalschema.sh apply -e DEV --password "YourSecurePassword123"
```

### Extending Secret Management (Advanced)

You can add custom secret lookup functions to integrate with external secret management systems (e.g., HashiCorp Vault, AWS Secrets Manager, Azure Key Vault) by implementing the `ExpressionLookup` SPI interface.

**Example: Custom Vault Integration**

```java
package com.example.security;

import io.github.totalschema.spi.lookup.ExpressionLookup;

public class VaultSecretLookup implements ExpressionLookup {
    @Override
    public String getKey() {
        return "vault";  // Use as ${vault:secret/path}
    }
    
    @Override
    public String apply(String secretPath) {
        // Integrate with your vault system
        return retrieveFromVault(secretPath);
    }
    
    private String retrieveFromVault(String path) {
        // Your HashiCorp Vault integration logic
        // ...
    }
}
```

**Register via Java SPI:** Create file `META-INF/services/io.github.totalschema.spi.lookup.ExpressionLookup`:
```
com.example.security.VaultSecretLookup
```

**Usage in configuration:**
```yaml
variables:
  dbPassword: ${vault:database/postgres/prod/password}
  apiKey: ${vault:api/external-service/key}
```

This allows you to centralize secret management while maintaining the same syntax and workflow in your TotalSchema configurations.

### CI/CD Integration

For CI/CD pipelines, store the encryption password as a secure secret and pass it via `--password` or create a password file.

**📖 [Complete CI/CD Examples](totalschema-cli/user_manual/README.md#cicd-integration)** - See the CLI Usage Guide for detailed GitHub Actions, Jenkins, GitLab CI, and Docker examples.

#### Quick Example: GitHub Actions

```yaml
name: Deploy Database Changes

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Create password file
        run: |
          echo "${{ secrets.TOTALSCHEMA_ENCRYPTION_KEY }}" > /tmp/encryption.key
          chmod 600 /tmp/encryption.key
          
      - name: Apply Changes
        run: |
          bin/totalschema.sh apply -e PROD --passwordFile /tmp/encryption.key
          
      - name: Cleanup
        if: always()
        run: rm -f /tmp/encryption.key
```

For more examples including Jenkins, GitLab CI, and Docker integration, see the [CLI Usage Guide](totalschema-cli/user_manual/README.md#cicd-integration).


## CLI Command Reference

For CLI users, see the **[CLI Usage Guide](totalschema-cli/user_manual/README.md)** for complete command reference, examples, and detailed usage instructions.

**Quick Reference:**

```bash
# Apply changes
bin/totalschema.sh apply -e <environment>

# Apply with preview (dry-run)
bin/totalschema.sh apply -e <environment> --dry-run

# Revert changes
bin/totalschema.sh revert -e <environment>

# Revert with preview (dry-run)
bin/totalschema.sh revert -e <environment> --dry-run

# Show pending apply changes
bin/totalschema.sh show pending-apply -e <environment>

# Show applicable reverts
bin/totalschema.sh show applicable-revert -e <environment>

# Display state
bin/totalschema.sh state display -e <environment>

# Validate changes
bin/totalschema.sh validate -e <environment>

# List environments
bin/totalschema.sh environments list

# List variables
bin/totalschema.sh variables list -e <environment>

# Encrypt secrets
bin/totalschema.sh secrets encrypt-string --clearTextValue <value> --password <password>
```

See [CLI-USAGE.md](totalschema-cli/user_manual/README.md) for the complete command reference.

## Maven Plugin Usage

For Maven users, see the **[Maven Plugin Guide](docs/MAVEN-USAGE.md)** for complete configuration, lifecycle integration, profiles, and detailed usage instructions.

**Quick Reference:**

```bash
# Apply changes
mvn totalschema:run -Denvironment=DEV

# Show pending changes
mvn totalschema:run -Darguments="execute,show,pending,-e,DEV"

# Validate changes
mvn totalschema:run -Darguments="validate,-e,DEV"

# Display state
mvn totalschema:run -Darguments="state,display,-e,PROD"
```

**Basic Plugin Configuration:**

```xml
<plugin>
    <groupId>io.github.totalschema</groupId>
    <artifactId>totalschema-maven-plugin</artifactId>
    <version>1.0-SNAPSHOT</version>
    <dependencies>
        <dependency>
            <groupId>io.github.totalschema</groupId>
            <artifactId>totalschema-core</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>42.7.3</version>
        </dependency>
    </dependencies>
    <configuration>
        <arguments>
            <argument>execute</argument>
            <argument>apply</argument>
            <argument>-e</argument>
            <argument>${environment}</argument>
        </arguments>
    </configuration>
</plugin>
```

See [MAVEN-USAGE.md](docs/MAVEN-USAGE.md) for lifecycle integration, multi-module projects, profiles, and CI/CD examples.

## Groovy Script Support

TotalSchema supports Groovy scripts with automatic `sql` object injection for database operations.

### Example Groovy Change Script

```groovy
// 0001.complex_data_transformation.DEV.apply.mydb.groovy
import groovy.sql.Sql

// The 'sql' object is automatically available
println "Starting data transformation..."

// Query data
sql.eachRow("SELECT id, name FROM old_table") { row ->
    // Transform and insert
    def transformedName = row.name.toUpperCase()
    sql.execute("""
        INSERT INTO new_table (id, transformed_name, created_at)
        VALUES (?, ?, CURRENT_TIMESTAMP)
    """, [row.id, transformedName])
}

println "Transformation completed successfully!"
```

### Benefits

- Full programmatic control over changes
- Complex data transformations
- Conditional logic
- Error handling
- Access to full Groovy language features

## Advanced Configuration Examples

### Multi-Database DWH Setup

```yaml
assets:
  # Operational database
  operational:
    type: jdbc
    driver:
      class: oracle.jdbc.OracleDriver
    jdbc:
      url: jdbc:oracle:thin:@//prod-ops:1521/${dbName}
    db:
      username: ops_user
      password: ${opsPassword}

  # Staging database
  staging:
    type: jdbc
    driver:
      class: org.postgresql.Driver
    jdbc:
      url: jdbc:postgresql://staging-db:5432/${dbName}
    db:
      username: staging_user
      password: ${stagingPassword}

  # Data warehouse
  warehouse:
    type: jdbc
    driver:
      class: com.google.cloud.bigquery.BiQueryDriver
    jdbc:
      url: jdbc:bigquery://project-id.dataset
    db:
      username: service_account
      password: ${bqPassword}

  # ETL server
  etlserver:
    type: ssh
    hostname: etl-server.example.com
    user:
      name: etl_user
      password: ${etlPassword}
```

### Complex Deployment Scenario

```
changes/
├── 1.X/
│   └── 1.0.0/
│       ├── 0001.create_schema.apply.operational.sql
│       ├── 0002.create_schema.apply.staging.sql
│       ├── 0003.deploy_etl_scripts.apply.etlserver.sh
│       ├── 0004.create_warehouse_tables.apply.warehouse.sql
│       └── 0005.provision_gcp_cluster.apply.gcp.sh
```

This allows a single deployment to:
1. Create tables in the operational database
2. Create corresponding staging tables
3. Deploy ETL scripts to the ETL server
4. Create warehouse tables in BigQuery
5. Provision a new GCP cluster for processing

All orchestrated as one atomic operation, with state tracked across all systems.

## Best Practices

### 1. Version Your Scripts Directory

Keep change scripts in version control (Git) alongside your application code.

### 2. Never Modify Applied Scripts

Once a change script is applied to any environment, never modify it. Create a new script instead.

### 3. Use Descriptive Names

```
# Good
0001.create_users_table.DEV.apply.mydb.sql
0002.add_email_index_to_users.DEV.apply.mydb.sql

# Bad
0001.change.DEV.apply.mydb.sql
0002.changes.DEV.apply.mydb.sql
```

### 4. Test Changes in Lower Environments

Always test in DEV → QA → PROD sequence.

### Step 5: Provide Revert Scripts

Create revert scripts for all non-trivial changes to enable rollback.

### 6. Use apply_always for Idempotent Operations

Views, stored procedures, and functions should use `apply_always` with CREATE OR REPLACE.

### 7. Enable Hash Validation

```yaml
validation:
  type: contentHash
```

This catches accidental modifications to applied change scripts.

### 8. Use Locking in Shared Environments

```yaml
lock:
  type: database
```

Prevents race conditions when multiple developers or CI/CD pipelines run simultaneously.

### 9. Keep Change Scripts Small

Each change script should do one logical thing. Multiple small scripts are better than one large script.

### 10. Document Complex Changes

Add comments explaining the purpose and any special considerations:

```sql
-- 0010.migrate_user_data.DEV.apply.mydb.sql
-- Purpose: Migrate users from legacy auth system to new system
-- Prerequisites: User sync must be complete
-- Rollback: Use 0010.migrate_user_data.DEV.revert.mydb.sql
-- Duration: ~10 minutes on production dataset

UPDATE users SET auth_system = 'new' WHERE auth_system = 'legacy';
```

## Troubleshooting

### Change Script Not Detected

**Problem**: Change script file exists but TotalSchema doesn't see it.

**Solutions**:
- Verify file naming convention is correct
- Check that file is in a directory under `changes/`
- Ensure environment in filename matches `-e` parameter
- Verify asset name matches configuration

### State Database Connection Failed

**Problem**: Cannot connect to the database where the state table is stored.

**Solutions**:
- Verify JDBC URL (`stateRepository.database.jdbc.url`) is correct
- Check that JDBC driver is on classpath
- Verify credentials (username/password)
- Ensure the target database/schema exists (or use `stateRepository.database.table.beforeCreate.sql` to auto-create it)

### Lock Timeout

**Problem**: `Lock timeout exceeded` error.

**Solutions**:
- Check if another TotalSchema process is running
- Increase `lock.timeout.seconds`
- Manually release lock (clear lock table) if process was killed
- Temporarily disable locking during troubleshooting

### Validation Failed

**Problem**: Validation reports modified files.

**Solutions**:
- If modification was intentional: create a new change script instead
- If modification was accidental: revert to original content
- Last resort: manually update hash in the state table (NOT RECOMMENDED)

### Asset Not Found

**Problem**: `Asset 'mydb' not found` error.

**Solutions**:
- Verify asset is defined in `totalschema.yml`
- Check asset name spelling in configuration and filename
- Ensure environment-specific asset overrides are correct

## FAQ

**Q: Can I run TotalSchema against an existing database?**
A: Yes. TotalSchema will create its state table (in the configured state database location) on first run and begin tracking changes from that point forward. Your existing application database remains unchanged except for any change scripts you explicitly run.

**Q: What happens if a change script fails halfway through?**
A: The change will NOT be recorded in the state table, so it will retry on next execution. Ensure your scripts are idempotent or handle partial execution.

**Q: Can I use TotalSchema with cloud databases like BigQuery or Redshift?**
A: Yes, as long as a JDBC driver is available. See `totalschema-gcp-bigquery` module for BigQuery support.

**Q: How do I handle environment-specific data?**
A: Use environment-specific variables and separate change script files per environment, or use conditional logic in Groovy scripts.

**Q: Can multiple teams use TotalSchema on the same database?**
A: Yes, but consider using different state database schemas or prefixes to avoid conflicts.

**Q: What about database vendor-specific SQL?**
A: Use separate change script files per database type: `0001.create_table.apply.postgres.sql` and `0001.create_table.apply.oracle.sql`

**Q: Can TotalSchema manage cloud infrastructure?**
A: Yes! You can use shell scripts to provision cloud resources (GCP clusters, AWS resources, etc.) alongside your database changes, all tracked and versioned together.

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes with tests
4. Submit a pull request

## Documentation

- **[CLI Usage Guide](totalschema-cli/user_manual/README.md)** - Complete command-line interface documentation
- **[Maven Plugin Guide](docs/MAVEN-USAGE.md)** - Complete Maven plugin documentation
- **[Main README](README.md)** - This document (architecture, concepts, configuration)

## Support

- **Issues**: https://github.com/totalschema/totalschema/issues
- **Documentation**: https://github.com/totalschema/totalschema
- **License**: GNU Affero General Public License v3.0

## License

This project is licensed under the GNU Affero General Public License version 3 License. 
See the LICENSE file for details.
