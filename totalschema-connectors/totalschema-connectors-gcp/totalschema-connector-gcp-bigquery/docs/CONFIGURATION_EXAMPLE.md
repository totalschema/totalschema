# BigQuery Connector Configuration Guide

## Connector-Level Defaults

The BigQuery connector supports connector-level default configuration for common GCP settings. This eliminates the need to specify these values in every action YAML file.

### Example Configuration

```yaml
connectors:
  bigquery-prod:
    type: bigquery
    # Connector defaults - used by all actions unless overridden
    defaults:
      projectId: my-gcp-project
      location: US  # or EU, us-central1, etc.
    
    # GCP credentials (optional - defaults to application default credentials)
    credentials:
      type: service_account  # or application_default
      path: /path/to/service-account-key.json
```

## Supported Defaults

| Configuration Key | Used By Actions | Description | Default Value |
|-------------------|----------------|-------------|---------------|
| `defaults.projectId` | CreateScheduledQuery, DisableScheduledQuery, CreateListing | GCP project ID | **Required** (no default) |
| `defaults.location` | CreateDataset, CreateScheduledQuery, DisableScheduledQuery, CreateListing | GCP location/region | `US` |

## Per-Action Override

Any action can override the connector-level defaults by specifying the value in its YAML file:

### Example: Dataset with Connector Default Location

```yaml
# Uses defaults.location from connector config
CreateDataset:
  datasetId: analytics_dataset
  description: Analytics data
```

### Example: Dataset with Custom Location

```yaml
# Overrides connector default
CreateDataset:
  datasetId: eu_analytics_dataset
  description: EU-specific analytics data
  location: EU  # Override connector default
```

### Example: Scheduled Query with Connector Defaults

```yaml
# Uses both defaults.projectId and defaults.location from connector config
CreateScheduledQuery:
  displayName: Daily Sales Summary
  query: |
    SELECT DATE(order_date) as date, SUM(amount) as total
    FROM `my-project.sales.orders`
    WHERE DATE(order_date) = CURRENT_DATE()
    GROUP BY date
  destinationDatasetId: analytics
  schedule: "every day 00:00"
  disabled: false
```

### Example: Scheduled Query with Custom Project

```yaml
# Overrides projectId, uses default location
CreateScheduledQuery:
  displayName: Cross-Project Query
  projectId: different-project  # Override connector default
  query: SELECT * FROM dataset.table
  destinationDatasetId: analytics
  schedule: "every day 00:00"
```

## Migration from Environment Variables

**Before (not recommended):**
```bash
export GOOGLE_CLOUD_PROJECT=my-gcp-project
./totalschema.sh apply -e PROD
```

**After (recommended):**
```yaml
# totalschema.yml
connectors:
  bigquery-prod:
    type: bigquery
    defaults:
      projectId: my-gcp-project
      location: US
```

## Benefits

1. **DRY Principle**: Specify common settings once, use everywhere
2. **Environment-Specific Config**: Different defaults per environment
3. **Explicit Configuration**: No hidden dependencies on environment variables
4. **Per-Action Flexibility**: Override defaults when needed
5. **Better Testability**: Configuration is self-contained in YAML files

## Variable Substitution

Connector defaults support TotalSchema variable substitution:

```yaml
variables:
  gcp_project: my-gcp-project
  gcp_location: US

connectors:
  bigquery-prod:
    type: bigquery
    defaults:
      projectId: ${gcp_project}
      location: ${gcp_location}
```

## Environment-Specific Defaults

```yaml
variables:
  gcp_project: my-dev-project
  gcp_location: us-central1

environments:
  PROD:
    variables:
      gcp_project: my-prod-project
      gcp_location: US

connectors:
  bigquery-main:
    type: bigquery
    defaults:
      projectId: ${gcp_project}    # my-dev-project (default) or my-prod-project (PROD)
      location: ${gcp_location}    # us-central1 (default) or US (PROD)
```

