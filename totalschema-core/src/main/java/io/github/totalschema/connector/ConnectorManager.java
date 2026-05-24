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
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.spi.ServiceLoaderFactory;
import java.util.List;

/** Manager for creating and caching connector instances. */
public interface ConnectorManager {

    static ConnectorManager getInstance() {
        return ServiceLoaderFactory.getSingleService(ConnectorManager.class)
                .orElseGet(DefaultConnectorManager::new);
    }

    /**
     * Get or create a connector by name.
     *
     * @param name the connector name from configuration
     * @param context the command context
     * @return the connector instance
     */
    Connector getConnectorByName(String name, Context context);

    /**
     * Validates that a named connector exists and is correctly configured, then — unless the
     * connection check has been disabled for that connector via {@code connectionCheck.enabled:
     * false} in {@code totalschema.yml} — verifies that the connector can actually reach its target
     * system.
     *
     * <p>Intended for pre-flight checks: call this for each connector before executing any change
     * files to surface configuration or connectivity problems early, before any changes are
     * applied.
     *
     * @param name the connector name from configuration
     * @param context the command context
     * @param plannedChangeFileIds the list of change file IDs that are planned to be executed using
     *     this connector
     */
    void checkConnector(String name, Context context, List<ChangeFile.Id> plannedChangeFileIds)
            throws InterruptedException;
}
