# BigQuery Connector for TotalSchema

This module provides a BigQuery connector for TotalSchema, enabling declarative schema management and query execution for Google Cloud BigQuery.

## Features

- ✅ **SQL Script Execution** - Execute SQL files via BigQuery API
- ✅ **Declarative YAML Actions** - Define BigQuery operations in YAML
- ✅ **Groovy Script Support** - Write scripts with `bigQuery` client binding
- ✅ **Variable Substitution** - Use TotalSchema variables in configurations
- ✅ **Zero Reflection** - ServiceLoader-based architecture
- ✅ **Standard SQL** - Uses BigQuery Standard SQL by default

## Configuration

### Basic Configuration

```yaml
connectors:
  mybigquery:
    type: bigquery
    projectId: my-gcp-project
```

### With Optional Settings

```yaml
connectors:
  mybigquery:
    type: bigquery
    projectId: my-gcp-project
    # Optional: Default dataset for queries
    defaultDataset: my_dataset
    # Optional: Default location for datasets
    location: US
```

## Supported File Types

### 1. SQL Files (`.sql`)

Execute SQL queries directly via BigQuery API:

```sql
-- 0001.create_table.DEV.apply.mybigquery.sql
CREATE TABLE my_dataset.users (
  id INT64,
  name STRING,
  email STRING
);
```

### 2. YAML Actions (`.yml` or `.yaml`)

Define declarative BigQuery operations:

#### Create Dataset

```yaml
# 0001.create_dataset.DEV.apply.mybigquery.yml
CreateDataset:
  datasetId: my_dataset
  description: My application dataset
  location: US
```

#### Execute Query

```yaml
# 0002.create_table.DEV.apply.mybigquery.yml
ExecuteQuery:
  defaultDataset: my_dataset
  query: |
    CREATE TABLE users (
      id INT64,
      name STRING,
      email STRING
    )
```

### 3. Groovy Scripts (`.groovy`)

Access the BigQuery client directly:

```groovy
// 0003.populate_data.DEV.apply.mybigquery.groovy

// bigQuery client is available as a binding
def query = """
  INSERT INTO my_dataset.users (id, name, email)
  VALUES (1, 'John Doe', 'john@example.com')
"""

def queryConfig = com.google.cloud.bigquery.QueryJobConfiguration
    .newBuilder(query)
    .setUseLegacySql(false)
    .build()

def jobId = com.google.cloud.bigquery.JobId.of(UUID.randomUUID().toString())
def job = bigQuery.create(com.google.cloud.bigquery.JobInfo.newBuilder(queryConfig).setJobId(jobId).build())
job = job.waitFor()

if (job.getStatus().getError() != null) {
    throw new RuntimeException("Query failed: " + job.getStatus().getError())
}

println "Data inserted successfully"
```

## Available YAML Actions

### CreateDataset

Creates a new BigQuery dataset.

```yaml
CreateDataset:
  datasetId: my_dataset
  description: Optional description
  location: US  # Optional: US, EU, etc.
```

### ExecuteQuery

Executes a SQL query.

```yaml
ExecuteQuery:
  query: |
    CREATE TABLE my_table (
      id INT64,
      name STRING
    )
  defaultDataset: my_dataset  # Optional
  useLegacySql: false  # Optional, defaults to false
```

## File Naming Convention

```
<order>.<description>.<environment>.<type>.<connector>.<ext>
```

**Examples:**
- `0001.create_dataset.DEV.apply.mybigquery.yml`
- `0002.create_users_table.DEV.apply.mybigquery.sql`
- `0003.seed_data.DEV.apply.mybigquery.groovy`

## Usage Examples

### Example 1: Basic Schema Setup

```bash
# Project structure
totalschema/
  0001.create_dataset.DEV.apply.mybigquery.yml
  0002.create_users_table.DEV.apply.mybigquery.sql
  0003.create_orders_table.DEV.apply.mybigquery.sql
```

**0001.create_dataset.DEV.apply.mybigquery.yml:**
```yaml
CreateDataset:
  datasetId: ecommerce
  description: E-commerce application dataset
  location: US
```

**0002.create_users_table.DEV.apply.mybigquery.sql:**
```sql
CREATE TABLE ecommerce.users (
  user_id INT64,
  username STRING,
  email STRING,
  created_at TIMESTAMP
);
```

**0003.create_orders_table.DEV.apply.mybigquery.sql:**
```sql
CREATE TABLE ecommerce.orders (
  order_id INT64,
  user_id INT64,
  total_amount NUMERIC,
  order_date DATE
);
```

### Example 2: Using Variables

**totalschema.yml:**
```yaml
variables:
  DATASET_NAME: my_dataset
  TABLE_PREFIX: app

connectors:
  mybigquery:
    type: bigquery
    projectId: my-gcp-project
    defaultDataset: ${DATASET_NAME}
```

