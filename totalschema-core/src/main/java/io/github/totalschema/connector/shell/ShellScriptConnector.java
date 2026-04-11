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

package io.github.totalschema.connector.shell;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.Connector;
import io.github.totalschema.connector.shell.spi.ShellScriptRunner;
import io.github.totalschema.connector.shell.spi.ShellScriptRunnerFactory;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.model.ChangeFile;
import java.util.Collections;

/**
 * Connector for executing shell scripts on the local machine.
 *
 * <p>For each script, a fresh {@link ShellScriptRunner} is obtained from {@link
 * ShellScriptRunnerFactory} and closed immediately afterwards via try-with-resources. The factory
 * receives the script's file name and is responsible for returning the appropriate runner —
 * PowerShell, {@code cmd.exe}, {@code sh}, or any custom implementation — based on the file
 * extension and the connector configuration.
 *
 * <p>No runner is held between executions, so this connector carries no long-lived resources and
 * does not need to be closed itself.
 *
 * <p>The connector type string is {@value #CONNECTOR_TYPE}. Declare it in {@code totalschema.yml}
 * like:
 *
 * <pre>{@code
 * connectors:
 *   localshell:
 *     type: shell
 * }</pre>
 *
 * <p>The interpreter can be overridden per-connector via {@code start.command} (comma-separated).
 * When set, the factory bypasses extension-based auto-detection and uses this prefix for every
 * script:
 *
 * <pre>{@code
 * connectors:
 *   pwsh_scripts:
 *     type: shell
 *     start:
 *       command: pwsh,-ExecutionPolicy,Bypass,-File
 * }</pre>
 *
 * @see ShellScriptRunnerFactory
 * @see io.github.totalschema.connector.shell.impl.DefaultShellScriptRunnerFactory
 */
public class ShellScriptConnector extends Connector {

    /** Connector type identifier used in {@code totalschema.yml} ({@code type: shell}). */
    public static final String CONNECTOR_TYPE = "shell";

    private final String name;
    private final Configuration configuration;
    private final ShellScriptRunnerFactory runnerFactory;

    /**
     * Constructs a new {@code ShellScriptConnector} using the default {@link
     * ShellScriptRunnerFactory}.
     *
     * <p>The connector is cheap to create: no runner or OS process is started here. Runner
     * acquisition is deferred to the first call to {@link #execute(ChangeFile, CommandContext)}.
     *
     * @param name the connector name as declared in {@code totalschema.yml}; used for logging and
     *     {@link #toString()}
     * @param configuration the connector-specific configuration block (may include a
     *     comma-separated {@code start.command} to bypass per-execution interpreter auto-detection)
     */
    public ShellScriptConnector(String name, Configuration configuration) {
        this(name, configuration, ShellScriptRunnerFactory.getInstance());
    }

    /**
     * Constructs a new {@code ShellScriptConnector} with an explicit {@link
     * ShellScriptRunnerFactory}.
     *
     * <p>This constructor is intended for testing: pass a mock or stub factory to control which
     * {@link ShellScriptRunner} is returned without touching the real OS.
     *
     * @param name the connector name as declared in {@code totalschema.yml}; used for logging and
     *     {@link #toString()}
     * @param configuration the connector-specific configuration block
     * @param runnerFactory the factory used to create a {@link ShellScriptRunner} on each execution
     */
    public ShellScriptConnector(
            String name, Configuration configuration, ShellScriptRunnerFactory runnerFactory) {
        this.name = name;
        this.configuration = configuration;
        this.runnerFactory = runnerFactory;
    }

    /**
     * Executes a shell script against the local machine.
     *
     * <p>The script's file name is passed to {@link ShellScriptRunnerFactory#getRunner} so the
     * factory can select the right interpreter for this specific file. The runner is used once and
     * immediately closed.
     *
     * @param changeFile the change file to execute
     * @param context the command context (not used directly by this connector)
     * @throws InterruptedException if the underlying process execution is interrupted
     */
    @Override
    public void execute(ChangeFile changeFile, CommandContext context) throws InterruptedException {

        String fileName = changeFile.getFile().getFileName().toString();

        try (ShellScriptRunner runner = runnerFactory.getRunner(name, configuration, fileName)) {
            runner.execute(
                    Collections.singletonList(changeFile.getFile().toAbsolutePath().toString()));
        }
    }

    @Override
    public String toString() {
        return "Shell Script Connector named '" + name + '\'';
    }
}
