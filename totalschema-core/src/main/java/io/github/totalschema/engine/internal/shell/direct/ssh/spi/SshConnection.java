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

package io.github.totalschema.engine.internal.shell.direct.ssh.spi;

import io.github.totalschema.engine.internal.shell.direct.TerminalSession;
import java.io.IOException;
import java.nio.file.Path;

/**
 * SSH connection interface for remote command execution and file transfer.
 *
 * <p>Implementations provide secure SSH connectivity with support for:
 *
 * <ul>
 *   <li>Remote command execution
 *   <li>File upload via SCP
 *   <li>Connection management
 * </ul>
 */
public interface SshConnection extends TerminalSession<String> {

    /**
     * Uploads a file to the remote server using SCP.
     *
     * @param localFile the local file to upload
     * @param remotePath the destination path on the remote server
     * @throws IOException if the file transfer fails
     * @throws InterruptedException if the operation is interrupted
     */
    void uploadFile(Path localFile, String remotePath) throws IOException, InterruptedException;
}
