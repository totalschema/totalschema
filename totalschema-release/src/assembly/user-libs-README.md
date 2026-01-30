# User Libraries Directory

This directory is for user-provided JAR files that extend TotalSchema functionality. Place any third-party libraries here that TotalSchema needs but cannot include in the distribution due to licensing restrictions or optional features.

## What Goes Here?

This directory is for:

1. **JDBC Drivers** - Database connectivity drivers
2. **Groovy** - For using Groovy-based extensions (groovy-all)
3. **Other Runtime Dependencies** - Any other JARs required by your specific setup

## Why This Directory?

TotalSchema cannot bundle certain libraries due to:
- **Licensing restrictions** - Some vendors don't allow redistribution
- **Size considerations** - Not all users need all drivers/frameworks
- **Version flexibility** - Users can choose their preferred versions
- **Optional features** - Features like Groovy support are optional

## How to Add Libraries

1. Download the JAR file(s) you need
2. Copy them to this directory (`user_libs/`)
3. Restart TotalSchema if it's running

Example:
```bash
cp postgresql-42.7.3.jar /path/to/totalschema/user_libs/
cp groovy-all-2.4.21.jar /path/to/totalschema/user_libs/
```

## Alternative: Environment Variable

You can also specify library locations using the `TOTALSCHEMA_USER_LIBS` environment variable:

### Linux/macOS:
```bash
export TOTALSCHEMA_USER_LIBS="/path/to/driver1.jar:/path/to/groovy-all.jar"
```

### Windows:
```bat
set TOTALSCHEMA_USER_LIBS=C:\path\to\driver1.jar;C:\path\to\groovy-all.jar
```

**Note:** Use colon (`:`) as separator on Linux/macOS and semicolon (`;`) on Windows.

## Common Libraries

### JDBC Drivers

| Database | Driver Download Link | Example JAR Name |
|----------|---------------------|------------------|
| PostgreSQL | https://jdbc.postgresql.org/ | postgresql-42.7.3.jar |
| Oracle | https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html | ojdbc11.jar |
| MySQL/MariaDB | https://dev.mysql.com/downloads/connector/j/ | mysql-connector-j-8.3.0.jar |
| Microsoft SQL Server | https://learn.microsoft.com/en-us/sql/connect/jdbc/ | mssql-jdbc-12.6.0.jre11.jar |
| H2 | https://h2database.com/ | h2-2.2.224.jar |
| BigQuery | https://cloud.google.com/bigquery/docs/reference/odbc-jdbc-drivers | SimbaJDBCDriverforGoogleBigQuery42.jar |

### Scripting Frameworks

| Framework | Download Link | Example JAR Name | Required For |
|-----------|--------------|------------------|--------------|
| Groovy | https://groovy.apache.org/download.html | groovy-all-2.4.21.jar | Groovy-based extensions |

## How It Works

TotalSchema automatically adds all JAR files from this directory to the classpath when it starts. The loading order is:

1. `user_libs/` directory (this directory)
2. `lib/jdbc/` directory (deprecated, for backward compatibility)
3. `TOTALSCHEMA_USER_LIBS` environment variable
4. `TOTALSCHEMA_JDBC_DRIVERS` environment variable (deprecated, for backward compatibility)

All locations are combined, so you can use multiple methods simultaneously.

## Example Setups

### PostgreSQL Database Only
```bash
user_libs/
  └── postgresql-42.7.3.jar
```

Configuration:
```properties
assets.mydb.type=jdbc
assets.mydb.jdbc.driver.class=org.postgresql.Driver
assets.mydb.jdbc.url=jdbc:postgresql://localhost:5432/mydb
```

### PostgreSQL + Groovy Extensions
```bash
user_libs/
  ├── postgresql-42.7.3.jar
  └── groovy-all-2.4.21.jar
```

This setup allows:
- Connecting to PostgreSQL databases
- Using `totalschema-groovy-extensions` for Groovy-based transformations

### Multiple Databases + Groovy
```bash
user_libs/
  ├── postgresql-42.7.3.jar
  ├── ojdbc11.jar
  ├── mysql-connector-j-8.3.0.jar
  └── groovy-all-2.4.21.jar
```

All libraries will be loaded and available for use in your TotalSchema configurations.

## Detailed Examples

### Using Groovy Extensions

1. **Install Groovy:**
   ```bash
   # Download groovy-all from https://groovy.apache.org/download.html
   cp groovy-all-2.4.21.jar user_libs/
   ```

2. **Verify Extension:**
   ```bash
   # The totalschema-groovy-extensions JAR is already in lib/
   # You only need to provide groovy-all
   ```

