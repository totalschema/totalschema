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

package io.github.totalschema.connector.jdbc;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.ChangeExecutorConnector;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.engine.core.command.api.ObjectsContext;
import io.github.totalschema.jdbc.JdbcDatabase;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.spi.database.sql.SQLDatabase;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JDBC connector for executing SQL and other database scripts.
 *
 * <p>Supports any JDBC-compliant database including PostgreSQL, MySQL, Oracle, H2, SQL Server,
 * BigQuery, and others.
 */
public class JdbcConnector extends ChangeExecutorConnector {

    public static final String CONNECTOR_TYPE = "jdbc";

    private static final Logger logger = LoggerFactory.getLogger(JdbcConnector.class);

    public JdbcConnector(String name, Configuration connectorConfiguration) {
        super(name, connectorConfiguration);
    }

    @Override
    public void checkConnection(Context context, List<ChangeFile.Id> plannedChangeFileIds)
            throws InterruptedException {
        // Acquiring the JdbcDatabase initialises the HikariCP connection pool and validates
        // connectivity (DefaultJdbcDatabase.init() calls Connection.isValid()). The instance is
        // cached by the context so the subsequent execute() calls reuse the same pool.
        logger.info("[{}] Verifying JDBC connectivity", name);
        try {

            SQLDatabase sqlDatabase =
                    context.get(SQLDatabase.class, CONNECTOR_TYPE, name, connectorConfiguration);
            requireJdbcDatabase(sqlDatabase);

        } catch (RuntimeException ex) {
            logger.error("[{}] JDBC connectivity check FAILED", name, ex);
            throw new JdbcConnectorException("JDBC connectivity check failed", ex);
        }

        logger.info("[{}] JDBC connection verified", name);
    }

    @Override
    protected Context getExecutionContext(Context parentContext) {

        SQLDatabase sqlDatabase =
                parentContext.get(SQLDatabase.class, CONNECTOR_TYPE, name, connectorConfiguration);

        JdbcDatabase jdbcDatabase = requireJdbcDatabase(sqlDatabase);

        DataSource dataSource = jdbcDatabase.getDataSource();

        return ObjectsContext.builder()
                .parentContext(parentContext)
                .setValue(JdbcDatabase.class, jdbcDatabase)
                .setValue(SQLDatabase.class, jdbcDatabase)
                .setValue(DataSource.class, dataSource)
                .build();
    }

    @Override
    protected Map<String, Object> getBindings(ChangeFile changeFile, Context context) {

        // Start with default bindings (configuration, environment)
        Map<String, Object> bindings = super.getBindings(changeFile, context);

        // Acquire the JdbcDatabase (cached by context so subsequent calls reuse it)
        SQLDatabase sqlDatabase =
                context.get(SQLDatabase.class, CONNECTOR_TYPE, name, connectorConfiguration);

        JdbcDatabase jdbcDatabase = requireJdbcDatabase(sqlDatabase);
        DataSource dataSource = jdbcDatabase.getDataSource();

        // Add JDBC-specific bindings
        bindings.put("jdbcDatabase", jdbcDatabase);
        bindings.put("dataSource", dataSource);

        return bindings;
    }

    private static JdbcDatabase requireJdbcDatabase(SQLDatabase sqlDatabase) {
        if (!(sqlDatabase instanceof JdbcDatabase)) {
            throw new IllegalStateException(
                    "This connector requires a JdbcDatabase SQLDatabase to be configured");
        }

        return (JdbcDatabase) sqlDatabase;
    }

    @Override
    public String toString() {
        return "JDBC Connector named '"
                + name
                + "'{"
                + " configuration='"
                + connectorConfiguration
                + '\''
                + '}';
    }
}
