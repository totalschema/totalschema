# GCP Bigtable Connector - Documentation Index

Welcome to the TotalSchema GCP Bigtable connector documentation!

## Quick Navigation

### 🚀 Getting Started
- **[Quick Start Guide](QUICK-START.md)** — Get up and running in 5 minutes
- **[Full User Manual](README.md)** — Comprehensive documentation covering all features

### 📚 Reference Documentation
- **[Actions Reference](ACTIONS-REFERENCE.md)** — Quick lookup for all YAML actions
- **[FAQ & Troubleshooting](FAQ.md)** — Common questions and solutions

### 💡 Learning Resources
- **[Examples Directory](../../examples/README.md)** — 40+ real-world examples
- **[Main TotalSchema Docs](../../../../docs/user_manual/README.md)** — Core concepts and CLI commands

---

## Documentation Structure

### 1. Quick Start Guide ([QUICK-START.md](QUICK-START.md))

Perfect for:
- First-time users
- Quick proof-of-concept
- Learning the basics

Covers:
- Minimal configuration setup
- First table creation
- Common workflows
- Troubleshooting tips

**Time to complete:** 5-10 minutes

---

### 2. User Manual ([README.md](README.md))

Perfect for:
- Understanding all features
- Production deployments
- Advanced configurations

Covers:
- Complete feature overview
- Configuration options (basic and advanced)
- All supported actions with examples
- Garbage collection rules in depth
- Variable substitution patterns
- Best practices and patterns
- Comprehensive troubleshooting

**Sections:**
1. Overview & Features
2. Prerequisites & Setup
3. Configuration (basic, Private Service Connection, authentication)
4. Change Script Formats (YAML, Groovy)
5. Supported Actions:
   - Table Management
   - Column Family Management
   - Backup & Restore
   - IAM Policy Management
   - Replication Management
   - Autoscaling Management
   - Instance Management
6. Garbage Collection Rules
7. Variable Substitution
8. Best Practices
9. Examples
10. Troubleshooting

---

### 3. Actions Reference ([ACTIONS-REFERENCE.md](ACTIONS-REFERENCE.md))

Perfect for:
- Quick action syntax lookup
- Command reference while writing scripts
- Learning action-specific properties

Organized by action category:
- Table Actions (CreateTable, DeleteTable, SetTableSplits, ClearTableSplits)
- Column Family Actions (CreateColumnFamily, ModifyColumnFamily, ResetColumnFamily, DeleteColumnFamily)
- Backup Actions (CreateBackup, DeleteBackup, RestoreTable)
- IAM Actions (SetTableIamPolicy, RemoveTableIamPolicy)
- Replication Actions (EnableTableReplication, DisableTableReplication)
- Autoscaling Actions (SetTableAutoscaling, DisableTableAutoscaling)
- Instance Actions (CreateInstance, DeleteInstance)

Plus:
- GC Rule Syntax (maxVersions, maxAge, intersection, union)
- Duration Format Reference
- Variable Substitution Examples
- Common Patterns

---

### 4. FAQ & Troubleshooting ([FAQ.md](FAQ.md))

Perfect for:
- Solving specific problems
- Understanding common patterns
- Learning from common mistakes

Organized by topic:
1. **General Questions** — Connector purpose, capabilities, file formats
2. **Authentication & Permissions** — GCP setup, service accounts, common auth errors
3. **Configuration Issues** — Instance not found, Private Service Connection, variables
4. **Action Execution Problems** — Syntax errors, validation issues, dry-run usage
5. **Backup & Restore Issues** — Timeouts, cluster IDs, automated backups
6. **GC Rules & Data Retention** — Rule syntax, intersection vs union, data deletion timing
7. **Performance & Cost** — Cost optimization, autoscaling, HDD vs SSD
8. **Integration Questions** — CI/CD, Terraform, Kubernetes, multi-tenancy

---

## Common Use Cases

