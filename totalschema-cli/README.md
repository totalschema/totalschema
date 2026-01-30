# totalschema-cli

This module contains the command-line interface (CLI) for the totalschema tool.

## Overview

The `totalschema-cli` module provides all the command-line commands and user interaction components for the totalschema database versioning tool. It depends on `totalschema-core` for the core engine functionality and uses PicoCLI for command-line parsing.

## Structure

- `io.github.totalschema.cli` - Main CLI package containing:
  - `Main` - Entry point for the CLI application
  - `execute/` - Execution-related commands (apply, revert)
  - `environment/` - Environment management commands
  - `variables/` - Variable management commands
  - `state/` - State management commands
  - `secret/` - Secret management commands
  - `show/` - Display commands for changes and state

## Building

To build the CLI module:

```bash
mvn clean package -pl totalschema-cli -am
```

This will create an executable shaded JAR at:
```
target/totalschema-cli-1.0-SNAPSHOT-shaded.jar
```

## Running

### Using the Distribution Package

After downloading the distribution, extract its content:

```bash
tar -xzf target/totalschema-1.0-SNAPSHOT.tar.gz
cd totalschema-1.0-SNAPSHOT
bin/totalschema.sh [COMMAND]
```

On Windows:
Extract with any ZIP utility

``
cd totalschema-1.0-SNAPSHOT
bin\totalschema.bat [COMMAND]
```

### Using the Shaded JAR

Execute the CLI directly using:

```bash
java -jar totalschema-cli/target/totalschema-cli-1.0-SNAPSHOT-shaded.jar [COMMAND]
```

Available commands:
- `apply` - Apply pending changes to database
- `revert` - Revert applied changes from database
- `show` - Show pending or applicable changes
- `environments` - Environments related subcommands
- `variables` - Variables related subcommands
- `state` - State management commands
- `secrets` - Secret management subcommands
- `validate` - Validate workspace files against state

## JDBC Driver Configuration

TotalSchema requires JDBC drivers to connect to databases. Due to licensing restrictions, these drivers are not included in the distribution and must be added separately.

### Method 1: Copy to jdbc Directory (Recommended)

1. Download the JDBC driver JAR for your database
2. Copy the JAR file to the `lib/jdbc/` directory
3. The driver will be automatically loaded

Example:
```bash
# PostgreSQL
cp postgresql-42.7.3.jar /path/to/totalschema/lib/jdbc/

# Oracle
cp ojdbc11.jar /path/to/totalschema/lib/jdbc/

# Multiple drivers can coexist
ls lib/jdbc/
# postgresql-42.7.3.jar
# ojdbc11.jar
# mysql-connector-j-8.3.0.jar
```

### Method 2: Environment Variable

Set the `TOTALSCHEMA_JDBC_DRIVERS` environment variable with driver paths:

**Linux/macOS:**
```bash
export TOTALSCHEMA_JDBC_DRIVERS="/path/to/driver1.jar:/path/to/driver2.jar"
bin/totalschema.sh apply -e DEV
```

**Windows:**
``.bat
set TOTALSCHEMA_JDBC_DRIVERS=C:\path\to\driver1.jar;C:\path\to\driver2.jar
bin\totalschema.bat apply -e DEV
```

**Note:** Use colon (`:`) separator on Linux/macOS and semicolon (`;`) on Windows.

### Method 3: Combining Both

You can use both methods simultaneously. Drivers from `lib/jdbc/` will be loaded first, followed by those specified in `TOTALSCHEMA_JDBC_DRIVERS`.

### Common JDBC Drivers

| Database | Download Link |
|----------|---------------|
| PostgreSQL | https://jdbc.postgresql.org/ |
| Oracle | https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html |
| MySQL/MariaDB | https://dev.mysql.com/downloads/connector/j/ |
| SQL Server | https://learn.microsoft.com/en-us/sql/connect/jdbc/ |
| H2 | https://h2database.com/ |
| BigQuery | https://cloud.google.com/bigquery/docs/reference/odbc-jdbc-drivers |

See `lib/jdbc/lib-jdbc-README.md` in the distribution for detailed driver installation instructions.

## Dependencies

- **totalschema-core** - Core engine and API
- **picocli** - Command-line interface framework
- **snakeyaml** - YAML configuration support
- **slf4j** - Logging framework

