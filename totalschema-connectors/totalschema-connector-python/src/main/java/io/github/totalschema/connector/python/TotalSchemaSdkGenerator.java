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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates a minimal {@code totalschema} Python package in a target directory so that user scripts
 * can access TotalSchema variables at runtime:
 *
 * <pre>{@code
 * from totalschema.sdk import Variable
 *
 * foo = Variable.get("foo")
 * }</pre>
 *
 * <p>The generated package is an implementation detail: it is always removed after the script
 * finishes via {@link #cleanup(Path)}, even if the script fails. The package is written to a
 * dedicated temporary directory (not the script's working directory) whose path is injected into
 * {@code PYTHONPATH} by the caller, keeping the working directory clean. Variable values are
 * base64-encoded inside the generated source file so that any content (multiline strings, quotes,
 * backslashes, Unicode) is handled safely without complex escaping.
 *
 * <p>Generated layout inside {@code targetDir}:
 *
 * <pre>
 * totalschema/
 *   __init__.py   (empty)
 *   sdk.py        (Variable class backed by a constant map of decoded values)
 * </pre>
 */
final class TotalSchemaSdkGenerator {

    /** Name of the generated Python package directory. */
    static final String PACKAGE_DIR = "totalschema";

    private static final Logger log = LoggerFactory.getLogger(TotalSchemaSdkGenerator.class);

    private final Map<String, String> variables;

    /**
     * Creates a generator for the given variable map.
     *
     * @param variables the name-to-value map that will be baked into the generated module; must not
     *     be {@code null}
     */
    TotalSchemaSdkGenerator(Map<String, String> variables) {
        this.variables = Objects.requireNonNull(variables, "variables is null");
    }

    /**
     * Creates a fresh temporary directory, then writes the {@code totalschema/} package into it.
     *
     * <p>Creates {@code <tempDir>/totalschema/__init__.py} (empty marker) and {@code
     * <tempDir>/totalschema/sdk.py} (the {@code Variable} class). The caller is responsible for
     * adding {@code tempDir} to {@code PYTHONPATH} so that {@code import totalschema} resolves, and
     * for calling {@link #cleanup(Path)} with the returned path afterwards.
     *
     * @return the temporary directory that was created and populated
     * @throws UncheckedIOException if any file-system operation fails
     */
    Path generate() {
        final Path tempDir;
        try {
            tempDir = Files.createTempDirectory("totalschema-sdk-");
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to create temporary directory for TotalSchema SDK", e);
        }
        final Path packageDir = tempDir.resolve(PACKAGE_DIR);
        log.debug("Generating TotalSchema SDK package in temp dir: {}", packageDir);
        try {
            Files.createDirectories(packageDir);
            Files.writeString(packageDir.resolve("__init__.py"), "", StandardCharsets.UTF_8);
            Files.writeString(
                    packageDir.resolve("sdk.py"), buildSdkModule(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to generate TotalSchema SDK package in " + tempDir, e);
        }
        log.debug("TotalSchema SDK package generated successfully in: {}", packageDir);
        return tempDir;
    }

    /**
     * Recursively removes the temporary directory that was created by {@link #generate()}.
     *
     * <p>This method is a no-op when {@code tempDir} does not exist. Deletion failures are logged
     * as warnings rather than thrown, so that a cleanup problem never masks a script error.
     *
     * @param tempDir the path returned by a previous call to {@link #generate()}
     */
    void cleanup(Path tempDir) {
        if (!Files.exists(tempDir)) {
            log.debug("TotalSchema SDK temp directory not found, nothing to clean up: {}", tempDir);
            return;
        }
        log.debug("Removing TotalSchema SDK temp directory: {}", tempDir);
        try {
            try (var walk = Files.walk(tempDir)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(
                                p -> {
                                    log.debug("Deleting SDK file: {}", p);
                                    try {
                                        Files.delete(p);
                                    } catch (IOException e) {
                                        log.warn("Could not delete SDK file: {}", p, e);
                                    }
                                });
            }
            log.debug("TotalSchema SDK temp directory removed: {}", tempDir);
        } catch (IOException e) {
            log.warn("Could not remove TotalSchema SDK temp directory: {}", tempDir, e);
        }
    }

    /**
     * Builds the content of {@code sdk.py}.
     *
     * <p>Variable values are stored as Base64-encoded string literals and decoded at import time.
     * This approach is safe for any value content: multiline strings, embedded quotes, backslashes,
     * and arbitrary Unicode all round-trip correctly without complex escaping.
     *
     * @return the full source text of the generated module
     */
    private String buildSdkModule() {
        final StringBuilder sb = new StringBuilder();
        sb.append("# Auto-generated by TotalSchema — do not edit.\n");
        sb.append("# Values are base64-encoded to preserve exact content regardless of\n");
        sb.append("# whitespace, quotes, or special characters.\n");
        sb.append("import base64 as _base64\n");
        sb.append("\n");
        sb.append("_VARIABLES = {\n");
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            final String encodedValue =
                    Base64.getEncoder()
                            .encodeToString(entry.getValue().getBytes(StandardCharsets.UTF_8));
            sb.append("    ")
                    .append(toPythonStringLiteral(entry.getKey()))
                    .append(": _base64.b64decode(")
                    .append(toPythonStringLiteral(encodedValue))
                    .append(").decode(\"utf-8\"),\n");
        }
        sb.append("}\n");
        sb.append("\n");
        sb.append("\n");
        sb.append("class Variable:\n");
        sb.append(
                "    \"\"\"Provides access to TotalSchema variables resolved at deployment time.\"\"\"\n");
        sb.append("\n");
        sb.append("    @staticmethod\n");
        sb.append("    def get(name):\n");
        sb.append("        \"\"\"Return the value of the named TotalSchema variable.\n");
        sb.append("\n");
        sb.append("        Args:\n");
        sb.append("            name: the variable name as declared under ``sdk.variables``\n");
        sb.append("                  in ``totalschema.yml``.\n");
        sb.append("\n");
        sb.append("        Returns:\n");
        sb.append("            The variable value as a ``str``.\n");
        sb.append("\n");
        sb.append("        Raises:\n");
        sb.append("            KeyError: if *name* is not present in the variable map.\n");
        sb.append("        \"\"\"\n");
        sb.append("        if name not in _VARIABLES:\n");
        sb.append("            raise KeyError(\"Unknown TotalSchema variable: \" + repr(name))\n");
        sb.append("        return _VARIABLES[name]\n");
        return sb.toString();
    }

    /**
     * Wraps {@code value} in a Python double-quoted string literal, escaping backslashes,
     * double-quotes, and ASCII control characters.
     *
     * <p>Since Base64-encoded values only contain {@code [A-Za-z0-9+/=]}, this method effectively
     * only needs to handle variable <em>keys</em> that may contain special characters.
     *
     * @param value the raw string to wrap
     * @return a Python string literal including the surrounding double-quote characters
     */
    private static String toPythonStringLiteral(String value) {
        final StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '"') {
                sb.append("\\\"");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else {
                sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
