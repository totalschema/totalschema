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

        public InputStream getErrorStream() {
            return process.getErrorStream();
        }

        public int waitFor() throws InterruptedException {
            return process.waitFor();
        }
    }

    @Override
    public void execute(List<String> command) {

        log.info("Executing command: {}", command);

        try (AutoCloseableProcess process = new AutoCloseableProcess(startProcess(command))) {

            Future<?> outReader =
                    submitReaderTask(process.getInputStream(), this::acceptStandardOut);
            Future<?> errorReader =
                    submitReaderTask(process.getErrorStream(), this::acceptStandardError);

            outReader.get();
            errorReader.get();

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

    protected Process startProcess(List<String> command) throws IOException {

        ProcessBuilder builder = new ProcessBuilder();
        builder.redirectErrorStream(false);

        builder.command(command);

        return builder.start();
    }

    protected void acceptStandardOut(String line) {
        System.out.format("[SHELL:O] %s%n", line);
    }

    protected void acceptStandardError(String line) {
        System.err.format("[SHELL:E] %s%n", line);
    }

    @Override
    public void close() {
        // no-op
    }
}
