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

import static java.util.Objects.requireNonNull;

import com.google.cloud.bigtable.admin.v2.BigtableInstanceAdminClient;
import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.gcp.bigtable.actions.Action;
import io.github.totalschema.connector.gcp.bigtable.actions.ActionFactory;
import io.github.totalschema.connector.gcp.bigtable.actions.data.DataAction;
import io.github.totalschema.connector.gcp.bigtable.actions.instance.InstanceAction;
import io.github.totalschema.connector.gcp.bigtable.actions.table.TableAction;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.change.executor.SimpleChangeExecutor;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes YAML-based change files for Google Cloud Bigtable operations.
 *
 * <p>This executor processes YAML/YML files containing declarative Bigtable schema definitions
 * (table creation, column family management, etc.) and translates them into administrative
 * operations via the Bigtable Admin API.
 *
 * <p><strong>Design Note:</strong> This class is intentionally <em>not</em> exposed as an IoC
 * component via {@code ServiceLoader}, as it is specific to the Bigtable connector implementation.
 * While it extends {@link SimpleChangeExecutor} for architectural consistency—allowing YAML change
 * files to be processed through the same execution pipeline as other script types (e.g., SQL)—its
 * lifecycle is managed directly by {@link BigTableConnector} rather than the container.
 *
 * <p><strong>Implementation Note:</strong> This executor needs access to the file Path (not just
 * content) because {@link ActionFactory} parses the YAML file directly from the path. Therefore, it
 * stores the ChangeFile temporarily during execution.
 *
 * @see BigTableConnector
 * @see Action
 * @see ActionFactory
 */
final class YmlBigTableChangeExecutor extends SimpleChangeExecutor {

    private static final Logger logger = LoggerFactory.getLogger(YmlBigTableChangeExecutor.class);

    private final String name;
    private final Configuration configuration;

    static boolean isChangeTypeSupported(String changeExecutorId) {
        return changeExecutorId.equalsIgnoreCase("yml")
                || changeExecutorId.equalsIgnoreCase("yaml");
    }

    YmlBigTableChangeExecutor(String name, Configuration configuration, Context context) {
        super(context);
        this.name = name;
        this.configuration = configuration;
    }

    @Override
    public void execute(InputStream inputStream, Context context) throws InterruptedException {

        requireNonNull(inputStream, "InputStream must not be null");
        requireNonNull(context, "Context must not be null");

        Action<?> action = ActionFactory.getAction(inputStream, context);
        requireNonNull(action, "Action returned for file content must not be null");

        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }

        logger.info("[{}] Executing: {}", name, action);

        if (action instanceof TableAction) {
            executeTableAction((TableAction) action, context);

        } else if (action instanceof InstanceAction) {
            executeInstanceAction((InstanceAction) action, context);

        } else if (action instanceof DataAction) {
            executeDataAction((DataAction) action, context);

        } else {
            throw new IllegalStateException(
                    "Unknown action type: "
                            + action.getClass().getName()
                            + ". Actions must implement either TableAction, InstanceAction, or DataAction.");
        }
    }

    private void executeTableAction(TableAction action, Context context) {

        BigtableTableAdminClient adminClient = context.get(BigtableTableAdminClient.class);

        action.execute(adminClient);
    }

    private void executeInstanceAction(InstanceAction action, Context context) {

        BigtableInstanceAdminClient adminClient = context.get(BigtableInstanceAdminClient.class);

        action.execute(adminClient);
    }

    private void executeDataAction(DataAction action, Context context) {

        BigtableDataClient dataClient =
                context.getOptional(BigtableDataClient.class)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "BigtableDataClient is not enabled in configuration"));

        action.execute(dataClient);
    }

    @Override
    public String toString() {
        return "YmlBigTableChangeExecutor{"
                + "name='"
                + name
                + '\''
                + ", configuration="
                + configuration
                + '}';
    }
}
