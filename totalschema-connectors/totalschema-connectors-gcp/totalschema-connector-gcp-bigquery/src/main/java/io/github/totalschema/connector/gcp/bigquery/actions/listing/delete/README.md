# DeleteListing Action

The `DeleteListing` action deletes a BigQuery Analytics Hub listing from a data exchange.

## Overview

Analytics Hub allows you to share and discover datasets across organizations. A listing represents a dataset that is made available through a data exchange. Deleting a listing removes it from the data exchange; subscribers will lose access to the shared dataset through that listing.

## Prerequisites

1. **Permissions**: The service account or user must have the following IAM role:
   - `roles/analyticshub.admin` or `roles/analyticshub.publisher` on the data exchange

## Action Properties

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `project` | String | No | GCP project ID containing the data exchange. Defaults to `defaults.projectId` from connector configuration. |
| `location` | String | No | Geographic location of the data exchange (e.g., `US`, `EU`, `asia-northeast1`). Defaults to `defaults.location` from connector configuration, falling back to `US`. |
| `dataExchangeId` | String | Yes | ID of the data exchange containing the listing |
| `listingId` | String | Yes | ID of the listing to delete |

## Example Usage

### Basic Example

```yaml
DeleteListing:
  project: my-gcp-project
  location: US
  dataExchangeId: my_exchange
  listingId: sales_listing
```

### Using Connector Defaults

```yaml
DeleteListing:
  dataExchangeId: my_exchange
  listingId: sales_listing
```

### Using with Variables

```yaml
DeleteListing:
  project: ${env.GCP_PROJECT}
  location: ${env.GCP_REGION}
  dataExchangeId: ${dataExchangeName}
  listingId: ${datasetName}_listing
```

## Usage in TotalSchema

### As a Change File

Name your file following TotalSchema conventions:
```
0011.delete_sales_listing.PRD.apply.bigquery.yml
```

The action will be executed when you run:
```bash
totalschema apply -e PRD
```

## Important Notes

1. **Irreversible**: Deleting a listing does not delete the underlying BigQuery dataset, only the shared listing entry in the exchange. Subscribers will lose access.
2. **Beta API**: This action uses the Analytics Hub v1beta1 API, which is subject to change.

## Error Handling

Common errors and solutions:

| Error | Cause | Solution |
|-------|-------|----------|
| `Listing not found` | The specified `listingId` doesn't exist in the data exchange | Verify the `dataExchangeId` and `listingId` are correct |
| `Permission denied` | Insufficient IAM permissions | Grant `roles/analyticshub.publisher` or `roles/analyticshub.admin` |

## See Also

- [BigQuery Analytics Hub Documentation](https://cloud.google.com/bigquery/docs/analytics-hub-introduction)
- [TotalSchema CreateListing Action](../create/README.md)

