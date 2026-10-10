# DeleteSubscription Action

The `DeleteSubscription` action deletes a BigQuery Analytics Hub subscription by dropping the linked ("destination") BigQuery dataset that was created for the subscriber.

## Overview

**Important:** The Analytics Hub v1beta1 API (used by [CreateSubscription](../create/README.md)) does not expose a dedicated "delete subscription" RPC in this client library version - it only exposes `subscribeListing` to create a subscription. A subscription's only tangible artifact is the linked BigQuery dataset created in the subscriber's project, so "deleting a subscription" means deleting that linked dataset. Consequently, unlike the other Analytics Hub actions in this connector, `DeleteSubscription` executes against the core BigQuery client rather than the Analytics Hub client.

## Prerequisites

1. **Permissions**: The service account or user must have the following IAM role:
   - `roles/bigquery.dataOwner` or `roles/bigquery.admin` on the linked dataset's project

## Action Properties

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `project` | String | No | Project ID containing the linked dataset. Defaults to the connector's own project if not specified. |
| `datasetId` | String | Yes | ID of the linked dataset (created by `CreateSubscription`) to delete |
| `deleteContents` | Boolean | No | If `true`, deletes all tables/views within the linked dataset before dropping it. Defaults to `false` (BigQuery will refuse to delete a non-empty dataset). |

## Example Usage

### Basic Example

```yaml
DeleteSubscription:
  datasetId: subscribed_sales_data
```

### Cross-Project Example

```yaml
DeleteSubscription:
  project: subscriber-project
  datasetId: customer_insights_linked
  deleteContents: true
```

## Usage in TotalSchema

### As a Change File

Name your file following TotalSchema conventions:
```
0012.unsubscribe_from_sales_listing.PRD.apply.bigquery.yml
```

The action will be executed when you run:
```bash
totalschema apply -e PRD
```

## Important Notes

1. **Deletes the Linked Dataset**: This action deletes the destination dataset created by `CreateSubscription`; it does not affect the original publisher's source dataset or the listing itself.
2. **Non-empty Datasets**: By default, BigQuery refuses to delete a dataset containing tables or views. Set `deleteContents: true` to force deletion.
3. **Irreversible**: Once deleted, the subscriber will need to re-subscribe (via `CreateSubscription`) to regain access to the shared data.

## Error Handling

Common errors and solutions:

| Error | Cause | Solution |
|-------|-------|----------|
| `Analytics Hub subscription (linked dataset) not found` | The specified `datasetId` doesn't exist | Verify the `datasetId` (and `project`, if cross-project) is correct |
| `Permission denied` | Insufficient IAM permissions | Grant `roles/bigquery.dataOwner` or `roles/bigquery.admin` |
| `Dataset ... still contains ...` | The dataset contains tables/views | Set `deleteContents: true` |

## See Also

- [BigQuery Analytics Hub Documentation](https://cloud.google.com/bigquery/docs/analytics-hub-introduction)
- [TotalSchema CreateSubscription Action](../create/README.md)

