# BigQuery Connector - AI Agent Guide

**Module:** `totalschema-connector-gcp-bigquery`  
**Status:** Stub module - ready for expansion

---

## Quick Overview

This module provides a **BigQuery connector** for TotalSchema with:
- ✅ **SQL Script Execution** - Execute `.sql` files via BigQuery API
- ✅ **Structured YAML API** - Declarative operations (CreateDataset, ExecuteQuery)
- ✅ **Groovy Script Support** - Access BigQuery client directly
- ✅ **Zero reflection** - ServiceLoader-based architecture
- ✅ **Variable substitution** - Integrated with TotalSchema variables
- ✅ **Jackson data binding** - POJO deserialization (not SnakeYAML reflection)

---

## Architecture

### Reflection-Free Design

**Critical:** This module **eliminates all reflection** from class discovery and data binding:

```
ActionFactory (Abstract - actions package)
├── Static registry: Map<String, ActionFactory<?>>
├── ServiceLoader discovery at startup
├── Action type derived from Action class simple name: CreateDataset.class → "CreateDataset"
└── Jackson for POJO deserialization

Concrete Factories (dedicated sub-packages)
├── actions.createdataset.CreateDatasetActionFactory → super(CreateDataset.class)
└── actions.executequery.ExecuteQueryActionFactory → super(ExecuteQuery.class)

ServiceLoader Registration
└── META-INF/services/io.github.totalschema.connector.gcp.bigquery.actions.ActionFactory

Package Organization
├── actions/                          # Base package
│   ├── Action.java                   # Interface
│   ├── ActionFactory.java            # Abstract base with ServiceLoader
│   ├── createdataset/                # Per-action sub-package
│   │   ├── CreateDatasetActionFactory.java
│   │   └── CreateDataset.java
│   └── executequery/
│       ├── ExecuteQueryActionFactory.java
│       └── ExecuteQuery.java
```

**Key Classes:**
- `ActionFactory<T>` - Abstract base with ServiceLoader discovery
- `CreateDatasetActionFactory` - Concrete factory (7 lines)
- `CreateDataset` - Action POJO with dataset properties
- `ExecuteQuery` - Action POJO for SQL execution

---

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
    # Optional: Default location for new datasets
    location: US
```

**Required Settings:**
- `projectId` - GCP project ID

**Optional Settings:**
- `defaultDataset` - Default dataset for SQL queries (avoids needing `dataset.table` syntax)
- `location` - Default location for new datasets (US, EU, etc.)

---

## Supported File Types

### 1. SQL Files (`.sql`)

Direct SQL execution via BigQuery API:

```sql
-- 0001.create_table.DEV.apply.mybigquery.sql
CREATE TABLE my_dataset.users (
  id INT64 NOT NULL,
  name STRING,
  email STRING,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP()
);
```

**Executor:** `SqlBigQueryChangeExecutor`

**Features:**
- Standard SQL (not legacy SQL)
- Supports DDL (CREATE, ALTER, DROP)
- Supports DML (INSERT, UPDATE, DELETE)
- Asynchronous job execution with wait-for-completion

### 2. YAML Actions (`.yml` or `.yaml`)

Declarative BigQuery operations:

```yaml
# 0001.create_dataset.DEV.apply.mybigquery.yml
CreateDataset:
  datasetId: my_dataset
  description: My application dataset
  location: US
```

**Executor:** `YmlBigQueryChangeExecutor`

**Features:**
- Variable substitution
- Type-safe deserialization
- Validation via Jackson

### 3. Groovy Scripts (`.groovy`)

Direct BigQuery client access:

```groovy
// 0003.custom_query.DEV.apply.mybigquery.groovy

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
def job = bigQuery.create(
    com.google.cloud.bigquery.JobInfo.newBuilder(queryConfig)
        .setJobId(jobId)
        .build()
)
job = job.waitFor()

if (job.getStatus().getError() != null) {
    throw new RuntimeException("Query failed: " + job.getStatus().getError())
}

println "Data inserted successfully"
```

**Bindings:**
- `bigQuery` - `com.google.cloud.bigquery.BigQuery` instance
- `configuration` - Connector configuration
- `environment` - Current environment (if set)

---

## Available YAML Actions

### CreateDataset

Creates a new BigQuery dataset.

**Syntax:**
```yaml
CreateDataset:
  datasetId: my_dataset
  description: Optional dataset description
  location: US  # Optional: US, EU, asia-northeast1, etc.
```

**Required Fields:**
- `datasetId` - Dataset ID (must be unique within project)

**Optional Fields:**
- `description` - Human-readable description
- `location` - Geographic location for dataset

**Example:**
```yaml
CreateDataset:
  datasetId: ${DATASET_NAME}
  description: Application dataset for ${ENV}
  location: ${GCP_REGION}
