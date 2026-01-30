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

package io.github.totalschema.engine.internal.shell.direct.local.impl;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.internal.shell.ExternalProcessTerminalSession;
import io.github.totalschema.engine.internal.shell.direct.local.spi.ShellScriptSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class DefaultShellScriptSession extends ExternalProcessTerminalSession
        implements ShellScriptSession {

    private final String name;
    private final List<String> startCommand;

    public DefaultShellScriptSession(String name, Configuration configuration) {
        this.name = name;

        this.startCommand =
                configuration
                        .getList("start.command")
                        .orElseGet(DefaultShellScriptSession::getAutoDetectedStartCommand);
    }

    private static List<String> getAutoDetectedStartCommand() {

        boolean isWindows =
                System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows");

        List<String> command;

        if (isWindows) {
            command = Arrays.asList("cmd.exe", "/c");
        } else {
            command = Arrays.asList("sh", "-c");
        }

        return command;
    }

    protected Process startProcess(List<String> command) throws IOException {

        LinkedList<String> newCommand = new LinkedList<>();
        newCommand.addAll(startCommand);
        newCommand.addAll(command);

        return super.startProcess(newCommand);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DefaultShellScriptSession{");
        sb.append("name='").append(name).append('\'');
        sb.append(", startCommand=").append(startCommand);
        sb.append('}');
        return sb.toString();
    }
}
