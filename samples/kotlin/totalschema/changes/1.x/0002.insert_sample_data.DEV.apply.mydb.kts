// Sample Kotlin script with prepared statements
// Filename: 0002.insert_sample_data.DEV.apply.mydb.kts

// The 'sql' object provides convenient methods similar to Groovy SQL
val users = listOf(
    "john_doe" to "john@example.com",
    "jane_smith" to "jane@example.com",
    "bob_wilson" to "bob@example.com"
)

users.forEach { (username, email) ->
    sql.execute("INSERT INTO users (username, email) VALUES (?, ?)", username, email)
    println("✓ Inserted user: $username")
}

// Query the data back (H2 uses uppercase column names by default)
val allUsers = sql.rows("SELECT * FROM users")
println("\nTotal users: ${allUsers.size}")
allUsers.forEach { row ->
    println("  - ${row["USERNAME"]} (${row["EMAIL"]})")
}

println("\n✓ All sample data inserted and verified successfully")

