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

package io.github.totalschema.connector.gcp.bigquery;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import com.google.api.gax.paging.Page;
import com.google.cloud.bigquery.*;
import com.google.cloud.bigquery.dataexchange.v1beta1.AnalyticsHubServiceClient;
import com.google.cloud.bigquery.datatransfer.v1.DataTransferServiceClient;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.spi.change.executor.ChangeExecutor;
import io.github.totalschema.spi.database.sql.SQLDatabase;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Tests for {@link BigQueryConnector}. */
public class BigQueryConnectorTest {

    private Configuration mockConfiguration;
    private Context mockContext;
    private BigQuery mockBigQuery;
    private DataTransferServiceClient mockDataTransferServiceClient;
    private AnalyticsHubServiceClient mockAnalyticsHubServiceClient;

    @BeforeMethod
    public void setUp() {
        mockConfiguration = createMock(Configuration.class);
        mockContext = createMock(Context.class);
        mockBigQuery = createMock(BigQuery.class);
        mockDataTransferServiceClient = createMock(DataTransferServiceClient.class);
        mockAnalyticsHubServiceClient = createMock(AnalyticsHubServiceClient.class);

        // Default configuration behavior
        expect(mockConfiguration.getString(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(mockConfiguration.getBoolean(anyString())).andReturn(Optional.empty()).anyTimes();
    }

    @Test
    public void testConnectorConstruction() {
        // Given: a configuration
        replay(mockConfiguration);

        // When: creating a connector
        BigQueryConnector connector = new BigQueryConnector("mybigquery", mockConfiguration);

        // Then: connector should be created successfully
        assertNotNull(connector);
        assertEquals(connector.toString().contains("mybigquery"), true);
    }

    @Test
    public void testConnectorType() {
        // Then: connector type should be "bigquery"
        assertEquals(BigQueryConnector.CONNECTOR_TYPE, "bigquery");
    }

    @Test
    public void testCheckConnectionSuccess() {
        // Given: a working BigQuery connection
        Page<Dataset> mockPage = createMock(Page.class);

        // Parent class may call getOptional for various types
        expect(mockContext.getOptional(anyObject())).andReturn(Optional.empty()).anyTimes();

        expect(mockContext.get(eq(BigQuery.class), isNull(), eq(mockConfiguration)))
                .andReturn(mockBigQuery);
        expect(mockBigQuery.listDatasets()).andReturn(mockPage);
        expect(mockPage.iterateAll()).andReturn(Collections.emptyList());

        replay(mockConfiguration, mockContext, mockBigQuery, mockPage);

        BigQueryConnector connector = new BigQueryConnector("mybigquery", mockConfiguration);

        // When: checking connection
        connector.checkConnection(mockContext, Collections.emptyList());

        // Then: no exception should be thrown
        verify(mockContext, mockBigQuery, mockPage);
    }

    @Test(expectedExceptions = BigQueryConnectorException.class)
    public void testCheckConnectionFailure() {
        // Given: a failing BigQuery connection
        // Parent class may call getOptional for various types
        expect(mockContext.getOptional(anyObject())).andReturn(Optional.empty()).anyTimes();

        expect(mockContext.get(eq(BigQuery.class), isNull(), eq(mockConfiguration)))
                .andReturn(mockBigQuery);
        expect(mockBigQuery.listDatasets()).andThrow(new RuntimeException("Connection failed"));

        replay(mockConfiguration, mockContext, mockBigQuery);

        BigQueryConnector connector = new BigQueryConnector("mybigquery", mockConfiguration);

        // When/Then: checking connection should throw exception
        connector.checkConnection(mockContext, Collections.emptyList());
    }

    @Test
    public void testGetCustomChangeExecutorForYml() {
        // Given: a connector
        replay(mockConfiguration, mockContext);

        BigQueryConnector connector = new BigQueryConnector("mybigquery", mockConfiguration);

        // When: getting executor for yml files
        Optional<ChangeExecutor> executor = connector.getCustomChangeExecutor("yml", mockContext);

        // Then: should return YmlBigQueryChangeExecutor
        assertTrue(executor.isPresent());
        assertTrue(executor.get() instanceof YmlBigQueryChangeExecutor);
    }

    @Test
    public void testGetCustomChangeExecutorForYaml() {
        // Given: a connector
        replay(mockConfiguration, mockContext);

        BigQueryConnector connector = new BigQueryConnector("mybigquery", mockConfiguration);

        // When: getting executor for yaml files
        Optional<ChangeExecutor> executor = connector.getCustomChangeExecutor("yaml", mockContext);

        // Then: should return YmlBigQueryChangeExecutor
        assertTrue(executor.isPresent());
        assertTrue(executor.get() instanceof YmlBigQueryChangeExecutor);
    }

    @Test
    public void testGetCustomChangeExecutorForSql() {
        // Given: a connector
        replay(mockConfiguration, mockContext);

        BigQueryConnector connector = new BigQueryConnector("mybigquery", mockConfiguration);

        // When: getting executor for sql files
        Optional<ChangeExecutor> executor = connector.getCustomChangeExecutor("sql", mockContext);

        // Then: SQL is handled by shared SqlChangeExecutor via SQLDatabase binding
        assertFalse(executor.isPresent());
    }

    @Test
    public void testGetCustomChangeExecutorForGroovy() {
        // Given: a connector
        replay(mockConfiguration, mockContext);

        BigQueryConnector connector = new BigQueryConnector("mybigquery", mockConfiguration);

        // When: getting executor for groovy files (not custom)
        Optional<ChangeExecutor> executor =
                connector.getCustomChangeExecutor("groovy", mockContext);

        // Then: should return empty (handled by default mechanism)
        assertFalse(executor.isPresent());
    }

    @Test
    public void testGetCustomChangeExecutorCaseInsensitive() {
        // Given: a connector
        replay(mockConfiguration, mockContext);

        BigQueryConnector connector = new BigQueryConnector("mybigquery", mockConfiguration);

        // When: getting executor with different case
        Optional<ChangeExecutor> ymlExecutor =
                connector.getCustomChangeExecutor("YML", mockContext);
        Optional<ChangeExecutor> yamlExecutor =
                connector.getCustomChangeExecutor("YAML", mockContext);
        Optional<ChangeExecutor> sqlExecutor =
                connector.getCustomChangeExecutor("SQL", mockContext);

        // Then: YML/YAML should return executors, SQL should use shared path
        assertTrue(ymlExecutor.isPresent());
        assertTrue(yamlExecutor.isPresent());
        assertFalse(sqlExecutor.isPresent());
    }

    @Test
    public void testGetExecutionContext() {
        // Given: a connector with context
        // Parent class may call getOptional for various types
        expect(mockContext.getOptional(anyObject())).andReturn(Optional.empty()).anyTimes();

        // Expect all three GCP client retrievals
        expect(mockContext.get(eq(BigQuery.class), isNull(), eq(mockConfiguration)))
                .andReturn(mockBigQuery);
        expect(
                        mockContext.get(
                                eq(DataTransferServiceClient.class),
                                isNull(),
                                eq(mockConfiguration)))
                .andReturn(mockDataTransferServiceClient);
        expect(
                        mockContext.get(
                                eq(AnalyticsHubServiceClient.class),
                                isNull(),
                                eq(mockConfiguration)))
                .andReturn(mockAnalyticsHubServiceClient);

        replay(
                mockConfiguration,
                mockContext,
                mockBigQuery,
                mockDataTransferServiceClient,
                mockAnalyticsHubServiceClient);

        BigQueryConnector connector = new BigQueryConnector("mybigquery", mockConfiguration);

        // When: getting execution context
        Context executionContext = connector.getExecutionContext(mockContext);

        // Then: should have all GCP clients and SQLDatabase in context
        assertNotNull(executionContext);
        assertNotNull(executionContext.get(SQLDatabase.class));
        verify(
                mockContext,
                mockBigQuery,
                mockDataTransferServiceClient,
                mockAnalyticsHubServiceClient);
    }

    @Test
    public void testGetBindings() {
        // Given: a connector with change file
        ChangeFile mockChangeFile = createMock(ChangeFile.class);

        // Parent class ChangeExecutorConnector.getBindings() calls getOptional(Environment.class)
        expect(mockContext.getOptional(anyObject())).andReturn(Optional.empty()).anyTimes();

        expect(mockContext.get(eq(BigQuery.class), isNull(), eq(mockConfiguration)))
                .andReturn(mockBigQuery);

        replay(mockConfiguration, mockContext, mockBigQuery, mockChangeFile);

        BigQueryConnector connector = new BigQueryConnector("mybigquery", mockConfiguration);

        // When: getting bindings
        Map<String, Object> bindings = connector.getBindings(mockChangeFile, mockContext);

        // Then: should have BigQuery client in bindings
        assertNotNull(bindings);
        assertTrue(bindings.containsKey("bigQuery"));
        assertEquals(bindings.get("bigQuery"), mockBigQuery);

        verify(mockContext, mockBigQuery);
    }

    @Test
    public void testToString() {
        // Given: a connector
        replay(mockConfiguration);

        BigQueryConnector connector = new BigQueryConnector("test-connector", mockConfiguration);

        // When: calling toString
        String result = connector.toString();

        // Then: should contain connector name and type
        assertTrue(result.contains("test-connector"));
        assertTrue(result.contains("BigQuery"));
    }

    @Test
    public void testYmlChangeExecutorSupportedTypes() {
        // Given/When/Then: verify supported change types
        assertTrue(YmlBigQueryChangeExecutor.isChangeTypeSupported("yml"));
        assertTrue(YmlBigQueryChangeExecutor.isChangeTypeSupported("yaml"));
        assertTrue(YmlBigQueryChangeExecutor.isChangeTypeSupported("YML"));
        assertTrue(YmlBigQueryChangeExecutor.isChangeTypeSupported("YAML"));
        assertFalse(YmlBigQueryChangeExecutor.isChangeTypeSupported("sql"));
        assertFalse(YmlBigQueryChangeExecutor.isChangeTypeSupported("groovy"));
    }

    @Test
    public void testSqlUsesSharedExecutorPath() {
        replay(mockConfiguration, mockContext);

        BigQueryConnector connector = new BigQueryConnector("mybigquery", mockConfiguration);

        Optional<ChangeExecutor> executor = connector.getCustomChangeExecutor("sql", mockContext);

        assertFalse(executor.isPresent());
    }
}
