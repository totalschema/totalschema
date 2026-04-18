/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2026 totalschema development team
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

package io.github.totalschema.connector.python;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.connector.Connector;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.model.ChangeFile;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connector for executing Python scripts on the local machine.
 *
 * <p>For each change file, the connector:
 *
 * <ol>
 *   <li>Optionally copies the script to a fresh temporary directory (see {@code copyToTempDir}).
 *   <li>Invokes the configured {@code executable} (default: {@code python3} on *nix, {@code python}
 *       on Windows) with the script file as the sole argument.
 *   <li>When {@code copyToTempDir} is enabled, deletes the temporary directory regardless of
 *       success or failure.
 * </ol>
 *
 * <p>The working directory defaults to the directory that contains the change file. It can be
 * overridden with the {@code workingDirectory} configuration key.
 *
 * <p>When {@code copyToTempDir: true} is set, a fresh temporary directory is created for every
 * script execution. The script is copied there and the temporary directory is used as the working
 * directory (unless {@code workingDirectory} is explicitly set). The temporary directory is always
 * deleted after the script finishes, even if the script fails.
 *
 * <p>Standard output and standard error of the Python process are logged line-by-line at {@code
 * INFO} level. A non-zero exit code is treated as a deployment failure.
 *
 * <p>Example configuration:
 *
 * <pre>{@code
 * connectors:
 *   myetl:
 *     type: python
 *     executable: python3             # optional, default: python3 (*nix) / python (Windows)
 *     workingDirectory: /path/to/scripts  # optional
 *     initCommands: pip install -r requirements.txt, pip install pandas  # optional, comma-separated; run once before first script
 *     copyToTempDir: true                # optional, default: false; copy script to a temp dir before execution
 *     modulesDirectory: scripts/lib      # optional; path prepended to PYTHONPATH so scripts can import local modules
 *     initFiles:                         # optional, files written to the working dir before initCommands run
 *       requirements.txt: |
 *         pandas==2.0.0
 *         requests==2.31.0
 *     sdk:
 *       enabled: true                  # optional, default: false; expose TotalSchema variables to scripts
 *       variables:                     # variables made available via Variable.get("name") in the script
 *         foo: ${foo}
 *         bar: some_literal
 *     environmentVariables:           # optional, extra variables merged into the process environment
 *       MY_API_KEY: secret123
 *       PYTHONPATH: /opt/mylibs
 * }</pre>
 */
public final class PythonConnector extends Connector {

    /** Connector type identifier used in {@code totalschema.yml} ({@code type: python}). */
    public static final String CONNECTOR_TYPE = "python";

    private static final String DEFAULT_EXECUTABLE =
            OperatingSystemInfo.IS_WINDOWS ? "python" : "python3";

    private static final Logger log = LoggerFactory.getLogger(PythonConnector.class);

    static final String PYTHONPATH_VARIABLE_NAME = "PYTHONPATH";

    private final String name;
    private final String executable;
    private final String workingDirectoryOverride; // nullable — absent when not configured
    private final List<String>
            initCommands; // nullable — absent when not configured; each entry is a full command
    // string
    private final Map<String, String>
            initFiles; // nullable — absent when not configured; filename → content
    private final boolean copyToTempDir;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final PythonProcessRunner processRunner;
    private final TotalSchemaSdkGenerator sdkGenerator; // null when SDK is disabled

    /**
     * Creates a {@code PythonConnector} using the default {@link DefaultPythonProcessRunner}.
     *
     * @param name the connector name as declared in {@code totalschema.yml}
     * @param configuration the connector-specific configuration block
     */
    public PythonConnector(String name, Configuration configuration) {
        this(name, configuration, buildProcessRunner(name, configuration));
    }

    /**
     * Builds the {@link DefaultPythonProcessRunner} for the public constructor.
     *
     * <p>Merges {@code environmentVariables} from config with the optional {@code modulesDirectory}
     * value. When {@code modulesDirectory} is set its absolute path is prepended to any {@code
     * PYTHONPATH} already present in {@code environmentVariables}, using the platform path
     * separator. This lets scripts import modules from that directory without the user having to
     * know about {@code PYTHONPATH} directly.
     *
     * @param name connector name, used for log messages
     * @param configuration connector-specific configuration block
     * @return a fully configured runner
     */
    private static DefaultPythonProcessRunner buildProcessRunner(
            String name, Configuration configuration) {
        final Map<String, String> envVars =
                new LinkedHashMap<>(
                        configuration
                                .getPrefixNamespace("environmentVariables")
                                .asMap()
                                .orElse(Collections.emptyMap()));

        final String modulesDirectory = configuration.getString("modulesDirectory").orElse(null);
        if (modulesDirectory != null) {
            final String resolvedPath = Path.of(modulesDirectory).toAbsolutePath().toString();
            final String existing = envVars.get(PYTHONPATH_VARIABLE_NAME);
            final String pythonPath =
                    existing != null ? resolvedPath + File.pathSeparator + existing : resolvedPath;
            envVars.put("PYTHONPATH", pythonPath);
            log.debug(
                    "[{}] modulesDirectory resolved to '{}'; PYTHONPATH set to '{}'",
                    name,
                    resolvedPath,
                    pythonPath);
        }

        return new DefaultPythonProcessRunner(name, envVars.isEmpty() ? null : envVars);
    }

