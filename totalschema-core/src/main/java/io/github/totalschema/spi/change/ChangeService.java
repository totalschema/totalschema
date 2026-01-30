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

package io.github.totalschema.spi.change;

import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.model.ChangeFile;

/**
 * Service interface for executing changes to connectors. Implementations route changes to the
 * appropriate connector based on environment and configuration.
 */
public interface ChangeService {
    /**
     * Executes a change file against the target connector.
     *
     * @param changeFile the change file to apply
     * @param context the command context containing configuration and services
     * @throws InterruptedException if the operation is interrupted
     */
    void execute(ChangeFile changeFile, CommandContext context) throws InterruptedException;
}
