# DeleteExchange Action

The `DeleteExchange` action deletes a BigQuery Analytics Hub data exchange.

## Overview

Deleting a data exchange removes the exchange container and its access policies. A data exchange must not contain any listings when it is deleted — all listings within it must be deleted first (see [DeleteListing](../../listing/delete/README.md)).

## Prerequisites

1. **Permissions**: The service account or user must have the following IAM role:
   - `roles/analyticshub.admin` or `roles/analyticshub.editor` on the project/location
2. **Empty Exchange**: All listings within the data exchange must be deleted before the exchange itself can be deleted.

## Action Properties

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `project` | String | No | GCP project ID containing the data exchange. Defaults to `defaults.projectId` from connector configuration. |
| `location` | String | No | Geographic location of the data exchange (e.g., `US`, `EU`, `asia-northeast1`). Defaults to `defaults.location` from connector configuration, falling back to `US`. |
| `dataExchangeId` | String | Yes | ID of the data exchange to delete |

## Example Usage

### Basic Example

```yaml
DeleteExchange:
  project: my-gcp-project
  location: US
  dataExchangeId: my_exchange
```

### Using Connector Defaults

```yaml
DeleteExchange:
  dataExchangeId: my_exchange
```

### Using with Variables

```yaml
DeleteExchange:
  project: ${env.GCP_PROJECT}
  location: ${env.GCP_REGION}
  dataExchangeId: ${dataExchangeName}
```

## Usage in TotalSchema

### As a Change File

Name your file following TotalSchema conventions:
```
0006.delete_analytics_exchange.PRD.apply.bigquery.yml
```

The action will be executed when you run:
```bash
totalschema apply -e PRD
```

### Typical Teardown Order

Delete listings before deleting the exchange that contains them:

```yaml
# File: 0006.teardown_analytics_hub.PRD.apply.bigquery.yml

DeleteListing:
  dataExchangeId: company_analytics_exchange
  listingId: shared_analytics_listing

DeleteExchange:
  dataExchangeId: company_analytics_exchange
```

## Important Notes

1. **Non-empty exchanges cannot be deleted**: Delete all listings first using `DeleteListing`.
2. **Irreversible**: Deleting a data exchange does not delete the underlying BigQuery datasets referenced by its (already-removed) listings.
3. **Beta API**: This action uses the Analytics Hub v1beta1 API, which is subject to change.

## Error Handling

Common errors and solutions:

| Error | Cause | Solution |
|-------|-------|----------|
| `Data exchange not found` | The specified `dataExchangeId` doesn't exist | Verify the `dataExchangeId` is correct |
| `Permission denied` | Insufficient IAM permissions | Grant `roles/analyticshub.admin` or `roles/analyticshub.editor` |
| `Data exchange is not empty` | Listings still exist within the exchange | Delete all listings first with `DeleteListing` |

## See Also

- [BigQuery Analytics Hub Documentation](https://cloud.google.com/bigquery/docs/analytics-hub-introduction)
- [TotalSchema CreateExchange Action](../create/README.md)
- [TotalSchema DeleteListing Action](../../listing/delete/README.md)

