# TotalSchema BigQuery Basic Sample

This is a minimal sample project demonstrating TotalSchema with Google BigQuery. It's designed to help you get started with the absolute basics of schema management.

The sample demonstrates:
- **SQL DDL** - Creating a simple table in BigQuery

## What's Included

**totalschema/changes** contains a single minimal change file:

- `1.X/1.0.0/01.create_phone_book_table.apply/revert.bq.sql` - Create/drop a simple phonebook table (SQL)

**totalschema.yml** contains the connector and environment configuration.

## Environments

Environments allow you to manage schema changes across different deployment targets (dev, qa, prod) with environment-specific configurations. Each environment can have its own GCP project and other variables.

This sample defines three environments in `totalschema.yml`:
- **dev** - Development environment (defaults to `sample-project-dev`)
- **qa** - Quality assurance environment (defaults to `sample-project-qa`)
- **prod** - Production environment (defaults to `sample-project-prod`)

When you run TotalSchema commands, you specify which environment to target using the `--environment` flag.

## Prerequisites

- BigQuery connector available in your TotalSchema installation
- Google Cloud authentication configured. Either:
  - `gcloud auth application-default login` (user credentials), or
  - a service account JSON key file: `export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json`
    (the BigQuery connector picks this up automatically via Application Default Credentials)
- A GCP project where your account can create tables

## Running the Sample

Update the `dev` environment's `gcpProjectId` in `totalschema.yml` to your actual GCP project ID, then run:

```bash
totalschema apply --environment=dev
```

This will create a `phone_book` table in BigQuery under the dev environment.

To view the state of applied changes:

```bash
totalschema state display --environment=dev
```

To revert changes:

```bash
totalschema revert --environment=dev
```

## Next Steps

For more advanced features like scheduled queries and Analytics Hub integration, see the **gcp-bigquery-complex** sample.



