# TotalSchema BigQuery Sample Project

This directory contains a sample project for TotalSchema with a Google BigQuery connector.

The sample demonstrates five BigQuery connector features:
1. **SQL DDL** - Creating a dataset and table
2. **ExecuteStatement YAML** - Inserting sample data using YAML configuration
3. **CreateScheduledQuery YAML** - Setting up a scheduled query for daily analytics
4. **CreateListing YAML** - Publishing the dataset as an Analytics Hub listing
5. **CreateSubscription YAML** - Subscribing to the listing from a separate GCP project

The sample uses a local H2 database for TotalSchema state and lock tables, and applies schema changes to BigQuery.

## Change Files

**totalschema/changes** contains example change files organized by version:

- `01.create_dataset.apply/revert.bq.sql` - Create/drop the BigQuery dataset (SQL)
- `02.create_contacts_table.apply/revert.bq.sql` - Create/drop a simple phonebook table (SQL)
- `03.insert_sample_records.DEV.apply/revert.bq.yml` - Insert 10 sample records (ExecuteStatement YAML)
- `04.create_daily_contact_summary.apply.bq.yml` - Create a daily scheduled query (CreateScheduledQuery YAML)
- `05.create_analytics_exchange.apply/revert.bq.yml` - Create the Analytics Hub data exchange
- `06.create_phone_book_listing.apply/revert.bq.yml` - Publish the phone book dataset as an Analytics Hub listing (CreateListing YAML)
- `07.subscribe_to_phone_book_listing.apply/revert.bq.yml` - Subscribe to the listing from another
  project, creating a linked dataset there (CreateSubscription YAML)

**totalschema.yml** contains connector and environment configuration.

## Prerequisites

- BigQuery connector available in your TotalSchema installation
- H2 JDBC driver available in your TotalSchema installation
- Google Cloud authentication configured. Either:
  - `gcloud auth application-default login` (user credentials), or
  - a service account JSON key file: `export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json`
    (the BigQuery connector picks this up automatically via Application Default Credentials)
- A GCP project where your account can create datasets and tables (`gcpProjectId`)
- A **second, separate** GCP project where your account can create linked/subscribed datasets
  (`gcpSubscriberProjectId`). Analytics Hub does not allow subscribing to a listing from within
  the same project that published it, so this must differ from `gcpProjectId`.
- An existing Analytics Hub **data exchange** (referenced by the `bqDataExchangeId` variable in
  `totalschema.yml`). Data exchanges cannot be created by TotalSchema and must be created manually
  beforehand via the GCP Console or `gcloud` CLI.

## Running the Sample

Update `gcpProjectId` and `gcpSubscriberProjectId` in `totalschema.yml` to your GCP project IDs, then run:

```bash
totalschema apply -e DEV --password FOOBAR
```

This will:
1. Create a dataset `totalschema_phonebook`
2. Create a `phone_book` table with 10 sample records
3. Set up a daily scheduled query that counts contacts
4. Publish the `phone_book` dataset as an Analytics Hub listing named `phone_book_listing`
5. Subscribe to that listing from `gcpSubscriberProjectId`, creating a linked dataset
   `phone_book_subscription` there


To view the state of applied changes:

```bash
totalschema state display -e DEV
```

To revert changes:

```bash
totalschema revert -e DEV --password FOOBAR
```

Note: scheduled query deletion requires manual GCP intervention. The Analytics Hub listing
also has no automated revert - the BigQuery connector does not currently provide a
`DeleteListing` action, so it must be deleted manually, e.g. via the GCP Console or:

```bash
gcloud alpha bigquery data-exchanges listings delete phone_book_listing \
  --data-exchange=<bqDataExchangeId> --location=<gcpLocation> --project=<gcpProjectId>
```



