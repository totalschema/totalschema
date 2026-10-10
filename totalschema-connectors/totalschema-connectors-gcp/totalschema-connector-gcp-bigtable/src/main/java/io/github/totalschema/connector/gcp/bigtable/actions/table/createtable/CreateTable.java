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

package io.github.totalschema.connector.gcp.bigtable.actions.table.createtable;

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.admin.v2.models.*;
import io.github.totalschema.connector.gcp.bigtable.actions.table.TableAction;
import java.util.List;

/**
 * Creates a BigTable table with optional column families and GC rules.
 *
 * <p>Example YAML configurations:
 *
 * <pre>
 * # Simple table with no column families
 * CreateTable:
 *   tableId: users
 *
 * # Table with simple families (no GC rules)
 * CreateTable:
 *   tableId: users
 *   families:
 *     - name: profile
 *     - name: settings
 *
 * # Table with GC rules
 * CreateTable:
 *   tableId: users
 *   families:
 *     - name: profile
 *       maxVersions: 3
 *     - name: settings
 *       maxAge: 7d
 *     - name: audit
 *       maxVersions: 10
 *       maxAge: 90d
 *
 * # Table with complex union/intersection rules
 * CreateTable:
 *   tableId: users
 *   families:
 *     - name: cache
 *       union:
 *         - maxVersions: 3
 *         - maxAge: 24h
 *     - name: important
 *       intersection:
 *         - maxVersions: 100
 *         - maxAge: 365d
 * </pre>
 */
public class CreateTable implements TableAction {

    private String tableId;
    private List<ColumnFamily> families;

    @Override
    public void execute(BigtableTableAdminClient adminClient) {
        if (tableId == null || tableId.isEmpty()) {
            throw new IllegalArgumentException("tableId is required");
        }

        CreateTableRequest createTableRequest = CreateTableRequest.of(tableId);

        // Add column families with optional GC rules
        if (families != null) {
            for (ColumnFamily family : families) {
                if (family.getName() == null || family.getName().isEmpty()) {
                    throw new IllegalArgumentException("Column family name is required");
                }

                if (family.hasGcRules()) {
                    GCRules.GCRule gcRule = family.buildGcRule();
                    createTableRequest.addFamily(family.getName(), gcRule);
                } else {
                    createTableRequest.addFamily(family.getName());
                }
            }
        }

        adminClient.createTable(createTableRequest);
    }

    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public List<ColumnFamily> getFamilies() {
        return families;
    }

    public void setFamilies(List<ColumnFamily> families) {
        this.families = families;
    }

    @Override
    public String toString() {
        return "CreateTable{" + "tableId='" + tableId + '\'' + ", families=" + families + '}';
    }
}
