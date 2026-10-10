# TotalSchema GCP Bigtable Connector - User Manual

The GCP Bigtable connector enables schema and infrastructure management for Google Cloud Bigtable instances and tables through declarative YAML change scripts and Groovy scripts.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
  - [Basic Configuration](#basic-configuration)
  - [Private Service Connection](#private-service-connection)
  - [Authentication](#authentication)
- [Change Script Formats](#change-script-formats)
  - [YAML Scripts](#yaml-scripts)
  - [Groovy Scripts](#groovy-scripts)
- [Supported Actions](#supported-actions)
  - [Table Management](#table-management)
  - [Column Family Management](#column-family-management)
  - [Backup and Restore](#backup-and-restore)
  - [IAM Policy Management](#iam-policy-management)
  - [Replication Management](#replication-management)
  - [Autoscaling Management](#autoscaling-management)
  - [Instance Management](#instance-management)
- [Garbage Collection Rules](#garbage-collection-rules)
- [Variable Substitution](#variable-substitution)
- [Best Practices](#best-practices)
- [Examples](#examples)
- [Troubleshooting](#troubleshooting)

---

## Overview

The Bigtable connector manages Google Cloud Bigtable resources declaratively, tracking applied changes just like database migrations. It supports:

- **Table lifecycle** — create, modify, delete tables
- **Column family evolution** — add, modify, remove column families with GC rules
- **Backup and restore** — disaster recovery automation
- **IAM policy management** — fine-grained access control
- **Replication** — enable/disable table replication
- **Autoscaling** — configure cluster autoscaling per table
- **Instance management** — create/delete instances (dev/test scenarios)

Unlike ad-hoc scripts, TotalSchema tracks which changes have been applied, prevents duplicate execution, and supports environment-specific configurations.

---

## Features

### Declarative YAML Syntax
Define table schemas, retention policies, and IAM bindings in human-readable YAML:

```yaml
CreateTable:
  tableId: users
  families:
    - name: profile
      maxVersions: 1
    - name: audit
      maxAge: 365d
```

### Change Tracking
Every applied change is recorded in the state repository. Re-running the same script is skipped automatically.

### Environment-Specific Configurations
Use variables to deploy the same change scripts to DEV, QA, and PROD with different settings:

```yaml
CreateBackup:
  sourceTableId: ${TABLE_NAME}
  backupId: ${TABLE_NAME}_backup_${BACKUP_DATE}
  expireTime: ${BACKUP_RETENTION_DAYS}d
  clusterId: ${CLUSTER_ID}
```

### Schema Evolution Without Downtime
Add, modify, or remove column families from existing tables without recreation:

```yaml
CreateColumnFamily:
  tableId: users
  familyName: analytics
  maxAge: 30d
```

### Flexible Garbage Collection Rules
- **MaxVersions** — keep only the N most recent cell versions
- **MaxAge** — delete cells older than a specified duration
- **Intersection** — apply multiple rules (AND logic)
- **Union** — flexible retention (OR logic)

---

## Prerequisites

### Google Cloud Setup
1. **GCP Project** with Bigtable API enabled
2. **Bigtable Instance** created (or use TotalSchema to create it)
3. **Service Account** with appropriate permissions:
   - `roles/bigtable.admin` for full table management
   - `roles/bigtable.user` for data operations (future support)
   - `roles/iam.serviceAccountUser` if managing IAM policies

### Authentication
The connector uses **Google Cloud Application Default Credentials (ADC)**:

```bash
# Local development - authenticate with your user account
gcloud auth application-default login

# CI/CD / Production - use service account key
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json
```

### TotalSchema Distribution
- TotalSchema CLI or Maven plugin installed
- Java 11 or later

---

## Configuration

### Basic Configuration

Add a Bigtable connector to your `totalschema.yml`:

```yaml
connectors:
  mybigtable:
    type: bigtable
    projectId: my-gcp-project
    instanceId: my-instance
```

| Property | Required | Description |
|----------|----------|-------------|
| `type` | ✅ | Must be `bigtable` |
| `projectId` | ✅ | GCP project ID |
| `instanceId` | ✅ | Bigtable instance ID |
| `dataclient.enabled` | ❌ | Enable data client for row-level operations (SetCell, DeleteCell, etc.). Default: `false`. Disabled by default to avoid unnecessary client initialization. |
| `adminEndpoint` | ❌ | Custom admin endpoint (see Private Service Connection below) |
| `dataEndpoint` | ❌ | Custom data endpoint for read/write operations (see Private Service Connection below) |
| `appProfileId` | ❌ | App profile ID for routing data requests (default: `"default"`) |
| `connectionCheck.enabled` | ❌ | Enable/disable connectivity check (default: `true`) |

### Private Service Connection

For Private Service Connection or custom endpoints:

```yaml
connectors:
  mybigtable:
    type: bigtable
    projectId: my-gcp-project
    instanceId: my-instance
    
    # Custom admin endpoint (for table management)
    adminEndpoint: bigtableadmin-custom-postfix.p.googleapis.com:443
    
    # Enable data client for row-level operations
    dataclient:
      enabled: true
    
    # Custom data endpoint (for read/write operations, only when dataclient.enabled: true)
    dataEndpoint: bigtable-custom-postfix.p.googleapis.com:443
    
    # App profile for data operations (only used when dataclient.enabled: true)
    appProfileId: my-app-profile
```

**Endpoint Types:**

Bigtable has two separate endpoints:
- **Admin endpoint** (`bigtableadmin.googleapis.com`) — table/instance management (CreateTable, ModifyColumnFamily, etc.). **Always available.**
- **Data endpoint** (`bigtable.googleapis.com`) — read/write data operations (SetCell, DeleteCell, DeleteCellVersions). **Only available when `dataclient.enabled: true`.**

Both endpoints are configurable for Private Service Connection scenarios.

**Data Client Configuration:**

The data client (`BigtableDataClient`) is **disabled by default** to avoid unnecessary initialization overhead. Most schema migration workflows only require admin operations (table creation, column family management, backups, etc.) and don't need row-level data access.

**Enable the data client when you need:**
- Row-level data operations in YAML (SetCell, DeleteCell, DeleteCellVersions)
- Data access in Groovy scripts via `bigtableDataClient` binding
- Seed data insertion during deployment
- GDPR compliance (targeted cell version deletion)

### Authentication

The connector uses Google Cloud Application Default Credentials (ADC). No explicit credentials are configured in `totalschema.yml`.

**Local development:**
```bash
gcloud auth application-default login
```

**CI/CD pipelines:**
```bash
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json
```

**GCE/GKE/Cloud Run:**
Service accounts are automatically available via the metadata server.

---

## Change Script Formats

### YAML Scripts

Declarative actions executed by the built-in YAML executor. File extension: `.yml` or `.yaml`

**Naming convention:**
```
<order>.<description>[.<env>].<changetype>.<connector>.yml
```

**Example:**
```
0001.create_users_table.DEV.apply.mybigtable.yml
0002.add_analytics_family.apply.mybigtable.yml
0010.backup_users.PROD.apply_always.mybigtable.yml
```

**Change types:**
- `apply` — execute once, skip if already applied
- `apply_always` — execute every run (e.g., backups, refreshes)
- `apply_on_change` — re-execute when file content changes
- `revert` — rollback script (paired with an `apply` script)

### Groovy Scripts

For advanced scenarios, use Groovy with full access to the Bigtable Admin API. File extension: `.groovy`

**Bindings available:**
- `bigtableTableAdminClient` — `BigtableTableAdminClient` instance
- `bigtableInstanceAdminClient` — `BigtableInstanceAdminClient` instance
- `configuration` — connector configuration
- `environment` — current environment name (if specified)

**Example:**
```groovy
// 0001.custom_logic.DEV.apply.mybigtable.groovy

import com.google.cloud.bigtable.admin.v2.models.CreateTableRequest

def tableId = "dynamic_table_${environment}"
def request = CreateTableRequest.of(tableId)
    .addFamily("cf1")

bigtableTableAdminClient.createTable(request)

println "Created table: ${tableId}"
```

**Note:** Groovy scripts require `groovy-all.jar` in the `user_libs/` directory.

---

## Supported Actions

All YAML actions support **variable substitution** using `${variableName}` syntax.

### Table Management

#### CreateTable

Creates a new Bigtable table with optional column families and garbage collection rules.

```yaml
CreateTable:
  tableId: users
  families:
    - name: profile
      maxVersions: 1
    - name: audit
      maxAge: 365d
    - name: sessions
      maxAge: 24h
```

**Properties:**
- `tableId` (required) — unique table identifier
- `families` (optional) — list of column families with GC rules

**GC rule options per family:**
- `maxVersions` — keep only N most recent versions
- `maxAge` — delete cells older than duration (e.g., `7d`, `24h`)
- `intersection` — list of rules (all must match)
- `union` — list of rules (any can match)

See [Garbage Collection Rules](#garbage-collection-rules) for detailed syntax.

#### DeleteTable

Permanently deletes a table and all its data.

```yaml
DeleteTable:
  tableId: old_table
```

**⚠️ WARNING:** This operation is irreversible. All data is permanently deleted.

### Column Family Management

#### CreateColumnFamily

Adds a new column family to an existing table without downtime.

```yaml
CreateColumnFamily:
  tableId: users
  familyName: analytics
  maxAge: 30d
```

**Properties:**
- `tableId` (required) — existing table ID
- `familyName` (required) — new column family name
- GC rule options: `maxVersions`, `maxAge`, `intersection`, `union`

#### ModifyColumnFamily

Updates garbage collection rules on an existing column family.

```yaml
ModifyColumnFamily:
  tableId: users
  familyName: profile
  maxVersions: 10
```

**Use cases:**
- Extend retention from 7d to 30d for compliance
- Reduce version count for performance tuning
- Adjust policies after usage analysis

#### ResetColumnFamily

Removes all garbage collection rules from a column family (keeps all versions indefinitely).

```yaml
ResetColumnFamily:
  tableId: users
  familyName: profile
```

**⚠️ WARNING:** Without GC rules, storage costs will grow continuously. Consider `ModifyColumnFamily` instead.

#### DeleteColumnFamily

Permanently removes a column family and all its data.

```yaml
DeleteColumnFamily:
  tableId: users
  familyName: old_analytics
```

**⚠️ WARNING:** This operation is irreversible. All data in the family is permanently deleted.

### Backup and Restore

#### CreateBackup

Creates a point-in-time backup of a table for disaster recovery.

```yaml
# Duration-based expiration
CreateBackup:
  sourceTableId: users
  backupId: users_backup_20260731
  expireTime: 30d
  clusterId: prod-cluster-us-east1
```

```yaml
# Timestamp-based expiration
CreateBackup:
  sourceTableId: users
  backupId: users_backup_20260731
  expireTime: "2026-08-31T00:00:00Z"
  clusterId: prod-cluster-us-east1
```

```yaml
# HOT backup (faster restore)
CreateBackup:
  sourceTableId: users
  backupId: users_backup_hot
  expireTime: 7d
  clusterId: prod-cluster-us-east1
  backupType: HOT
```

**Properties:**
- `sourceTableId` (required) — table to back up
- `backupId` (required) — unique backup identifier
- `expireTime` (required) — duration (`7d`, `30d`) or ISO 8601 timestamp
- `clusterId` (required) — cluster where backup is stored
- `backupType` (optional) — `STANDARD` (default) or `HOT`

**Expiration formats:**
- Duration: `7d`, `24h`, `30m`, `3600s`
- Timestamp: `2026-08-31T00:00:00Z` (ISO 8601)

**Notes:**
- Backup creation is asynchronous but the action waits for completion
- Backups incur storage costs based on table size and retention period
- HOT backups offer faster restore but may have different pricing

#### DeleteBackup

Deletes a backup to free storage.

```yaml
DeleteBackup:
  backupId: users_backup_20260701
  clusterId: prod-cluster-us-east1
```

#### RestoreTable

Restores a table from a backup.

```yaml
# Restore to new table
RestoreTable:
  backupId: users_backup_20260731
  tableId: users_restored
  clusterId: prod-cluster-us-east1
```

```yaml
# Restore and overwrite existing table
RestoreTable:
  backupId: users_backup_20260731
  tableId: users
  clusterId: prod-cluster-us-east1
  overwrite: true
```

**Properties:**
- `backupId` (required) — backup to restore from
- `tableId` (required) — destination table ID
- `clusterId` (required) — cluster containing the backup
- `overwrite` (optional, default: `false`) — if `true`, deletes existing table first

**⚠️ WARNING:** Setting `overwrite: true` permanently deletes the existing table before restoring.

### IAM Policy Management

#### SetTableIamPolicy

Grants IAM permissions to users, service accounts, or groups.

```yaml
# Single role
SetTableIamPolicy:
  tableId: users
  bindings:
    - role: roles/bigtable.reader
      members:
        - serviceAccount:analytics@project.iam.gserviceaccount.com
```

```yaml
# Multiple roles
SetTableIamPolicy:
  tableId: customer_data
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
- If role already exists, new members are added to existing member list
- Uses read-modify-write pattern (safe for concurrent updates)

**Member formats:**
- `user:email@domain.com` — individual user
- `serviceAccount:sa@project.iam.gserviceaccount.com` — service account
- `group:team@domain.com` — Google Group
- `domain:example.com` — all users in a domain
- `allUsers` — public access (⚠️ use with caution!)
- `allAuthenticatedUsers` — any authenticated user

**Common roles:**
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

**Behavior:**
- Removes only specified members from specified roles
- Other members in the same role are preserved
- If all members are removed from a role, the role binding is removed
- Silently succeeds if member doesn't exist (idempotent)

**Use cases:**
- Employee offboarding
- Decommissioning services
- Security incident response
- Access control cleanup

### Replication Management

#### EnableTableReplication

Enables replication for a table across clusters.

```yaml
# Eventual consistency
EnableTableReplication:
  tableId: users
  consistencyMode: EVENTUAL
```

```yaml
# Strong consistency (transactional)
EnableTableReplication:
  tableId: financial_transactions
  consistencyMode: STRONG
```

**Properties:**
- `tableId` (required) — table to enable replication for
- `consistencyMode` (required) — `EVENTUAL` or `STRONG`

**Consistency modes:**
- `EVENTUAL` — lower latency, eventual consistency across clusters
- `STRONG` — transactional consistency, single-row transactions

#### DisableTableReplication

Disables replication for a table.

```yaml
DisableTableReplication:
  tableId: users
```

**Use cases:**
- Cost optimization for non-critical data
- Reducing cross-region traffic
- Testing single-cluster behavior

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

**Properties:**
- `tableId` (required) — table to configure autoscaling for
- `clusterId` (required) — target cluster
- `minNodes` (required) — minimum node count
- `maxNodes` (required) — maximum node count
- `cpuTargetPercent` (required) — CPU utilization target (1-99)

**Notes:**
- Autoscaling adjusts cluster size based on CPU utilization
- Changes take effect within minutes but may take longer for large adjustments
- Costs scale linearly with node count

#### DisableTableAutoscaling

Disables autoscaling for a table.

```yaml
DisableTableAutoscaling:
  tableId: users
  clusterId: prod-cluster-us-east1
```

### Instance Management

**⚠️ Important:** Instance management should typically be handled by infrastructure-as-code tools (Terraform, Pulumi) for production. Use TotalSchema instance actions for:
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
```

```yaml
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

**Properties:**
- `instanceId` (required) — unique instance identifier
- `displayName` (required) — human-readable name
- `instanceType` (required) — `DEVELOPMENT` or `PRODUCTION`
- `clusters` (required) — list of clusters
- `labels` (optional) — key-value labels for organization

**Cluster properties:**
- `clusterId` (required) — unique cluster identifier
- `zone` (required) — GCP zone (e.g., `us-central1-b`)
- `nodes` (required if no autoscaling) — node count
- `storageType` (required) — `SSD` or `HDD`
- `autoscaling` (optional) — autoscaling configuration

**Cost warning:** Bigtable instances incur hourly charges (~$0.65/hour per node with SSD). Always delete unused instances!

#### DeleteInstance

Permanently deletes an instance and all its data.

```yaml
DeleteInstance:
  instanceId: dev-bigtable-instance
  confirmDeletion: true
  confirmInstanceName: dev-bigtable-instance
```

**⚠️ WARNING:** This operation is irreversible. All tables, backups, and data are permanently deleted.

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

**Duration formats:**
- Days: `7d`, `30days`, `90d`
- Hours: `24h`, `48hours`
- Minutes: `60m`, `120min`
- Seconds: `3600s`, `86400s`

### Intersection (AND Logic)

All rules must be satisfied for a cell to be kept:

```yaml
families:
  - name: audit
    intersection:
      - maxVersions: 100  # AND
      - maxAge: 365d      # Keep up to 100 versions, but delete after 1 year
```

**Implicit intersection:**
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

**Use case:** Keep recent versions for quick access OR time-based retention for compliance.

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

## Variable Substitution

All YAML action properties support variable substitution using `${variableName}` syntax.

### Configuration

Define variables globally or per environment in `totalschema.yml`:

```yaml
variables:
  backupRetentionDays: 30
  clusterName: prod-cluster-us-east1

environments:
  DEV:
    variables:
      tablePrefix: dev
      clusterName: dev-cluster-us-central1
  PROD:
    variables:
      tablePrefix: prod
      clusterName: prod-cluster-us-east1
```

### Usage

```yaml
CreateTable:
  tableId: ${tablePrefix}_users
  families:
    - name: profile
      maxVersions: 1

CreateBackup:
  sourceTableId: ${tablePrefix}_users
  backupId: ${tablePrefix}_users_backup_${BUILD_ID}
  expireTime: ${backupRetentionDays}d
  clusterId: ${clusterName}
```

### System Properties

Override variables at runtime:

```bash
totalschema.sh apply -e PROD -DbackupRetentionDays=90
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

### Instance Management

1. **Use Terraform/Pulumi for production instances**
   - TotalSchema instance actions are best for ephemeral environments

2. **Always delete test instances**
   - Instance costs accrue hourly (~$0.65/hour per node)
   - Use labels to mark auto-cleanup candidates:
     ```yaml
     labels:
       auto-cleanup: true
       created-date: "2026-07-31"
     ```

### Change Script Organization

1. **Use numeric prefixes for ordering**
   ```
   changes/
   ├── 0001.create_users.apply.mybigtable.yml
   ├── 0002.add_analytics_family.apply.mybigtable.yml
   ├── 0100.daily_backup.apply_always.mybigtable.yml
   ```

2. **Environment-specific scripts**
   ```
   0050.prod_iam_policies.PROD.apply.mybigtable.yml
   ```

3. **Use labels for logical grouping**
   ```yaml
   # In totalschema-labels.yml
   targetRelease: 2027-Q1-01
   feature: analytics-v2
   ```

---

## Examples

See the `examples/` directory for 40+ real-world examples covering:

- Table creation with various GC rules
- Column family lifecycle management
- Backup and restore workflows
- IAM policy management patterns
- Replication configuration
- Autoscaling setup
- Instance lifecycle management

**Quick links:**
- [Basic table creation](../../examples/01-simple-families.yml)
- [E-commerce real-world schema](../../examples/06-ecommerce-real-world.yml)
- [Adding column families](../../examples/09-add-column-family-simple.yml)
- [Backup creation](../../examples/20-create-backup-duration.yml)
- [IAM policy management](../../examples/27-set-iam-policy-simple.yml)
- [Instance management](../../examples/00-create-instance.yml)

---

## Troubleshooting

### Authentication Issues

**Symptom:** `Failed to create BigtableTableAdminClient` or `401 Unauthorized`

**Solution:**
```bash
# Local development
gcloud auth application-default login

# Verify credentials
gcloud auth application-default print-access-token

# CI/CD - ensure service account key is available
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/key.json
```

### Permission Denied

**Symptom:** `403 Forbidden` or `Permission denied on resource`

**Solution:**
- Verify service account has required roles:
  ```bash
  gcloud projects get-iam-policy PROJECT_ID \
    --flatten="bindings[].members" \
    --filter="bindings.members:serviceAccount:SA_EMAIL"
  ```
- Grant required role:
  ```bash
  gcloud projects add-iam-policy-binding PROJECT_ID \
    --member="serviceAccount:SA_EMAIL" \
    --role="roles/bigtable.admin"
  ```

### Instance Not Found

**Symptom:** `Instance not found: projects/PROJECT/instances/INSTANCE`

**Solution:**
- Verify instance exists:
  ```bash
  gcloud bigtable instances list --project=PROJECT_ID
  ```
- Check `projectId` and `instanceId` in `totalschema.yml`

### Backup Creation Timeout

**Symptom:** Backup creation takes a very long time or times out

**Notes:**
- Backup creation time depends on table size
- Large tables (TB+) can take hours
- The action waits for completion by default

**Solution:**
- Consider HOT backups for faster creation (if supported for your use case)
- Ensure sufficient cluster capacity for backup operations
- Monitor backup progress in GCP Console

### Table Already Exists

**Symptom:** `Table already exists: TABLE_ID`

**Solution:**
- This is expected if the script was already applied
- TotalSchema tracks applied changes and skips duplicates
- If you need to recreate the table:
  1. Add a `DeleteTable` revert script
  2. Run `totalschema.sh revert -e ENV`
  3. Re-run `totalschema.sh apply -e ENV`

### GC Rule Validation Errors

**Symptom:** `Invalid GC rule configuration`

**Solution:**
- Ensure duration formats are correct: `7d`, `24h`, `30m`, `3600s`
- `maxVersions` must be positive integer
- Cannot have both `intersection` and `union` at same level
- Cannot mix shorthand (`maxVersions`, `maxAge`) with explicit `intersection`/`union`

### Private Service Connection Issues

**Symptom:** Connection fails when using custom endpoints (`adminEndpoint` or `dataEndpoint`)

**Solution:**
- Verify endpoint format: `hostname:port`
- Ensure Private Service Connection is properly configured in GCP
- Test connectivity from the TotalSchema host:
  ```bash
  # Test admin endpoint
  nc -zv bigtableadmin-custom.p.googleapis.com 443
  
  # Test data endpoint
  nc -zv bigtable-custom.p.googleapis.com 443
  ```
- Verify DNS resolution:
  ```bash
  nslookup bigtableadmin-custom.p.googleapis.com
  nslookup bigtable-custom.p.googleapis.com
  ```

### State Repository Lock Issues

**Symptom:** `Failed to acquire lock` or `Lock held by another process`

**Solution:**
- Wait for other TotalSchema process to complete
- If a previous run crashed, manually release the lock:
  ```sql
  DELETE FROM totalschema_lock WHERE lock_id = 'totalschema_main_lock';
  ```

### Variable Substitution Not Working

**Symptom:** Literal `${variableName}` appears in error messages

**Solution:**
- Verify variable is defined in `totalschema.yml` under `variables` or `environments.<ENV>.variables`
- Ensure you're running with correct environment: `-e DEV`
- Check for typos in variable names (case-sensitive)

---

## Support

- **Main Documentation:** [TotalSchema README](../../../../docs/user_manual/README.md)
- **CLI Reference:** [CLI Usage](../../../../docs/user_manual/CLI-USAGE.md)
- **Examples:** [Bigtable Examples](../../examples/README.md)
- **Issues:** https://github.com/totalschema/totalschema/issues
- **Source:** https://github.com/totalschema/totalschema

---

## License

GNU Affero General Public License v3.0 - See LICENSE file for details

