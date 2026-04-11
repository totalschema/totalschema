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

package io.github.totalschema.spi.script;

import io.github.totalschema.engine.api.Context;

/**
 * Executes a script file against a JDBC database.
 *
 * <p>Implementations are discovered by the IoC container using the file extension as a qualifier.
 * {@link io.github.totalschema.connector.jdbc.JdbcConnector} acquires a {@link
 * io.github.totalschema.jdbc.JdbcDatabase}, places it in the {@link
 * io.github.totalschema.engine.api.Context}, and then dispatches to the matching executor.
 *
 * <p>Two implementations are provided out of the box:
 *
 * <ul>
 *   <li>{@code SqlScriptExecutor} (qualifier {@code "sql"}) — splits the script on a configurable
 *       separator and executes each statement via JDBC; optionally substitutes {@code ${varName}}
 *       placeholders.
 *   <li>{@code GroovyScriptExecutor} (qualifier {@code "groovy"}) — evaluates the script with a
 *       Groovy shell, injecting a {@code groovy.sql.Sql} binding for database access.
 * </ul>
 *
 * <p>To add support for a new scripting language, extend {@link
 * AbstractScriptExecutorComponentFactory} and register the factory via {@code
 * META-INF/services/io.github.totalschema.spi.factory.ComponentFactory}.
 */
public interface ScriptExecutor {

    /**
     * Executes the given script content.
     *
     * <p>When invoked from {@link io.github.totalschema.connector.jdbc.JdbcConnector}, the {@code
     * context} always contains a {@link io.github.totalschema.jdbc.JdbcDatabase} instance that can
     * be retrieved with {@code context.get(JdbcDatabase.class)}.
     *
     * @param script the full text of the script file to execute
     * @param context the command context; provides access to the {@code JdbcDatabase} and engine
     *     services
     * @throws InterruptedException if execution is interrupted
     */
    void execute(String script, Context context) throws InterruptedException;
}
