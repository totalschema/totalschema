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
import io.github.totalschema.connector.AbstractTerminalConnector;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.engine.internal.shell.direct.local.spi.LocalShellSessionFactory;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Connector for executing shell scripts on the local machine.
 *
 * <p>Scripts are executed using the local shell (sh, bash, cmd.exe, etc. depending on the operating
 * system).
 */
public class ShellScriptConnector extends AbstractTerminalConnector<List<String>> {

    public static final String CONNECTOR_TYPE = "shell";

    private final String name;

    public ShellScriptConnector(String name, Configuration connectorConfiguration) {
        super(
                LocalShellSessionFactory.getInstance()
                        .getLocalShellSession(name, connectorConfiguration));

        this.name = name;
    }

    @Override
    public String toString() {
        return "Shell Script Connector named '" + name + "'{session='" + session + '\'' + '}';
    }

    @Override
    protected void execute(Path scriptFile, CommandContext context) throws InterruptedException {
        session.execute(Collections.singletonList(scriptFile.toAbsolutePath().toString()));
    }
}
