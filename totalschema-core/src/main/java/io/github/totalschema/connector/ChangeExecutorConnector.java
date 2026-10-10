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

package io.github.totalschema.connector;

import static java.util.Objects.requireNonNull;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.engine.core.container.FactoryNotFoundException;
import io.github.totalschema.model.ChangeFile;
import io.github.totalschema.spi.change.executor.ChangeExecutor;
import io.github.totalschema.spi.change.executor.ObjectBindingChangeExecutor;
import io.github.totalschema.spi.change.executor.SimpleChangeExecutor;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for connectors that execute change files through {@link ChangeExecutor}
 * implementations.
 *
 * <p>This class implements the core execution workflow for change files by coordinating:
 *
 * <ul>
 *   <li>Resolving the appropriate {@link ChangeExecutor} based on file extension, with support for
 *       both factory-based lookup and connector-specific custom executors
 *   <li>Creating the execution context containing connector-specific services for the executor
 *   <li>Building the script bindings map to expose objects as variables in user scripts (for {@link
 *       ObjectBindingChangeExecutor} instances only)
 *   <li>Delegating execution to the selected executor using type-based dispatch
 * </ul>
 *
 * <h2>Execution Flow</h2>
 *
 * <p>The {@link #execute(ChangeFile, Context)} method follows this sequence:
 *
 * <ol>
 *   <li>Extract the file extension to determine the executor type
 *   <li>Attempt to resolve a custom executor via {@link #getCustomChangeExecutor(String, Context)}
 *   <li>If no custom executor is provided, resolve via the IoC container using the extension as the
 *       qualifier
 *   <li>Create an execution context via {@link #getExecutionContext(Context)}
 *   <li>Dispatch to the executor:
 *       <ul>
 *         <li>For {@link SimpleChangeExecutor}: invoke directly without bindings
 *         <li>For {@link ObjectBindingChangeExecutor}: build bindings via {@link
 *             #getBindings(ChangeFile, Context)} and invoke with the bindings map
 *       </ul>
 * </ol>
 *
 * <h2>Extension Points</h2>
 *
 * <p>Subclasses should override:
 *
 * <ul>
 *   <li>{@link #getExecutionContext(Context)} to provide connector-specific services (required)
 *   <li>{@link #getBindings(ChangeFile, Context)} to expose connector-specific objects to scripts
 *       (optional)
 *   <li>{@link #getCustomChangeExecutor(String, Context)} to provide connector-specific executors
 *       that should not be registered globally (optional)
 * </ul>
 *
 * @see ChangeExecutor
 * @see SimpleChangeExecutor
 * @see ObjectBindingChangeExecutor
 */
public abstract class ChangeExecutorConnector extends Connector {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected final String name;
    protected final Configuration connectorConfiguration;

    /**
     * Constructs a new {@code ChangeExecutorConnector} with the specified name and configuration.
     *
     * @param name the connector name as defined in {@code totalschema.yml}
     * @param connectorConfiguration the connector-specific configuration block from {@code
     *     totalschema.yml}
     */
    public ChangeExecutorConnector(String name, Configuration connectorConfiguration) {
        this.name = requireNonNull(name, "name must not be null");
        this.connectorConfiguration =
                requireNonNull(connectorConfiguration, "connectorConfiguration must not be null");
    }

    /**
     * Executes the specified change file by resolving the appropriate {@link ChangeExecutor} and
     * delegating to it.
     *
     * <p>This method implements the core execution workflow:
     *
     * <ol>
     *   <li>Determines the executor type from the file extension
     *   <li>Resolves the executor (custom or factory-based)
     *   <li>Creates the execution context with connector-specific services
     *   <li>Dispatches to the executor based on its type ({@link SimpleChangeExecutor} or {@link
     *       ObjectBindingChangeExecutor})
     * </ol>
     *
     * @param changeFile the change file to execute
     * @param context the dependency injection context for resolving components
     * @throws InterruptedException if the execution thread is interrupted
     * @throws RuntimeException if the executor cannot be resolved or execution fails
     */
    @Override
    public void execute(ChangeFile changeFile, Context context) throws InterruptedException {

        requireNonNull(changeFile, "changeFile must not be null");
        requireNonNull(context, "context must not be null");

        ChangeExecutor changeExecutor = getChangeExecutor(changeFile, context);

        logger.info("[{}] {} executing '{}'", name, changeExecutor, changeFile.getRelativePath());

        Context executionContext = getExecutionContext(context);
        logger.debug("[{}] Created execution Context: {}", name, executionContext);

        // Type-based dispatch: only build bindings for script executors that need them
        if (changeExecutor instanceof SimpleChangeExecutor) {
            execute((SimpleChangeExecutor) changeExecutor, changeFile, executionContext);

        } else if (changeExecutor instanceof ObjectBindingChangeExecutor) {
            execute(
                    (ObjectBindingChangeExecutor) changeExecutor,
                    changeFile,
                    context,
                    executionContext);

        } else {
            throw new IllegalStateException(
                    "ChangeExecutor must extend either ObjectBindingChangeExecutor or"
                            + " SimpleChangeExecutor: "
                            + changeExecutor.getClass().getName());
        }
    }

    protected void execute(
            SimpleChangeExecutor changeExecutor, ChangeFile changeFile, Context executionContext)
            throws InterruptedException {
        logger.debug("[{}] ChangeExecutor is a SimpleChangeExecutor: {}", name, changeExecutor);

        changeExecutor.execute(changeFile, executionContext);
    }

    private void execute(
            ObjectBindingChangeExecutor changeExecutor,
            ChangeFile changeFile,
            Context context,
            Context executionContext)
            throws InterruptedException {
        logger.debug(
                "[{}] ChangeExecutor is an ObjectBindingChangeExecutor: {}", name, changeExecutor);

        Map<String, Object> bindings = getBindings(changeFile, context);

        logger.debug("[{}] Created bindings: {} for: {}", name, bindings, changeExecutor);

        changeExecutor.execute(changeFile, executionContext, bindings);
    }

    private ChangeExecutor getChangeExecutor(ChangeFile changeFile, Context context) {

        String extension = getChangeExecutorId(changeFile);

        Optional<ChangeExecutor> customChangeExecutor = getCustomChangeExecutor(extension, context);
        requireNonNull(customChangeExecutor, "customChangeExecutor must not be null");
        if (customChangeExecutor.isPresent()) {
            return customChangeExecutor.get();
        }

        try {
            logger.debug(
                    "[{}] Creating '{}' ChangeExecutor for: '{}'",
                    name,
                    extension,
                    changeFile.getFile());

            return context.get(ChangeExecutor.class, extension, connectorConfiguration);

        } catch (FactoryNotFoundException ex) {

            throw new RuntimeException(
                    String.format(
                            "Factory not found for '%s' ChangeExecutor. "
                                    + "The extension library is likely missing from the classpath/dependencies.",
                            extension),
                    ex);

        } catch (RuntimeException ex) {
            throw new RuntimeException(
                    String.format(
                            "Failed to create '%s' ChangeExecutor: examine the stack trace for details.",
                            extension),
                    ex);
        }
    }

    /**
     * Extracts the change executor identifier from the change file.
     *
     * <p>The executor identifier is derived from the file extension (e.g., {@code "sql"}, {@code
     * "groovy"}, {@code "py"}) and is used to resolve the appropriate {@link ChangeExecutor} via
     * factory lookup or custom resolution.
     *
     * @param changeFile the change file from which to extract the executor identifier
     * @return the file extension to use as the executor identifier
     */
    private String getChangeExecutorId(ChangeFile changeFile) {
        requireNonNull(changeFile, "changeFile must not be null");

        ChangeFile.Id id = changeFile.getId();

        requireNonNull(id, "id in ChangeFile must not be null");

        return id.getExtension();
    }

    /**
     * Template method allowing subclasses to provide connector-specific custom executors that
     * should not be registered globally in the IoC container.
     *
     * <p>This method is invoked before attempting factory-based executor resolution. If a custom
     * executor is returned, the factory lookup is bypassed entirely.
     *
     * <p><strong>Use Case:</strong> Connectors may implement specialized executors for file types
     * that are specific to the connector's domain and should not be available to other connectors.
     * For example, a BigTable connector might provide a custom YAML executor that understands
     * BigTable-specific table definitions, while other connectors should use a different YAML
     * executor or none at all.
     *
     * <p>The default implementation returns {@link Optional#empty()}, delegating to the standard
     * factory-based resolution.
     *
     * @param changeExecutorId the change executor identifier (file extension) extracted from the
     *     change file
     * @param context the dependency injection context for resolving components
     * @return an {@link Optional} containing a custom {@link ChangeExecutor} if the connector
     *     provides a specialized implementation for the given executor ID; otherwise {@link
     *     Optional#empty()} to fall back to factory-based resolution
     */
    protected Optional<ChangeExecutor> getCustomChangeExecutor(
            String changeExecutorId, Context context) {
        return Optional.empty();
    }

    /**
     * Creates the execution {@code Context} to be passed to the {@code ChangeExecutor}.
     *
     * <p>Implementations should create an {@link
     * io.github.totalschema.engine.core.command.api.ObjectsContext} with the provided {@code
     * parentContext} as its parent, and populate it with connector-specific services as unqualified
     * objects. This pattern enables {@link ChangeExecutor} instances to access connector-provided
     * services (such as database connections, API clients, or other resources) without requiring
     * explicit dependency injection.
     *
     * @param parentContext the parent context, typically the {@code ComponentContainer}
     * @return the execution context containing connector-specific services for the {@code
     *     ChangeExecutor}
     */
    protected abstract Context getExecutionContext(Context parentContext);

    /**
     * Builds the bindings map containing objects to expose as variables in user scripts.
     *
     * <p>The default implementation includes:
     *
     * <ul>
     *   <li>{@code "configuration"} - the connector configuration
     *   <li>{@code "environment"} - the current environment (if present)
     * </ul>
     *
     * <p>Subclasses should override this method to add connector-specific bindings (e.g., database
     * connections, API clients).
     *
     * @param changeFile the change file being executed
     * @param context the dependency injection context for resolving objects
     * @return a mutable map of bindings to expose to scripts
     */
    protected Map<String, Object> getBindings(ChangeFile changeFile, Context context) {

        Map<String, Object> bindings = new HashMap<>();

        Optional<Environment> optionalEnvironment = context.getOptional(Environment.class);
        optionalEnvironment.ifPresent(environment -> bindings.put("environment", environment));

        bindings.put("configuration", connectorConfiguration);

        return bindings;
    }
}
