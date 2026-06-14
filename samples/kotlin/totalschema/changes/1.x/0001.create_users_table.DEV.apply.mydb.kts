// Sample Kotlin script for TotalSchema
// Filename: 0001.create_users_table.DEV.apply.mydb.kts

// This example demonstrates using the raw JDBC 'connection' object
// (as opposed to the 'sql' helper object used in other examples)

// Create statement using raw JDBC connection
val statement = connection.createStatement()

try {
    statement.execute("""
        CREATE TABLE users (
            id INT PRIMARY KEY AUTO_INCREMENT,
            username VARCHAR(100) NOT NULL UNIQUE,
            email VARCHAR(255) NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """)

    println("✓ Users table created successfully using raw JDBC connection")

} finally {
    // Always close the statement
    statement.close()
}

println("\n--- Comparison: Raw JDBC vs sql helper ---\n")

// Option 1: Using raw JDBC connection (verbose, manual resource management)
println("Using raw JDBC connection:")
val jdbcStatement = connection.createStatement()
try {
    val resultSet = jdbcStatement.executeQuery("SELECT COUNT(*) as COUNT FROM users")
    if (resultSet.next()) {
        val count = resultSet.getInt("COUNT")
        println("  User count (raw JDBC): $count")
    }
    resultSet.close()
} finally {
    jdbcStatement.close()
}

// Option 2: Using sql helper object (simpler, cleaner, automatic resource cleanup)
println("\nUsing sql helper object:")
val result = sql.firstRow("SELECT COUNT(*) as total FROM users")
val count = result?.get("TOTAL")
println("  User count (sql helper): $count")

println("\n✓ The 'sql' object provides a much simpler API!")
println("  - No manual statement/resultset closing")
println("  - Direct access to results as Map")
println("  - Less boilerplate code")

