# CreateSubscription Action

The `CreateSubscription` action subscribes to a BigQuery Analytics Hub listing, creating a linked ("destination") BigQuery dataset in the subscriber's project that mirrors the publisher's shared data live, without copying it.

## Overview

Subscribing is the consumer-side counterpart to [CreateListing](../../listing/create/README.md). It creates a linked dataset in the subscriber's own GCP project that behaves like a normal read-only BigQuery dataset — you can query it with standard SQL and join it with your own tables — but it always reflects the publisher's live, up-to-date data. No ETL or data copy jobs are involved.

## Prerequisites

1. **Existing Listing**: The listing being subscribed to must already exist (see [CreateListing](../../listing/create/README.md)).
2. **Permissions**:
   - `roles/analyticshub.subscriber` (or equivalent) on the listing/data exchange being subscribed to
   - `roles/bigquery.user` or higher on the destination project, to create the linked dataset

## Action Properties

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `project` | String | No | GCP project ID containing the listing to subscribe to. Defaults to `defaults.projectId` from connector configuration. |
| `location` | String | No | Geographic location of the listing to subscribe to (e.g., `US`, `EU`). Defaults to `defaults.location` from connector configuration, falling back to `US`. |
| `dataExchangeId` | String | Yes | ID of the data exchange containing the listing |
| `listingId` | String | Yes | ID of the listing to subscribe to |
| `destinationDataset` | Object | No | Configuration for the linked dataset created in the subscriber's project (see structure below) |

### Destination Dataset Structure

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `project` | String | No | Project in which to create the linked dataset (defaults to the action's/connector's project) |
| `datasetId` | String | Yes (if `destinationDataset` is specified) | Dataset ID to create for the subscriber |
| `location` | String | No | Location for the linked dataset (defaults to the action's/connector's location) |
| `friendlyName` | String | No | Human-readable name for the linked dataset |
| `description` | String | No | Description for the linked dataset |

## Example Usage

### Basic Example

```yaml
CreateSubscription:
  project: publisher-project
  location: US
  dataExchangeId: my_exchange
  listingId: sales_listing
  destinationDataset:
    project: subscriber-project
    datasetId: subscribed_sales_data
```

### Complete Example with All Properties

```yaml
CreateSubscription:
  project: ${PUBLISHER_PROJECT_ID}
  location: US
  dataExchangeId: analytics_exchange
  listingId: customer_insights_listing
  destinationDataset:
    project: ${SUBSCRIBER_PROJECT_ID}
    datasetId: customer_insights_linked
    location: US
    friendlyName: Customer Insights (Linked)
    description: Live-linked subscription to the customer insights listing
```

## Usage in TotalSchema

### As a Change File

Name your file following TotalSchema conventions:
```
0011.subscribe_to_sales_listing.PRD.apply.bigquery.yml
```

The action will be executed when you run:
```bash
totalschema apply -e PRD
```

## Important Notes

1. **Listing Must Exist**: The listing specified by `dataExchangeId`/`listingId` must already exist.
2. **No Data Movement**: The linked dataset is a live pointer to the publisher's data - no copying/ETL occurs.
3. **Beta API**: This action uses the Analytics Hub v1beta1 API, which is subject to change.
4. **No Dedicated Subscription Resource**: The Analytics Hub v1beta1 API does not expose a first-class "Subscription" resource with its own lifecycle RPCs in this client version - subscribing simply creates the linked dataset described above. See [DeleteSubscription](../delete/README.md) for how this affects deletion.

## Error Handling

Common errors and solutions:

| Error | Cause | Solution |
|-------|-------|----------|
| `Listing not found` | The specified `dataExchangeId`/`listingId` doesn't exist | Verify the listing identifiers are correct |
| `Permission denied` | Insufficient IAM permissions | Grant `roles/analyticshub.subscriber` and `roles/bigquery.user` |
| `Dataset already exists` | A dataset with the same ID already exists in the destination project | Use a different `datasetId` or remove the existing dataset |

## See Also

- [BigQuery Analytics Hub Documentation](https://cloud.google.com/bigquery/docs/analytics-hub-introduction)
- [TotalSchema DeleteSubscription Action](../delete/README.md)
- [TotalSchema CreateListing Action](../../listing/create/README.md)

