
// The 'sql' object is automatically available
println "Removing sample records..."

sql.execute("DELETE FROM phone_book WHERE name like 'Contact Sample%'")

println "Sample records deleted successfully!"