3. **Use in Configuration:**
   ```properties
   # Now you can use Groovy-based transformations
   connector.mydata.type=groovy
   connector.mydata.script=/path/to/script.groovy
   ```

### Using JDBC with Oracle

1. **Download Oracle JDBC Driver:**
   - Visit: https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html
   - Download: `ojdbc11.jar` (for Java 11+)

2. **Install:**
   ```bash
   cp ojdbc11.jar /path/to/totalschema/user_libs/
   ```

3. **Configure:**
   ```properties
   assets.oracle_db.type=jdbc
   assets.oracle_db.jdbc.driver.class=oracle.jdbc.OracleDriver
   assets.oracle_db.jdbc.url=jdbc:oracle:thin:@//localhost:1521/ORCL
   assets.oracle_db.jdbc.user=${env:ORACLE_USER}
   assets.oracle_db.jdbc.password=${env:ORACLE_PASSWORD}
   ```

### Using Environment Variable Instead

If you prefer not to copy files:

```bash
# Linux/macOS
export TOTALSCHEMA_USER_LIBS="/opt/jdbc/postgresql-42.7.3.jar:/opt/groovy/groovy-all-2.4.21.jar"
./bin/totalschema.sh apply

# Windows
set TOTALSCHEMA_USER_LIBS=C:\jdbc\postgresql-42.7.3.jar;C:\groovy\groovy-all-2.4.21.jar
bin\totalschema.bat apply
```

## Troubleshooting

### ClassNotFoundException

**Symptom:** Error message like `ClassNotFoundException: org.postgresql.Driver`

**Solutions:**
1. Verify the JAR file is in the `user_libs/` directory
2. Check the JAR filename matches what you downloaded
3. Ensure the JAR is not corrupted (re-download if needed)
4. Restart TotalSchema after adding JARs

### NoClassDefFoundError

**Symptom:** Error about missing class definition even though JAR is present

**Solutions:**
1. Some libraries require multiple JAR files (check vendor documentation)
2. Verify you downloaded the correct version (e.g., Java 11+ compatible)
3. Check for version conflicts with bundled libraries

### Groovy Extensions Not Working

**Symptom:** Groovy-based features fail or are not recognized

**Solutions:**
1. Ensure `groovy-all-X.X.X.jar` is in `user_libs/`
2. Verify the version is compatible (2.4.x or 3.x recommended)
3. Check that `totalschema-groovy-extensions-*.jar` is in the `lib/` directory
4. Review logs for specific Groovy-related errors

### Driver Class Not Found

**Symptom:** Configuration error about missing driver class

**Solutions:**
1. Verify the driver class name in your configuration matches the actual driver
2. Common class names:
   - PostgreSQL: `org.postgresql.Driver`
   - Oracle: `oracle.jdbc.OracleDriver`
   - MySQL: `com.mysql.cj.jdbc.Driver`
   - SQL Server: `com.microsoft.sqlserver.jdbc.SQLServerDriver`
   - H2: `org.h2.Driver`

## Backward Compatibility

For backward compatibility, TotalSchema still supports:
- The old `lib/jdbc/` directory (will be checked if it exists)
- The old `TOTALSCHEMA_JDBC_DRIVERS` environment variable

However, we recommend using:
- The new `user_libs/` directory
- The new `TOTALSCHEMA_USER_LIBS` environment variable

This provides a cleaner, more consistent approach for all user-provided libraries.

## Migration from lib/jdbc

If you're currently using `lib/jdbc/`, migration is simple:

```bash
# Option 1: Move files
mv lib/jdbc/* user_libs/

# Option 2: Keep both (both will be loaded)
# No action needed - backward compatibility ensures both work

# Option 3: Update environment variable
# Old: TOTALSCHEMA_JDBC_DRIVERS=/path/to/drivers/*
# New: TOTALSCHEMA_USER_LIBS=/path/to/libs/*
```

## Best Practices

1. **Version Control:** Keep track of which JAR versions you're using
2. **Documentation:** Document required JARs in your project README
3. **Automation:** Consider scripting the JAR download/installation process
4. **Testing:** Test with the specific JAR versions you'll use in production
5. **Updates:** Periodically update JARs for security patches

## Security Considerations

- **Download from official sources:** Only download JARs from vendor websites
- **Verify checksums:** When available, verify JAR file checksums
- **Scan for vulnerabilities:** Use tools to scan JARs for known CVEs
- **Keep updated:** Regularly update to patched versions

## Getting Help

If you encounter issues:

1. Check TotalSchema logs for detailed error messages
2. Verify JAR file integrity and version compatibility
3. Consult vendor documentation for the specific library
4. Review TotalSchema documentation for configuration examples
5. Contact support with specific error messages and configuration details
