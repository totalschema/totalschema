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

package io.github.totalschema.engine.internal.shell;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ExternalProcessTerminalSession extends AbstractTerminalSession<List<String>> {

    private static final Logger log = LoggerFactory.getLogger(ExternalProcessTerminalSession.class);

    protected static final class AutoCloseableProcess implements AutoCloseable {
        private final Process process;

        private AutoCloseableProcess(Process process) {
            this.process = process;
        }

        @Override
        public void close() {
            process.destroy();
        }

        public InputStream getInputStream() {
            return process.getInputStream();
        }

        public int waitFor() throws InterruptedException {
            return process.waitFor();
        }
    }

    @Override
    public void execute(List<String> command) {

        List<String> actualCommand = buildActualCommand(command);

        log.info("Executing command: {}", actualCommand);

        try (AutoCloseableProcess process = startProcess(actualCommand)) {

            // stderr is merged into stdout by startProcess(); a single reader thread is
            // sufficient and preserves the exact chronological order of all process output.
            Future<?> outReader = submitReaderTask(process.getInputStream(), this::acceptOutput);

            outReader.get();

            int exitStatus = process.waitFor();
            if (exitStatus != 0) {
                throw new RuntimeException(
                        "Exit status " + exitStatus + " received for command: " + command);
            }

        } catch (ExecutionException e) {
            throw new RuntimeException("Execution failed", e.getCause());

        } catch (IOException ex) {
            throw new RuntimeException(ex);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new RuntimeException("interrupt received", e);
        }
    }

    /**
     * Starts the OS process for the given command.
     *
     * @param command the command to execute, as a list of tokens (e.g. {@code ["sh",
     *     "/opt/changes/0001.setup.sh"]})
     */
    private AutoCloseableProcess startProcess(List<String> command) throws IOException {

        ProcessBuilder builder = new ProcessBuilder();
        builder.redirectErrorStream(true);

        builder.command(command);

        Process startedProcess = builder.start();

        return new AutoCloseableProcess(startedProcess);
    }

    /**
     * Transforms the logical command into the actual OS-level token list passed to {@link
     * ProcessBuilder}.
     *
     * <p>The default implementation returns the command unchanged. Subclasses may override this to,
     * for example, prepend an interpreter prefix.
     */
    protected List<String> buildActualCommand(List<String> command) {
        return command;
    }

    /**
     * Called for every line of output produced by the process (stdout and stderr merged).
     *
     * <p>Subclasses may override this method to redirect output to a logger, a result collector,
     * etc.
     */
    protected void acceptOutput(String line) {
        System.out.format("[Output] %s%n", line);
    }

    @Override
    public void close() {
        // no-op
    }
}
