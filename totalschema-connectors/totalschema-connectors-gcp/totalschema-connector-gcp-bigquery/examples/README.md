# BigQuery Connector Examples

This directory contains example change files demonstrating how to use the BigQuery connector with TotalSchema.

## File Organization

Examples are numbered in logical order:

1. **01-create-dataset.yml** - Create a BigQuery dataset
2. **02-create-table.yml** - Create a table using YAML (ExecuteStatement action)
3. **03-create-orders-table.sql** - Create a table using SQL file
4. **04-scheduled-queries.yml** - Create scheduled queries (Data Transfer Service)
5. **05-delete-scheduled-queries.yml** - Delete scheduled queries
6. **06-enable-disable-scheduled-queries.yml** - Enable/disable scheduled queries
7. **07-create-listing.yml** - Create Analytics Hub listings

## Connector Configuration

All examples assume you've configured connector defaults in your `totalschema.yml`:

```yaml
connectors:
  mybigquery:
    type: bigquery
    defaults:
      projectId: my-gcp-project  # Your GCP project ID
      location: US               # Default location (US, EU, etc.)
    credentials:
      type: service_account
      path: /path/to/service-account-key.json
```

## Benefits of Connector Defaults

With connector defaults configured:

- ✅ **No repetition** - Specify `projectId` and `location` once
- ✅ **Cleaner actions** - Focus on what's being created, not configuration
- ✅ **Per-action overrides** - Override defaults when needed (e.g., EU-specific resources)
- ✅ **Environment-specific** - Use TotalSchema variables for different environments

## Using Examples

### Copy to Your Project

```bash
cp examples/01-create-dataset.yml totalschema/0001.create_dataset.DEV.apply.mybigquery.yml
cp examples/02-create-table.yml totalschema/0002.create_users.DEV.apply.mybigquery.yml
```

### File Naming Convention

TotalSchema change files follow this pattern:

```
<order>.<description>.<env>.<type>.<connector>.<ext>
```

Example:
```
0001.create_dataset.DEV.apply.mybigquery.yml
```

- `0001` - Execution order
- `create_dataset` - Description
- `DEV` - Environment (optional)
- `apply` - Type (apply, apply_always, apply_on_change, revert)
- `mybigquery` - Connector name (must match `connectors` in totalschema.yml)
- `.yml` - File extension

## Example Workflows

### Basic Setup (Examples 01-03)

Create a dataset and tables:

```bash
# 1. Create dataset
totalschema apply -e DEV -f totalschema/0001.create_dataset.DEV.apply.mybigquery.yml

# 2. Create tables
totalschema apply -e DEV -f totalschema/0002.create_users.DEV.apply.mybigquery.yml
totalschema apply -e DEV -f totalschema/0003.create_orders.DEV.apply.mybigquery.sql
```

### Scheduled Queries (Examples 04-06)

Manage scheduled queries lifecycle:

```bash
# Create scheduled queries
totalschema apply -e PROD -f totalschema/0010.daily_reports.PROD.apply.mybigquery.yml

# Disable during maintenance
totalschema apply -e PROD -f totalschema/0011.disable_reports.PROD.apply_always.mybigquery.yml

# Re-enable after maintenance
totalschema apply -e PROD -f totalschema/0012.enable_reports.PROD.apply_always.mybigquery.yml

# Delete when no longer needed
totalschema apply -e PROD -f totalschema/0013.delete_old_reports.PROD.apply.mybigquery.yml
```

### Analytics Hub (Example 07)

Share datasets via Analytics Hub:

```bash
totalschema apply -e PROD -f totalschema/0020.share_sales_data.PROD.apply.mybigquery.yml
```

## Action Types

### Dataset Operations
- **CreateDataset** - Create a new dataset

### Query Execution
- **ExecuteStatement** - Execute SQL (DDL, DML, queries)

### Scheduled Queries (Data Transfer Service)
- **CreateScheduledQuery** - Create a scheduled query
- **DisableScheduledQuery** - Pause a scheduled query
- **EnableScheduledQuery** - Resume a scheduled query  
- **DeleteScheduledQuery** - Delete a scheduled query

### Analytics Hub
- **CreateListing** - Share a dataset via Analytics Hub

## Per-Action Overrides

Override connector defaults when needed:

```yaml
# Use EU location instead of default US
CreateDataset:
  datasetId: eu_dataset
  location: EU  # Override connector default

# Use different project
CreateScheduledQuery:
  projectId: analytics-project  # Override connector default
  displayName: Cross-Project Query
  query: "SELECT ..."
```

## Variables

Use TotalSchema variables for environment-specific values:

```yaml
# totalschema.yml
variables:
  dataset_prefix: dev

environments:
  PROD:
    variables:
      dataset_prefix: prod

# Change file
CreateDataset:
  datasetId: ${dataset_prefix}_analytics
```

## Documentation

For complete documentation, see:
- [Connector Configuration Guide](../CONFIGURATION_EXAMPLE.md)
- [TotalSchema User Manual](../../../../docs/user_manual/README.md)
- [BigQuery Connector Architecture](../REFACTORING_SUMMARY.md)

