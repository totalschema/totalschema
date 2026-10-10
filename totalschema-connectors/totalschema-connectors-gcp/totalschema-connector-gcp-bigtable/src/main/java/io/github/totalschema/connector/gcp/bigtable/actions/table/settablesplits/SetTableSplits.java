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

package io.github.totalschema.connector.gcp.bigtable.actions.table.settablesplits;

import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.admin.v2.models.CreateTableRequest;
import com.google.protobuf.ByteString;
import io.github.totalschema.connector.gcp.bigtable.actions.table.TableAction;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pre-splits a BigTable table for optimal key distribution and performance.
 *
 * <p><strong>Important Note:</strong> In BigTable, table splits can only be configured at table
 * creation time. This action will:
 *
 * <ul>
 *   <li>Create a new table with the specified split keys if the table doesn't exist
 *   <li>Throw an error if the table already exists (splits cannot be modified after creation)
 * </ul>
 *
 * <p><strong>What This Action Does:</strong>
 *
 * <ul>
 *   <li>Pre-splits tables to evenly distribute data across multiple tablets
 *   <li>Prevents hotspots in write-heavy workloads
 *   <li>Optimizes performance for known key ranges
 *   <li>Improves bulk load and batch write performance
 * </ul>
 *
 * <p><strong>Use Cases:</strong>
 *
 * <ul>
 *   <li>Performance optimization for known key distributions
 *   <li>Preventing write hotspots in sequential key patterns
 *   <li>Optimizing batch data loads
 *   <li>Multi-tenant tables with predictable tenant ID ranges
 * </ul>
 *
 * <p><strong>Example YAML configurations:</strong>
 *
 * <pre>
 * # Simple split for user table (4 regions)
 * SetTableSplits:
 *   tableId: users
 *   splitKeys:
 *     - "user#1000"
 *     - "user#2000"
 *     - "user#3000"
 *     - "user#4000"
 *
 * # Split for multi-tenant table (by tenant ID)
 * SetTableSplits:
 *   tableId: tenant_data
 *   splitKeys:
 *     - "tenant_100#"
 *     - "tenant_200#"
 *     - "tenant_300#"
 *     - "tenant_400#"
 *     - "tenant_500#"
 *
 * # Split for time-series data (by date prefix)
 * SetTableSplits:
 *   tableId: events
 *   splitKeys:
 *     - "2026-01#"
 *     - "2026-02#"
 *     - "2026-03#"
 *     - "2026-04#"
 *     - "2026-05#"
 *     - "2026-06#"
 * </pre>
 *
 * <p><strong>Configuration Guidelines:</strong>
 *
 * <ul>
 *   <li><strong>Number of splits:</strong> Typically 1-100 splits; more splits = more tablets =
 *       better parallelism
 *   <li><strong>Split key selection:</strong> Choose keys that evenly divide your expected data
 *       distribution
 *   <li><strong>Key format:</strong> Use string keys that match your row key design (will be
 *       converted to bytes)
 *   <li><strong>Ordering:</strong> Split keys must be in ascending lexicographical order
 * </ul>
 *
 * <p><strong>Performance Implications:</strong>
 *
 * <ul>
 *   <li>Proper pre-splitting can improve write throughput by 10-100x
 *   <li>Prevents initial "hotspot" period while BigTable auto-splits
 *   <li>Critical for bulk loads and high-throughput applications
 *   <li>Over-splitting (too many tablets) can reduce efficiency
 * </ul>
 *
 * <p><strong>Limitations:</strong>
 *
 * <ul>
 *   <li>Splits can only be set at table creation time
 *   <li>Cannot modify splits on existing tables without recreation
 *   <li>Once set, BigTable may further auto-split based on actual data patterns
 *   <li>Automatic splitting may merge or split tablets over time
 * </ul>
 *
 * <p><strong>Best Practices:</strong>
 *
 * <ul>
 *   <li>Analyze your key distribution before choosing split points
 *   <li>Use hashed prefixes for naturally sequential keys (timestamps, auto-increment IDs)
 *   <li>Plan splits based on expected data volume per region
 *   <li>For production tables, test split strategy in dev/staging first
 *   <li>Monitor tablet distribution after deployment using Cloud Console
 * </ul>
 *
 * @see <a href="https://cloud.google.com/bigtable/docs/schema-design#row-keys">BigTable Row Key
 *     Design</a>
 * @see <a href="https://cloud.google.com/bigtable/docs/performance#pre-splitting">Pre-splitting
 *     Tables</a>
 */
public class SetTableSplits implements TableAction {

    private static final Logger logger = LoggerFactory.getLogger(SetTableSplits.class);

    private String tableId;
    private List<String> splitKeys;

    @Override
    public void execute(BigtableTableAdminClient adminClient) {
        // Validation
        if (tableId == null || tableId.isEmpty()) {
            throw new IllegalArgumentException("tableId is required. Example: tableId: my-table");
        }

        if (splitKeys == null || splitKeys.isEmpty()) {
            throw new IllegalArgumentException(
                    "splitKeys is required and must contain at least one split key. "
                            + "Example: splitKeys: [\"key1\", \"key2\", \"key3\"]");
        }

        // Validate that split keys are in ascending order
        for (int i = 1; i < splitKeys.size(); i++) {
            String prev = splitKeys.get(i - 1);
            String curr = splitKeys.get(i);
            if (prev.compareTo(curr) >= 0) {
                throw new IllegalArgumentException(
                        "Split keys must be in ascending lexicographical order. "
                                + "Found '"
                                + prev
                                + "' >= '"
                                + curr
                                + "' at position "
                                + i);
            }
        }

        // Check if table already exists
        if (adminClient.exists(tableId)) {
            throw new IllegalArgumentException(
                    "Table '"
                            + tableId
                            + "' already exists. "
                            + "BigTable does not support modifying splits on existing tables. "
                            + "To change splits, you must delete and recreate the table, "
                            + "or use ClearTableSplits followed by SetTableSplits.");
        }

        // Create table with splits
        try {
            CreateTableRequest request = CreateTableRequest.of(tableId);

            // Add split keys
            for (String splitKey : splitKeys) {
                ByteString splitBytes = ByteString.copyFromUtf8(splitKey);
                request.addSplit(splitBytes);
            }

            adminClient.createTable(request);

            logger.info(
                    "Created table '{}' with {} split keys: {}",
                    tableId,
                    splitKeys.size(),
                    splitKeys);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create table '"
                            + tableId
                            + "' with splits. Error: "
                            + e.getMessage(),
                    e);
        }
    }

    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public List<String> getSplitKeys() {
        return splitKeys;
    }

    public void setSplitKeys(List<String> splitKeys) {
        this.splitKeys = splitKeys;
    }

    @Override
    public String toString() {
        return "SetTableSplits{" + "tableId='" + tableId + '\'' + ", splitKeys=" + splitKeys + '}';
    }
}
