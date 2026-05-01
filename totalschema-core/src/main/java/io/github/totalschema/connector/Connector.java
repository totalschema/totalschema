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

package io.github.totalschema.connector;

import io.github.totalschema.engine.api.Context;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.model.ChangeFile;
import java.util.List;

/**
 * Base class for all connectors in TotalSchema.
 *
 * <p>A connector represents a connection mechanism to interact with external systems (databases,
 * remote servers, local shell, etc.) and execute change scripts against them.
 *
 * <p>Connectors are configured in the configuration file under the "connectors" section and are
 * referenced by name in change file names.
 */
public abstract class Connector {

    /**
     * Returns a human-readable representation of the connector.
     *
     * @return a string representation of the connector
     */
    @Override
    public abstract String toString();

    /**
     * Checks whether this connector can successfully reach its target system.
     *
     * <p>Connectors that can meaningfully test reachability (e.g. open a JDBC connection, attempt a
     * trivial SSH command, verify an interpreter is on the PATH) should implement this method and
     * throw a {@link RuntimeException} when the target cannot be reached.
     *
     * <p>The check can be disabled per connector via {@code connectionCheck.enabled: false} in
     * {@code totalschema.yml}.
     *
     * @param context the current command context, available for IoC-managed resources (e.g. {@link
     *     io.github.totalschema.jdbc.JdbcDatabase})
     * @param plannedChangeFileIds the list of change file IDs that are planned to be executed using
     *     this connector; provided as context for pre-flight validation
     */
    public abstract void checkConnection(Context context, List<ChangeFile.Id> plannedChangeFileIds)
            throws InterruptedException;

    /**
     * Execute a change file against this connector's target system.
     *
     * @param changeFile the change file to execute
     * @param context the command context
     * @throws InterruptedException if execution is interrupted
     */
    public abstract void execute(ChangeFile changeFile, CommandContext context)
            throws InterruptedException;
}
