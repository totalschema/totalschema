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

import com.google.cloud.bigquery.BigQuery;
import io.github.totalschema.config.Configuration;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Abstract base class for BigQuery integration tests that provides a fully configured BigQuery
 * client connected to the BigQuery emulator.
 *
 * <p>This class simplifies writing integration tests by handling all the boilerplate setup:
 *
 * <ul>
 *   <li>Starts the BigQuery emulator in a TestContainer (via {@link
 *       BigQueryEmulatorIntegrationTestBase})
 *   <li>Creates and configures a {@link BigQuery} client pointing to the emulator
 *   <li>Manages the complete test lifecycle (setup, execution, teardown)
 *   <li>Automatically skips tests if Docker/Podman is not available
 *   <li>Cleans up all resources after tests complete
 * </ul>
 *
 * <p><strong>When to Use:</strong> Extend this class when you need to test code that interacts with
 * BigQuery APIs (datasets, tables, queries, jobs). The {@code bigQuery} field provides immediate
 * access to a working BigQuery client.
 *
 * <p><strong>Test Lifecycle:</strong>
 *
 * <ol>
 *   <li>{@code @BeforeClass}: Emulator container starts and {@code bigQuery} client is created
 *   <li>{@code @Test}: Your test methods execute using the {@code bigQuery} field
 *   <li>{@code @AfterClass}: All resources (client and container) are automatically cleaned up
 * </ol>
 *
 * <p><strong>Available Fields:</strong>
 *
 * <ul>
 *   <li>{@link #bigQuery} - Fully configured BigQuery client (ready to use in test methods)
 *   <li>{@link #TEST_PROJECT_ID} - The test project ID (inherited from parent, value:
 *       "test-project")
 *   <li>{@link #bigQueryContainer} - The TestContainer running the emulator (inherited from parent)
 * </ul>
 *
 * <p><strong>Requirements:</strong> Tests require Docker or Podman to be installed and running.
 * Tests are automatically skipped with a {@code SkipException} if container support is unavailable.
 *
 * <p><strong>Emulator Limitations:</strong> The BigQuery emulator may not support all BigQuery
 * features. Check the <a href="https://github.com/goccy/bigquery-emulator">goccy/bigquery-emulator
 * documentation</a> for current limitations.
 *
 * <p><strong>Usage Example:</strong>
 *
 * <pre>{@code
 * public class MyBigQueryTest extends AbstractBigQueryIntegrationTest {
 *
 *     @Test
 *     public void testCreateAndQueryDataset() {
 *         // Create a dataset using the inherited bigQuery client
 *         DatasetInfo datasetInfo = DatasetInfo.newBuilder("my_dataset")
 *                 .setDescription("Test dataset")
 *                 .build();
 *         Dataset dataset = bigQuery.create(datasetInfo);
 *
 *         // Verify dataset was created
 *         assertNotNull(dataset);
 *         assertEquals(dataset.getDatasetId().getDataset(), "my_dataset");
 *
 *         // The TEST_PROJECT_ID constant is available
 *         assertEquals(dataset.getDatasetId().getProject(), TEST_PROJECT_ID);
 *     }
 *
 *     @Test
 *     public void testExecuteQuery() {
 *         // You can execute queries directly
 *         QueryJobConfiguration queryConfig = QueryJobConfiguration
 *                 .newBuilder("SELECT 1 as value")
 *                 .build();
 *         TableResult result = bigQuery.query(queryConfig);
 *
 *         // Verify query results
 *         assertTrue(result.iterateAll().iterator().hasNext());
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Architecture:</strong> This class follows the Template Method pattern. The parent
 * class {@link BigQueryEmulatorIntegrationTestBase} manages the emulator container lifecycle and
 * calls template methods {@link #createClients} and {@link #getResourcesToTearDown} to allow
 * subclasses to customize client creation and cleanup.
 *
 * <p><strong>Thread Safety:</strong> Each test class instance gets its own emulator container and
 * client. The {@code @BeforeClass} and {@code @AfterClass} annotations ensure proper
 * single-threaded setup and teardown per test class.
 *
 * @see BigQueryEmulatorIntegrationTestBase
 * @see BigQueryEmulatorClientsFactory
 * @see BigQuery
 */
public abstract class AbstractBigQueryIntegrationTest extends BigQueryEmulatorIntegrationTestBase {

    /**
     * BigQuery client configured for the emulator.
     *
     * <p>This client is fully configured and ready to use in test methods. It is:
     *
     * <ul>
     *   <li>Created during {@code @BeforeClass} setup
     *   <li>Connected to the BigQuery emulator (not production GCP)
     *   <li>Configured with no authentication (emulator doesn't require credentials)
     *   <li>Using HTTP connections (not HTTPS)
     *   <li>Automatically closed during {@code @AfterClass} teardown
     * </ul>
     *
     * <p><strong>Usage:</strong> Access this field directly in any {@code @Test} method to interact
     * with BigQuery:
     *
     * <pre>{@code
     * @Test
     * public void testDatasetOperations() {
     *     // Create a dataset
     *     DatasetInfo info = DatasetInfo.newBuilder("test_ds").build();
     *     Dataset dataset = bigQuery.create(info);
     *
     *     // Execute queries
     *     TableResult result = bigQuery.query(QueryJobConfiguration.of("SELECT 1"));
     *
     *     // Manage tables, jobs, etc.
     * }
     * }</pre>
     *
     * @see BigQuery
     * @see BigQueryEmulatorClientsFactory#createBigQueryClient(String, String)
     */
    protected BigQuery bigQuery;

    /**
     * Mock Configuration for testing actions that require Configuration parameter.
     *
     * <p>This is a simple mock that returns Optional.empty() for any getString() call. Individual
     * tests can override this if they need specific configuration values.
     */
    protected Configuration mockConfiguration;

    /**
     * Creates the BigQuery client for testing.
     *
     * <p>This method is called by the parent class after the emulator container has started and is
     * ready to accept connections. It initializes the {@link #bigQuery} field using {@link
     * BigQueryEmulatorClientsFactory}.
     *
     * <p><strong>Lifecycle:</strong> This is invoked automatically during {@code @BeforeClass}
     * setup. Subclasses should not call this method directly.
     *
     * <p><strong>Implementation Note:</strong> The factory creates a client with:
     *
     * <ul>
     *   <li>No credentials (emulator doesn't require authentication)
     *   <li>HTTP transport (emulator doesn't support HTTPS)
     *   <li>Custom host pointing to the containerized emulator endpoint
     *   <li>60-second connect and read timeouts
     * </ul>
     *
     * @param projectId the test project ID (typically {@link #TEST_PROJECT_ID})
     * @param emulatorEndpoint the emulator HTTP endpoint (e.g., "http://localhost:9050")
     * @throws IOException if client creation fails
     * @see BigQueryEmulatorClientsFactory#createBigQueryClient(String, String)
     */
    @Override
    protected void createClients(String projectId, String emulatorEndpoint) throws IOException {
        bigQuery = BigQueryEmulatorClientsFactory.createBigQueryClient(projectId, emulatorEndpoint);

        // Create mock configuration that returns empty Optional for any getString() call
        mockConfiguration = createMock(Configuration.class);
        expect(mockConfiguration.getString(anyString())).andReturn(Optional.empty()).anyTimes();
        replay(mockConfiguration);
    }

    /**
     * Declares resources that need cleanup after tests complete.
     *
     * <p>This implementation returns only the BigQuery container. The parent class will
     * automatically close all resources in the returned list during {@code @AfterClass} teardown.
     *
     * <p><strong>Cleanup Order:</strong> Resources are closed in list order. In this
     * implementation:
     *
     * <ol>
     *   <li>{@link #bigQueryContainer} - Stops the emulator container
     * </ol>
     *
     * <p><strong>Note:</strong> The {@link #bigQuery} client does not need explicit cleanup because
     * it is automatically closed when the container stops.
     *
     * <p><strong>Exception Handling:</strong> The parent class ensures all cleanup attempts are
     * made even if some fail, collecting exceptions as suppressed exceptions.
     *
     * <p><strong>Lifecycle:</strong> This method is called automatically during {@code @AfterClass}
     * teardown. Subclasses should not invoke it directly.
     *
     * @return list of AutoCloseable resources to close during tearDown (currently just the
     *     container)
     * @see BigQueryEmulatorIntegrationTestBase#tearDown()
     */
    @Override
    protected List<AutoCloseable> getResourcesToTearDown() {
        return new LinkedList<>(List.of(bigQueryContainer));
    }
}
