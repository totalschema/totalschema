# TotalSchema Kotlin Sample

This directory contains sample Kotlin scripts (`.kts` files) for TotalSchema.

## Prerequisites

1. Build and install the `totalschema-kotlin-extensions` module
2. Download Kotlin scripting JARs and place them in `user_libs/`:
   - `kotlin-scripting-jsr223-1.9.24.jar`
   - `kotlin-stdlib-1.9.24.jar`
   - `kotlin-script-runtime-1.9.24.jar`
   - `kotlin-scripting-common-1.9.24.jar`
   - `kotlin-scripting-jvm-1.9.24.jar`
   - `kotlin-compiler-embeddable-1.9.24.jar`

## Configuration

Edit `totalschema.yml` to configure your database connection and connector.

## Running

```bash
# From the CLI distribution directory
./totalschema.sh apply -e DEV -w /path/to/samples/kotlin
```

## Sample Scripts

- `0001.create_users_table.DEV.apply.mydb.kts` - Creates a users table
- `0002.insert_sample_data.DEV.apply.mydb.kts` - Inserts sample data using prepared statements

## Injected Bindings

Your Kotlin scripts have access to:

- `connection: java.sql.Connection` - Active database connection
- `configuration: io.github.totalschema.config.Configuration` - Connector configuration
- `environment: io.github.totalschema.config.environment.Environment` - Current environment (if set)

## Tips

- Use the `sql` object for most operations - it's cleaner than raw JDBC
- **Use closures** (`eachRow`) for large result sets - more memory efficient than `rows()`
- Use Kotlin's language features: lambdas, when expressions, extension functions
- Use string templates for cleaner output: `println("User: $username")`
- Leverage Kotlin's null safety: `row?.get("column")`
- Query results are `List<Map<String, Object>>` - use Kotlin collection operations
- For advanced JDBC features, access the raw `connection` object