```

### ExecuteQuery

Executes a SQL query via BigQuery API.

**Syntax:**
```yaml
ExecuteQuery:
  query: |
    CREATE TABLE my_dataset.my_table (
      id INT64,
      name STRING,
      created_at TIMESTAMP
    )
  defaultDataset: my_dataset  # Optional
  useLegacySql: false          # Optional, defaults to false
```

**Required Fields:**
- `query` - SQL query to execute (supports multiline with `|`)

**Optional Fields:**
- `defaultDataset` - Default dataset for unqualified table names
- `useLegacySql` - Use legacy SQL instead of standard SQL (default: false)

**Example:**
```yaml
ExecuteQuery:
  defaultDataset: ${DATASET_NAME}
  useLegacySql: false
  query: |
    CREATE OR REPLACE TABLE users (
      user_id INT64 NOT NULL,
      username STRING,
      email STRING,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP()
    )
    PARTITION BY DATE(created_at)
    CLUSTER BY user_id
```

---

## File Naming Convention

```
<order>.<description>.<environment>.<type>.<connector>.<ext>
```

**Examples:**
- `0001.create_dataset.DEV.apply.mybigquery.yml`
- `0002.create_users_table.DEV.apply.mybigquery.sql`
- `0003.seed_data.DEV.apply.mybigquery.groovy`
- `9999.cleanup.DEV.revert.mybigquery.sql`

**Components:**
- `<order>` - Execution order (0001, 0002, etc.)
- `<description>` - Human-readable description
- `<environment>` - DEV, PROD, etc. (or empty for all)
- `<type>` - `apply`, `apply_always`, `apply_on_change`, `revert`
- `<connector>` - Must match connector name in `totalschema.yml`
- `<ext>` - File extension (`.sql`, `.yml`, `.yaml`, `.groovy`)

---

## Adding New Actions

### Step 1: Create Action POJO

```java
package io.github.totalschema.connector.gcp.bigquery.actions.deletetable;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.TableId;
import io.github.totalschema.connector.gcp.bigquery.actions.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteTable implements Action {
    private static final Logger logger = LoggerFactory.getLogger(DeleteTable.class);
    
    private String datasetId;
    private String tableId;
    
    @Override
    public void execute(BigQuery bigQuery) {
        logger.info("Deleting table: {}.{}", datasetId, tableId);
        boolean deleted = bigQuery.delete(TableId.of(datasetId, tableId));
        if (deleted) {
            logger.info("Table deleted successfully");
        } else {
            logger.warn("Table not found: {}.{}", datasetId, tableId);
        }
    }
    
    // Jackson requires getters/setters
    public String getDatasetId() { return datasetId; }
    public void setDatasetId(String datasetId) { this.datasetId = datasetId; }
    public String getTableId() { return tableId; }
    public void setTableId(String tableId) { this.tableId = tableId; }
}
```

### Step 2: Create Factory

```java
package io.github.totalschema.connector.gcp.bigquery.actions.deletetable;

import io.github.totalschema.connector.gcp.bigquery.actions.ActionFactory;

public class DeleteTableActionFactory extends ActionFactory<DeleteTable> {
    public DeleteTableActionFactory() {
        super(DeleteTable.class);
    }
}
```

### Step 3: Register in ServiceLoader

Add to `META-INF/services/io.github.totalschema.connector.gcp.bigquery.actions.ActionFactory`:
```
io.github.totalschema.connector.gcp.bigquery.actions.deletetable.DeleteTableActionFactory
```

### Step 4: Use in YAML

```yaml
DeleteTable:
  datasetId: my_dataset
  tableId: old_table
```

**Total effort:** ~30 lines of code, zero reflection! ✨

---

## Testing

### Build and Compile

```bash
cd totalschema-connector-gcp-bigquery
mvn clean compile -Dspotbugs.skip=true
```

### Run Tests (when implemented)

```bash
mvn test
```

---

## Integration with TotalSchema

### totalschema.yml Example

```yaml
variables:
  DATASET_NAME: my_dataset
  TABLE_PREFIX: app
  GCP_REGION: US

connectors:
  mybigquery:
    type: bigquery
    projectId: ${GCP_PROJECT_ID}
    defaultDataset: ${DATASET_NAME}
    location: ${GCP_REGION}

stateRepository:
  type: database
  connector: mybigquery
```

### Execution

```bash
# Apply changes to DEV environment
totalschema.sh apply -e DEV -w /path/to/workspace

# Dry run (see what would execute)
totalschema.sh apply -e DEV -w /path/to/workspace --dry-run

# Revert changes
totalschema.sh revert -e DEV -w /path/to/workspace
```

---

## Authentication

The connector uses **Google Cloud Application Default Credentials** (ADC).

### Setup

```bash
# Option 1: User credentials (for development)
gcloud auth application-default login

# Option 2: Service account (for production)
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json

