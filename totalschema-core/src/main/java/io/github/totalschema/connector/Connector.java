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

import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.model.ChangeFile;
import java.io.Closeable;

/**
 * Base class for all connectors in TotalSchema.
 *
 * <p>A connector represents a connection mechanism to interact with external systems (databases,
 * remote servers, local shell, etc.) and execute change scripts against them.
 *
 * <p>Connectors are configured in the configuration file under the "connectors" section and are
 * referenced by name in change file names.
 */
public abstract class Connector implements Closeable {

    @Override
    public abstract String toString();

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
