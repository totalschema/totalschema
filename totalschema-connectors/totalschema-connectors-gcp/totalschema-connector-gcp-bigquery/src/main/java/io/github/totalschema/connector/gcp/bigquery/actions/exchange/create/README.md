# CreateExchange Action

The `CreateExchange` action creates a BigQuery Analytics Hub data exchange, a logical container used to group and share one or more listings.

## Overview

An Analytics Hub data exchange is a container for listings. It provides shared metadata (display name, description, primary contact, documentation) and its own IAM policy that controls who can browse or subscribe to the listings within it, in addition to any per-listing permissions. Data exchanges must exist before listings can be created inside them (see [CreateListing](../../listing/create/README.md)).

## Prerequisites

1. **Permissions**: The service account or user must have the following IAM role:
   - `roles/analyticshub.admin` or `roles/analyticshub.editor` on the project/location

## Action Properties

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `project` | String | No | GCP project ID in which to create the data exchange. Defaults to `defaults.projectId` from connector configuration. |
| `location` | String | No | Geographic location for the data exchange (e.g., `US`, `EU`, `asia-northeast1`). Defaults to `defaults.location` from connector configuration, falling back to `US`. |
| `dataExchangeId` | String | Yes | Unique identifier for the data exchange |
| `displayName` | String | No | Human-readable name shown in Analytics Hub (defaults to `dataExchangeId` if not provided) |
| `description` | String | No | Detailed description of the data exchange |
| `primaryContact` | String | No | Email address of the primary contact for questions about the exchange |
| `documentation` | String | No | URL to documentation about the exchange |

## Example Usage

### Basic Example

```yaml
CreateExchange:
  project: my-gcp-project
  location: US
  dataExchangeId: my_exchange
  displayName: My Data Exchange
```

### Complete Example with All Properties

```yaml
CreateExchange:
  project: ${GCP_PROJECT_ID}
  location: US
  dataExchangeId: analytics_exchange
  displayName: Company Analytics Exchange
  description: |
    Central exchange for company-wide analytics datasets.
  primaryContact: data-team@example.com
  documentation: https://docs.example.com/exchanges/analytics
```

## Usage in TotalSchema

### As a Change File

Name your file following TotalSchema conventions:
```
0005.create_analytics_exchange.PRD.apply.bigquery.yml
```

The action will be executed when you run:
```bash
totalschema apply -e PRD
```

### In a Deployment Script

Create the exchange before creating listings inside it:

```yaml
# File: 0005.setup_analytics_hub.PRD.apply.bigquery.yml

CreateExchange:
  project: ${GCP_PROJECT}
  location: US
  dataExchangeId: company_analytics_exchange
  displayName: Company Analytics Exchange
  description: Company-wide analytics data exchange
  primaryContact: analytics-team@example.com

CreateListing:
  project: ${GCP_PROJECT}
  location: US
  dataExchangeId: company_analytics_exchange
  listingId: shared_analytics_listing
  displayName: Shared Analytics Dataset
  sourceDataset:
    dataset: shared_analytics
```

## Important Notes

1. **Listings Depend on Exchanges**: A data exchange must exist before any listings can be created inside it via `CreateListing`.
2. **Beta API**: This action uses the Analytics Hub v1beta1 API, which is subject to change.

## Error Handling

Common errors and solutions:

| Error | Cause | Solution |
|-------|-------|----------|
| `Permission denied` | Insufficient IAM permissions | Grant `roles/analyticshub.admin` or `roles/analyticshub.editor` |
| `Data exchange already exists` | A data exchange with the same ID already exists | Use a different `dataExchangeId` or delete the existing exchange |

## See Also

- [BigQuery Analytics Hub Documentation](https://cloud.google.com/bigquery/docs/analytics-hub-introduction)
- [Data Exchange Documentation](https://cloud.google.com/bigquery/docs/analytics-hub-exchange)
- [TotalSchema DeleteExchange Action](../delete/README.md)
- [TotalSchema CreateListing Action](../../listing/create/README.md)