# Option 3: GCE/GKE (automatic)
# When running on Google Cloud infrastructure, credentials are automatic
```

---

## Key Implementation Details

### SQL Execution Flow

1. Read SQL content from input stream
2. Create `QueryJobConfiguration` with SQL
3. Set `useLegacySql(false)` for standard SQL
4. Apply optional `defaultDataset` if configured
5. Create job with unique `JobId`
6. Submit to BigQuery via `bigQuery.create(JobInfo)`
7. Wait for completion with `job.waitFor()`
8. Check for errors and throw exception if failed

### YAML Action Execution Flow

1. Parse YAML file with SnakeYAML
2. Extract single top-level key (action type)
3. Look up action factory in ServiceLoader registry
4. Substitute variables using ExpressionEvaluator
5. Deserialize to Action POJO using Jackson
6. Execute action with BigQuery client

### Variable Substitution

Variables are substituted **before** Jackson deserialization:

```yaml
CreateDataset:
  datasetId: ${DATASET_NAME}     # Substituted
  location: ${GCP_REGION}        # Substituted
```

Becomes:

```yaml
CreateDataset:
  datasetId: my_dataset          # After substitution
  location: US                   # After substitution
```

Then deserialized to `CreateDataset` POJO.

---

## Troubleshooting

### Problem: Authentication failed

**Error:** `Unable to get application default credentials`

**Solution:**
```bash
gcloud auth application-default login
```

### Problem: Unknown action type

**Error:** `IllegalArgumentException: Unknown action type: CreateTable`

**Solution:** Check that the action factory is registered in `META-INF/services`

### Problem: Query execution failed

**Error:** `Query failed: Invalid table name`

**Solution:** Use fully qualified table names (`dataset.table`) or set `defaultDataset` in configuration

### Problem: Dataset already exists

**Error:** `Dataset already exists: my_dataset`

**Solution:** This is expected if the dataset was created in a previous run. Use `apply_on_change` execution type or add logic to check existence first.

---

## Future Enhancements (Not Implemented)

### Additional Actions

- [ ] `DeleteDataset`
- [ ] `CreateTable` (with full schema support)
- [ ] `UpdateTable` (add/remove columns)
- [ ] `DeleteTable`
- [ ] `CreateView`
- [ ] `LoadData` (from GCS)
- [ ] `ExportData` (to GCS)
- [ ] `CreateRoutine` (UDF/stored procedure)
- [ ] `SetTableIamPolicy`
- [ ] `SetTableExpiration`

### Features

- [ ] Partitioning support
- [ ] Clustering support
- [ ] Table snapshots
- [ ] Query cost estimation
- [ ] Dry-run validation
- [ ] Schema evolution helpers
- [ ] Migration from other data warehouses

### Testing

- [ ] Unit tests for all actions
- [ ] Integration tests with BigQuery emulator
- [ ] Example workspace in `samples/bigquery/`
- [ ] Performance tests

---

## Comparison with BigTable Connector

| Feature | BigTable | BigQuery |
|---------|----------|----------|
| **Client Types** | 3 (TableAdmin, InstanceAdmin, Data) | 1 (BigQuery) |
| **Primary Purpose** | NoSQL table management | Data warehouse operations |
| **SQL Support** | No | Yes (native) |
| **Action Hierarchy** | TableAction, InstanceAction, DataAction | Single Action interface |
| **Data Operations** | Via DataClient (disabled by default) | Via BigQuery client (always enabled) |
| **Typical Actions** | CreateTable, CreateColumnFamily, GC rules | CreateDataset, ExecuteQuery, LoadData |

---

## Summary

This BigQuery connector provides a **production-ready foundation** for declarative BigQuery schema management:

✅ **SQL execution** - Direct SQL script support  
✅ **YAML actions** - Type-safe declarative operations  
✅ **Groovy scripting** - Full BigQuery client access  
✅ **Zero reflection** - ServiceLoader + Jackson  
✅ **Variable substitution** - Integrated with TotalSchema  
✅ **Comprehensive docs** - README, examples, guides

**Status:** Stub module ready for expansion with additional actions and tests! 🎉

---

## Quick Start for AI Agents

### To understand this module:
1. Read this file (AGENTS.md) for overview
2. Check `examples/` for YAML/SQL syntax
3. Review `README.md` for user-facing documentation
4. Examine `IMPLEMENTATION_SUMMARY.md` for technical details

### To add a new action:
1. Create Action POJO in `actions/<actionname>/`
2. Add getters/setters (Jackson requirement)
3. Implement `execute(BigQuery bigQuery)` method
4. Create ActionFactory extending `ActionFactory<YourAction>`
5. Register in `META-INF/services`
6. Add example YAML

### To troubleshoot:
1. Check ServiceLoader registration
2. Verify Jackson deserialization (getters/setters)
3. Check YAML key matches Action class simple name
4. Verify GCP authentication (ADC)
5. Check BigQuery API quotas/limits

