# GCP Bigtable Connector - FAQ & Troubleshooting

Frequently asked questions and solutions to common issues.

## Table of Contents

- [General Questions](#general-questions)
- [Authentication & Permissions](#authentication--permissions)
- [Configuration Issues](#configuration-issues)
- [Action Execution Problems](#action-execution-problems)
- [Backup & Restore Issues](#backup--restore-issues)
- [GC Rules & Data Retention](#gc-rules--data-retention)
- [Performance & Cost](#performance--cost)
- [Integration Questions](#integration-questions)

---

## General Questions

### Q: What is the difference between Bigtable connector and JDBC connector?

**A:** The Bigtable connector is specifically designed for Google Cloud Bigtable (NoSQL, wide-column store), while JDBC connects to relational databases (PostgreSQL, MySQL, etc.). Bigtable uses declarative YAML for table/column family management, not SQL.

### Q: Can I use this to migrate data between Bigtable instances?

**A:** Not directly. The Bigtable connector manages **schema** and **infrastructure** (tables, column families, backups, IAM). Data migration requires custom Groovy scripts using the Bigtable Admin API or separate data transfer tools.

### Q: Do I need to manage Bigtable instances with TotalSchema?

**A:** No. Instance management actions (`CreateInstance`, `DeleteInstance`) are provided for ephemeral dev/test scenarios. For production, use infrastructure-as-code tools like Terraform or Pulumi. TotalSchema excels at managing tables, column families, and operational changes.

### Q: What file extensions are supported?

**A:** 
- `.yml` / `.yaml` — Declarative YAML actions (recommended)
- `.groovy` — Groovy scripts with Bigtable Admin API bindings (requires `groovy-all.jar` in `user_libs/`)
- JSR-223 scripting languages (if configured)

### Q: Are Bigtable change scripts environment-specific?

**A:** They can be. Use the optional environment suffix in the filename:
- `0001.create_users.apply.mybigtable.yml` — applies to all environments
- `0001.create_users.DEV.apply.mybigtable.yml` — applies only to DEV

Use variables for environment-specific values (table names, cluster IDs, etc.).

### Q: How does TotalSchema track applied changes for Bigtable?

**A:** Just like database migrations. Every applied change is recorded in the state repository (typically a relational database like H2, PostgreSQL, MySQL). TotalSchema skips changes that have already been applied.

---

## Authentication & Permissions

### Q: How do I authenticate with GCP?

**A:** The connector uses **Application Default Credentials (ADC)**:

**Local development:**
```bash
gcloud auth application-default login
```

**CI/CD or production:**
```bash
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json
```

**GCE/GKE/Cloud Run:**
Service accounts are automatically available via the metadata server.

### Q: What GCP permissions do I need?

**A:** Depends on what actions you're performing:

| Action Type | Required Role |
|------------|---------------|
| Table management | `roles/bigtable.admin` |
| Backup/restore | `roles/bigtable.admin` |
| IAM policy management | `roles/bigtable.admin` + `roles/iam.serviceAccountUser` |
| Instance management | `roles/bigtable.admin` |

For production, create a custom role with minimal required permissions.

### Q: Error: "Failed to create BigtableTableAdminClient"

**A:** Authentication issue. Check:

1. **Verify credentials exist:**
   ```bash
   gcloud auth application-default print-access-token
   ```

2. **Re-authenticate:**
   ```bash
   gcloud auth application-default login
   ```

3. **Verify service account key (CI/CD):**
   ```bash
   echo $GOOGLE_APPLICATION_CREDENTIALS
   cat $GOOGLE_APPLICATION_CREDENTIALS  # Should show valid JSON
   ```

### Q: Error: "403 Permission denied on resource"

**A:** Service account lacks required permissions.

1. **Check current permissions:**
   ```bash
   gcloud projects get-iam-policy PROJECT_ID \
     --flatten="bindings[].members" \
     --filter="bindings.members:serviceAccount:YOUR_SA@PROJECT.iam.gserviceaccount.com"
   ```

2. **Grant required role:**
   ```bash
   gcloud projects add-iam-policy-binding PROJECT_ID \
     --member="serviceAccount:YOUR_SA@PROJECT.iam.gserviceaccount.com" \
     --role="roles/bigtable.admin"
   ```

### Q: Can I use different service accounts for different environments?

**A:** Yes. Set the environment variable before running TotalSchema:

```bash
# DEV
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/dev-sa-key.json
totalschema.sh apply -e DEV

# PROD
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/prod-sa-key.json
totalschema.sh apply -e PROD
```

Or wrap in a script:

```bash
#!/bin/bash
ENV=$1
export GOOGLE_APPLICATION_CREDENTIALS="/secure/keys/${ENV}-sa-key.json"
totalschema.sh apply -e $ENV
```

---

## Configuration Issues

### Q: Error: "Instance not found: projects/PROJECT/instances/INSTANCE"

**A:** Instance doesn't exist or wrong name in config.

1. **List available instances:**
   ```bash
   gcloud bigtable instances list --project=PROJECT_ID
   ```

2. **Verify `totalschema.yml`:**
   ```yaml
   connectors:
     mybigtable:
       projectId: my-gcp-project  # Must match exactly
       instanceId: my-instance    # Must match exactly
   ```

### Q: How do I use Private Service Connection?

**A:** Set the `adminEndpoint` and/or `dataEndpoint` properties:

```yaml
connectors:
  mybigtable:
    type: bigtable
    projectId: my-gcp-project
    instanceId: my-instance
    
    # For table management operations
    adminEndpoint: bigtableadmin-custom-postfix.p.googleapis.com:443
    
    # For read/write data operations
    dataEndpoint: bigtable-custom-postfix.p.googleapis.com:443
    
    # Optional: specify app profile
    appProfileId: my-app-profile
```

**Endpoint Usage:**
- `adminEndpoint` — Used by YAML actions (CreateTable, ModifyColumnFamily, etc.)
- `dataEndpoint` — Used by BigtableDataClient in Groovy scripts for data operations
- `appProfileId` — Controls routing behavior for data requests

### Q: Can I disable the connection check?

**A:** Yes, set `connectionCheck.enabled: false`:

```yaml
connectors:
  mybigtable:
    type: bigtable
    projectId: my-gcp-project
    instanceId: my-instance
    connectionCheck:
      enabled: false
```

Useful for testing or when you know the instance may not exist yet (e.g., creating it via TotalSchema).

### Q: How do I use variables in Bigtable actions?

**A:** Define variables in `totalschema.yml` and reference with `${variableName}`:

**totalschema.yml:**
```yaml
variables:
  tablePrefix: prod
  backupRetentionDays: 30

environments:
  DEV:
    variables:
      tablePrefix: dev
      backupRetentionDays: 7
```

**Change script:**
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

---

## Action Execution Problems

### Q: Error: "Table already exists: TABLE_ID"

**A:** This is expected if the script was already applied. TotalSchema tracks applied changes and skips duplicates. If you intentionally want to recreate the table:

1. Create a revert script:
   ```yaml
   # 0001.create_users.DEV.revert.mybigtable.yml
   DeleteTable:
     tableId: users
   ```

2. Revert:
   ```bash
   totalschema.sh revert execute -e DEV
   ```

3. Re-apply:
   ```bash
   totalschema.sh apply -e DEV
   ```

### Q: Error: "Unknown action type: CreateTables"

**A:** Typo in action name. Valid action names are case-sensitive:
- ✅ `CreateTable` (singular)
- ❌ `CreateTables` (plural)

Check the [Actions Reference](ACTIONS-REFERENCE.md) for correct names.

### Q: Error: "Configuration must contain exactly one top-level key"

**A:** YAML file has multiple actions or incorrect structure. Each YAML file should contain **one action**:

**Wrong:**
```yaml
CreateTable:
  tableId: users
CreateTable:
  tableId: orders
```

**Correct:**
```yaml
# File 1: 0001.create_users.apply.mybigtable.yml
CreateTable:
  tableId: users

# File 2: 0002.create_orders.apply.mybigtable.yml
CreateTable:
  tableId: orders
```

### Q: How do I see what will be applied without executing?

**A:** Use `--dry-run`:

```bash
totalschema.sh apply -e DEV --dry-run
totalschema.sh show pending-apply -e DEV
```

### Q: Why isn't my `apply_always` script running every time?

**A:** Check the filename. It must contain `apply_always` as the change type:

```
0100.daily_backup.apply_always.mybigtable.yml  ✅
0100.daily_backup.apply.mybigtable.yml         ❌
```

### Q: Error: "Invalid GC rule configuration"

**A:** GC rule syntax error. Common issues:

1. **Cannot mix shorthand with explicit blocks:**
   ```yaml
   # Wrong:
   maxVersions: 5
   intersection:
     - maxAge: 7d
   
   # Correct:
   intersection:
     - maxVersions: 5
     - maxAge: 7d
   ```

2. **Cannot have both intersection and union at same level:**
   ```yaml
   # Wrong:
   intersection:
     - maxVersions: 10
   union:
     - maxAge: 7d
   
   # Correct (nest one inside the other):
   union:
     - maxVersions: 10
     - intersection:
         - maxVersions: 100
         - maxAge: 30d
   ```

3. **Invalid duration format:**
   ```yaml
   # Wrong:
   maxAge: "7 days"
   maxAge: 7
   
   # Correct:
   maxAge: 7d
   maxAge: 24h
   ```

---

## Backup & Restore Issues

### Q: Backup creation takes forever / times out

**A:** Backup creation time depends on table size:
- Small tables (GB): seconds to minutes
- Large tables (TB+): can take hours

**Solutions:**
- Ensure sufficient cluster capacity
- Monitor progress in GCP Console
- Consider HOT backups for faster creation (if supported for your use case)
- For very large tables, schedule backups during off-peak hours

### Q: How do I list available cluster IDs?

**A:** Use gcloud:

```bash
gcloud bigtable clusters list --instances=YOUR_INSTANCE --project=PROJECT_ID
```

Output shows cluster IDs you can use in `CreateBackup`, `RestoreTable`, etc.

### Q: Error: "Backup not found"

**A:** 
1. Verify backup exists:
   ```bash
   gcloud bigtable backups list --instances=YOUR_INSTANCE --project=PROJECT_ID
   ```

2. Check `backupId` and `clusterId` in your script:
   ```yaml
   RestoreTable:
     backupId: users_backup_20260731  # Must match exactly
     clusterId: prod-cluster-us-east1 # Must match exactly
   ```

### Q: Can I restore a backup from one instance to another?

**A:** Not directly with TotalSchema actions. Bigtable backups are instance-scoped. For cross-instance restores, use GCP Console or `gcloud` CLI.

### Q: What's the difference between STANDARD and HOT backups?

**A:**
- **STANDARD** — regular backups, standard restore speed, standard pricing
- **HOT** — optimized for faster restore operations, may have different pricing

Use HOT for backups you may need to restore quickly (disaster recovery scenarios).

### Q: How do I automate daily backups?

**A:** Create an `apply_always` script with variables:

```yaml
# changes/0100.daily_backup.apply_always.mybigtable.yml
CreateBackup:
  sourceTableId: ${tablePrefix}_users
  backupId: ${tablePrefix}_users_backup_${DATE}
  expireTime: 30d
  clusterId: ${clusterName}
```

Set `DATE` variable at runtime:

```bash
DATE=$(date +%Y%m%d) totalschema.sh apply -e PROD
```

Or use a cron job:

```bash
0 2 * * * cd /path/to/workspace && DATE=$(date +\%Y\%m\%d) totalschema.sh apply -e PROD
```

---

## GC Rules & Data Retention

### Q: What happens if I don't specify GC rules?

**A:** Bigtable keeps **all versions indefinitely**. This can lead to:
- Increased storage costs
- Slower queries (more versions to scan)
- Larger backup sizes

Always specify appropriate GC rules.

### Q: How do I keep only the latest version?

**A:**
```yaml
families:
  - name: profile
    maxVersions: 1
```

### Q: How do I delete cells older than 30 days?

**A:**
```yaml
families:
  - name: sessions
    maxAge: 30d
```

### Q: What's the difference between intersection and union?

**A:**

**Intersection (AND)** — All rules must be satisfied:
```yaml
families:
  - name: audit
    intersection:
      - maxVersions: 100  # Keep up to 100 versions...
      - maxAge: 365d      # ...but delete after 1 year regardless
```
Result: Keep at most 100 versions, but delete everything older than 1 year.

**Union (OR)** — Any rule can be satisfied:
```yaml
families:
  - name: analytics
    union:
      - maxVersions: 5    # Keep 5 recent versions...
      - maxAge: 7d        # ...OR keep last 7 days of data
```
Result: Keep at least 5 versions OR 7 days of data (whichever retains more).

### Q: Can I change GC rules on an existing column family?

**A:** Yes, use `ModifyColumnFamily`:

```yaml
ModifyColumnFamily:
  tableId: users
  familyName: profile
  maxVersions: 10  # Change from 1 to 10
```

**Important:** Test in DEV first. Changing from aggressive to lenient rules is safe. Changing from lenient to aggressive (e.g., reducing retention) will cause data deletion on next compaction.

### Q: How do I remove all GC rules?

**A:** Use `ResetColumnFamily`:

```yaml
ResetColumnFamily:
  tableId: users
  familyName: profile
```

**⚠️ WARNING:** This causes Bigtable to keep all versions indefinitely. Storage costs will grow continuously. Consider using `ModifyColumnFamily` with explicit rules instead.

### Q: When do GC rules actually delete data?

**A:** Bigtable applies GC rules during **compaction**, which happens automatically in the background. Data deletion is not immediate. Cells marked for deletion may remain visible for minutes to hours.

---

## Performance & Cost

### Q: How can I reduce Bigtable storage costs?

**A:**
1. **Use aggressive GC rules:**
   - Keep only recent versions: `maxVersions: 1` or `maxVersions: 5`
   - Short retention periods: `maxAge: 7d` or `maxAge: 30d`

2. **Delete unused column families:**
   ```yaml
   DeleteColumnFamily:
     tableId: users
     familyName: old_analytics
   ```

3. **Delete old backups:**
   ```yaml
   DeleteBackup:
     backupId: users_backup_20260501
     clusterId: prod-cluster-us-east1
   ```

4. **Monitor table sizes:**
   ```bash
   gcloud bigtable instances tables list --instances=INSTANCE --project=PROJECT
   ```

### Q: How much do Bigtable instances cost?

**A:** Approximate costs (as of 2026, subject to change):
- **SSD nodes:** ~$0.65/hour per node (~$470/month)
- **HDD nodes:** ~$0.045/hour per node (~$33/month)
- **Storage:** $0.17/GB-month (SSD), $0.026/GB-month (HDD)
- **Backups:** Storage charges apply

**Cost warning:** Always delete unused instances and backups!

### Q: Should I use autoscaling?

**A:** Yes, for production workloads with variable traffic:

```yaml
SetTableAutoscaling:
  tableId: users
  clusterId: prod-cluster-us-east1
  minNodes: 3
  maxNodes: 10
  cpuTargetPercent: 70
```

Benefits:
- Automatic scaling based on CPU utilization
- Cost savings during low-traffic periods
- Ensures performance during traffic spikes

### Q: When should I use HDD vs SSD?

**A:**
- **SSD** — production workloads, latency-sensitive applications (default)
- **HDD** — dev/test, archival data, batch processing, cost-sensitive applications

**Important:** Cannot change storage type after cluster creation. Must create new cluster/instance.

---

## Integration Questions

### Q: Can I use TotalSchema with CI/CD pipelines?

**A:** Yes! Example GitLab CI:

```yaml
deploy-bigtable-dev:
  stage: deploy
  script:
    - export GOOGLE_APPLICATION_CREDENTIALS=/secure/dev-sa-key.json
    - totalschema.sh apply -e DEV --dry-run
    - totalschema.sh apply -e DEV
  only:
    - develop

deploy-bigtable-prod:
  stage: deploy
  script:
    - export GOOGLE_APPLICATION_CREDENTIALS=/secure/prod-sa-key.json
    - totalschema.sh apply -e PROD --dry-run
    - totalschema.sh apply -e PROD
  only:
    - main
  when: manual
```

### Q: How do I integrate with Terraform/Pulumi?

**A:** Use IaC for instance management, TotalSchema for table/schema management:

**Terraform creates the instance:**
```hcl
resource "google_bigtable_instance" "production" {
  name = "prod-instance"
  # ...
}
```

**TotalSchema manages tables:**
```yaml
# changes/0001.create_users.apply.mybigtable.yml
CreateTable:
  tableId: users
  families:
    - name: profile
      maxVersions: 1
```

### Q: Can I use TotalSchema with multiple Bigtable instances?

**A:** Yes, define multiple connectors:

```yaml
connectors:
  bigtable-prod-us:
    type: bigtable
    projectId: my-project
    instanceId: prod-us-instance
  
  bigtable-prod-eu:
    type: bigtable
    projectId: my-project
    instanceId: prod-eu-instance
```

Separate change scripts by connector:
```
0001.create_users.apply.bigtable-prod-us.yml
0001.create_users.apply.bigtable-prod-eu.yml
```

### Q: How do I handle multi-tenant deployments?

**A:** Use variables for per-tenant configuration:

**totalschema.yml:**
```yaml
variables:
  tenantId: tenant1
  instanceId: ${tenantId}-instance

connectors:
  bigtable:
    type: bigtable
    projectId: my-saas-platform
    instanceId: ${instanceId}
```

**Deploy per tenant:**
```bash
tenantId=tenant1 totalschema.sh apply -e PROD -DtenantId=tenant1
tenantId=tenant2 totalschema.sh apply -e PROD -DtenantId=tenant2
```

### Q: Can I use TotalSchema with Kubernetes?

**A:** Yes. Example deployment:

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: totalschema-apply
spec:
  template:
    spec:
      serviceAccountName: totalschema-sa  # With workload identity
      containers:
      - name: totalschema
        image: myregistry/totalschema:latest
        command:
        - /bin/sh
        - -c
        - |
          cd /workspace
          totalschema.sh apply -e $ENVIRONMENT
        env:
        - name: ENVIRONMENT
          value: "PROD"
        volumeMounts:
        - name: workspace
          mountPath: /workspace
      volumes:
      - name: workspace
        configMap:
          name: totalschema-workspace
      restartPolicy: OnFailure
```

---

## Still Need Help?

- **[Full User Manual](README.md)** — comprehensive documentation
- **[Quick Start Guide](QUICK-START.md)** — get started in 5 minutes
- **[Actions Reference](ACTIONS-REFERENCE.md)** — quick action lookup
- **[Examples](../../examples/README.md)** — 40+ real-world examples
- **Issues:** https://github.com/totalschema/totalschema/issues
- **Source:** https://github.com/totalschema/totalschema

