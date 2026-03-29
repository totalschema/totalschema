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

package io.github.totalschema.engine.services.sql;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.spi.sql.SqlDialect;
import java.util.Optional;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CreateTableBuilderTest {

    private SqlDialect mockDialect;
    private Configuration mockConfiguration;

    @BeforeMethod
    public void setUp() {
        mockDialect = createMock(SqlDialect.class);
        mockConfiguration = createMock(Configuration.class);
    }

    @Test
    public void testBasicTableCreation() {
        // Given
        expect(mockDialect.varchar(255)).andReturn("VARCHAR(255)");
        expect(mockDialect.timestamp()).andReturn("TIMESTAMP");
        expect(mockConfiguration.getString("table.columns.id.type")).andReturn(Optional.empty());
        expect(mockConfiguration.getString("table.columns.created_at.type"))
                .andReturn(Optional.empty());
        expect(mockConfiguration.getBoolean("table.primaryKeyClause.omit"))
                .andReturn(Optional.empty());
        expect(mockConfiguration.getString("table.primaryKeyClause.definition"))
                .andReturn(Optional.empty());
        replay(mockDialect, mockConfiguration);

        // When
        String sql =
                CreateTableBuilder.create(mockDialect, "users", mockConfiguration)
                        .column("id", d -> d.varchar(255))
                        .column("created_at", SqlDialect::timestamp)
                        .primaryKey("id")
                        .build();

        // Then
        assertEquals(
                sql, "CREATE TABLE users (id VARCHAR(255), created_at TIMESTAMP, PRIMARY KEY(id))");
        verify(mockDialect, mockConfiguration);
    }

    @Test
    public void testMultipleColumnsWithCompositePrimaryKey() {
        // Given
        expect(mockDialect.varchar(100)).andReturn("VARCHAR(100)").times(2);
        expect(mockDialect.varchar(255)).andReturn("VARCHAR(255)");
        expect(mockDialect.timestamp()).andReturn("TIMESTAMP");
        expect(mockConfiguration.getString("table.columns.tenant_id.type"))
                .andReturn(Optional.empty());
        expect(mockConfiguration.getString("table.columns.user_id.type"))
                .andReturn(Optional.empty());
        expect(mockConfiguration.getString("table.columns.email.type")).andReturn(Optional.empty());
        expect(mockConfiguration.getString("table.columns.created_at.type"))
                .andReturn(Optional.empty());
        expect(mockConfiguration.getBoolean("table.primaryKeyClause.omit"))
                .andReturn(Optional.empty());
        expect(mockConfiguration.getString("table.primaryKeyClause.definition"))
                .andReturn(Optional.empty());
        replay(mockDialect, mockConfiguration);

        // When
        String sql =
                CreateTableBuilder.create(mockDialect, "users", mockConfiguration)
                        .column("tenant_id", d -> d.varchar(100))
                        .column("user_id", d -> d.varchar(100))
                        .column("email", d -> d.varchar(255))
                        .column("created_at", SqlDialect::timestamp)
                        .primaryKey("tenant_id", "user_id")
                        .build();

        // Then
        assertEquals(
                sql,
                "CREATE TABLE users (tenant_id VARCHAR(100), user_id VARCHAR(100), email VARCHAR(255), created_at TIMESTAMP, PRIMARY KEY(tenant_id, user_id))");
        verify(mockDialect, mockConfiguration);
    }

    @Test
    public void testColumnTypeOverride() {
        // Given
        expect(mockDialect.varchar(255)).andReturn("VARCHAR(255)").times(2);
        expect(mockConfiguration.getString("table.columns.id.type"))
                .andReturn(Optional.of("CHAR(36)"));
        expect(mockConfiguration.getString("table.columns.name.type")).andReturn(Optional.empty());
        expect(mockConfiguration.getBoolean("table.primaryKeyClause.omit"))
                .andReturn(Optional.empty());
        expect(mockConfiguration.getString("table.primaryKeyClause.definition"))
                .andReturn(Optional.empty());
        replay(mockDialect, mockConfiguration);

        // When
        String sql =
                CreateTableBuilder.create(mockDialect, "items", mockConfiguration)
                        .column("id", d -> d.varchar(255))
                        .column("name", d -> d.varchar(255))
                        .primaryKey("id")
                        .build();

        // Then
        assertEquals(sql, "CREATE TABLE items (id CHAR(36), name VARCHAR(255), PRIMARY KEY(id))");
        verify(mockDialect, mockConfiguration);
    }

    @Test
    public void testPrimaryKeyOverride() {
        // Given
        expect(mockDialect.varchar(255)).andReturn("VARCHAR(255)");
        expect(mockConfiguration.getString("table.columns.id.type")).andReturn(Optional.empty());
        expect(mockConfiguration.getBoolean("table.primaryKeyClause.omit"))
                .andReturn(Optional.empty());
        expect(mockConfiguration.getString("table.primaryKeyClause.definition"))
                .andReturn(Optional.of("CONSTRAINT pk_custom PRIMARY KEY (id)"));
        replay(mockDialect, mockConfiguration);

        // When
        String sql =
                CreateTableBuilder.create(mockDialect, "products", mockConfiguration)
                        .column("id", d -> d.varchar(255))
                        .primaryKey("id")
                        .build();

        // Then
        assertEquals(
                sql,
                "CREATE TABLE products (id VARCHAR(255), CONSTRAINT pk_custom PRIMARY KEY (id))");
        verify(mockDialect, mockConfiguration);
    }

    @Test
    public void testOmitPrimaryKey() {
        // Given
        expect(mockDialect.varchar(255)).andReturn("VARCHAR(255)");
        expect(mockConfiguration.getString("table.columns.id.type")).andReturn(Optional.empty());
        expect(mockConfiguration.getBoolean("table.primaryKeyClause.omit"))
                .andReturn(Optional.of(true));
        replay(mockDialect, mockConfiguration);

        // When
        String sql =
                CreateTableBuilder.create(mockDialect, "logs", mockConfiguration)
                        .column("id", d -> d.varchar(255))
                        .primaryKey("id")
                        .build();

        // Then
        assertEquals(sql, "CREATE TABLE logs (id VARCHAR(255))");
        verify(mockDialect, mockConfiguration);
    }

    @Test
    public void testTableWithNoPrimaryKey() {
        // Given
        expect(mockDialect.varchar(255)).andReturn("VARCHAR(255)");
        expect(mockDialect.timestamp()).andReturn("TIMESTAMP");
        expect(mockConfiguration.getString("table.columns.message.type"))
                .andReturn(Optional.empty());
        expect(mockConfiguration.getString("table.columns.timestamp.type"))
                .andReturn(Optional.empty());
        replay(mockDialect, mockConfiguration);

        // When
        String sql =
                CreateTableBuilder.create(mockDialect, "audit_log", mockConfiguration)
                        .column("message", d -> d.varchar(255))
                        .column("timestamp", SqlDialect::timestamp)
                        .build();

        // Then
        assertEquals(sql, "CREATE TABLE audit_log (message VARCHAR(255), timestamp TIMESTAMP)");
        verify(mockDialect, mockConfiguration);
    }

    @Test
    public void testQualifiedTableName() {
        // Given
        expect(mockDialect.varchar(255)).andReturn("VARCHAR(255)");
        expect(mockConfiguration.getString("table.columns.id.type")).andReturn(Optional.empty());
        expect(mockConfiguration.getBoolean("table.primaryKeyClause.omit"))
                .andReturn(Optional.empty());
        expect(mockConfiguration.getString("table.primaryKeyClause.definition"))
                .andReturn(Optional.empty());
        replay(mockDialect, mockConfiguration);

        // When
        String sql =
                CreateTableBuilder.create(mockDialect, "myschema.mytable", mockConfiguration)
                        .column("id", d -> d.varchar(255))
                        .primaryKey("id")
                        .build();

        // Then
        assertEquals(sql, "CREATE TABLE myschema.mytable (id VARCHAR(255), PRIMARY KEY(id))");
        verify(mockDialect, mockConfiguration);
    }

    @Test
    public void testDirectColumnTypeExpression() {
        // Given
        expect(mockConfiguration.getString("table.columns.id.type")).andReturn(Optional.empty());
        expect(mockConfiguration.getString("table.columns.data.type")).andReturn(Optional.empty());
        expect(mockConfiguration.getBoolean("table.primaryKeyClause.omit"))
                .andReturn(Optional.empty());
        expect(mockConfiguration.getString("table.primaryKeyClause.definition"))
                .andReturn(Optional.empty());
        replay(mockDialect, mockConfiguration);

        // When
        String sql =
                CreateTableBuilder.create(mockDialect, "custom", mockConfiguration)
                        .column("id", "BIGINT AUTO_INCREMENT")
                        .column("data", "JSON")
                        .primaryKey("id")
                        .build();

        // Then
        assertEquals(
                sql, "CREATE TABLE custom (id BIGINT AUTO_INCREMENT, data JSON, PRIMARY KEY(id))");
        verify(mockDialect, mockConfiguration);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testBuildWithoutColumns() {
        // Given
        replay(mockDialect, mockConfiguration);

        // When/Then
        CreateTableBuilder.create(mockDialect, "empty", mockConfiguration).build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testPrimaryKeyWithNoColumns() {
        // Given
        replay(mockDialect, mockConfiguration);

        // When/Then
        CreateTableBuilder.create(mockDialect, "test", mockConfiguration).primaryKey();
    }

    @Test
    public void testIntegrationWithStaticFactoryMethod() {
        // Given
        expect(mockDialect.varchar(255)).andReturn("VARCHAR(255)");
        expect(mockConfiguration.getString("table.columns.id.type")).andReturn(Optional.empty());
        expect(mockConfiguration.getBoolean("table.primaryKeyClause.omit"))
                .andReturn(Optional.empty());
        expect(mockConfiguration.getString("table.primaryKeyClause.definition"))
                .andReturn(Optional.empty());
        replay(mockDialect, mockConfiguration);

        // When
        String sql =
                CreateTableBuilder.create(mockDialect, "test_table", mockConfiguration)
                        .column("id", d -> d.varchar(255))
                        .primaryKey("id")
                        .build();

        // Then
        assertEquals(sql, "CREATE TABLE test_table (id VARCHAR(255), PRIMARY KEY(id))");
        verify(mockDialect, mockConfiguration);
    }
}
