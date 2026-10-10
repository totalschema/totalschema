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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

import com.google.cloud.bigquery.*;
import io.github.totalschema.config.Configuration;
import java.sql.SQLException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Tests for {@link BigQuerySQLDatabase}. */
public class BigQuerySQLDatabaseTest {

    private Configuration mockConfiguration;
    private BigQuery mockBigQuery;

    @BeforeMethod
    public void setUp() {
        mockConfiguration = createMock(Configuration.class);
        mockBigQuery = createMock(BigQuery.class);

        expect(mockConfiguration.getString("defaultDataset"))
                .andReturn(java.util.Optional.empty())
                .anyTimes();

        expect(mockConfiguration.getBoolean("logSql"))
                .andReturn(java.util.Optional.empty())
                .anyTimes();
    }

    @Test
    public void testExecuteSqlSuccess() throws InterruptedException, SQLException {
        String sql = "CREATE TABLE users (id INT64, name STRING)";

        BigQueryOptions mockOptions = createMock(BigQueryOptions.class);
        Job mockJob = createMock(Job.class);
        JobStatus mockStatus = createMock(JobStatus.class);

        expect(mockBigQuery.getOptions()).andReturn(mockOptions).anyTimes();
        expect(mockOptions.getProjectId()).andReturn("test-project").anyTimes();
        expect(mockBigQuery.create(anyObject(JobInfo.class))).andReturn(mockJob);
        expect(mockJob.waitFor()).andReturn(mockJob);
        expect(mockJob.getStatus()).andReturn(mockStatus).anyTimes();
        expect(mockStatus.getError()).andReturn(null);

        replay(mockConfiguration, mockBigQuery, mockOptions, mockJob, mockStatus);

        BigQuerySQLDatabase sqlDatabase =
                new BigQuerySQLDatabase("test-bigquery", mockBigQuery, mockConfiguration);
        sqlDatabase.execute(sql);

        verify(mockBigQuery, mockJob, mockStatus);
    }

    @Test
    public void testExecuteSqlWithDefaultDataset() throws InterruptedException, SQLException {
        String sql = "CREATE TABLE users (id INT64, name STRING)";

        Configuration configWithDataset = createMock(Configuration.class);
        expect(configWithDataset.getString("defaultDataset"))
                .andReturn(java.util.Optional.of("my_dataset"))
                .anyTimes();
        expect(configWithDataset.getBoolean("logSql"))
                .andReturn(java.util.Optional.empty())
                .anyTimes();

        BigQueryOptions mockOptions = createMock(BigQueryOptions.class);
        Job mockJob = createMock(Job.class);
        JobStatus mockStatus = createMock(JobStatus.class);

        expect(mockBigQuery.getOptions()).andReturn(mockOptions).anyTimes();
        expect(mockOptions.getProjectId()).andReturn("test-project").anyTimes();
        expect(mockBigQuery.create(anyObject(JobInfo.class)))
                .andAnswer(
                        () -> {
                            JobInfo jobInfo = (JobInfo) getCurrentArguments()[0];
                            QueryJobConfiguration queryConfig = jobInfo.getConfiguration();
                            assertNotNull(queryConfig.getDefaultDataset());
                            assertEquals(
                                    queryConfig.getDefaultDataset().getDataset(), "my_dataset");
                            return mockJob;
                        });
        expect(mockJob.waitFor()).andReturn(mockJob);
        expect(mockJob.getStatus()).andReturn(mockStatus).anyTimes();
        expect(mockStatus.getError()).andReturn(null);

        replay(configWithDataset, mockBigQuery, mockOptions, mockJob, mockStatus);

        BigQuerySQLDatabase sqlDatabase =
                new BigQuerySQLDatabase("test-bigquery", mockBigQuery, configWithDataset);
        sqlDatabase.execute(sql);

        verify(mockBigQuery, mockJob, mockStatus);
    }

    @Test(expectedExceptions = SQLException.class)
    public void testExecuteSqlFailure() throws InterruptedException, SQLException {
        String sql = "INVALID SQL SYNTAX";

        BigQueryOptions mockOptions = createMock(BigQueryOptions.class);
        Job mockJob = createMock(Job.class);
        JobStatus mockStatus = createMock(JobStatus.class);
        BigQueryError mockError = mock(BigQueryError.class);
        when(mockError.toString()).thenReturn("Syntax error");

        expect(mockBigQuery.getOptions()).andReturn(mockOptions).anyTimes();
        expect(mockOptions.getProjectId()).andReturn("test-project").anyTimes();
        expect(mockBigQuery.create(anyObject(JobInfo.class))).andReturn(mockJob);
        expect(mockJob.waitFor()).andReturn(mockJob);
        expect(mockJob.getStatus()).andReturn(mockStatus).anyTimes();
        expect(mockStatus.getError()).andReturn(mockError).anyTimes();

        replay(mockConfiguration, mockBigQuery, mockOptions, mockJob, mockStatus);

        BigQuerySQLDatabase sqlDatabase =
                new BigQuerySQLDatabase("test-bigquery", mockBigQuery, mockConfiguration);
        sqlDatabase.execute(sql);
    }

