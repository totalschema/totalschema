/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2025 totalschema development team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.totalschema.cli;

import static org.testng.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Integration test that validates the TotalSchema CLI by applying sample changes to an H2 database.
 * This test:
 *
 * <ul>
 *   <li>Copies the sample project to an isolated test directory
 *   <li>Starts with an empty database
 *   <li>Applies all changes from the sample configuration
 *   <li>Validates the resulting schema and data from a business perspective
 * </ul>
 *
 * <p>The test validates:
 *
 * <ul>
 *   <li>Table creation (phone_book table with correct structure)
 *   <li>Column additions (email, first_name, last_name)
 *   <li>Data insertion (10 sample records)
 *   <li>Data transformations (first_name and last_name extracted from name)
 * </ul>
 */
public class SampleIntegrationTest {

    private static final String SAMPLE_PROJECT_SOURCE = "../sample";
    private static final String TEST_WORKSPACE_NAME = "test-sample-workspace/" + System.currentTimeMillis();
    private Path testWorkspacePath;
    private Path changesDirectory;
    private String sampleDbJdbcUrl;
    private static final String ENVIRONMENT = "DEV";
    private static final String DB_PASSWORD = "FOOBAR";

    @BeforeMethod
    public void setUp() throws Exception {
        // Determine the test workspace path
        Path testDir = Paths.get("target/test-workspaces").toAbsolutePath();
        testWorkspacePath = testDir.resolve(TEST_WORKSPACE_NAME);
        changesDirectory = testWorkspacePath.resolve("totalschema/changes");


        // Clean up any previous test run
        if (Files.exists(testWorkspacePath)) {
            FileUtils.deleteDirectory(testWorkspacePath.toFile());
        }

        // Create test workspace directory
        Files.createDirectories(testWorkspacePath);

        // Copy the sample project to test workspace
        Path sampleSourcePath = Paths.get(SAMPLE_PROJECT_SOURCE).toAbsolutePath();
        if (!Files.exists(sampleSourcePath)) {
            fail("Sample project not found at: " + sampleSourcePath);
        }

        FileUtils.copyDirectory(sampleSourcePath.toFile(), testWorkspacePath.toFile());

        // Set up H2 database path
        Path sampleDbPath = testWorkspacePath.resolve("h2db/sample_db");
        Files.createDirectories(sampleDbPath);

        // Construct JDBC URL for H2 database
        sampleDbJdbcUrl = "jdbc:h2:file:" + sampleDbPath.resolve("dev_db");
        System.out.println(sampleDbJdbcUrl);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        // Clean up test workspace
        if (Files.exists(testWorkspacePath)) {
            FileUtils.deleteDirectory(testWorkspacePath.toFile());
        }
    }

    @Test
    public void testApplySampleChanges() throws Exception {
        // We cannot rely on System.setProperty("user.dir") for Java file operations
        // Instead, we'll change the working directory by using the Main.run method
        // but we need to set up a custom configuration that knows about the paths

        // For now, we'll skip the subprocess approach and instead use absolute paths
        // by modifying how we load the configuration

        try {
            // Change user.dir to the test workspace
            System.setProperty("totalschema.changes.directory", changesDirectory.toString());
            System.setProperty("totalschema.workspace.directory", testWorkspacePath.toString());
            System.setProperty("totalschema.variables.sampleDatabasesRootPath", testWorkspacePath.toString());

            // Apply changes using the Main CLI entry point
            int exitCode =
                    Main.run(new String[] {"apply", "-e", ENVIRONMENT, "--password", DB_PASSWORD});

            // Verify successful execution
            assertEquals(exitCode, 0, "CLI should execute successfully");

            // Validate the schema and data
            validatePhonebookSchema();
            validatePhonebookData();
        } finally {
            System.clearProperty("totalschema.changes.directory");
            System.clearProperty("totalschema.workspace.directory");
        }
    }

