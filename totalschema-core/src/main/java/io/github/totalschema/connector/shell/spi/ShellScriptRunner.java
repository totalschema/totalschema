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

package io.github.totalschema.connector.shell.spi;

import io.github.totalschema.engine.internal.shell.direct.TerminalSession;
import java.util.List;

/**
 * A runner that executes local shell scripts as OS processes.
 *
 * <p>Specialises {@link TerminalSession} for a command representation of {@code List<String>},
 * where the list holds the interpreter prefix followed by the script's absolute path.
 *
 * <p>The built-in implementation is {@link
 * io.github.totalschema.connector.shell.impl.DefaultShellScriptRunner}. Custom implementations can
 * be provided by implementing {@link ShellScriptRunnerFactory}.
 */
public interface ShellScriptRunner extends TerminalSession<List<String>> {}
