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

package io.github.totalschema.spi.database.sql;

import static java.util.Objects.requireNonNull;

import io.github.totalschema.config.Configuration;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for {@link SQLDatabase} implementations, providing shared support for instance naming
 * and configurable SQL statement logging.
 *
 * <p>Concrete subclasses (e.g. a JDBC-backed implementation, or a BigQuery-backed implementation)
 * typically differ only in how they physically connect to and execute statements against their
 * target database. The bookkeeping around naming the instance (for diagnostic log messages) and
 * deciding whether/how to log the SQL statements and parameters being executed is identical across
 * implementations, so it is centralized here to avoid duplication.
 *
 * <h2>Configuration</h2>
 *
 * <ul>
 *   <li><b>logSql</b> (optional, default: {@code true}): whether SQL statements and parameters
 *       should be logged at {@code INFO} level before execution.
 * </ul>
 *
 * <p>Subclasses should call {@link #logStatement(String, String, Parameter[])} immediately before
 * executing a SQL statement, in each of their {@link SQLDatabase} method implementations.
 */
public abstract class AbstractSQLDatabase implements SQLDatabase {

    /** Logger instance for the concrete subclass, resolved via {@link #getClass()}. */
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /** The name of this database instance, used to identify it in log messages. */
    protected final String name;

    private final boolean logSql;

    /**
     * Creates a new instance.
     *
     * @param name the name of this database instance, used to identify it in log messages; must not
     *     be null
     * @param configuration the configuration to read the {@code logSql} setting from; must not be
     *     null
     */
    protected AbstractSQLDatabase(String name, Configuration configuration) {
        this.name = requireNonNull(name, "name must not be null");
        requireNonNull(configuration, "configuration must not be null");

        this.logSql = configuration.getBoolean("logSql").orElse(true);
    }

    /**
     * Returns the name of this database instance.
     *
     * @return the name of this database instance (never {@code null})
     */
    protected final String getName() {
        return name;
    }

    /**
     * Returns whether SQL statement logging is enabled for this database instance.
     *
     * @return {@code true} if SQL statement logging is enabled, {@code false} otherwise
     */
    protected final boolean isLogSqlEnabled() {
        return logSql;
    }

    /**
     * Logs the given SQL statement and its parameters at {@code INFO} level, provided SQL statement
     * logging is enabled for this database instance.
     *
     * <p>Subclasses should call this method immediately before executing a SQL statement.
     *
     * @param operationType a short description of the kind of operation being performed (e.g.
     *     {@code "execute"}, {@code "executeUpdate"}, {@code "query"}), used to identify the
     *     operation in the log message
     * @param sql the SQL statement being executed
     * @param parameters the parameters bound to the SQL statement, may be null or empty
     */
    protected final void logStatement(
            String operationType, String sql, Parameter<?>... parameters) {

        if (logSql && log.isInfoEnabled()) {

            log.info("[{}] database: {} SQL: {}", name, operationType, sql);

            if (parameters != null && parameters.length > 0) {

                String parameterString =
                        Stream.of(parameters)
                                .map(Parameter::toString)
                                .collect(Collectors.joining(", "));

                log.info("[{}] database: {} parameters: {}", name, operationType, parameterString);
            }
        }
    }
}
