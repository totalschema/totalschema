// Advanced Kotlin script demonstrating closure/lambda support
// Filename: 0004.closure_examples.DEV.apply.mydb.kts

// Setup - create and populate a test table
sql.execute("""
    CREATE TABLE IF NOT EXISTS employees (
        id INT PRIMARY KEY AUTO_INCREMENT,
        name VARCHAR(100) NOT NULL,
        department VARCHAR(50),
        salary DECIMAL(10, 2)
    )
""")

// Insert some test data
val employees = listOf(
    arrayOf("Alice Johnson", "Engineering", 95000.0),
    arrayOf("Bob Smith", "Engineering", 87000.0),
    arrayOf("Carol Williams", "Sales", 78000.0),
    arrayOf("David Brown", "Engineering", 92000.0),
    arrayOf("Eve Davis", "Sales", 81000.0),
    arrayOf("Frank Miller", "HR", 65000.0)
)

sql.executeBatch("INSERT INTO employees (name, department, salary) VALUES (?, ?, ?)", employees)
println("✓ Created and populated employees table\n")

// === CLOSURE EXAMPLES ===
// Note: H2 uses uppercase column names by default

// Example 1: Basic eachRow - process each row without parameters
println("=== All Employees ===")
sql.eachRow("SELECT * FROM employees ORDER BY name") { row ->
    println("${row["NAME"]} - ${row["DEPARTMENT"]} - $${row["SALARY"]}")
}

// Example 2: eachRow with parameters
println("\n=== Engineering Department ===")
sql.eachRow("SELECT * FROM employees WHERE department = ? ORDER BY salary DESC", arrayOf("Engineering")) { row ->
    println("${row["NAME"]}: $${row["SALARY"]}")
}

// Example 3: eachRowWithMetadata - get row numbers
println("\n=== Top 3 Earners (with row numbers) ===")
sql.eachRowWithMetadata("SELECT name, salary FROM employees ORDER BY salary DESC LIMIT 3") { row, meta ->
    println("${meta.rowNumber}. ${row["NAME"]}: $${row["SALARY"]} (${meta.columnCount} columns)")
}

// Example 4: Calculate aggregates using closures
println("\n=== Salary Statistics ===")
var totalSalary = 0.0
var count = 0
sql.eachRow("SELECT salary FROM employees") { row ->
    totalSalary += (row["SALARY"] as Number).toDouble()
    count++
}
val avgSalary = totalSalary / count
println("Total employees: $count")
println("Total salaries: $$totalSalary")
println("Average salary: $${"%.2f".format(avgSalary)}")

// Example 5: Build custom data structures using closures
println("\n=== Employees by Department ===")
val byDepartment = mutableMapOf<String, MutableList<String>>()
sql.eachRow("SELECT name, department FROM employees ORDER BY department, name") { row ->
    val dept = row["DEPARTMENT"] as String
    val name = row["NAME"] as String
    byDepartment.getOrPut(dept) { mutableListOf() }.add(name)
}
byDepartment.forEach { (dept, names) ->
    println("$dept: ${names.joinToString(", ")}")
}

// Example 6: Filter and transform using closures
println("\n=== High Earners (>80k) with Bonus Calculation ===")
sql.eachRow("SELECT name, salary FROM employees WHERE salary > ? ORDER BY salary DESC", arrayOf(80000.0)) { row ->
    val name = row["NAME"] as String
    val salary = (row["SALARY"] as Number).toDouble()
    val bonus = salary * 0.10
    println("$name: Salary $$salary + Bonus $${"%.2f".format(bonus)} = Total $${"%.2f".format(salary + bonus)}")
}

// Example 7: Conditional processing with metadata
println("\n=== All Employees (highlight every 2nd row) ===")
sql.eachRowWithMetadata("SELECT name, department FROM employees ORDER BY name") { row, meta ->
    val prefix = if (meta.rowNumber % 2 == 0) "▶" else " "
    println("$prefix ${meta.rowNumber}. ${row["NAME"]} (${row["DEPARTMENT"]})")
}

// Example 8: Early termination pattern (count until condition)
println("\n=== First 3 Engineering Employees ===")
var engineeringCount = 0
sql.eachRow("SELECT name FROM employees WHERE department = ? ORDER BY name", arrayOf("Engineering")) { row ->
    if (engineeringCount < 3) {
        println("${engineeringCount + 1}. ${row["NAME"]}")
        engineeringCount++
    }
}

// Comparison: Memory efficiency - eachRow vs rows
println("\n=== Memory Efficiency Comparison ===")

// Using rows() - builds entire list in memory
val allRows = sql.rows("SELECT * FROM employees")
println("rows() loaded ${allRows.size} rows into memory at once")

// Using eachRow() - processes one at a time
var processedCount = 0
sql.eachRow("SELECT * FROM employees") { row ->
    processedCount++
    // Each row is processed and can be discarded
}
println("eachRow() processed $processedCount rows one at a time (more memory efficient)")

println("\n✓ Closure examples completed!")