    /**
     * Creates a {@code PythonConnector} with an explicit {@link PythonProcessRunner}.
     *
     * <p>This constructor is intended for testing: pass a mock runner to verify connector behaviour
     * without spawning real OS processes.
     *
     * @param name the connector name as declared in {@code totalschema.yml}
     * @param configuration the connector-specific configuration block
     * @param processRunner the runner used to execute OS processes
     */
    PythonConnector(String name, Configuration configuration, PythonProcessRunner processRunner) {
        this.name = Objects.requireNonNull(name, "name is null");
        this.executable = configuration.getString("executable").orElse(DEFAULT_EXECUTABLE);
        this.workingDirectoryOverride = configuration.getString("workingDirectory").orElse(null);
        this.initCommands =
                configuration
                        .getList("initCommands")
                        .map(Collections::unmodifiableList)
                        .orElse(null);
        this.initFiles =
                configuration
                        .getPrefixNamespace("initFiles")
                        .asMap()
                        .map(Collections::unmodifiableMap)
                        .orElse(null);
        this.copyToTempDir = configuration.getBoolean("copyToTempDir").orElse(false);
        this.processRunner = Objects.requireNonNull(processRunner, "processRunner is null");
        final Configuration sdkConfig = configuration.getPrefixNamespace("sdk");
        final boolean sdkEnabled = sdkConfig.getBoolean("enabled").orElse(false);
        if (sdkEnabled) {
            final Map<String, String> sdkVariables =
                    sdkConfig
                            .getPrefixNamespace("variables")
                            .asMap()
                            .map(Collections::unmodifiableMap)
                            .orElse(Collections.emptyMap());
            this.sdkGenerator = new TotalSchemaSdkGenerator(sdkVariables);
            log.debug(
                    "[{}] TotalSchema SDK enabled with {} variable(s)", name, sdkVariables.size());
        } else {
            this.sdkGenerator = null;
        }
    }

    @Override
    public void execute(ChangeFile changeFile, CommandContext context) throws InterruptedException {

        Path file = changeFile.getFile();
        Objects.requireNonNull(file, "file is null");

        log.debug("[{}] Resolving working directory for script: {}", name, file);
        Path workingDir = resolveWorkingDir(file);
        log.debug("[{}] Resolved working directory: {}", name, workingDir);

        initialize(workingDir);

        if (copyToTempDir) {
            executeWithTempDir(file, workingDir);
        } else {
            runScript(file.toAbsolutePath(), workingDir);
        }
    }

    /**
     * Resolves the working directory for a given script file.
     *
     * <p>Priority:
     *
     * <ol>
     *   <li>{@code workingDirectory} from configuration, if set.
     *   <li>Parent directory of the script file.
     *   <li>Current directory ({@code .}) as a last resort when the script has no parent.
     * </ol>
     *
     * @param file the script file whose parent is used as the default
     * @return the resolved working directory
     */
    private Path resolveWorkingDir(Path file) {
        final Path result;
        if (workingDirectoryOverride != null) {
            log.debug(
                    "[{}] Using configured workingDirectory override: {}",
                    name,
                    workingDirectoryOverride);
            result = Path.of(workingDirectoryOverride);
        } else {
            Path parent = file.toAbsolutePath().getParent();
            result = Objects.requireNonNullElseGet(parent, () -> Path.of("."));
            log.debug("[{}] Derived working directory from script parent: {}", name, result);
        }
        return result;
    }

    /**
     * Invokes the Python executable with {@code scriptFile} as the sole argument, using {@code
     * workingDir} as the process working directory.
     *
     * <p>When the TotalSchema SDK is enabled, a dedicated temporary directory is created, the
     * {@code totalschema} package is generated inside it, and the directory is prepended to {@code
     * PYTHONPATH} for this invocation only. The temporary directory is unconditionally deleted
     * after the script finishes, keeping the working directory and the changes tree clean.
     *
     * @param scriptFile absolute path to the script to execute
     * @param workingDir working directory for the Python process
     */
    private void runScript(Path scriptFile, Path workingDir) throws InterruptedException {
        Path sdkTempDir = null;
        if (sdkGenerator != null) {
            sdkTempDir = sdkGenerator.generate();
            log.debug("[{}] TotalSchema SDK generated in temp dir: {}", name, sdkTempDir);
        }
        try {
            final Map<String, String> extraEnv;
            if (sdkTempDir != null) {
                extraEnv = Map.of(PYTHONPATH_VARIABLE_NAME, sdkTempDir.toString());
            } else {
                extraEnv = Collections.emptyMap();
            }
            log.info("[{}] Executing Python script: {}", name, scriptFile);
            processRunner.run(List.of(executable, scriptFile.toString()), workingDir, extraEnv);
        } finally {
            if (sdkTempDir != null) {
                log.debug("[{}] Cleaning up TotalSchema SDK temp dir: {}", name, sdkTempDir);
                sdkGenerator.cleanup(sdkTempDir);
            }
        }
    }

