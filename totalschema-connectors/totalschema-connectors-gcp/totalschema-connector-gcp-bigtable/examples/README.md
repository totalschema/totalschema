# BigTable Actions Examples

This directory contains practical examples demonstrating BigTable operations in TotalSchema YAML configuration files.

## Quick Reference

| Example | Description | Key Features |
|---------|-------------|--------------|
| `00-create-instance.yml` | Instance lifecycle management | CreateInstance, DeleteInstance, autoscaling |
| `01-simple-families.yml` | Basic families without GC rules | Default BigTable behavior |
| `02-max-versions.yml` | Version-based retention | `maxVersions: N` |
| `03-max-age.yml` | Time-based retention | `maxAge: "7d"` |
| `04-intersection.yml` | AND logic (all rules must match) | Implicit & explicit syntax |
| `05-union.yml` | OR logic (any rule can match) | Flexible retention |
| `06-ecommerce-real-world.yml` | Complete e-commerce table | Mixed retention policies |
| `07-iot-tiered-retention.yml` | IoT sensor data | Tiered retention strategy |
| `08-delete-table.yml` | Table deletion | DeleteTable |
| `09-add-column-family-simple.yml` | Add column family to existing table | CreateColumnFamily, no GC rules |
| `10-add-column-family-max-versions.yml` | Add family with version retention | CreateColumnFamily with maxVersions |
| `11-add-column-family-max-age.yml` | Add family with time-based retention | CreateColumnFamily with maxAge |
| `12-add-column-family-intersection.yml` | Add family with intersection rule | CreateColumnFamily with both maxVersions AND maxAge |
| `13-add-column-family-union.yml` | Add family with union rule | CreateColumnFamily with flexible OR logic |
| `14-remove-column-family.yml` | Remove column family from table | DeleteColumnFamily |
| `15-modify-column-family-max-versions.yml` | Modify family version retention | ModifyColumnFamily with maxVersions |
| `16-modify-column-family-max-age.yml` | Modify family age retention | ModifyColumnFamily with maxAge |
| `17-modify-column-family-intersection.yml` | Modify family with intersection rule | ModifyColumnFamily with both rules |
| `18-modify-column-family-union.yml` | Modify family with union rule | ModifyColumnFamily with OR logic |
| `19-reset-column-family.yml` | Remove all GC rules from family | ResetColumnFamily (keep all versions) |
| `20-create-backup-duration.yml` | Create backup with duration | CreateBackup with duration-based expiry |
| `21-create-backup-timestamp.yml` | Create backup with timestamp | CreateBackup with specific expiry date |
| `22-delete-backup.yml` | Delete backup | DeleteBackup |
| `23-create-backup-hot.yml` | Create HOT backup | CreateBackup for recent data |
| `24-restore-table-new.yml` | Restore to new table | RestoreTable (create new) |
| `25-restore-table-overwrite.yml` | Restore and overwrite | RestoreTable (replace existing) |
| `26-restore-table-staging.yml` | Restore production to staging | RestoreTable for testing |
| `27-set-iam-policy-simple.yml` | Grant simple IAM access | SetTableIamPolicy, single role |
| `28-set-iam-policy-multiple-roles.yml` | Grant multiple IAM roles | SetTableIamPolicy, multiple roles and members |
| `29-set-iam-policy-multi-tenant.yml` | Multi-tenant access control | SetTableIamPolicy with variables |
| `30-remove-iam-policy-simple.yml` | Revoke simple IAM access | RemoveTableIamPolicy, single member |
| `31-remove-iam-policy-multiple.yml` | Revoke multiple IAM bindings | RemoveTableIamPolicy, cleanup |
| `32-remove-iam-policy-revoke-all.yml` | Revoke all access for service | RemoveTableIamPolicy, security incident |

## Instance Management

**⚠️ Important:** Instance creation is typically handled by infrastructure-as-code tools (Terraform/Pulumi) for production. Use TotalSchema instance actions for:
- Ephemeral dev/test instances
- Automated testing pipelines  
- SaaS per-customer provisioning
- Disaster recovery automation

See `00-create-instance.yml` for examples.

## Schema Evolution with Column Families

TotalSchema supports schema evolution without table recreation using CreateColumnFamily and DeleteColumnFamily:

### Adding Column Families (CreateColumnFamily)

Add new column families to existing tables without downtime:

```yaml
# Simple family
CreateColumnFamily:
  tableId: users
  familyName: analytics

# With GC rules
CreateColumnFamily:
  tableId: users
  familyName: audit_log
  maxVersions: 100
  maxAge: 365d

# With flexible retention
CreateColumnFamily:
  tableId: users
  familyName: cache
  union:
    - maxVersions: 3
    - maxAge: 24h
```

See examples `09-14` for detailed use cases.

### Removing Column Families (DeleteColumnFamily)

⚠️ **WARNING:** Removes column family and **permanently deletes all its data**.

```yaml
DeleteColumnFamily:
  tableId: users
  familyName: old_analytics
```

See example `14-remove-column-family.yml`.

### Modifying Column Families (ModifyColumnFamily)

Update GC rules on existing column families without recreation. Use this to change retention policies for compliance or performance tuning:

```yaml
# Update version retention
ModifyColumnFamily:
  tableId: users
  familyName: profile
  maxVersions: 10

# Update age-based retention
ModifyColumnFamily:
  tableId: users
  familyName: audit
  maxAge: 90d

# Update with intersection (both rules)
ModifyColumnFamily:
  tableId: users
  familyName: sensitive
  maxVersions: 100
  maxAge: 365d

# Update with union (flexible retention)
ModifyColumnFamily:
  tableId: users
  familyName: analytics
  union:
    - maxVersions: 5
    - maxAge: 30d
```

