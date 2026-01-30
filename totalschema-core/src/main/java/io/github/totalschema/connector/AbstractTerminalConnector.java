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

import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.engine.internal.shell.direct.TerminalSession;
import io.github.totalschema.model.ChangeFile;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Base class for connectors that use terminal/shell sessions.
 *
 * <p>Handles the lifecycle of the terminal session and delegates script execution to subclasses.
 *
 * @param <C> the command type for the terminal session
 */
public abstract class AbstractTerminalConnector<C> extends Connector {

    /** The terminal session used to execute commands. */
    protected final TerminalSession<C> session;

    /**
     * Constructs an AbstractTerminalConnector with the specified terminal session.
     *
     * @param session the terminal session to use for command execution
     */
    protected AbstractTerminalConnector(TerminalSession<C> session) {
        this.session = session;
    }

    @Override
    public void execute(ChangeFile changeFile, CommandContext context) throws InterruptedException {
        execute(changeFile.getFile(), context);
    }

    /**
     * Execute a script file using the terminal session.
     *
     * @param scriptFile the script file to execute
     * @param context the command context
     * @throws InterruptedException if execution is interrupted
     */
    protected abstract void execute(Path scriptFile, CommandContext context)
            throws InterruptedException;

    @Override
    public void close() throws IOException {
        session.close();
    }
}
