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

package io.github.totalschema.connector.gcp.bigtable;

import com.google.cloud.bigtable.admin.v2.BigtableInstanceAdminClient;
import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.ChangeExecutorConnector;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.engine.core.command.api.ObjectsContext;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.spi.change.executor.ChangeExecutor;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BigTable connector for executing change scripts against Google Cloud Bigtable.
 *
 * <p>Supports multiple execution formats via file extension:
 *
 * <ul>
 *   <li>{@code .yaml} or {@code .yml} — declarative table definitions (requires
 *       YamlBigTableExecutor)
 *   <li>{@code .groovy} — Groovy scripts with {@code bigtableAdmin} binding
 *   <li>Other extensions via JSR-223 or custom executors
 * </ul>
 *
 * <h3>Client Types and Configuration</h3>
 *
 * <p>BigTable uses two separate client types:
 *
 * <ul>
 *   <li><strong>Admin Clients</strong> (always enabled) - for table/instance management:
 *       <ul>
 *         <li>{@link BigtableTableAdminClient} - table schema operations
 *         <li>{@link BigtableInstanceAdminClient} - instance management
 *       </ul>
 *   <li><strong>Data Client</strong> (disabled by default) - for data read/write operations:
 *       <ul>
 *         <li>{@link BigtableDataClient} - row-level data operations (SetCell, DeleteCell,
 *             DeleteCellVersions, etc.)
 *       </ul>
 * </ul>
 *
 * <p><strong>Important:</strong> The data client is <em>disabled by default</em> to avoid
 * unnecessary client initialization and configuration overhead, as most schema migration workflows
 * only require admin operations. If you need to perform data operations (e.g., inserting seed data,
 * deleting specific cell versions), you must explicitly enable the data client by setting {@code
 * dataclient.enabled: true}.
 *
 * <p>Example configuration (admin operations only):
 *
 * <pre>{@code
 * connectors:
 *   mybigtable:
 *     type: bigtable
 *     projectId: my-gcp-project
 *     instanceId: my-instance
 *     # Optional: Custom admin endpoint for Private Service Connection
 *     # Default: bigtableadmin.googleapis.com
 *     adminEndpoint: https://custom-bigtableadmin-endpoint.googleapis.com
 * }</pre>
 *
 * <p>Example configuration (with data operations enabled):
 *
 * <pre>{@code
 * connectors:
 *   mybigtable:
 *     type: bigtable
 *     projectId: my-gcp-project
 *     instanceId: my-instance
 *     # Enable data client for row-level operations
 *     dataclient:
 *       enabled: true
 * }</pre>
 *
 * <p>Note: BigTable uses two separate API endpoints:
 *
 * <ul>
 *   <li>Admin endpoint (bigtableadmin.googleapis.com) - for table/instance management
 *   <li>Data endpoint (bigtable.googleapis.com) - for data read/write operations
 * </ul>
 *
 * Currently, only {@code adminEndpoint} is configurable for custom Private Service Connection
 * setups.
 */
final class BigTableConnector extends ChangeExecutorConnector {

    /** Connector type identifier used in {@code totalschema.yml} ({@code type: bigtable}). */
    static final String CONNECTOR_TYPE = "bigtable";

    private static final Logger logger = LoggerFactory.getLogger(BigTableConnector.class);

    private final boolean dataClientEnabled;

    BigTableConnector(String name, Configuration configuration) {
        super(name, configuration);

        this.dataClientEnabled = configuration.getBoolean("dataclient.enabled").orElse(false);
    }

    @Override
    public void checkConnection(Context context, List<ChangeFile.Id> plannedChangeFileIds) {
        logger.info("[{}] Verifying BigTable connectivity", name);
        try {
            BigtableTableAdminClient adminClient =
                    context.get(BigtableTableAdminClient.class, null, connectorConfiguration);

            List<String> tableNames = adminClient.listTables();
            logger.debug("[{}] Connection test listed {} tables", name, tableNames.size());

        } catch (RuntimeException ex) {
            logger.error("[{}] BigTable connectivity check FAILED", name, ex);
            throw new BigTableConnectorException("BigTable connectivity check failed", ex);
        }

        logger.info("[{}] BigTable connection verified", name);
    }

    @Override
    protected Optional<ChangeExecutor> getCustomChangeExecutor(
            String changeExecutorId, Context context) {

        if (YmlBigTableChangeExecutor.isChangeTypeSupported(changeExecutorId)) {
            /*
            YmlBigTableChangeExecutor is specific to BigTable connector, so
            we do not expose it as a component. Technically, it does not have
            to be a ChangeExecutor component to function correctly, but
            following this pattern allows the rest of the code handle YML config
            in the exact same way as e.g. a Groovy ChangeExecutor.
             */
            return Optional.of(
                    new YmlBigTableChangeExecutor(name, connectorConfiguration, context));

        } else {
            return Optional.empty();
        }
    }

    @Override
    protected Context getExecutionContext(Context parentContext) {

        BigtableInstanceAdminClient bigtableInstanceAdminClient =
                parentContext.get(BigtableInstanceAdminClient.class, null, connectorConfiguration);

        BigtableTableAdminClient bigtableTableAdminClient =
                parentContext.get(BigtableTableAdminClient.class, null, connectorConfiguration);

        if (dataClientEnabled) {
            parentContext.get(BigtableDataClient.class, null, connectorConfiguration);
        }

        return ObjectsContext.builder()
                .parentContext(parentContext)
                .setValue(BigtableInstanceAdminClient.class, bigtableInstanceAdminClient)
                .setValue(BigtableTableAdminClient.class, bigtableTableAdminClient)
                .build();
    }

    @Override
    protected Map<String, Object> getBindings(ChangeFile changeFile, Context context) {

        // Start with default bindings (configuration, environment)
        Map<String, Object> bindings = super.getBindings(changeFile, context);

        // Acquire the BigTable admin clients (cached by context so subsequent calls reuse them)
        BigtableInstanceAdminClient bigtableInstanceAdminClient =
                context.get(BigtableInstanceAdminClient.class, null, connectorConfiguration);

        BigtableTableAdminClient bigtableTableAdminClient =
                context.get(BigtableTableAdminClient.class, null, connectorConfiguration);

        // Add BigTable-specific bindings
        bindings.put("bigtableInstanceAdminClient", bigtableInstanceAdminClient);
        bindings.put("bigtableTableAdminClient", bigtableTableAdminClient);

        if (dataClientEnabled) {
            BigtableDataClient bigtableDataClient =
                    context.get(BigtableDataClient.class, null, connectorConfiguration);

            bindings.put("bigtableDataClient", bigtableDataClient);
        }

        return bindings;
    }

    @Override
    public String toString() {
        return "BigTable Connector named '"
                + name
                + "'{"
                + " configuration='"
                + connectorConfiguration
                + '\''
                + '}';
    }
}
