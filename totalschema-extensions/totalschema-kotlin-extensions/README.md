# TotalSchema Kotlin Extensions

This module provides Kotlin scripting support for TotalSchema via JSR-223.

## Features

- Execute `.kts` (Kotlin script) files against JDBC databases
- Full access to database connections via injected bindings
- Environment-aware script execution

## Usage

### 1. Install Kotlin Scripting JARs

Download and place the following JARs in `user_libs/`:

```bash
kotlin-scripting-jsr223-1.9.24.jar
kotlin-stdlib-1.9.24.jar
kotlin-script-runtime-1.9.24.jar
kotlin-scripting-common-1.9.24.jar
kotlin-scripting-jvm-1.9.24.jar
kotlin-compiler-embeddable-1.9.24.jar
```

You can download these from [Maven Central](https://repo1.maven.org/maven2/org/jetbrains/kotlin/).

### 2. Create Kotlin Script Files

Name your files with the `.kts` extension:

```
0001.create_users.DEV.apply.mydb.kts
```

### 3. Use Injected Bindings

The following objects are injected into every Kotlin script:

- `connection` (`java.sql.Connection`) — active database connection
- `configuration` (`io.github.totalschema.config.Configuration`) — connector config
- `environment` (`io.github.totalschema.config.environment.Environment`) — current environment (if set)

### Example Script

```kotlin
// 0001.create_table.DEV.apply.mydb.kts

val statement = connection.createStatement()
statement.execute("""
    CREATE TABLE users (
        id INT PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        email VARCHAR(100)
    )
""")
statement.close()

println("Table users created successfully")
```

### With Prepared Statements

```kotlin
// 0002.insert_data.DEV.apply.mydb.kts

val stmt = connection.prepareStatement("INSERT INTO users (id, name, email) VALUES (?, ?, ?)")

stmt.setInt(1, 1)
stmt.setString(2, "John Doe")
stmt.setString(3, "john@example.com")
stmt.executeUpdate()

stmt.setInt(1, 2)
stmt.setString(2, "Jane Smith")
stmt.setString(3, "jane@example.com")
stmt.executeUpdate()

stmt.close()

println("Inserted sample data")
```

## Configuration

No additional configuration is needed beyond placing Kotlin JARs in `user_libs/`. The script executor is automatically discovered via Java ServiceLoader.

## Architecture

This extension follows TotalSchema's script executor pattern:

1. **KotlinScriptExecutor** — implements `ScriptExecutor`, uses **modern Kotlin Scripting Host API** (not JSR-223)
2. **KotlinSql** — Kotlin-friendly SQL helper providing convenience methods similar to Groovy's `groovy.sql.Sql`
3. **KotlinScriptExecutorComponentFactory** — extends `AbstractScriptExecutorComponentFactory` with qualifier `"kts"`
4. Registered via `META-INF/services/io.github.totalschema.spi.factory.ComponentFactory`

## Notes

- Uses **Kotlin Scripting Host API** (`kotlin.script.experimental.*`) instead of deprecated JSR-223
- The `sql` object provides a clean API similar to Groovy's SQL support
- For advanced cases, the raw `connection` is also available
- Kotlin scripts have full access to Kotlin's language features (lambdas, extension functions, etc.)
- Scripts are compiled and evaluated using `BasicJvmScriptingHost`
- For complex logic, consider using helper Kotlin files and importing them into your scripts

## License

AGPL-3.0 (same as TotalSchema core)

