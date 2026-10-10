/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2025-2026 totalschema development team
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

package io.github.totalschema.connector.gcp.bigquery.actions.statement.execute;

import static org.testng.Assert.*;

import com.google.cloud.bigquery.*;
import io.github.totalschema.connector.gcp.bigquery.AbstractBigQueryIntegrationTest;
import io.github.totalschema.connector.gcp.bigquery.BigQueryConnectorException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Integration tests for {@link ExecuteStatement} action using the BigQuery emulator.
 *
 * <p>These tests verify that the ExecuteStatement action correctly executes SQL queries in BigQuery
 * using the real BigQuery API against the emulator.
 */
public class ExecuteStatementIntegrationTest extends AbstractBigQueryIntegrationTest {

    @BeforeMethod
    public void setUpDataset() {
        // Create a test dataset for query tests
        DatasetInfo datasetInfo =
                DatasetInfo.newBuilder("test_query_dataset").setDescription("Test dataset").build();
        try {
            bigQuery.create(datasetInfo);
        } catch (Exception e) {
            // Dataset might already exist, ignore
        }
    }

    @Test
    public void testExecuteCreateTableQuery() throws InterruptedException {
        // Given: an ExecuteStatement action to create a table
        ExecuteStatement action = new ExecuteStatement();
        action.setStatement(
                "CREATE TABLE test_query_dataset.users ("
                        + "  id INT64,"
                        + "  name STRING,"
                        + "  email STRING"
                        + ")");
        action.setDefaultDataset("test_query_dataset");

        // When: executing the action
        action.execute(bigQuery, mockConfiguration);

        // Then: table should be created
        Table table = bigQuery.getTable("test_query_dataset", "users");
        assertNotNull(table, "Table should be created");
        assertEquals(table.getTableId().getTable(), "users");

        // Verify schema
        Schema schema = table.getDefinition().getSchema();
        assertNotNull(schema);
        assertEquals(schema.getFields().size(), 3);
    }

    @Test
    public void testExecuteInsertQuery() throws InterruptedException {
        // Given: a table and an insert query
        // First create the table
        ExecuteStatement createTable = new ExecuteStatement();
        createTable.setStatement(
                "CREATE TABLE test_query_dataset.test_table (id INT64, value STRING)");
        createTable.execute(bigQuery, mockConfiguration);

        // Then execute insert
        ExecuteStatement insertAction = new ExecuteStatement();
        insertAction.setStatement(
                "INSERT INTO test_query_dataset.test_table (id, value) VALUES (1, 'test')");
        insertAction.setDefaultDataset("test_query_dataset");

        // When: executing the insert
        insertAction.execute(bigQuery, mockConfiguration);

        // Then: data should be inserted (verify table still exists)
        Table table = bigQuery.getTable("test_query_dataset", "test_table");
        assertNotNull(table, "Table should exist after insert");
    }

    @Test
    public void testExecuteSelectQuery() throws InterruptedException {
        // Given: a table with data
        ExecuteStatement setupAction = new ExecuteStatement();
        setupAction.setStatement(
                "CREATE TABLE test_query_dataset.select_test (id INT64, name STRING)");
        setupAction.execute(bigQuery, mockConfiguration);

        // When: executing a select query
        ExecuteStatement selectAction = new ExecuteStatement();
        selectAction.setStatement("SELECT COUNT(*) as count FROM test_query_dataset.select_test");
        selectAction.setDefaultDataset("test_query_dataset");

        // Then: query should execute successfully
        selectAction.execute(bigQuery, mockConfiguration);
        // Note: Emulator may have limitations on returning results
    }

    @Test
    public void testExecuteMultilineQuery() throws InterruptedException {
        // Given: a multiline query
        ExecuteStatement action = new ExecuteStatement();
        action.setStatement(
                "CREATE TABLE test_query_dataset.multiline_table (\n"
                        + "  user_id INT64,\n"
                        + "  username STRING,\n"
                        + "  email STRING,\n"
                        + "  created_at TIMESTAMP\n"
                        + ")");
        action.setDefaultDataset("test_query_dataset");

        // When: executing the query
        action.execute(bigQuery, mockConfiguration);

        // Then: table should be created
        Table table = bigQuery.getTable("test_query_dataset", "multiline_table");
        assertNotNull(table, "Table should be created");

        // Verify all columns
        Schema schema = table.getDefinition().getSchema();
        assertEquals(schema.getFields().size(), 4);
    }

