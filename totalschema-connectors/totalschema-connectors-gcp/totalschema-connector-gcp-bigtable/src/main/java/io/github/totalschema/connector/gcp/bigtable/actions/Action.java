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

package io.github.totalschema.connector.gcp.bigtable.actions;

import io.github.totalschema.connector.gcp.bigtable.actions.instance.InstanceAction;
import io.github.totalschema.connector.gcp.bigtable.actions.table.TableAction;

/**
 * Base interface for all BigTable actions.
 *
 * <p>Actions are categorized into two types:
 *
 * <ul>
 *   <li>{@link TableAction} - Operations on tables (create, delete, modify column families, etc.)
 *   <li>{@link InstanceAction} - Operations on instances (create, delete, configure clusters, etc.)
 * </ul>
 *
 * <p>The generic {@link #execute(Object)} method accepts any admin client type, allowing
 * polymorphic execution. Concrete action types ({@link TableAction} and {@link InstanceAction})
 * provide type-safe execute methods and bridge to this generic method via default implementations.
 */
public interface Action<C> {
    /**
     * Executes the action with the appropriate admin client.
     *
     * <p>This generic method is implemented by sub-interfaces ({@link TableAction} and {@link
     * InstanceAction}) using default methods that delegate to their type-safe execute methods.
     *
     * @param adminClient the BigTable admin client (either BigtableTableAdminClient or
     *     BigtableInstanceAdminClient)
     */
    void execute(C adminClient);
}