**Use Cases:**
- Extending retention from 7d to 30d for compliance
- Adjusting version limits for performance tuning
- Compliance requirement updates

See examples `15-18` for detailed use cases.

### Resetting Column Families (ResetColumnFamily)

Reset a column family to default configuration (no GC rules). After reset, BigTable keeps all versions indefinitely:

```yaml
ResetColumnFamily:
  tableId: users
  familyName: profile
```

⚠️ **WARNING:** This removes all GC rules, which means BigTable will keep all cell versions indefinitely. This can lead to increased storage costs and slower queries. Consider using `ModifyColumnFamily` instead.

**Use Cases:**
- Removing unwanted GC policies
- Reverting to default state
- Preparation for applying new GC rules (reset then modify)

See example `19-reset-column-family.yml`.

## IAM Policy Management

TotalSchema supports table-level IAM policy management for fine-grained access control:

### Setting IAM Policies (SetTableIamPolicy)

Grant IAM permissions to users, service accounts, and groups:

```yaml
# Grant read access to analytics service
SetTableIamPolicy:
  tableId: users
  bindings:
    - role: roles/bigtable.reader
      members:
        - serviceAccount:analytics@project.iam.gserviceaccount.com

# Grant multiple roles
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

**Member Formats:**
- `user:email@domain.com` - Individual user
- `serviceAccount:sa@project.iam.gserviceaccount.com` - Service account
- `group:team@domain.com` - Google Group
- `domain:example.com` - All users in a domain
- `allUsers` - Public access (use with caution!)
- `allAuthenticatedUsers` - Any authenticated user

**Common Roles:**
- `roles/bigtable.reader` - Read-only access
- `roles/bigtable.user` - Read and write access
- `roles/bigtable.admin` - Full table administration

See examples `27-29` for detailed use cases.

### Removing IAM Policies (RemoveTableIamPolicy)

Revoke IAM permissions from specific members:

```yaml
# Revoke access from ex-employee
RemoveTableIamPolicy:
  tableId: users
  bindings:
    - role: roles/bigtable.reader
      members:
        - user:ex-employee@company.com

# Revoke multiple roles from decommissioned service
RemoveTableIamPolicy:
  tableId: sensitive_data
  bindings:
    - role: roles/bigtable.reader
      members:
        - serviceAccount:old-service@project.iam.gserviceaccount.com
    - role: roles/bigtable.user
      members:
        - serviceAccount:old-service@project.iam.gserviceaccount.com
```

**Behavior:**
- Removes only specified members from specified roles
- Other members in the same role are preserved
- If all members are removed from a role, the role binding is removed
- Silently succeeds if member doesn't exist (idempotent)

**Use Cases:**
- Employee offboarding
- Decommissioning services
- Security incident response
- Access control cleanup

See examples `30-32` for detailed use cases.

### Multi-Tenancy Pattern

Use variables for dynamic IAM policy configuration per tenant:

```yaml
SetTableIamPolicy:
  tableId: tenant_${TENANT_ID}_data
  bindings:
    - role: roles/bigtable.user
      members:
        - serviceAccount:tenant-${TENANT_ID}@${GCP_PROJECT_ID}.iam.gserviceaccount.com
```

Set variables in `totalschema.yml` or via environment-specific configuration.

## Backup and Restore

Disaster recovery and data protection using BigTable backups:

### Creating Backups (CreateBackup)

```yaml
# Duration-based expiry
CreateBackup:
  sourceTableId: users
  backupId: users_backup_${BACKUP_DATE}
  expireTime: 30d
  clusterId: my-cluster

# Timestamp-based expiry
CreateBackup:
  sourceTableId: users
  backupId: users_backup_20260720
  expireTime: "2026-08-20T00:00:00Z"
```

See examples `20-23`.

### Restoring Tables (RestoreTable)

```yaml
# Restore to new table
RestoreTable:
  backupId: users_backup_20260720
  tableId: users_restored
  clusterId: my-cluster

# Restore and overwrite (DANGER!)
RestoreTable:
  backupId: users_backup_20260720
  tableId: users
  overwrite: true
```

See examples `24-26`.

## Duration Format

All examples use human-readable duration strings:

```
7d      = 7 days
24h     = 24 hours
30m     = 30 minutes
3600s   = 3600 seconds
1000ms  = 1000 milliseconds
```

## Common Patterns

### Keep Only Latest (Configuration Data)
```yaml
- name: current_config
  maxVersions: 1
```

### Short-Term Cache (24 Hours)
```yaml
- name: cache
  maxAge: 24h
```

### Audit Trail (10 Versions, 1 Year Max)
```yaml
- name: audit
  maxVersions: 10
  maxAge: 365d
```

### Flexible Retention (Recent Versions OR Time-Based)
```yaml
- name: flexible
  union:
    - maxVersions: 5
    - maxAge: 7d
```

## Testing Examples

You can test any example by creating a TotalSchema workspace:

```bash
# Create workspace structure
mkdir -p my-workspace/totalschema

# Copy example
cp examples/09-add-column-family-simple.yml \
   my-workspace/totalschema/0002.add_analytics.DEV.apply.bigtable.yml

# Configure connector in totalschema.yml
# ... add BigTable connector configuration ...

# Run
totalschema.sh apply -e DEV -w my-workspace
```

## Learn More

See `GC_RULES_FEATURE.md` for complete documentation.