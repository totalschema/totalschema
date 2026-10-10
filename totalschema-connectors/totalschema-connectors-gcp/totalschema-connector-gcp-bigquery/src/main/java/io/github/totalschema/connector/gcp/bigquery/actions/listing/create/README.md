# CreateListing Action

The `CreateListing` action creates a BigQuery Analytics Hub listing to share datasets with other Google Cloud users through a data exchange.

## Overview

Analytics Hub allows you to share and discover datasets across organizations. A listing represents a dataset that is made available through a data exchange. Users can subscribe to listings to access shared data.

## Prerequisites

1. **Data Exchange**: You must have an existing Analytics Hub data exchange where the listing will be created.
2. **Permissions**: The service account or user must have the following IAM roles:
   - `roles/analyticshub.admin` or `roles/analyticshub.publisher` on the data exchange
   - `roles/bigquery.dataOwner` or `roles/bigquery.admin` on the source dataset

## Action Properties

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `project` | String | Yes | GCP project ID containing the data exchange |
| `location` | String | Yes | Geographic location of the data exchange (e.g., `US`, `EU`, `asia-northeast1`) |
| `dataExchangeId` | String | Yes | ID of the existing data exchange where the listing will be created |
| `listingId` | String | Yes | Unique identifier for the listing |
| `displayName` | String | No | Human-readable name shown in Analytics Hub (defaults to `listingId` if not provided) |
| `description` | String | No | Detailed description of the dataset being shared |
| `primaryContact` | String | No | Email address of the primary contact for questions about the dataset |
| `documentation` | String | No | URL to documentation about the dataset |
| `categories` | List<String> | No | Categories for dataset discovery (see available categories below) |
| `sourceDataset` | Object | Yes | The BigQuery dataset to share (see structure below) |

### Source Dataset Structure

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `project` | String | No | Project ID containing the dataset (defaults to the listing project if not specified) |
| `dataset` | String | Yes | Dataset ID to share |

### Available Categories

Use these category values to help users discover your listing:

- `CATEGORY_OTHERS` - General category
- `CATEGORY_ADVERTISING_AND_MARKETING` - Marketing and advertising data
- `CATEGORY_COMMERCE` - E-commerce and transaction data
- `CATEGORY_CLIMATE_AND_ENVIRONMENT` - Environmental and climate data
- `CATEGORY_DEMOGRAPHICS` - Population and demographic data
- `CATEGORY_ECONOMICS` - Economic indicators and financial metrics
- `CATEGORY_EDUCATION` - Educational data and statistics
- `CATEGORY_ENERGY` - Energy production and consumption data
- `CATEGORY_FINANCIAL` - Financial services data
- `CATEGORY_GAMING` - Gaming and entertainment data
- `CATEGORY_GEOSPATIAL` - Geographic and location data
- `CATEGORY_HEALTHCARE_AND_LIFE_SCIENCE` - Healthcare and life sciences data
- `CATEGORY_MEDIA` - Media and content data
- `CATEGORY_PUBLIC_SECTOR` - Government and public sector data
- `CATEGORY_RETAIL` - Retail and consumer data
- `CATEGORY_SPORTS` - Sports and athletics data
- `CATEGORY_SCIENCE_AND_RESEARCH` - Scientific research data
- `CATEGORY_TRANSPORTATION_AND_LOGISTICS` - Transportation and logistics data
- `CATEGORY_TRAVEL_AND_TOURISM` - Travel and tourism data

## Example Usage

### Basic Example

```yaml
CreateListing:
  project: my-gcp-project
  location: US
  dataExchangeId: my_exchange
  listingId: sales_listing
  displayName: Monthly Sales Data
  sourceDataset:
    dataset: sales_data
```

### Complete Example with All Properties

```yaml
CreateListing:
  project: ${GCP_PROJECT_ID}
  location: US
  dataExchangeId: analytics_exchange
  listingId: customer_insights_listing
  displayName: Customer Insights Dataset
  description: |
    Comprehensive customer analytics dataset including:
    - Purchase history
    - Customer demographics
    - Behavior patterns
    Updated daily at 2:00 AM UTC
  primaryContact: data-team@example.com
  documentation: https://docs.example.com/datasets/customer-insights
  categories:
    - CATEGORY_RETAIL
    - CATEGORY_DEMOGRAPHICS
  sourceDataset:
    project: analytics-prod-project
    dataset: customer_insights
```

### Using with Variables

```yaml
CreateListing:
  project: ${env.GCP_PROJECT}
  location: ${env.GCP_REGION}
  dataExchangeId: ${dataExchangeName}
  listingId: ${datasetName}_listing
  displayName: ${datasetDisplayName}
  description: ${datasetDescription}
  primaryContact: ${contactEmail}
  documentation: ${docsUrl}
  categories:
    - ${primaryCategory}
  sourceDataset:
    project: ${sourceProject}
    dataset: ${sourceDataset}
```

## Usage in TotalSchema

### As a Change File

Name your file following TotalSchema conventions:
```
0010.create_sales_listing.PRD.apply.bigquery.yml
```

The action will be executed when you run:
```bash
totalschema apply -e PRD
```

### In a Deployment Script

Include the action in a larger deployment YAML file alongside other BigQuery actions:

```yaml
# File: 0020.setup_analytics_hub.PRD.apply.bigquery.yml

CreateDataset:
  datasetId: shared_analytics
  description: Shared analytics dataset
  location: US

CreateListing:
  project: ${GCP_PROJECT}
  location: US
  dataExchangeId: company_analytics_exchange
  listingId: shared_analytics_listing
  displayName: Shared Analytics Dataset
  description: Company-wide analytics data for reporting
  primaryContact: analytics-team@example.com
  sourceDataset:
    dataset: shared_analytics
```

## Important Notes

1. **Data Exchange Must Exist**: The data exchange specified in `dataExchangeId` must be created before creating a listing. Analytics Hub data exchanges are created through the GCP Console or `gcloud` CLI, not through TotalSchema.

2. **Dataset Permissions**: The listing shares the dataset, but subscribers will only see data they have permissions to access based on BigQuery IAM policies.

3. **Listing Updates**: To update an existing listing, you'll need to delete and recreate it or use the Analytics Hub Console/API directly.

4. **Billing**: Listing data in Analytics Hub is free, but subscribers may incur query costs when accessing the data.

5. **Beta API**: This action uses the Analytics Hub v1beta1 API, which is subject to change.

## Error Handling

Common errors and solutions:

| Error | Cause | Solution |
|-------|-------|----------|
| `Data exchange not found` | The specified `dataExchangeId` doesn't exist | Create the data exchange first using GCP Console or `gcloud` CLI |
| `Permission denied` | Insufficient IAM permissions | Grant `roles/analyticshub.publisher` and `roles/bigquery.dataOwner` |
| `Dataset not found` | The source dataset doesn't exist | Create the BigQuery dataset before creating the listing |
| `Listing already exists` | A listing with the same ID already exists | Use a different `listingId` or delete the existing listing |

## See Also

- [BigQuery Analytics Hub Documentation](https://cloud.google.com/bigquery/docs/analytics-hub-introduction)
- [Data Exchange Documentation](https://cloud.google.com/bigquery/docs/analytics-hub-exchange)
- [Analytics Hub IAM Roles](https://cloud.google.com/bigquery/docs/analytics-hub-iam)
- [TotalSchema CreateDataset Action](../createdataset/CreateDataset.java)