    @Test
    public void testExecuteMultilineSql() throws InterruptedException, SQLException {
        String sql =
                "CREATE TABLE users (\n"
                        + "  id INT64 NOT NULL,\n"
                        + "  name STRING,\n"
                        + "  email STRING,\n"
                        + "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP()\n"
                        + ")";

        BigQueryOptions mockOptions = createMock(BigQueryOptions.class);
        Job mockJob = createMock(Job.class);
        JobStatus mockStatus = createMock(JobStatus.class);

        expect(mockBigQuery.getOptions()).andReturn(mockOptions).anyTimes();
        expect(mockOptions.getProjectId()).andReturn("test-project").anyTimes();
        expect(mockBigQuery.create(anyObject(JobInfo.class)))
                .andAnswer(
                        () -> {
                            JobInfo jobInfo = (JobInfo) getCurrentArguments()[0];
                            QueryJobConfiguration queryConfig = jobInfo.getConfiguration();
                            assertTrue(queryConfig.getQuery().contains("\n"));
                            return mockJob;
                        });
        expect(mockJob.waitFor()).andReturn(mockJob);
        expect(mockJob.getStatus()).andReturn(mockStatus).anyTimes();
        expect(mockStatus.getError()).andReturn(null);

        replay(mockConfiguration, mockBigQuery, mockOptions, mockJob, mockStatus);

        BigQuerySQLDatabase sqlDatabase =
                new BigQuerySQLDatabase("test-bigquery", mockBigQuery, mockConfiguration);
        sqlDatabase.execute(sql);

        verify(mockBigQuery, mockJob, mockStatus);
    }

    @Test
    public void testExecuteSqlUsesStandardSql() throws InterruptedException, SQLException {
        String sql = "SELECT * FROM users";

        BigQueryOptions mockOptions = createMock(BigQueryOptions.class);
        Job mockJob = createMock(Job.class);
        JobStatus mockStatus = createMock(JobStatus.class);

        expect(mockBigQuery.getOptions()).andReturn(mockOptions).anyTimes();
        expect(mockOptions.getProjectId()).andReturn("test-project").anyTimes();
        expect(mockBigQuery.create(anyObject(JobInfo.class)))
                .andAnswer(
                        () -> {
                            JobInfo jobInfo = (JobInfo) getCurrentArguments()[0];
                            QueryJobConfiguration queryConfig = jobInfo.getConfiguration();
                            assertFalse(queryConfig.useLegacySql());
                            return mockJob;
                        });
        expect(mockJob.waitFor()).andReturn(mockJob);
        expect(mockJob.getStatus()).andReturn(mockStatus).anyTimes();
        expect(mockStatus.getError()).andReturn(null);

        replay(mockConfiguration, mockBigQuery, mockOptions, mockJob, mockStatus);

        BigQuerySQLDatabase sqlDatabase =
                new BigQuerySQLDatabase("test-bigquery", mockBigQuery, mockConfiguration);
        sqlDatabase.execute(sql);

        verify(mockBigQuery, mockJob, mockStatus);
    }

    @Test
    public void testExecuteThrowsWhenJobMissing() throws InterruptedException {
        String sql = "SELECT 1";

        BigQueryOptions mockOptions = createMock(BigQueryOptions.class);
        Job mockJob = createMock(Job.class);

        expect(mockBigQuery.getOptions()).andReturn(mockOptions).anyTimes();
        expect(mockOptions.getProjectId()).andReturn("test-project").anyTimes();
        expect(mockBigQuery.create(anyObject(JobInfo.class))).andReturn(mockJob);
        expect(mockJob.waitFor()).andReturn(null);

        replay(mockConfiguration, mockBigQuery, mockOptions, mockJob);

        BigQuerySQLDatabase sqlDatabase =
                new BigQuerySQLDatabase("test-bigquery", mockBigQuery, mockConfiguration);

        SQLException ex = expectThrows(SQLException.class, () -> sqlDatabase.execute(sql));
        assertTrue(ex.getMessage().contains("no longer exists"));
    }
}
