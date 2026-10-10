# GCP Bigtable Actions Reference

Quick reference guide for all Bigtable YAML actions.

## Table of Contents

- [Table Actions](#table-actions)
- [Column Family Actions](#column-family-actions)
- [Backup Actions](#backup-actions)
- [IAM Actions](#iam-actions)
- [Replication Actions](#replication-actions)
- [Autoscaling Actions](#autoscaling-actions)
- [Instance Actions](#instance-actions)
- [GC Rule Syntax](#gc-rule-syntax)
- [Duration Format](#duration-format)

---

## Table Actions

### CreateTable

Creates a new table with optional column families.

```yaml
CreateTable:
  tableId: string (required)
  families:  # optional
    - name: string (required)
      maxVersions: integer (optional)
      maxAge: duration (optional)
      intersection: [rules] (optional)
      union: [rules] (optional)
```

**Example:**
```yaml
CreateTable:
  tableId: users
  families:
    - name: profile
      maxVersions: 1
    - name: audit
      maxAge: 365d
```

### DeleteTable

Permanently deletes a table.

```yaml
DeleteTable:
  tableId: string (required)
```

**Example:**
```yaml
DeleteTable:
  tableId: old_table
```

### SetTableSplits

Configures table split points for pre-splitting.

```yaml
SetTableSplits:
  tableId: string (required)
  splitKeys:  # required
    - string
```

**Example:**
```yaml
SetTableSplits:
  tableId: users
  splitKeys:
    - "user#1000"
    - "user#2000"
    - "user#3000"
```

### ClearTableSplits

Removes all table split points.

```yaml
ClearTableSplits:
  tableId: string (required)
```

**Example:**
```yaml
ClearTableSplits:
  tableId: users
```

---

## Column Family Actions

### CreateColumnFamily

Adds a new column family to an existing table.

```yaml
CreateColumnFamily:
  tableId: string (required)
  familyName: string (required)
  maxVersions: integer (optional)
  maxAge: duration (optional)
  intersection: [rules] (optional)
  union: [rules] (optional)
```

**Example:**
```yaml
CreateColumnFamily:
  tableId: users
  familyName: analytics
  maxAge: 30d
```

### ModifyColumnFamily

Updates GC rules on an existing column family.

```yaml
ModifyColumnFamily:
  tableId: string (required)
  familyName: string (required)
  maxVersions: integer (optional)
  maxAge: duration (optional)
  intersection: [rules] (optional)
  union: [rules] (optional)
```

**Example:**
```yaml
ModifyColumnFamily:
  tableId: users
  familyName: profile
  maxVersions: 10
```

### ResetColumnFamily

Removes all GC rules from a column family.

```yaml
ResetColumnFamily:
  tableId: string (required)
  familyName: string (required)
```

**Example:**
```yaml
ResetColumnFamily:
  tableId: users
  familyName: profile
```

### DeleteColumnFamily

Permanently removes a column family.

```yaml
DeleteColumnFamily:
  tableId: string (required)
  familyName: string (required)
```

**Example:**
```yaml
DeleteColumnFamily:
  tableId: users
  familyName: old_analytics
```

---

## Backup Actions

### CreateBackup

Creates a point-in-time backup of a table.

```yaml
CreateBackup:
  sourceTableId: string (required)
  backupId: string (required)
  expireTime: duration|timestamp (required)
  clusterId: string (required)
  backupType: string (optional, default: STANDARD)
```

**Duration-based expiration:**
```yaml
CreateBackup:
  sourceTableId: users
  backupId: users_backup_20260731
  expireTime: 30d
  clusterId: prod-cluster-us-east1
```

**Timestamp-based expiration:**
```yaml
CreateBackup:
  sourceTableId: users
  backupId: users_backup_20260731
  expireTime: "2026-08-31T00:00:00Z"
  clusterId: prod-cluster-us-east1
```

**HOT backup:**
```yaml
CreateBackup:
  sourceTableId: users
  backupId: users_backup_hot
  expireTime: 7d
  clusterId: prod-cluster-us-east1
  backupType: HOT
```

### DeleteBackup

Deletes a backup.

```yaml
DeleteBackup:
  backupId: string (required)
  clusterId: string (required)
```

**Example:**
```yaml
DeleteBackup:
  backupId: users_backup_20260701
  clusterId: prod-cluster-us-east1
```

### RestoreTable

Restores a table from a backup.

```yaml
RestoreTable:
  backupId: string (required)
  tableId: string (required)
  clusterId: string (required)
  overwrite: boolean (optional, default: false)
```

**Restore to new table:**
```yaml
RestoreTable:
  backupId: users_backup_20260731
  tableId: users_restored
  clusterId: prod-cluster-us-east1
```

**Restore and overwrite:**
```yaml
RestoreTable:
  backupId: users_backup_20260731
  tableId: users
  clusterId: prod-cluster-us-east1
  overwrite: true
```

---

## IAM Actions

### SetTableIamPolicy

Grants IAM permissions.

```yaml
SetTableIamPolicy:
  tableId: string (required)
  bindings:  # required
    - role: string (required)
      members:  # required
        - string
```

**Single role:**
```yaml
SetTableIamPolicy:
  tableId: users
  bindings:
    - role: roles/bigtable.reader
      members:
        - serviceAccount:analytics@project.iam.gserviceaccount.com
```

**Multiple roles:**
```yaml
SetTableIamPolicy:
  tableId: customer_data
  bindings:
    - role: roles/bigtable.reader
      members:
        - serviceAccount:analytics@project.iam.gserviceaccount.com
        - user:analyst@company.com
    - role: roles/bigtable.user
      members:
        - serviceAccount:app-backend@project.iam.gserviceaccount.com
```

**Member formats:**
- `user:email@domain.com`
- `serviceAccount:sa@project.iam.gserviceaccount.com`
- `group:team@domain.com`
- `domain:example.com`
- `allUsers`
- `allAuthenticatedUsers`

**Common roles:**
- `roles/bigtable.reader` — read-only
- `roles/bigtable.user` — read/write
- `roles/bigtable.admin` — full admin

### RemoveTableIamPolicy

Revokes IAM permissions.

```yaml
RemoveTableIamPolicy:
  tableId: string (required)
  bindings:  # required
    - role: string (required)
      members:  # required
        - string
```

**Example:**
```yaml
RemoveTableIamPolicy:
  tableId: users
  bindings:
    - role: roles/bigtable.reader
      members:
        - user:ex-employee@company.com
```

---

## Replication Actions

### EnableTableReplication

Enables replication for a table.

```yaml
EnableTableReplication:
  tableId: string (required)
  consistencyMode: string (required)
```

**Consistency modes:**
- `EVENTUAL` — lower latency, eventual consistency
- `STRONG` — transactional consistency

**Example:**
```yaml
EnableTableReplication:
  tableId: users
  consistencyMode: EVENTUAL
```

### DisableTableReplication

Disables replication for a table.

```yaml
DisableTableReplication:
  tableId: string (required)
```

**Example:**
```yaml
DisableTableReplication:
  tableId: users
```

---

## Autoscaling Actions

### SetTableAutoscaling

Configures cluster autoscaling for a table.

```yaml
SetTableAutoscaling:
  tableId: string (required)
  clusterId: string (required)
  minNodes: integer (required)
  maxNodes: integer (required)
  cpuTargetPercent: integer (required, 1-99)
```

**Example:**
```yaml
SetTableAutoscaling:
  tableId: users
  clusterId: prod-cluster-us-east1
  minNodes: 3
  maxNodes: 10
  cpuTargetPercent: 70
```

### DisableTableAutoscaling

Disables autoscaling for a table.

```yaml
DisableTableAutoscaling:
  tableId: string (required)
  clusterId: string (required)
```

**Example:**
```yaml
DisableTableAutoscaling:
  tableId: users
  clusterId: prod-cluster-us-east1
```

---

## Instance Actions

### CreateInstance

Creates a new Bigtable instance.

```yaml
CreateInstance:
  instanceId: string (required)
  displayName: string (required)
  instanceType: string (required)
  clusters:  # required
    - clusterId: string (required)
      zone: string (required)
      nodes: integer (required if no autoscaling)
      storageType: string (required)
      autoscaling:  # optional
        minNodes: integer (required)
        maxNodes: integer (required)
        cpuTargetPercent: integer (required)
  labels:  # optional
    key: value
```

**Instance types:**
- `DEVELOPMENT` — lower cost, single cluster
- `PRODUCTION` — full features, multi-cluster support

**Storage types:**
- `SSD` — high performance
- `HDD` — lower cost

**Development instance:**
```yaml
CreateInstance:
  instanceId: dev-bigtable-instance
  displayName: "Development Instance"
  instanceType: DEVELOPMENT
  clusters:
    - clusterId: dev-cluster-us-central1
      zone: us-central1-b
      nodes: 1
      storageType: SSD
  labels:
    environment: development
```

**Production instance with autoscaling:**
```yaml
CreateInstance:
  instanceId: prod-bigtable-instance
  displayName: "Production Instance"
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
```

### DeleteInstance

Permanently deletes an instance.

```yaml
DeleteInstance:
  instanceId: string (required)
  confirmDeletion: boolean (required)
  confirmInstanceName: string (optional)
```

**Example:**
```yaml
DeleteInstance:
  instanceId: dev-bigtable-instance
  confirmDeletion: true
  confirmInstanceName: dev-bigtable-instance
```

---

## GC Rule Syntax

### MaxVersions

Keep only N most recent versions:

```yaml
maxVersions: 1
```

### MaxAge

Delete cells older than duration:

```yaml
maxAge: 7d
```

### Intersection (AND)

All rules must be satisfied:

```yaml
intersection:
  - maxVersions: 100
  - maxAge: 365d
```

**Shorthand (implicit intersection):**
```yaml
maxVersions: 100
maxAge: 365d
```

### Union (OR)

Any rule can be satisfied:

```yaml
union:
  - maxVersions: 5
  - maxAge: 7d
```

### Nested Rules

```yaml
union:
  - maxVersions: 3
  - intersection:
      - maxVersions: 100
      - maxAge: 90d
```

---

## Duration Format

Human-readable duration strings:

| Format | Example | Meaning |
|--------|---------|---------|
| Days | `7d`, `30days`, `90d` | 7, 30, 90 days |
| Hours | `24h`, `48hours` | 24, 48 hours |
| Minutes | `60m`, `120min` | 60, 120 minutes |
| Seconds | `3600s`, `86400seconds` | 3600, 86400 seconds |
| Milliseconds | `1000ms` | 1000 milliseconds |

**Examples:**
- `maxAge: 7d` — 7 days
- `maxAge: 24h` — 24 hours
- `expireTime: 30d` — 30 days

---

## Variable Substitution

All string properties support `${variableName}` substitution:

```yaml
CreateTable:
  tableId: ${tablePrefix}_users
  families:
    - name: profile
      maxVersions: 1

CreateBackup:
  sourceTableId: ${tablePrefix}_users
  backupId: ${tablePrefix}_backup_${DATE}
  expireTime: ${backupRetentionDays}d
  clusterId: ${clusterName}
```

Define variables in `totalschema.yml`:

```yaml
variables:
  tablePrefix: prod
  backupRetentionDays: 30
  clusterName: prod-cluster-us-east1

environments:
  DEV:
    variables:
      tablePrefix: dev
      clusterName: dev-cluster-us-central1
```

---

## Common Patterns

### Configuration Data (Latest Only)

```yaml
- name: current_config
  maxVersions: 1
```

### Short-Term Cache

```yaml
- name: cache
  maxAge: 24h
```

### Audit Trail

```yaml
- name: audit
  maxVersions: 10
  maxAge: 365d
```

### Flexible Retention

```yaml
- name: analytics
  union:
    - maxVersions: 5
    - maxAge: 7d
```

### Session Data

```yaml
- name: sessions
  maxAge: 1h
```

---

## Learn More

- **[Full User Manual](README.md)** — comprehensive documentation
- **[Quick Start Guide](QUICK-START.md)** — get started in 5 minutes
- **[Examples](../../examples/README.md)** — 40+ real-world examples
- **[CLI Reference](../../../../docs/user_manual/CLI-USAGE.md)** — all CLI commands

