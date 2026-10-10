# TotalSchema GCP Bigtable Connector

A connector for managing Google Cloud Bigtable infrastructure and schema evolution through declarative YAML and Groovy scripts.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Supported Actions](#supported-actions)
- [Garbage Collection Rules](#garbage-collection-rules)
- [Examples](#examples)
- [Architecture](#architecture)
- [Documentation](#documentation)
- [Testing](#testing)
- [License](#license)

---

## Overview

The Bigtable connector enables declarative management of Google Cloud Bigtable resources within TotalSchema's deployment orchestration framework. Unlike one-off scripts or manual console operations, TotalSchema tracks applied changes, supports environment-specific configurations, and provides a complete audit trail of infrastructure evolution.

**Use this connector to:**

- **Manage table schemas** — create tables, add/modify/remove column families, configure garbage collection rules
- **Automate backups** — create, restore, and delete backups with duration or timestamp-based expiration
- **Control access** — grant and revoke IAM permissions at the table level
- **Configure replication** — enable eventual or strong consistency across clusters
- **Optimize performance** — configure autoscaling, manage table splits
- **Manage instances** — create and delete Bigtable instances (development/testing scenarios)

**Key Differentiators:**

- ✅ **Change tracking** — never apply the same change twice
- ✅ **Environment isolation** — deploy to DEV, QA, PROD with different configurations
- ✅ **Variable substitution** — parameterize scripts for reusability
- ✅ **Reflection-free** — ServiceLoader-based architecture for fast startup
- ✅ **Comprehensive** — 19 action types covering the full Bigtable lifecycle

---

## Features

### 🎯 Declarative YAML Syntax

Define infrastructure as code with clean, human-readable YAML:

```yaml
CreateTable:
  tableId: users
  families:
    - name: profile
      maxVersions: 1
    - name: audit
      maxAge: 365d
    - name: cache
      union:
        - maxVersions: 3
        - maxAge: 24h
```

### 📦 Schema Evolution Without Downtime

Add column families to existing tables without recreation:

```yaml
CreateColumnFamily:
  tableId: users
  familyName: analytics
  maxAge: 30d
```

### 🔒 IAM Policy Management

Grant and revoke table-level access with fine-grained control:

```yaml
SetTableIamPolicy:
  tableId: customer_data
  bindings:
    - role: roles/bigtable.reader
      members:
        - serviceAccount:analytics@project.iam.gserviceaccount.com
```

### 💾 Backup and Restore

Automate disaster recovery workflows:

```yaml
CreateBackup:
  sourceTableId: users
  backupId: users_backup_${DATE}
  expireTime: 30d
  clusterId: prod-cluster-us-east1
```

### 🌐 Variable Substitution

Deploy the same scripts across environments with different configurations:

```yaml
CreateTable:
  tableId: ${TABLE_PREFIX}_users
  families:
    - name: data
      maxAge: ${RETENTION_DAYS}d
```

### 🚀 High Performance

- **Zero reflection** — ServiceLoader-based plugin architecture
- **Fast startup** — ~60ms vs Spring's ~2.5s
- **Jackson data binding** — efficient YAML deserialization

### 📊 Comprehensive Action Support

19 action types covering the complete Bigtable lifecycle:

| Category | Actions |
|----------|---------|
| **Tables** | CreateTable, DeleteTable |
| **Column Families** | CreateColumnFamily, ModifyColumnFamily, ResetColumnFamily, DeleteColumnFamily |
| **Backup & Restore** | CreateBackup, DeleteBackup, RestoreTable |
| **IAM** | SetTableIamPolicy, RemoveTableIamPolicy |
| **Replication** | EnableTableReplication, DisableTableReplication |
| **Autoscaling** | SetTableAutoscaling, DisableTableAutoscaling |
| **Table Splits** | SetTableSplits, ClearTableSplits |
| **Instances** | CreateInstance, DeleteInstance |

---

## Quick Start

### 1. Prerequisites

- **Google Cloud project** with Bigtable API enabled
- **Bigtable instance** (or use TotalSchema to create one)
- **Service account** with `roles/bigtable.admin`
- **TotalSchema CLI** installed (v1.3.0 or later)

### 2. Authenticate

```bash
# Local development
gcloud auth application-default login

# CI/CD pipelines
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json
```

### 3. Configure Connector

Create or update `totalschema.yml`:

```yaml
connectors:
  mybigtable:
    type: bigtable
    projectId: my-gcp-project
    instanceId: my-bigtable-instance

stateRepository:
  type: database
  database: mybigtable
```

### 4. Create a Change Script

Create `totalschema/0001.create_users_table.DEV.apply.mybigtable.yml`:

```yaml
CreateTable:
  tableId: users
  families:
    - name: profile
      maxVersions: 1
    - name: activity
      maxAge: 90d
```

### 5. Apply Changes

```bash
totalschema.sh apply -e DEV -w /path/to/workspace
```

Output:
```
✓ Applied: 0001.create_users_table.DEV.apply.mybigtable.yml
✓ Table 'users' created successfully
```

---

## Configuration

### Basic Configuration

```yaml
connectors:
  mybigtable:
    type: bigtable
    projectId: my-gcp-project         # GCP project ID
    instanceId: my-bigtable-instance  # Bigtable instance ID
```

### Advanced Configuration

```yaml
connectors:
  mybigtable:
    type: bigtable
    projectId: my-gcp-project
    instanceId: my-bigtable-instance
    
    # Custom admin endpoint (Private Service Connection or testing)
    # Default: bigtableadmin.googleapis.com
    adminEndpoint: bigtableadmin-custom.p.googleapis.com:443
    
    # Custom data endpoint (Private Service Connection or testing)
    # Default: bigtable.googleapis.com
    dataEndpoint: bigtable-custom.p.googleapis.com:443
    
    # App profile ID for routing data requests
    # Default: "default"
    appProfileId: my-app-profile
    
    # Data client configuration (disabled by default)
    dataclient:
      enabled: true  # Enable for row-level data operations
    
    # Connection health check (default: true)
    connectionCheck.enabled: true
```

**Client Configuration:**

- **`dataclient.enabled`** — **Disabled by default**. Enable only if you need row-level data operations (SetCell, DeleteCell, DeleteCellVersions). Most schema migration workflows only require admin operations and don't need the data client. Keeping it disabled avoids unnecessary client initialization and configuration overhead.

**Endpoint Configuration:**

- **`adminEndpoint`** — Used for table management operations (create/delete tables, column families, backups, IAM policies). Required for YAML-based schema management actions. **Always available.**
- **`dataEndpoint`** — Used for read/write data operations. Required for Groovy scripts that interact with table data using `BigtableDataClient`. **Only available when `dataclient.enabled: true`.**
- **`appProfileId`** — Routing profile for data operations. Use different profiles for isolation, QoS, or single-cluster routing. **Only used when data client is enabled.**

**When to enable the data client:**

- **Seeding initial data** — Insert reference data or default rows during deployment
- **Data migrations** — Copy or transform data between tables or column families
- **GDPR compliance** — Delete specific cell versions within time ranges
- **Groovy scripts** — Access `bigtableDataClient` binding for custom data operations

**When to use custom endpoints:**

- **Private Service Connect (PSC)** — Access Bigtable through a private endpoint in your VPC
- **Testing** — Point to Bigtable emulator or test environment
- **Multi-region optimization** — Route to regional endpoints

### Multi-Environment Setup

```yaml
variables:
  backupRetentionDays: 30
  clusterName: default-cluster

environments:
  DEV:
    variables:
      tablePrefix: dev
      clusterName: dev-cluster-us-central1
      backupRetentionDays: 7
  
  PROD:
    variables:
      tablePrefix: prod
      clusterName: prod-cluster-us-east1
      backupRetentionDays: 90
```

---

## Supported Actions

### Table Management

#### CreateTable

Creates a new Bigtable table with optional column families and GC rules.

```yaml
CreateTable:
  tableId: users
  families:
    - name: profile
      maxVersions: 1
    - name: audit
      maxAge: 365d
```

**Properties:**
- `tableId` (required) — unique table identifier
- `families` (optional) — list of column families with GC rules

#### DeleteTable

Permanently deletes a table and all its data.

```yaml
DeleteTable:
  tableId: old_table
```

⚠️ **WARNING:** This operation is irreversible.

---

### Column Family Management

#### CreateColumnFamily

Adds a new column family to an existing table without downtime.

```yaml
CreateColumnFamily:
  tableId: users
  familyName: analytics
  maxAge: 30d
```

#### ModifyColumnFamily

Updates garbage collection rules on an existing column family.

```yaml
ModifyColumnFamily:
  tableId: users
  familyName: profile
  maxVersions: 10
```

#### ResetColumnFamily

Removes all garbage collection rules from a column family.

```yaml
ResetColumnFamily:
  tableId: users
  familyName: profile
```

⚠️ **WARNING:** Without GC rules, storage costs will grow continuously.

#### DeleteColumnFamily

Permanently removes a column family and all its data.

```yaml
DeleteColumnFamily:
  tableId: users
  familyName: old_analytics
```

⚠️ **WARNING:** This operation is irreversible.

---

### Backup and Restore

#### CreateBackup

Creates a point-in-time backup of a table.

```yaml
# Duration-based expiration
CreateBackup:
  sourceTableId: users
  backupId: users_backup_20260731
  expireTime: 30d
  clusterId: prod-cluster-us-east1

# Timestamp-based expiration
CreateBackup:
  sourceTableId: users
  backupId: users_backup_20260731
  expireTime: "2026-08-31T00:00:00Z"
  clusterId: prod-cluster-us-east1

# HOT backup (faster restore)
CreateBackup:
  sourceTableId: users
  backupId: users_backup_hot
  expireTime: 7d
  clusterId: prod-cluster-us-east1
  backupType: HOT
```

#### RestoreTable

Restores a table from a backup.

```yaml
# Restore to new table
RestoreTable:
  backupId: users_backup_20260731
  tableId: users_restored
  clusterId: prod-cluster-us-east1

# Restore and overwrite existing table
RestoreTable:
  backupId: users_backup_20260731
  tableId: users
  clusterId: prod-cluster-us-east1
  overwrite: true
```

#### DeleteBackup

Deletes a backup to free storage.

```yaml
DeleteBackup:
  backupId: users_backup_20260701
  clusterId: prod-cluster-us-east1
```

---

### IAM Policy Management

#### SetTableIamPolicy

Grants IAM permissions to users, service accounts, or groups.

```yaml
SetTableIamPolicy:
  tableId: users
  bindings:
    - role: roles/bigtable.reader
      members:
        - serviceAccount:analytics@project.iam.gserviceaccount.com
        - user:analyst@company.com
        - group:data-team@company.com
    - role: roles/bigtable.user
      members:
        - serviceAccount:app-backend@project.iam.gserviceaccount.com
```

**Behavior:**
- Adds bindings to existing policy (preserves other bindings)
- If role already exists, new members are added
- Uses read-modify-write pattern (safe for concurrent updates)

**Common Roles:**
- `roles/bigtable.reader` — read-only access
- `roles/bigtable.user` — read and write access
- `roles/bigtable.admin` — full table administration

#### RemoveTableIamPolicy

Revokes IAM permissions from specific members.

```yaml
RemoveTableIamPolicy:
  tableId: users
  bindings:
    - role: roles/bigtable.reader
      members:
        - user:ex-employee@company.com
```

**Use Cases:**
- Employee offboarding
- Decommissioning services
- Security incident response

---

### Replication Management

#### EnableTableReplication

Enables replication for a table across clusters.

```yaml
# Eventual consistency
EnableTableReplication:
  tableId: users
  consistencyMode: EVENTUAL

# Strong consistency (transactional)
EnableTableReplication:
  tableId: financial_transactions
  consistencyMode: STRONG
```

**Consistency Modes:**
- `EVENTUAL` — lower latency, eventual consistency
- `STRONG` — transactional consistency, single-row transactions

#### DisableTableReplication

Disables replication for a table.

```yaml
DisableTableReplication:
  tableId: users
```

---

### Autoscaling Management

#### SetTableAutoscaling

Configures cluster autoscaling for a specific table.

```yaml
SetTableAutoscaling:
  tableId: users
  clusterId: prod-cluster-us-east1
  minNodes: 3
  maxNodes: 10
  cpuTargetPercent: 70
```

#### DisableTableAutoscaling

Disables autoscaling for a table.

```yaml
DisableTableAutoscaling:
  tableId: users
  clusterId: prod-cluster-us-east1
```

---

### Table Splits Management

#### SetTableSplits

Configures pre-split points for a table.

```yaml
SetTableSplits:
  tableId: users
  splits:
    - "user#1000"
    - "user#2000"
    - "user#3000"
```

#### ClearTableSplits

Removes all pre-split points from a table.

```yaml
ClearTableSplits:
  tableId: users
```

---

### Instance Management

⚠️ **Best Practice:** Use Terraform/Pulumi for production instances. TotalSchema instance actions are ideal for:
- Ephemeral dev/test instances
- Automated testing pipelines
- SaaS per-customer provisioning
- Disaster recovery automation

#### CreateInstance

Creates a new Bigtable instance.

```yaml
# Development instance
CreateInstance:
  instanceId: dev-bigtable-instance
  displayName: "Development BigTable Instance"
  instanceType: DEVELOPMENT
  clusters:
    - clusterId: dev-cluster-us-central1
      zone: us-central1-b
      nodes: 1
      storageType: SSD
  labels:
    environment: development
    managed-by: totalschema

# Production instance with autoscaling
CreateInstance:
  instanceId: prod-bigtable-instance
  displayName: "Production BigTable Instance"
  instanceType: PRODUCTION
  clusters:
    - clusterId: prod-cluster-us-east1
      zone: us-east1-b
      nodes: 5
      storageType: SSD
      autoscaling:
        minNodes: 3
        maxNodes: 10
        cpuTargetPercent: 70
  labels:
    environment: production
    criticality: high
```

**Cost Warning:** Bigtable instances incur hourly charges (~$0.65/hour per node with SSD).

#### DeleteInstance

Permanently deletes an instance and all its data.

```yaml
DeleteInstance:
  instanceId: dev-bigtable-instance
  confirmDeletion: true
  confirmInstanceName: dev-bigtable-instance
```

⚠️ **WARNING:** This operation is irreversible. All tables, backups, and data are permanently deleted.

---

## Garbage Collection Rules

Garbage collection (GC) rules control cell retention in column families. Without GC rules, Bigtable keeps all versions indefinitely.

### MaxVersions

Keep only the N most recent cell versions:

```yaml
families:
  - name: profile
    maxVersions: 1  # Keep only current version
```

### MaxAge

Delete cells older than a specified duration:

```yaml
families:
  - name: sessions
    maxAge: 24h  # Delete after 24 hours
```

**Duration Formats:**
```
7d      = 7 days
24h     = 24 hours
30m     = 30 minutes
3600s   = 3600 seconds
1000ms  = 1000 milliseconds
```

### Intersection (AND Logic)

All rules must be satisfied for a cell to be kept:

```yaml
families:
  - name: audit
    intersection:
      - maxVersions: 100  # AND
      - maxAge: 365d      # Keep up to 100 versions, but delete after 1 year
```

**Implicit Intersection:**
```yaml
families:
  - name: audit
    maxVersions: 100
    maxAge: 365d  # Same as intersection above
```

### Union (OR Logic)

A cell is kept if ANY rule is satisfied:

```yaml
families:
  - name: flexible
    union:
      - maxVersions: 5    # Keep 5 recent versions
      - maxAge: 7d        # OR keep cells from last 7 days
```

### Common Patterns

```yaml
# Configuration data (only latest matters)
- name: current_config
  maxVersions: 1

# Short-term cache
- name: cache
  maxAge: 24h

# Audit trail (limited versions, long retention)
- name: audit
  maxVersions: 10
  maxAge: 365d

# Recent activity tracking (flexible retention)
- name: analytics
  union:
    - maxVersions: 5
    - maxAge: 7d
```

---

## Examples

The `examples/` directory contains 40+ real-world examples:

### Basic Usage

- [`01-simple-families.yml`](examples/01-simple-families.yml) — Basic table with families (no GC rules)
- [`02-max-versions.yml`](examples/02-max-versions.yml) — Version-based retention
- [`03-max-age.yml`](examples/03-max-age.yml) — Time-based retention
- [`04-intersection.yml`](examples/04-intersection.yml) — AND logic (all rules must match)
- [`05-union.yml`](examples/05-union.yml) — OR logic (any rule can match)

### Real-World Scenarios

- [`06-ecommerce-real-world.yml`](examples/06-ecommerce-real-world.yml) — E-commerce application schema
- [`07-iot-tiered-retention.yml`](examples/07-iot-tiered-retention.yml) — IoT sensor data with tiered retention

### Column Family Evolution

- [`09-add-column-family-simple.yml`](examples/09-add-column-family-simple.yml) — Add family without GC rules
- [`10-add-column-family-max-versions.yml`](examples/10-add-column-family-max-versions.yml) — Add family with version retention
- [`15-modify-column-family-max-versions.yml`](examples/15-modify-column-family-max-versions.yml) — Update retention policy

### Backup and Restore

- [`20-create-backup-duration.yml`](examples/20-create-backup-duration.yml) — Duration-based backup expiry
- [`21-create-backup-timestamp.yml`](examples/21-create-backup-timestamp.yml) — Timestamp-based backup expiry
- [`24-restore-table-new.yml`](examples/24-restore-table-new.yml) — Restore to new table

### IAM Management

- [`27-set-iam-policy-simple.yml`](examples/27-set-iam-policy-simple.yml) — Grant simple access
- [`28-set-iam-policy-multiple-roles.yml`](examples/28-set-iam-policy-multiple-roles.yml) — Multiple roles and members
- [`30-remove-iam-policy-simple.yml`](examples/30-remove-iam-policy-simple.yml) — Revoke access

### Advanced Features

- [`35-enable-replication-global.yml`](examples/35-enable-replication-global.yml) — Global replication
- [`40-enable-autoscaling-prod.yml`](examples/40-enable-autoscaling-prod.yml) — Production autoscaling

See [`examples/README.md`](examples/README.md) for the complete index.

---

## Architecture

### Reflection-Free Design

The Bigtable connector uses a **zero-reflection architecture** for fast startup and predictable behavior:

```
ActionFactory (Abstract Base)
├── ServiceLoader discovery at startup
├── Action type derived from class simple name
└── Jackson POJO deserialization

Concrete Factories (Sub-packages)
├── createtable.CreateTableActionFactory → "CreateTable"
├── deletetable.DeleteTableActionFactory → "DeleteTable"
├── createcolumnfamily.CreateColumnFamilyActionFactory → "CreateColumnFamily"
└── ... (19 total action types)

ServiceLoader Registration
└── META-INF/services/io.github.totalschema.connector.gcp.bigtable.actions.ActionFactory
```

**Key Benefits:**

- ✅ **Fast startup** — ServiceLoader discovers factories once at class load (~60ms)
- ✅ **No runtime reflection** — all types known at compile time
- ✅ **Type safety** — Jackson enforces schema validation
- ✅ **Extensible** — add new actions without modifying existing code

### Action Discovery Flow

1. JVM loads `ActionFactory` class
2. Static initializer runs: `ServiceLoader.load(ActionFactory.class)`
3. ServiceLoader reads `META-INF/services` file
4. Instantiates registered factories: `new CreateTableActionFactory()`
5. Each factory passes its Action class to base constructor: `super(CreateTable.class)`
6. Base constructor derives action type from class name: `CreateTable.class.getSimpleName()` → `"CreateTable"`
7. Builds registry: `map.put("CreateTable", factoryInstance)`
8. Runtime lookups: `ActionFactory.getAction(yamlMap, context)`

### Variable Substitution Pipeline

1. Parse YAML with SnakeYAML
2. Extract action type from top-level key
3. Lookup factory in registry
4. Recursively substitute variables in configuration using `ExpressionEvaluator`
5. Deserialize with Jackson into typed POJO
6. Execute action against Bigtable Admin API

---

## Documentation

### User Documentation

- **[User Manual](docs/user_manual/README.md)** — Complete user guide with all actions, configuration options, and troubleshooting
- **[Actions Reference](docs/user_manual/ACTIONS-REFERENCE.md)** — Detailed reference for all 19 action types
- **[Quick Start Guide](docs/user_manual/QUICK-START.md)** — Get up and running in 5 minutes
- **[FAQ](docs/user_manual/FAQ.md)** — Frequently asked questions

### Developer Documentation

- **[AGENTS.md](AGENTS.md)** — AI agent guide for understanding and extending the connector
- **[Architecture Documentation](docs/developer/)** — In-depth architectural documentation
  - `ACTIONFACTORY_REFACTORING_SUMMARY.md` — ServiceLoader refactoring details
  - `GC_RULES_FEATURE.md` — Garbage collection rules implementation
  - `IMPLEMENTATION_SUMMARY.md` — Complete implementation overview

### Examples

- **[Examples Directory](examples/)** — 40+ real-world YAML examples
- **[Examples README](examples/README.md)** — Comprehensive examples index with use cases

---

## Testing

### Running Tests

```bash
# Run all tests
cd totalschema-connector-gcp-bigtable
mvn test

# Run specific test class
mvn test -Dtest=ActionFactoryTest

# Run with TestNG XML
java -cp "..." org.testng.TestNG testng.xml
```

### Test Coverage

The connector has comprehensive test coverage:

- **Unit Tests** — All action factories, GC rule builders, duration parsers
- **Integration Tests** — End-to-end workflows with H2 state repository
- **Service Tests** — Bigtable Admin API interactions (mocked)

**Key Test Classes:**

- `ActionFactoryTest` — ServiceLoader discovery, YAML deserialization, validation
- `ColumnFamilyTest` — GC rule building, duration parsing, rule combinations
- `BigtableConnectorTest` — Connector initialization, connection checks
- `YmlBigTableChangeExecutorTest` — YAML execution flow

### Manual Testing

Use the provided workspace for manual testing:

```bash
# 1. Set up GCP authentication
gcloud auth application-default login

# 2. Configure workspace
cp examples/01-simple-families.yml workspace/totalschema/0001.create_test.DEV.apply.bigtable.yml

# 3. Update totalschema.yml with your project/instance IDs

# 4. Apply changes
totalschema.sh apply -e DEV -w workspace/
```

---

## Best Practices

### Schema Evolution

1. **Use CreateColumnFamily instead of recreating tables**
   - Add new families without downtime
   - Existing data and queries are unaffected

2. **Test GC rule changes in DEV first**
   - Incorrect rules can lead to data loss or excessive storage costs
   - Use `ModifyColumnFamily` to adjust after monitoring

3. **Avoid ResetColumnFamily in production**
   - Without GC rules, storage grows indefinitely
   - Use `ModifyColumnFamily` with explicit rules instead

### Backup Strategy

1. **Automate regular backups with `apply_always`**
   ```
   0100.daily_backup.PROD.apply_always.mybigtable.yml
   ```

2. **Use variables for backup IDs**
   ```yaml
   backupId: users_backup_${DATE}
   ```

3. **Test restore procedures regularly**
   - Create staging environment restores to verify backup integrity

### IAM Management

1. **Use least privilege principle**
   - Grant `roles/bigtable.reader` for read-only access
   - Reserve `roles/bigtable.admin` for administrative scripts only

2. **Document IAM changes in script descriptions**
   ```
   0050.grant_analytics_access.PROD.apply.mybigtable.yml
   ```

3. **Use RemoveTableIamPolicy for offboarding**
   - Track access revocation in version control

### Cost Optimization

1. **Use appropriate GC rules**
   - Aggressive GC rules reduce storage costs
   - Monitor Bigtable metrics to tune retention

2. **Choose correct backup type**
   - `STANDARD` for long-term archives
   - `HOT` only for backups requiring fast restore

3. **Delete unused backups promptly**
   ```yaml
   DeleteBackup:
     backupId: users_backup_20260601
     clusterId: prod-cluster-us-east1
   ```

---

## Troubleshooting

### Authentication Issues

**Symptom:** `Failed to create BigtableTableAdminClient` or `401 Unauthorized`

**Solution:**
```bash
# Local development
gcloud auth application-default login

# CI/CD
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json
```

### Permission Denied

**Symptom:** `403 Forbidden` or `Permission denied on resource`

**Solution:** Verify service account has `roles/bigtable.admin`:
```bash
gcloud projects add-iam-policy-binding PROJECT_ID \
  --member="serviceAccount:SA_EMAIL" \
  --role="roles/bigtable.admin"
```

### Variable Substitution Not Working

**Symptom:** Literal `${variableName}` appears in error messages

**Solution:**
- Verify variable is defined in `totalschema.yml`
- Ensure you're running with correct environment: `-e DEV`
- Check for typos in variable names (case-sensitive)

See [`docs/user_manual/README.md#troubleshooting`](docs/user_manual/README.md#troubleshooting) for complete troubleshooting guide.

---

## Contributing

Contributions are welcome! Please:

1. Read the [AGENTS.md](AGENTS.md) guide for architecture overview
2. Follow TotalSchema's coding standards (run `mvn spotless:apply`)
3. Add tests for new actions
4. Update documentation

### Adding New Actions

1. Create Action POJO implementing `Action<T>`
2. Create ActionFactory extending `ActionFactory<YourAction>`
3. Register in `META-INF/services/io.github.totalschema.connector.gcp.bigtable.actions.ActionFactory`
4. Add tests
5. Add example YAML
6. Update documentation

See [AGENTS.md#adding-new-actions](AGENTS.md#adding-new-actions) for detailed guide.

---

## License

This project is licensed under the **GNU Affero General Public License v3.0** (AGPL-3.0).

See [LICENSE](../../LICENSE) file for details.

---

## Support

- **Documentation:** [docs/user_manual/](docs/user_manual/)
- **Examples:** [examples/](examples/)
- **Issues:** https://github.com/totalschema/totalschema/issues
- **Source:** https://github.com/totalschema/totalschema

---

## Version

**Current Version:** 1.4.0-SNAPSHOT

**Compatibility:**
- TotalSchema Core: 1.3.0+
- Java: 11+
- Google Cloud Bigtable API: 2.x (via BOM 26.84.0)
- Jackson: 2.22.1

---

**Built with ❤️ by the TotalSchema team**