    /**
     * Copies the script to a freshly created temporary directory, executes it there, and always
     * removes the temporary directory afterwards.
     *
     * <p>The temporary directory is used as the working directory for the Python process unless
     * {@code workingDirectory} was explicitly configured.
     *
     * @param file the original script file
     * @param defaultWorkingDir the working directory to use when no override is configured
     */
    private void executeWithTempDir(Path file, Path defaultWorkingDir) throws InterruptedException {
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("totalschema-python-");
            log.debug("[{}] Created temporary directory: {}", name, tempDir);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to create temporary directory for Python script", e);
        }

        try {
            Path tempScript = tempDir.resolve(file.getFileName());
            log.debug("[{}] Copying script {} to {}", name, file, tempScript);
            try {
                Files.copy(file, tempScript);
            } catch (IOException e) {
                throw new UncheckedIOException(
                        "Failed to copy Python script to temporary directory: " + file, e);
            }
            log.debug("[{}] Script copied successfully", name);

            Path runDir = workingDirectoryOverride != null ? defaultWorkingDir : tempDir;
            log.debug("[{}] Using run directory: {}", name, runDir);
            log.info(
                    "[{}] Executing Python script (via temp dir {}): {}",
                    name,
                    tempDir,
                    tempScript);
            runScript(tempScript.toAbsolutePath(), runDir);
        } finally {
            log.debug("[{}] Deleting temporary directory: {}", name, tempDir);
            deleteTempDir(tempDir);
        }
    }

    /**
     * Recursively deletes {@code dir}, logging a warning on failure instead of propagating.
     *
     * @param dir directory to delete
     */
    private void deleteTempDir(Path dir) {
        try {
            try (var walk = Files.walk(dir)) {
                walk.sorted(java.util.Comparator.reverseOrder())
                        .forEach(
                                p -> {
                                    log.debug("[{}] Deleting temp path: {}", name, p);
                                    try {
                                        Files.delete(p);
                                    } catch (IOException e) {
                                        log.warn("[{}] Could not delete temp path: {}", name, p, e);
                                    }
                                });
            }
            log.debug("[{}] Temporary directory deleted: {}", name, dir);
        } catch (IOException e) {
            log.warn("[{}] Could not walk temp directory for deletion: {}", name, dir, e);
        }
    }

    /**
     * Runs initialization logic exactly once, the first time this connector is used:
     *
     * <ol>
     *   <li>Writes any {@code initFiles} entries to {@code workingDir}.
     *   <li>Runs each {@code initCommands} entry sequentially.
     * </ol>
     *
     * <p>Thread-safe: subsequent calls are no-ops.
     *
     * @param workingDir the directory in which files are written and commands are executed
     */
    private void initialize(Path workingDir) throws InterruptedException {
        if ((initFiles == null && initCommands == null)
                || !initialized.compareAndSet(false, true)) {
            log.debug("[{}] Skipping initialisation (nothing to do or already initialised)", name);
            return;
        }
        log.debug("[{}] Running initialisation in working directory: {}", name, workingDir);
        if (initFiles != null) {
            log.debug("[{}] Writing {} init file(s)", name, initFiles.size());
            for (Map.Entry<String, String> entry : initFiles.entrySet()) {
                Path target = workingDir.resolve(entry.getKey());
                log.info("[{}] Writing init file: {}", name, target);
                try {
                    Files.writeString(target, entry.getValue(), StandardCharsets.UTF_8);
                    log.debug("[{}] Init file written: {}", name, target);
                } catch (IOException e) {
                    throw new UncheckedIOException(
                            "Failed to write init file '" + entry.getKey() + "' to " + workingDir,
                            e);
                }
            }
        }
        if (initCommands != null) {
            log.debug("[{}] Running {} init command(s)", name, initCommands.size());
            for (String command : initCommands) {
                List<String> tokens = List.of(command.trim().split("\\s+"));
                log.debug("[{}] Init command tokens: {}", name, tokens);
                log.info("[{}] Running init command: {}", name, command.trim());
                processRunner.run(tokens, workingDir);
            }
        }
        log.debug("[{}] Initialization complete", name);
    }

    @Override
    public String toString() {
        return "PythonConnector named '"
                + name
                + "'{"
                + " executable='"
                + executable
                + '\''
                + ", workingDirectoryOverride='"
                + workingDirectoryOverride
                + '\''
                + ", initCommands="
                + initCommands
                + ", initFiles="
                + initFiles
                + ", copyToTempDir="
                + copyToTempDir
                + ", initialized="
                + initialized
                + ", processRunner="
                + processRunner
                + '}';
    }
}
