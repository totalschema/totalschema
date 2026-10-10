# GCP Bigtable Connector - Quick Start Guide

Get started with TotalSchema Bigtable connector in 5 minutes.

## Prerequisites

- Google Cloud project with Bigtable API enabled
- Bigtable instance created
- Authenticated with GCP (`gcloud auth application-default login`)
- TotalSchema CLI installed

## Step 1: Configure Connector

Create or edit `totalschema.yml` in your workspace:

```yaml
version: 1

stateRepository:
  type: database
  database:
    username: admin
    password: ${dbPassword}
    jdbc:
      url: jdbc:h2:file:./h2db/totalschema_system/state

validation:
  type: contentHash

lock:
  type: none  # For single-user development; use 'database' for production

variables:
  gcpProjectId: my-gcp-project
  bigtableInstanceId: my-instance

environments:
  DEV:
    variables:
      dbPassword: devpass

connectors:
  mybigtable:
    type: bigtable
    projectId: ${gcpProjectId}
    instanceId: ${bigtableInstanceId}
```

## Step 2: Authenticate with GCP

```bash
# Local development
gcloud auth application-default login

# Or set service account key
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json
```

## Step 3: Create Your First Change Script

Create `changes/0001.create_users_table.DEV.apply.mybigtable.yml`:

```yaml
CreateTable:
  tableId: users
  families:
    - name: profile
      maxVersions: 1
    - name: audit
      maxAge: 365d
```

## Step 4: Apply Changes

```bash
# Preview what will be applied
totalschema.sh show pending-apply -e DEV

# Apply changes
totalschema.sh apply -e DEV

# Verify the table was created
gcloud bigtable instances list --project=my-gcp-project
```

## Step 5: Add a Column Family

Create `changes/0002.add_analytics_family.DEV.apply.mybigtable.yml`:

```yaml
CreateColumnFamily:
  tableId: users
  familyName: analytics
  maxAge: 30d
```

Apply:

```bash
totalschema.sh apply -e DEV
```

## Next Steps

### Create a Backup

Create `changes/0100.daily_backup.apply_always.mybigtable.yml`:

```yaml
CreateBackup:
  sourceTableId: users
  backupId: users_backup_${DATE}
  expireTime: 30d
  clusterId: my-cluster-us-central1
```

This will run every time (useful for automated backup scripts).

### Set IAM Policy

Create `changes/0050.grant_analytics_access.apply.mybigtable.yml`:

```yaml
SetTableIamPolicy:
  tableId: users
  bindings:
    - role: roles/bigtable.reader
      members:
        - serviceAccount:analytics@my-project.iam.gserviceaccount.com
```

### Use Groovy for Advanced Logic

Create `changes/0003.custom_logic.DEV.apply.mybigtable.groovy`:

```groovy
import com.google.cloud.bigtable.admin.v2.models.CreateTableRequest

def tableId = "dynamic_table_${environment}"
def request = CreateTableRequest.of(tableId).addFamily("cf1")

bigtableTableAdminClient.createTable(request)
println "Created table: ${tableId}"
```

**Note:** Requires `groovy-all.jar` in `user_libs/` directory.

## Common Workflows

### Deploy to Production

1. Test in DEV:
   ```bash
   totalschema.sh apply -e DEV --dry-run
   totalschema.sh apply -e DEV
   ```

2. Deploy to PROD:
   ```bash
   totalschema.sh apply -e PROD --dry-run
   totalschema.sh apply -e PROD
   ```

### Roll Back a Change

Create a revert script `changes/0001.create_users_table.DEV.revert.mybigtable.yml`:

```yaml
DeleteTable:
  tableId: users
```

Execute:

```bash
totalschema.sh revert execute -e DEV
```

### Check What's Been Applied

```bash
# List all applied changes
totalschema.sh state display -e DEV

# Show pending changes
totalschema.sh show pending-apply -e DEV
```

### Clean Up Orphaned State Records

After deleting old change scripts:

```bash
# Preview what would be removed
totalschema.sh state purge-orphaned -e DEV --dry-run

# Remove orphaned records
totalschema.sh state purge-orphaned -e DEV
```

## Troubleshooting

### "Permission denied on resource"

Grant your service account the required role:

```bash
gcloud projects add-iam-policy-binding my-gcp-project \
  --member="serviceAccount:your-sa@project.iam.gserviceaccount.com" \
  --role="roles/bigtable.admin"
```

### "Instance not found"

Verify instance exists:

```bash
gcloud bigtable instances list --project=my-gcp-project
```

Ensure `projectId` and `instanceId` match in `totalschema.yml`.

### "Failed to create BigtableTableAdminClient"

Check authentication:

```bash
gcloud auth application-default print-access-token
```

If empty, re-authenticate:

```bash
gcloud auth application-default login
```

## Learn More

- **[Full User Manual](README.md)** — comprehensive documentation
- **[Examples](../../examples/README.md)** — 40+ real-world examples
- **[CLI Reference](../../../../docs/user_manual/CLI-USAGE.md)** — all CLI commands
- **[Main TotalSchema Docs](../../../../docs/user_manual/README.md)** — core concepts

## Example Workspace Structure

```
my-workspace/
├── totalschema.yml
└── changes/
    ├── 0001.create_users_table.DEV.apply.mybigtable.yml
    ├── 0002.add_analytics_family.apply.mybigtable.yml
    ├── 0050.grant_analytics_access.apply.mybigtable.yml
    ├── 0100.daily_backup.apply_always.mybigtable.yml
    └── prod/
        └── 0001.prod_backups.PROD.apply_always.mybigtable.yml
```

## Complete Example

**totalschema.yml:**
```yaml
version: 1

stateRepository:
  type: database
  database:
    username: admin
    password: ${dbPassword}
    jdbc:
      url: jdbc:h2:file:./h2db/totalschema_system/state

validation:
  type: contentHash

lock:
  type: none

variables:
  gcpProjectId: my-gcp-project
  bigtableInstanceId: my-instance
  backupCluster: my-cluster-us-central1

environments:
  DEV:
    variables:
      dbPassword: devpass
      tablePrefix: dev
  PROD:
    variables:
      dbPassword: '${secret:!SECRET;1.0;...}'
      tablePrefix: prod

connectors:
  mybigtable:
    type: bigtable
    projectId: ${gcpProjectId}
    instanceId: ${bigtableInstanceId}
```

**changes/0001.create_users.apply.mybigtable.yml:**
```yaml
CreateTable:
  tableId: ${tablePrefix}_users
  families:
    - name: profile
      maxVersions: 1
    - name: audit
      maxAge: 365d
    - name: sessions
      maxAge: 24h
```

**Apply:**
```bash
totalschema.sh apply -e DEV
totalschema.sh apply -e PROD
```

Done! You now have version-controlled Bigtable schema management across environments.

