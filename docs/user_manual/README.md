# TotalSchema User Manual

TotalSchema is a multi-connector deployment orchestration tool for data engineering. It tracks
which change scripts have been applied to which systems and ensures every environment stays in
sync — across relational databases, cloud-native stores, remote servers, and local shell
environments — all from a single version-controlled workspace.

For the CLI command reference see **[CLI-USAGE.md](CLI-USAGE.md)**.

## Table of Contents

- [Distribution Contents](#distribution-contents)
- [User Libraries](#user-libraries)
- [Quick Start](#quick-start)
- [Workspace Structure](#workspace-structure)
- [Configuration: totalschema.yml](#configuration-totalschemayml)
  - [State Repository](#state-repository)
  - [Validation](#validation)
  - [Locking](#locking)
  - [Variables and Environments](#variables-and-environments)
  - [Connectors](#connectors)
- [Change Script Naming Convention](#change-script-naming-convention)
- [Change Types](#change-types)
- [Connector Types](#connector-types)
  - [JDBC](#jdbc-databases)
  - [SSH Script](#ssh-script)
  - [SSH Commands](#ssh-commands)
  - [Shell](#local-shell)
- [Variable Substitution](#variable-substitution)
- [Secret Management](#secret-management)
- [State Tracking](#state-tracking)
- [Troubleshooting](#troubleshooting)
- [Support](#support)

---

## Distribution Contents

```
totalschema-<version>/
├── bin/
│   ├── totalschema.sh      # Linux/macOS launcher
│   └── totalschema.bat     # Windows launcher
├── lib/                    # TotalSchema JARs and bundled dependencies
└── user_libs/              # Place your JDBC drivers and other libraries here
    └── README.md           # Detailed user_libs documentation
```

---

## User Libraries

TotalSchema cannot bundle JDBC drivers or optional language runtimes due to licensing. Place
them in the `user_libs/` directory next to `bin/`.

| What to add | When needed |
|---|---|
| JDBC driver JAR | Any `jdbc` connector |
| `groovy-all` JAR | Groovy script execution |
| JSR-223 engine JAR | JavaScript, Kotlin Script, JRuby, etc. |

```bash
# Examples
cp postgresql-42.7.3.jar  totalschema-<version>/user_libs/
cp ojdbc11.jar            totalschema-<version>/user_libs/
cp groovy-all-2.4.21.jar  totalschema-<version>/user_libs/
```

Alternatively, point the `TOTALSCHEMA_USER_LIBS` environment variable at a directory or
colon-separated JAR list:

```bash
# Linux/macOS
export TOTALSCHEMA_USER_LIBS="/opt/drivers/postgresql.jar:/opt/drivers/ojdbc11.jar"

# Windows
set TOTALSCHEMA_USER_LIBS=C:\drivers\postgresql.jar;C:\drivers\ojdbc11.jar
```

---

## Quick Start

1. Add your JDBC driver to `user_libs/` (see above).
2. Place your `totalschema.yml` and `changes/` directory in a workspace folder.
3. Run:

```bash
# Linux/macOS
bin/totalschema.sh apply -e DEV -w /path/to/workspace

# Windows
bin\totalschema.bat apply -e DEV -w C:\path\to\workspace
```

If `totalschema.yml` is in the current working directory the `-w` flag can be omitted.

---

## Workspace Structure

A TotalSchema workspace contains exactly two things: a configuration file and a directory of
change scripts.

```
my-workspace/
├── totalschema.yml          # Configuration: connectors, environments, variables
└── changes/                 # Change scripts, organised in subdirectories
    ├── 1.X/
    │   ├── 1.0.0/
    │   │   ├── 0001.create_users.DEV.apply.mydb.sql
    │   │   └── 0001.create_users.DEV.revert.mydb.sql
    │   └── 1.1.0/
    │       └── 0001.add_column.DEV.apply.mydb.sql
    └── 2.X/
        └── 2.0.0/
            └── 0001.migrate_data.apply.mydb.sql
```

Directories are processed in **lexicographic-numeric order** (`1.X` before `2.X`). A directory
named `99` is always processed last within its parent. Files within a directory are processed
by their leading order number.

---

## Configuration: totalschema.yml

The configuration file declares how TotalSchema stores state, which environments exist, and
which target systems (connectors) are available.

```yaml
version: 1

stateRepository:   # Where to record applied changes
  ...

validation:        # How to detect tampered change files
  ...

lock:              # Optional distributed lock
  ...

variables:         # Global variables, available in all environments
  ...

environments:      # Per-environment variable overrides
  DEV:
    variables: ...
  PROD:
    variables: ...

connectors:        # Target systems to deploy to
  mydb:
    type: jdbc
    ...
```

### State Repository

The state repository records which change scripts have been applied. On first run the table
is created automatically.

#### Database (recommended)

```yaml
stateRepository:
  type: database
  database:
    username: ${systemSchema}
    password: ${dbPassword}
    jdbc:
      url: jdbc:postgresql://localhost:5432/mydb
    table:
      catalog: ${systemSchema}
      beforeCreate:
        sql: CREATE SCHEMA IF NOT EXISTS ${systemSchema}
```

The default table name is `totalschema_state`. Column types and the primary-key clause can
be customised under `table.column.*` and `table.primaryKeyClause`.

#### CSV file (single-developer / prototype use only)

```yaml
stateRepository:
  type: csv
  file:
    path:
      pattern: totalschema/state/${environment}/state-${environment}.csv
```

⚠️ CSV state has no concurrent-access protection. Do not use it in shared or CI/CD
environments; use database state instead.

### Validation

```yaml
validation:
  type: contentHash   # Store SHA-256 of each applied file and detect changes
  # type: none        # Disable file-hash validation
```

With `contentHash` enabled, `apply_on_change` scripts re-execute when their content changes,
and the `validate` command can detect modified scripts after deployment.

### Locking

A database lock prevents concurrent TotalSchema executions from interfering with each other.

```yaml
lock:
  type: database
  database:
    jdbc:
      url: jdbc:postgresql://localhost:5432/mydb
    username: ${lockUser}
    password: ${lockPassword}
    table:
      catalog: ts_system
      beforeCreate:
        sql: CREATE SCHEMA IF NOT EXISTS ts_system

# Or disable entirely (single-user / local development)
lock:
  type: none
```

The lock table is separate from the state table and can live in the same or a different
database. Records are cleaned up after execution. To release a stuck lock manually:

```sql
DELETE FROM totalschema_lock WHERE lock_id = 'totalschema_main_lock';
```

### Variables and Environments

```yaml
variables:
  dbPort: 5432
  appName: myapp
  systemSchema: ${appName}_ts_system

environments:
  DEV:
    variables:
      dbHost: localhost
      dbPassword: devpass
      dbName: dev_db
  PROD:
    variables:
      dbHost: prod-db.example.com
      dbPassword: '${secret:!SECRET;1.0;...}'
      dbName: prod_db
```

Environment variables override global variables. System properties (`-DmyVar=value`) override
both. See [Variable Substitution](#variable-substitution) for the full syntax.

### Connectors

Every connector has a **name** (used in change script filenames) and a **type**. See
[Connector Types](#connector-types) for per-type configuration.

```yaml
connectors:
  mydb:          # name referenced in change scripts as *.mydb.sql
    type: jdbc
    ...
  myserver:      # name referenced as *.myserver.sh
    type: ssh-script
    ...
```

---

## Change Script Naming Convention

```
<order>.<description>[.<environment>].<changetype>.<connector>.<extension>
```

| Part | Description | Examples |
|---|---|---|
| `order` | Numeric execution order | `0001`, `0100` |
| `description` | Human-readable label | `create_users`, `add_index` |
| `environment` | Optional — restricts to one environment | `DEV`, `PROD` |
| `changetype` | Execution mode (see below) | `apply`, `revert` |
| `connector` | Name of a connector defined in `totalschema.yml` | `mydb`, `myserver` |
| `extension` | Script language / file type | `sql`, `sh`, `groovy` |

**Examples:**

```
0001.create_users.DEV.apply.mydb.sql          # DEV-only, run once
0002.insert_seed_data.apply.mydb.sql          # all environments, run once
0010.refresh_views.apply_always.mydb.sql      # every run
0020.deploy_proc.apply_on_change.mydb.sql     # re-run when content changes
0001.create_users.DEV.revert.mydb.sql         # rollback for the apply above
0003.configure_server.PROD.apply.myserver.sh  # remote server script, PROD only
```

---

## Change Types

| Type | When it runs | Tracked in state? |
|---|---|---|
| `apply` | Once — skipped if already recorded | ✅ Yes |
| `apply_always` | Every run, unconditionally | ❌ No |
| `apply_on_change` | When file content hash differs from stored hash | ✅ Yes (hash) |
| `revert` | When the `revert` command is run | ✅ Removes record |

**apply** — structural changes that must execute exactly once:

```sql
-- 0001.create_orders.DEV.apply.mydb.sql
CREATE TABLE orders (id INT PRIMARY KEY, total DECIMAL(10,2));
```

**apply_always** — idempotent operations to run on every deployment:

```sql
-- 0010.refresh_summary.apply_always.mydb.sql
REFRESH MATERIALIZED VIEW daily_summary;
```

**apply_on_change** — re-deploy when content changes (procedures, views, config files):

```sql
-- 0020.upsert_procedure.apply_on_change.mydb.sql
CREATE OR REPLACE PROCEDURE calculate_totals() ...
```

**revert** — undo a previous `apply`:

```sql
-- 0001.create_orders.DEV.revert.mydb.sql
DROP TABLE orders;
```

---

## Connector Types

### JDBC Databases

Executes SQL (and optionally Groovy/JSR-223) scripts against any JDBC-compliant database.

```yaml
connectors:
  mydb:
    type: jdbc
    driver:
      class: org.postgresql.Driver        # Optional; many drivers auto-register
    jdbc:
      url: jdbc:postgresql://${dbHost}:${dbPort}/${dbName}
    username: ${dbUser}
    password: ${dbPassword}
    db:
      type: generic                       # generic | h2 | bigquery | …
    scriptExecutors:
      sql:
        variableSubstitution: false       # Set true to expand ${vars} inside .sql files
```

The JDBC driver JAR must be present in `user_libs/`.

### SSH Script

Uploads the script file to the remote host and executes it as a single shell session.
Variables, functions, and `cd` commands persist across lines.

```yaml
connectors:
  myserver:
    type: ssh-script
    host: ${serverHost}
    port: 22
    user: deployuser
    password: ${serverPassword}   # or use key-based auth
    shell: /bin/bash
    remote:
      temp:
        dir: /tmp
```

```bash
# 0001.deploy_app.PROD.apply.myserver.sh
APP_DIR=/opt/myapp
cd $APP_DIR          # persists to the next line
./install.sh
```

### SSH Commands

Executes each non-blank, non-comment line as an **independent** SSH command. There is no
shared shell context between lines.

```yaml
connectors:
  myserver:
    type: ssh-commands
    host: ${serverHost}
    user: deployuser
    password: ${serverPassword}
```

```bash
# 0002.restart_services.PROD.apply.myserver.txt
systemctl restart nginx
systemctl restart myapp
```

> Use `ssh-script` when lines depend on each other. Use `ssh-commands` for independent
> one-liners.

### Local Shell

Executes scripts on the local machine.

```yaml
connectors:
  local:
    type: shell
```

```bash
# 0001.backup.apply.local.sh
pg_dump mydb > backup_$(date +%Y%m%d).sql
```

---

## Variable Substitution

Use `${variableName}` anywhere in `totalschema.yml` values. Variables are resolved in this
priority order (highest wins): system properties → environment variables → environment-level
variables → global variables.

Self-referencing and cross-referencing are supported:

```yaml
variables:
  dbSchema: ${appName}_schema          # references another variable
  jdbcUrl: jdbc:h2:file:${basePath}/db # concatenation
```

**Variable substitution in script files is opt-in.** Enable it per executor:

```yaml
connectors:
  mydb:
    type: jdbc
    scriptExecutors:
      sql:
        variableSubstitution: true   # enables ${var} replacement inside .sql files
```

Do not enable this for `.groovy` scripts — Groovy uses `${...}` natively for GString
interpolation and substituting at the text level would corrupt the script.

---

## Secret Management

Sensitive values (passwords, API keys, private keys) can be stored encrypted in
`totalschema.yml` and decrypted at runtime.

### Encrypting a value

```bash
bin/totalschema.sh secrets encrypt-string \
  --clearTextValue "MySecretPassword" \
  --passwordFile /secure/master.key

# Output:
# !SECRET;1.0;0F0E000000...
```

### Encrypting a file (e.g. SSH private key)

```bash
bin/totalschema.sh secrets encrypt-file \
  --clearTextFile id_rsa \
  --encryptedFile id_rsa.secret \
  --passwordFile /secure/master.key
```

### Using encrypted secrets in configuration

Wrap the `!SECRET;...` token in the `${secret:...}` lookup expression:

```yaml
environments:
  PROD:
    variables:
      dbPassword: '${secret:!SECRET;1.0;0F0E000000...}'
```

Three lookup prefixes are available:

| Prefix | Returns |
|---|---|
| `secret` | Decrypted string value |
| `secretFileContent` | Decrypted file content as a string |
| `decodedFilePath` | Path to a temporary decrypted copy of a file |

```yaml
connectors:
  myserver:
    type: ssh-script
    password: '${secretFileContent:/secure/password.txt.secret}'
```

### Passing the decryption password at runtime

Every command that touches encrypted values requires either `--password` or `--passwordFile`:

```bash
bin/totalschema.sh apply -e PROD --passwordFile /secure/master.key
bin/totalschema.sh validate -e PROD --password "MyMasterKey"
```

---

## State Tracking

TotalSchema maintains a state table that records every applied change script. The table is
created automatically on first run.

Default schema:

```
CHANGE_FILE_ID   — relative path of the script (primary key)
FILE_HASH        — SHA-256 of the script content (when contentHash validation is on)
APPLY_TIMESTAMP  — when it was applied
APPLIED_BY       — user / process that ran the deployment
```

Useful queries:

```sql
-- List all applied changes
SELECT * FROM totalschema_state ORDER BY apply_timestamp;

-- Check whether a specific script has been applied
SELECT * FROM totalschema_state
WHERE change_file_id LIKE '%create_users%';
```

Use the `state display` command to inspect state without querying the database directly:

```bash
bin/totalschema.sh state display -e DEV
bin/totalschema.sh state display -e PROD -f "1.X/*"
```

---

## Troubleshooting

**`bin/totalschema.sh: No such file or directory`**  
Ensure you are inside the distribution directory and the script is executable:
```bash
cd totalschema-<version>
chmod +x bin/totalschema.sh
```

**`No suitable driver found for jdbc:...`**  
The JDBC driver JAR is missing from `user_libs/`. Add it and retry.

**`Permission denied` on the shell script**  
```bash
chmod +x bin/totalschema.sh
```

**`java: command not found`**  
TotalSchema requires Java 11 or later:
```bash
java -version
# Ubuntu/Debian:  sudo apt-get install openjdk-17-jre
# macOS (Homebrew): brew install openjdk@17
```

**Lock not released after a crashed run**  
```sql
DELETE FROM totalschema_lock WHERE lock_id = 'totalschema_main_lock';
```
Only do this when you are certain no TotalSchema process is running.

**`apply_on_change` script not re-executing**  
Ensure `validation.type: contentHash` is set in `totalschema.yml`. Without it, content hashes
are not stored and the `apply_on_change` mode cannot detect changes.

---

## Support

- **Issues:** https://github.com/totalschema/totalschema/issues
- **Source:** https://github.com/totalschema/totalschema
- **License:** GNU Affero General Public License v3.0