    /** Validates that the phone_book table has the correct structure with all expected columns */
    private void validatePhonebookSchema() throws Exception {
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            // Get columns for the PHONE_BOOK table using JDBC metadata API
            ResultSet rs = metaData.getColumns(null, null, "PHONE_BOOK", null);

            List<String> columnNames = new ArrayList<>();

            while (rs.next()) {
                columnNames.add(rs.getString("COLUMN_NAME"));
            }
            rs.close();

            // Verify expected columns exist
            assertTrue(columnNames.contains("ID"), "Table should have ID column");
            assertTrue(columnNames.contains("NAME"), "Table should have NAME column");
            assertTrue(
                    columnNames.contains("PHONE_NUMBER"), "Table should have PHONE_NUMBER column");
            assertTrue(
                    columnNames.contains("EMAIL"),
                    "Table should have EMAIL column (added in 2.0.0)");
            assertTrue(
                    columnNames.contains("FIRST_NAME"),
                    "Table should have FIRST_NAME column (added in 2.5.0)");
            assertTrue(
                    columnNames.contains("LAST_NAME"),
                    "Table should have LAST_NAME column (added in 2.5.0)");

            System.out.println("✓ Schema validation passed. Columns found: " + columnNames);
        }
    }

    /** Validates that the sample data has been inserted and transformed correctly */
    private void validatePhonebookData() throws Exception {
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {

            // Verify row count
            ResultSet countRs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM PHONE_BOOK");
            assertTrue(countRs.next(), "Should have results");
            int recordCount = countRs.getInt("cnt");
            assertEquals(recordCount, 10, "Should have 10 sample records inserted");

            System.out.println(
                    "✓ Record count validation passed: " + recordCount + " records found");

            // Verify sample data structure and transformations
            ResultSet dataRs =
                    stmt.executeQuery(
                            "SELECT ID, NAME, PHONE_NUMBER, EMAIL, FIRST_NAME, LAST_NAME "
                                    + "FROM PHONE_BOOK ORDER BY ID");

            int rowCount = 0;
            while (dataRs.next()) {
                rowCount++;
                int id = dataRs.getInt("ID");
                String name = dataRs.getString("NAME");
                String phoneNumber = dataRs.getString("PHONE_NUMBER");
                String firstName = dataRs.getString("FIRST_NAME");
                String lastName = dataRs.getString("LAST_NAME");

                // Validate expected data format
                assertEquals(id, rowCount, "ID should match row number");
                assertNotNull(name, "Name should not be null");
                assertTrue(
                        name.contains("Contact Sample #" + id),
                        "Name should contain expected format");
                assertNotNull(phoneNumber, "Phone number should not be null");
                // Phone number should be repeated digits (e.g., "11111111" for id 1)
                String expectedPhoneNumber = String.valueOf(id).repeat(8);
                assertEquals(
                        phoneNumber, expectedPhoneNumber, "Phone number format should be correct");

                // Email column should exist but may be NULL (not explicitly set in changes)
                // This is fine - we're just checking the column exists

                // Validate name transformations
                // first_name should be extracted from name (first word)
                assertNotNull(firstName, "First name should be populated (transformed from name)");
                assertTrue(!firstName.isEmpty(), "First name should not be empty");

                // last_name should be extracted from name (remaining words)
                assertNotNull(lastName, "Last name should be populated (transformed from name)");
                assertTrue(!lastName.isEmpty(), "Last name should not be empty");

                System.out.println(
                        "✓ Row " + id + ": name='" + name + "', phone='" + phoneNumber
                                + "', firstName='" + firstName + "', lastName='" + lastName
                                + "'");
            }

            assertEquals(rowCount, 10, "Should have processed all 10 records");
            System.out.println(
                    "✓ Data validation passed. All records have correct structure and transformations");
        }
    }

    /** Creates a JDBC connection to the test H2 database */
    private Connection getConnection() throws Exception {
        Class.forName("org.h2.Driver");
        return DriverManager.getConnection(sampleDbJdbcUrl, "sa", DB_PASSWORD);
    }
}
