
// The 'sql' object is automatically available
println "Adding sample records..."

for (int i = 1; i <= 10; i++) {
    def contactName = "Contact Sample #${i}".toString()
    def phoneNumber = "${i}".repeat(8)

    sql.execute("""
        insert into PHONE_BOOK (id, name, phone_number)
        values (?, ?, ?)
    """, [i, contactName, phoneNumber])
    println "Added record ${i}: ${contactName}"
}

println "Sample records added successfully!"