**0001.create_table.yml:**
```yaml
ExecuteQuery:
  query: |
    CREATE TABLE ${DATASET_NAME}.${TABLE_PREFIX}_users (
      id INT64,
      name STRING
    )
```

## Execution

```bash
# Apply changes to DEV environment
totalschema.sh apply -e DEV -w /path/to/workspace

# Dry run (see what would execute)
totalschema.sh apply -e DEV -w /path/to/workspace --dry-run
```

## Architecture

### Module Structure

```
totalschema-connector-gcp-bigquery/
├── src/main/java/io/github/totalschema/connector/gcp/bigquery/
│   ├── BigQueryConnector.java                 # Main connector
│   ├── BigQueryConnectorComponentFactory.java  # ServiceLoader registration
│   ├── BigQueryClientComponentFactory.java     # BigQuery client factory
│   ├── BigQueryConnectorException.java        # Custom exception
│   ├── YmlBigQueryChangeExecutor.java         # YAML executor
│   ├── SqlBigQueryChangeExecutor.java         # SQL executor
│   └── actions/
│       ├── Action.java                        # Action interface
│       ├── ActionFactory.java                 # Abstract factory
│       ├── createdataset/
│       │   ├── CreateDataset.java             # Action POJO
│       │   └── CreateDatasetActionFactory.java # Factory
│       └── executequery/
│           ├── ExecuteQuery.java              # Action POJO
│           └── ExecuteQueryActionFactory.java  # Factory
├── src/main/resources/META-INF/services/
│   ├── io.github.totalschema.spi.factory.ComponentFactory
│   └── io.github.totalschema.connector.gcp.bigquery.actions.ActionFactory
└── pom.xml
```

### Design Principles

1. **Zero Reflection** - Uses ServiceLoader for discovery instead of reflection
2. **Jackson Data Binding** - YAML deserialization via Jackson (not SnakeYAML reflection)
3. **Action Pattern** - Each operation is a discrete action class
4. **Variable Substitution** - Integrates with TotalSchema's variable system

## Adding Custom Actions

### Step 1: Create Action POJO

```java
package io.github.totalschema.connector.gcp.bigquery.actions.deletedata;

import com.google.cloud.bigquery.BigQuery;
import io.github.totalschema.connector.gcp.bigquery.actions.Action;

public class DeleteDataset implements Action {
    private String datasetId;
    
    @Override
    public void execute(BigQuery bigQuery) {
        bigQuery.delete(datasetId);
    }
    
    // Jackson requires getters/setters
    public String getDatasetId() { return datasetId; }
    public void setDatasetId(String datasetId) { this.datasetId = datasetId; }
}
```

### Step 2: Create Factory

```java
package io.github.totalschema.connector.gcp.bigquery.actions.deletedataset;

import io.github.totalschema.connector.gcp.bigquery.actions.ActionFactory;

public class DeleteDatasetActionFactory extends ActionFactory<DeleteDataset> {
    public DeleteDatasetActionFactory() {
        super(DeleteDataset.class);
    }
}
```

### Step 3: Register in ServiceLoader

Add to `META-INF/services/io.github.totalschema.connector.gcp.bigquery.actions.ActionFactory`:
```
io.github.totalschema.connector.gcp.bigquery.actions.deletedataset.DeleteDatasetActionFactory
```

### Step 4: Use in YAML

```yaml
DeleteDataset:
  datasetId: old_dataset
```

## Dependencies

### Required at Runtime
- `com.google.cloud:google-cloud-bigquery` (via BOM)
- `com.fasterxml.jackson.core:jackson-databind:2.22.1`

### Test Dependencies
- `org.testng:testng`
- `org.easymock:easymock`

## Authentication

The connector uses the default Google Cloud authentication mechanism. Ensure your environment has appropriate credentials:

```bash
# Option 1: Application Default Credentials
gcloud auth application-default login

# Option 2: Service Account
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json
```

## Troubleshooting

### Problem: Authentication Failed

**Error:** `Unable to get application default credentials`

**Solution:** Configure authentication:
```bash
gcloud auth application-default login
```

### Problem: Unknown action type

**Error:** `IllegalArgumentException: Unknown action type: CreateTable`

**Solution:** Check that the action factory is registered in `META-INF/services`

### Problem: Query execution failed

**Error:** `Query failed: Invalid table name`

**Solution:** Ensure the table name includes dataset: `dataset.table` or set `defaultDataset`

## Status

This is a **stub module** ready for expansion. Current features:
- ✅ Basic connector infrastructure
- ✅ SQL execution support
- ✅ YAML action framework
- ✅ Groovy script support
- ✅ Two example actions (CreateDataset, ExecuteQuery)

**Next Steps (to be implemented):**
- [ ] Additional actions (CreateTable, DeleteDataset, etc.)
- [ ] Comprehensive tests
- [ ] Example workspace
- [ ] Detailed documentation