    @Test
    public void testExecuteQueryWithDefaultDataset() throws InterruptedException {
        // Given: a table in the default dataset and a query without a dataset prefix
        ExecuteStatement createTable = new ExecuteStatement();
        createTable.setStatement(
                "CREATE TABLE test_query_dataset.no_prefix_table (id INT64, name STRING)");
        createTable.execute(bigQuery, mockConfiguration);

        ExecuteStatement action = new ExecuteStatement();
        action.setStatement("SELECT * FROM no_prefix_table");
        action.setDefaultDataset("test_query_dataset");

        // When: executing the query
        action.execute(bigQuery, mockConfiguration);

        // Then: the unqualified table reference should resolve to the default dataset
        Table table = bigQuery.getTable("test_query_dataset", "no_prefix_table");
        assertNotNull(table, "Table should exist in default dataset");
    }

    @Test
    public void testExecuteQueryWithStandardSQL() throws InterruptedException {
        // Given: a standard SQL query (not legacy)
        ExecuteStatement action = new ExecuteStatement();
        action.setStatement(
                "CREATE TABLE test_query_dataset.standard_sql_table ("
                        + "  id INT64 NOT NULL,"
                        + "  value STRING"
                        + ")");
        action.setUseLegacySql(false);

        // When: executing the query
        action.execute(bigQuery, mockConfiguration);

        // Then: query should execute successfully
        Table table = bigQuery.getTable("test_query_dataset", "standard_sql_table");
        assertNotNull(table);
    }

    @Test(expectedExceptions = BigQueryConnectorException.class)
    public void testExecuteInvalidQuery() throws InterruptedException {
        // Given: an invalid SQL query
        ExecuteStatement action = new ExecuteStatement();
        action.setStatement("CREATE TABLE INVALID SYNTAX HERE");

        // When/Then: executing should throw exception
        action.execute(bigQuery, mockConfiguration);
    }

    @Test
    public void testExecuteMultipleQueries() throws InterruptedException {
        // Given: multiple query executions
        ExecuteStatement action1 = new ExecuteStatement();
        action1.setStatement("CREATE TABLE test_query_dataset.table1 (id INT64)");

        ExecuteStatement action2 = new ExecuteStatement();
        action2.setStatement("CREATE TABLE test_query_dataset.table2 (id INT64, name STRING)");

        ExecuteStatement action3 = new ExecuteStatement();
        action3.setStatement(
                "CREATE TABLE test_query_dataset.table3 (id INT64, name STRING, email STRING)");

        // When: executing all queries
        action1.execute(bigQuery, mockConfiguration);
        action2.execute(bigQuery, mockConfiguration);
        action3.execute(bigQuery, mockConfiguration);

        // Then: all tables should be created
        assertNotNull(bigQuery.getTable("test_query_dataset", "table1"));
        assertNotNull(bigQuery.getTable("test_query_dataset", "table2"));
        assertNotNull(bigQuery.getTable("test_query_dataset", "table3"));

        // Verify schemas
        assertEquals(
                bigQuery.getTable("test_query_dataset", "table1")
                        .getDefinition()
                        .getSchema()
                        .getFields()
                        .size(),
                1);
        assertEquals(
                bigQuery.getTable("test_query_dataset", "table2")
                        .getDefinition()
                        .getSchema()
                        .getFields()
                        .size(),
                2);
        assertEquals(
                bigQuery.getTable("test_query_dataset", "table3")
                        .getDefinition()
                        .getSchema()
                        .getFields()
                        .size(),
                3);
    }

    @Test
    public void testExecuteCreateTableWithDefaultValues() throws InterruptedException {
        // Given: a CREATE TABLE query with DEFAULT values
        ExecuteStatement action = new ExecuteStatement();
        action.setStatement(
                "CREATE TABLE test_query_dataset.with_defaults ("
                        + "  id INT64,"
                        + "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP()"
                        + ")");

        // When: executing the query
        action.execute(bigQuery, mockConfiguration);

        // Then: table should be created
        Table table = bigQuery.getTable("test_query_dataset", "with_defaults");
        assertNotNull(table);
    }
}
