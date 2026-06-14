# KotlinSql - Groovy-like SQL API for Kotlin

## Overview

Yes! There **is** a similar API in Kotlin now. I've added a `KotlinSql` class that provides convenience methods very similar to Groovy's `groovy.sql.Sql` object.

## Comparison: Groovy vs Kotlin

### Groovy (Original)
```groovy
// Groovy binding
sql.execute("CREATE TABLE users (id INT, name VARCHAR(100))")
sql.execute("INSERT INTO users VALUES (?, ?)", [1, "John"])
rows = sql.rows("SELECT * FROM users")
```

### Kotlin (New)
```kotlin
// Kotlin binding - now very similar!
sql.execute("CREATE TABLE users (id INT, name VARCHAR(100))")
sql.execute("INSERT INTO users VALUES (?, ?)", 1, "John")
val rows = sql.rows("SELECT * FROM users")
```

## KotlinSql API

The `KotlinSql` class provides these methods:

### Execute DDL/DML
```kotlin
// Simple execution
sql.execute("CREATE TABLE products (id INT, name VARCHAR(100))")

// With parameters
sql.execute("INSERT INTO products VALUES (?, ?)", 1, "Laptop")
```

### Execute Updates
```kotlin
// Returns affected row count
val count = sql.executeUpdate("UPDATE products SET price = ? WHERE id = ?", 999.99, 1)
println("Updated $count rows")
```

### Query Data
```kotlin
// Get all rows as List<Map<String, Object>>
val products = sql.rows("SELECT * FROM products WHERE price > ?", 100.0)
products.forEach { row ->
    println("Name: ${row["name"]}, Price: ${row["price"]}")
}

// Get first row as Map<String, Object>
val product = sql.firstRow("SELECT * FROM products WHERE id = ?", 1)
if (product != null) {
    println("Found: ${product["name"]}")
}
```

### Batch Operations
```kotlin
val batch = listOf(
    arrayOf("Laptop", 999.99),
    arrayOf("Mouse", 29.99),
    arrayOf("Keyboard", 79.99)
)
sql.executeBatch("INSERT INTO products (name, price) VALUES (?, ?)", batch)
```

### Advanced: Raw Connection
```kotlin
// Still have access to raw JDBC when needed
val connection = sql.connection
val metadata = connection.metaData
println(metadata.databaseProductName)
```

## Implementation Details

### Key Classes

**KotlinSql.java** — Lightweight wrapper around JDBC Connection
- Provides convenience methods similar to Groovy SQL
- Automatically handles resource cleanup
- Returns query results as `List<Map<String, Object>>`
- Uses parameterized queries with `PreparedStatement`

**Methods:**
- `execute(sql)` / `execute(sql, params...)` — Execute statements
- `executeUpdate(sql, params...)` — Execute and return affected rows
- `rows(sql, params...)` — Query all rows
- `firstRow(sql, params...)` — Query first row
- `executeBatch(sql, paramsList)` — Batch operations
- `getConnection()` — Access raw connection

### Injected Bindings

Kotlin scripts now have:

| Binding | Type | Description |
|---------|------|-------------|
| `sql` | `KotlinSql` | Convenience wrapper (like Groovy's `sql`) |
| `connection` | `Connection` | Raw JDBC (for advanced usage) |
| `configuration` | `Configuration` | Connector config |
| `environment` | `Environment` | Current environment |

## Benefits

### ✅ Compared to Raw JDBC
- **Less boilerplate** — No manual statement/resultset closing
- **Cleaner code** — Fewer lines, more readable
- **Type-safe parameters** — Uses varargs instead of indexed setters
- **Map-based results** — Access columns by name, not index
- **Kotlin-friendly** — Works naturally with Kotlin features

### ✅ Compared to Groovy
- **Same conceptual model** — `sql` object with similar methods
- **Familiar API** — Easy migration from Groovy scripts
- **Kotlin benefits** — Type inference, null safety, lambdas
- **Still lightweight** — No heavy ORM framework

## Example: Before & After

### Before (Raw JDBC)
```kotlin
val stmt = connection.prepareStatement("INSERT INTO users (name, email) VALUES (?, ?)")
users.forEach { (name, email) ->
    stmt.setString(1, name)
    stmt.setString(2, email)
    stmt.executeUpdate()
}
stmt.close()

val queryStmt = connection.createStatement()
val rs = queryStmt.executeQuery("SELECT * FROM users")
while (rs.next()) {
    println("${rs.getString("name")} - ${rs.getString("email")}")
}
rs.close()
queryStmt.close()
```

### After (KotlinSql)
```kotlin
users.forEach { (name, email) ->
    sql.execute("INSERT INTO users (name, email) VALUES (?, ?)", name, email)
}

sql.rows("SELECT * FROM users").forEach { row ->
    println("${row["name"]} - ${row["email"]}")
}
```

**66% less code, much more readable!**

## Sample Scripts

See the updated examples:

- `samples/kotlin/totalschema/0001.create_users_table.DEV.apply.mydb.kts` — Simple DDL
- `samples/kotlin/totalschema/0002.insert_sample_data.DEV.apply.mydb.kts` — Parameterized DML
- `samples/kotlin/totalschema/0003.advanced_sql_examples.DEV.apply.mydb.kts` — All features

## Architecture Notes

### Why Not Use a Library?

**Considered:**
- **Exposed** — Too heavy, requires schema definition
- **jOOQ** — Compile-time code generation needed
- **JDBI** — Additional transitive dependencies

**Decision:** Lightweight custom wrapper
- Zero additional dependencies
- Simple, focused API
- Easy to understand and maintain
- Similar to Groovy's proven approach

### Design Principles

1. **Similarity to Groovy** — Familiar API for users migrating from Groovy scripts
2. **Resource Safety** — Auto-close statements and result sets
3. **Flexibility** — Provide both convenience (`sql`) and raw access (`connection`)
4. **Kotlin-Friendly** — Works naturally with Kotlin idioms

## Conclusion

**Yes, there is now a full-featured Groovy-like SQL API for Kotlin!**

The `KotlinSql` class provides:

✅ Clean, convenient API similar to Groovy's `groovy.sql.Sql`  
✅ **Full closure support** with `eachRow` and `eachRowWithMetadata`  
✅ Memory-efficient processing for large result sets  
✅ Familiar syntax for Groovy users migrating to Kotlin  
✅ Raw JDBC access when needed  

Database scripts in Kotlin are now **just as pleasant as in Groovy**!

**See also:**
- `CLOSURE_SUPPORT.md` — Comprehensive closure/lambda documentation with patterns and examples
- `samples/kotlin/totalschema/0004.closure_examples.DEV.apply.mydb.kts` — Live demonstration