### 1. First-Time Setup
1. Read [Quick Start Guide](QUICK-START.md)
2. Configure connector in `totalschema.yml`
3. Create first table with [CreateTable example](QUICK-START.md#step-3-create-your-first-change-script)
4. Apply changes

### 2. Adding a New Column Family
1. Check [Actions Reference - CreateColumnFamily](ACTIONS-REFERENCE.md#createcolumnfamily)
2. Review [examples/09-add-column-family-simple.yml](../../examples/09-add-column-family-simple.yml)
3. Create change script
4. Apply with `totalschema.sh apply -e ENV`

### 3. Setting Up Backups
1. Read [User Manual - Backup & Restore](README.md#backup-and-restore)
2. Review [examples/20-create-backup-duration.yml](../../examples/20-create-backup-duration.yml)
3. Create `apply_always` script for automated backups
4. Schedule with cron or CI/CD

### 4. Managing IAM Policies
1. Read [User Manual - IAM Policy Management](README.md#iam-policy-management)
2. Check [Actions Reference - SetTableIamPolicy](ACTIONS-REFERENCE.md#settableiampolicy)
3. Review [examples/27-set-iam-policy-simple.yml](../../examples/27-set-iam-policy-simple.yml)
4. Create change script with appropriate roles

### 5. Configuring GC Rules
1. Read [User Manual - Garbage Collection Rules](README.md#garbage-collection-rules)
2. Check [Actions Reference - GC Rule Syntax](ACTIONS-REFERENCE.md#gc-rule-syntax)
3. Review examples:
   - [02-max-versions.yml](../../examples/02-max-versions.yml)
   - [03-max-age.yml](../../examples/03-max-age.yml)
   - [04-intersection.yml](../../examples/04-intersection.yml)
   - [05-union.yml](../../examples/05-union.yml)
4. Choose appropriate rule pattern
5. Apply to new tables or modify existing families

### 6. Troubleshooting Authentication
1. Check [FAQ - Authentication & Permissions](FAQ.md#authentication--permissions)
2. Verify credentials: `gcloud auth application-default print-access-token`
3. Check permissions: see [FAQ Q: "403 Permission denied"](FAQ.md#q-error-403-permission-denied-on-resource)
4. Re-authenticate if needed

### 7. Production Deployment
1. Read [User Manual - Best Practices](README.md#best-practices)
2. Set up proper state repository (database, not CSV)
3. Configure distributed locking
4. Use encrypted secrets for passwords
5. Test in DEV with `--dry-run`
6. Deploy to PROD with appropriate IAM controls

---

## Examples Directory

The [examples](../../examples/) directory contains 40+ real-world YAML examples demonstrating all features:

### Table Management (Examples 01-08)
- Simple families without GC rules
- Version-based retention (maxVersions)
- Time-based retention (maxAge)
- Intersection (AND logic)
- Union (OR logic)
- Real-world e-commerce schema
- IoT tiered retention
- Table deletion

### Column Family Management (Examples 09-19)
- Adding column families (simple, with GC rules)
- Removing column families
- Modifying GC rules
- Resetting families to default

### Backup & Restore (Examples 20-26)
- Creating backups (duration, timestamp, HOT)
- Deleting backups
- Restoring tables (new table, overwrite, staging)

### IAM Management (Examples 27-32)
- Setting IAM policies (simple, multiple roles, multi-tenant)
- Removing IAM policies (simple, multiple, revoke all)

### Replication (Examples 33-38)
- Enabling replication (eventual, strong, global)
- Disabling replication (simple, transactional, cost optimization)

### Autoscaling (Examples 39-44)
- Enabling autoscaling (dev, prod, balanced)
- Disabling autoscaling (dev, prod, staging)

### Instance Management (Example 00)
- Creating instances (development, production, CI/CD)
- Deleting instances

**Browse:** [examples/README.md](../../examples/README.md)

---

## Documentation Conventions

### Code Examples

YAML action examples:
```yaml
CreateTable:
  tableId: example_table
  families:
    - name: family_name
      maxVersions: 5
```

Shell commands:
```bash
totalschema.sh apply -e DEV --dry-run
```

### Symbols

- ✅ Recommended / Correct
- ❌ Not recommended / Incorrect
- ⚠️ Warning / Important note
- 📖 See also / Reference

### File Naming Convention

Change scripts follow this pattern:
```
<order>.<description>[.<environment>].<changetype>.<connector>.yml
```

Example:
```
0001.create_users.DEV.apply.mybigtable.yml
```

---

## Related Documentation

### TotalSchema Core Documentation
- **[Main User Manual](../../../../docs/user_manual/README.md)** — Core concepts, state tracking, variables
- **[CLI Usage](../../../../docs/user_manual/CLI-USAGE.md)** — All CLI commands and options
- **[AGENTS.md](../../../../AGENTS.md)** — Developer guide to TotalSchema architecture

### Connector-Specific Documentation
- **[BigQuery Connector](../../../totalschema-gcp-bigquery/docs/user_manual/)** — SQL-based BigQuery deployments
- **[SSH Connector](../../../totalschema-connector-ssh/docs/)** — Remote server deployments
- **[Shell Connector](../../../totalschema-connector-shell/docs/)** — Local shell scripts
- **[Python Connector](../../../totalschema-connector-python/docs/)** — Python script execution

### Google Cloud Documentation
- [Bigtable Overview](https://cloud.google.com/bigtable/docs)
- [Bigtable Schema Design Best Practices](https://cloud.google.com/bigtable/docs/schema-design)
- [Garbage Collection Rules](https://cloud.google.com/bigtable/docs/garbage-collection)
- [Bigtable IAM](https://cloud.google.com/bigtable/docs/access-control)
- [Backups and Restore](https://cloud.google.com/bigtable/docs/backups)

---

## Contributing

Found an issue or have a suggestion? Please file an issue:
- **Issues:** https://github.com/totalschema/totalschema/issues
- **Source:** https://github.com/totalschema/totalschema

---

## License

GNU Affero General Public License v3.0 - See [LICENSE](../../../../LICENSE) file for details.

---

## Quick Links Summary

| Document | Purpose | Target Audience |
|----------|---------|-----------------|
| [QUICK-START.md](QUICK-START.md) | Get started in 5 minutes | First-time users |
| [README.md](README.md) | Comprehensive manual | All users |
| [ACTIONS-REFERENCE.md](ACTIONS-REFERENCE.md) | Quick action lookup | Script authors |
| [FAQ.md](FAQ.md) | Common questions & troubleshooting | All users |
| [Examples](../../examples/README.md) | Real-world examples | All users |

---

## Version Information

- **Connector Type:** `bigtable`
- **TotalSchema Version:** 1.3.0+
- **Last Updated:** July 2026
- **Documentation Version:** 1.0

---

**Need help?** Start with the [Quick Start Guide](QUICK-START.md) or browse the [FAQ](FAQ.md).

