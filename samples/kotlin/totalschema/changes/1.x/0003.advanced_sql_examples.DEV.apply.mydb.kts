// Advanced Kotlin script demonstrating all sql object features
// Filename: 0003.advanced_sql_examples.DEV.apply.mydb.kts

// Example 1: Simple DDL execution
sql.execute("""
    CREATE TABLE IF NOT EXISTS products (
        id INT PRIMARY KEY AUTO_INCREMENT,
        name VARCHAR(255) NOT NULL,
        price DECIMAL(10, 2),
        stock INT DEFAULT 0
    )
""")
println("✓ Products table created")

// Example 2: Parameterized inserts
sql.execute("INSERT INTO products (name, price, stock) VALUES (?, ?, ?)", "Laptop", 999.99, 10)
sql.execute("INSERT INTO products (name, price, stock) VALUES (?, ?, ?)", "Mouse", 29.99, 50)
sql.execute("INSERT INTO products (name, price, stock) VALUES (?, ?, ?)", "Keyboard", 79.99, 25)
println("✓ Products inserted")

// Example 3: Query all rows (H2 uses uppercase column names by default)
val allProducts = sql.rows("SELECT * FROM products ORDER BY price DESC")
println("\nAll Products:")
allProducts.forEach {
    println("  ${it["NAME"]}: $${it["PRICE"]} (Stock: ${it["STOCK"]})")
}

// Example 4: Query single row
val mostExpensive = sql.firstRow("SELECT * FROM products ORDER BY price DESC LIMIT 1")
if (mostExpensive != null) {
    println("\nMost expensive: ${mostExpensive["NAME"]} at $${mostExpensive["PRICE"]}")
}

// Example 5: Query with parameters
val affordableProducts = sql.rows("SELECT * FROM products WHERE price < ?", 100.0)
println("\nAffordable products (< $100):")
affordableProducts.forEach {
    println("  ${it["NAME"]}: $${it["PRICE"]}")
}

// Example 6: Execute update and get affected count
val updated = sql.executeUpdate("UPDATE products SET stock = stock + ? WHERE price < ?", 10, 50.0)
println("\n✓ Updated stock for $updated product(s)")

// Example 7: Batch insert
val batchData = listOf(
    arrayOf("Monitor", 299.99, 15),
    arrayOf("Webcam", 89.99, 30),
    arrayOf("Headset", 149.99, 20)
)
val batchResults = sql.executeBatch(
    "INSERT INTO products (name, price, stock) VALUES (?, ?, ?)",
    batchData
)
println("✓ Batch inserted ${batchResults.size} products")

// Example 8: Access raw connection for advanced usage if needed
val metadata = connection.metaData
println("\nDatabase: ${metadata.databaseProductName} ${metadata.databaseProductVersion}")

// Final verification
val totalCount = sql.firstRow("SELECT COUNT(*) as total FROM products")
println("\n✓ Total products in database: ${totalCount?.get("TOTAL")}")

