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

package io.github.totalschema.engine.internal.changefile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses a {@code .totalschemaignore} file and answers whether a given relative path should be
 * excluded from change-file discovery.
 *
 * <h2>File placement</h2>
 *
 * <p>An ignore file can be placed at the changes-root level and/or inside any sub-directory:
 *
 * <ul>
 *   <li><b>Root-level</b> {@code .totalschemaignore} — patterns are matched against the full
 *       relative path from the changes root. Both name-only patterns ({@code my_lib/}) and
 *       path-qualified patterns ({@code 3.X/3.0.0/scratch.py}) are supported.
 *   <li><b>Per-directory</b> {@code .totalschemaignore} — patterns are matched against the direct
 *       child name only (relative to the directory that contains the ignore file). Useful for
 *       isolating ignore rules to a specific subtree.
 * </ul>
 *
 * <p>The ignore file is placed at the root of the configured changes directory. Each non-blank line
 * that does not start with {@code #} is treated as a glob pattern:
 *
 * <ul>
 *   <li>A pattern ending with {@code /} is a <em>directory pattern</em>: it is matched against
 *       directory paths and implicitly excludes everything under that directory.
 *   <li>Any other pattern is a <em>file pattern</em>.
 *   <li>Patterns containing {@code /} (after stripping a trailing {@code /}) are matched against
 *       the full relative path provided by the caller (the changes root for root-level files; the
 *       directory name for per-directory files).
 *   <li>Patterns <em>not</em> containing {@code /} are matched against the last path component only
 *       (i.e. the file name or directory name), anywhere in the tree — analogous to {@code
 *       .gitignore} semantics.
 * </ul>
 *
 * <h2>Example</h2>
 *
 * <pre>{@code
 * # Ignore the helper-library directory and everything inside it
 * my_sample_py_lib/
 *
 * # Ignore all __init__.py files everywhere
 * __init__.py
 *
 * # Ignore a specific file by its full relative path
 * 3.X/3.0.0/scratch.py
 * }</pre>
 */
public final class ChangeFileIgnorePatterns {

    /** Name of the ignore file looked up in the changes root directory. */
    public static final String IGNORE_FILE_NAME = ".totalschemaignore";

    private static final ChangeFileIgnorePatterns EMPTY =
            new ChangeFileIgnorePatterns(Collections.emptyList(), Collections.emptyList());

    /**
     * Returns an instance that never ignores anything.
     *
     * <p>Useful as a no-op placeholder when no ignore file is present or when per-directory
     * patterns are not needed.
     *
     * @return the shared empty instance
     */
    public static ChangeFileIgnorePatterns empty() {
        return EMPTY;
    }

    private static final Logger log = LoggerFactory.getLogger(ChangeFileIgnorePatterns.class);

    /**
     * A compiled pattern that may match by full relative path or by name component only, depending
     * on whether the raw pattern contained a path separator.
     */
    private static final class CompiledPattern {

        private final PathMatcher matcher;
        private final boolean fullPathMatch; // true → match full relative path; false → name only

        CompiledPattern(PathMatcher matcher, boolean fullPathMatch) {
            this.matcher = matcher;
            this.fullPathMatch = fullPathMatch;
        }

        boolean matches(Path relativePath) {
            final boolean result;
            if (fullPathMatch) {
                result = matcher.matches(relativePath);
            } else {
                Path name = relativePath.getFileName();
                result = name != null && matcher.matches(name);
            }
            return result;
        }
    }

    private final List<CompiledPattern> filePatterns;
    private final List<CompiledPattern> directoryPatterns;

    private ChangeFileIgnorePatterns(
            List<CompiledPattern> filePatterns, List<CompiledPattern> directoryPatterns) {
        this.filePatterns = filePatterns;
        this.directoryPatterns = directoryPatterns;
    }

    /**
     * Returns a new instance whose patterns are the union of {@code this} and {@code other}.
     *
     * <p>Neither {@code this} nor {@code other} is modified. The returned instance evaluates {@code
     * isIgnoredFile} / {@code isIgnoredDirectory} as a logical OR of both sets of patterns, which
     * allows callers to accumulate ignore rules across multiple levels (root + per-directory)
     * without having to propagate separate instances.
     *
     * @param other the patterns to merge in; must not be {@code null}
     * @return a new combined {@code ChangeFileIgnorePatterns}
     */
    public ChangeFileIgnorePatterns combine(ChangeFileIgnorePatterns other) {
        Objects.requireNonNull(other, "other is null");
        if (other == EMPTY) {
            return this;
        }
        if (this == EMPTY) {
            return other;
        }
        final List<CompiledPattern> mergedFilePatterns =
                new ArrayList<>(filePatterns.size() + other.filePatterns.size());
        mergedFilePatterns.addAll(filePatterns);
        mergedFilePatterns.addAll(other.filePatterns);

        final List<CompiledPattern> mergedDirPatterns =
                new ArrayList<>(directoryPatterns.size() + other.directoryPatterns.size());
        mergedDirPatterns.addAll(directoryPatterns);
        mergedDirPatterns.addAll(other.directoryPatterns);

        return new ChangeFileIgnorePatterns(
                Collections.unmodifiableList(mergedFilePatterns),
                Collections.unmodifiableList(mergedDirPatterns));
    }

    /**
     * Loads ignore patterns from {@code <changesRoot>/.totalschemaignore}.
     *
     * <p>Returns an empty (no-op) instance when the file does not exist.
     *
     * @param changesRoot the root directory of change-file discovery
     * @return the loaded patterns, or an empty instance when the file is absent
     * @throws UncheckedIOException if the file exists but cannot be read
     */
    public static ChangeFileIgnorePatterns load(Path changesRoot) {
        final Path ignoreFile = changesRoot.resolve(IGNORE_FILE_NAME);
        if (!Files.exists(ignoreFile)) {
            log.debug("No {} found in {}; no paths will be ignored", IGNORE_FILE_NAME, changesRoot);
            return EMPTY;
        }

        log.debug("Loading ignore patterns from: {}", ignoreFile);

        final List<String> lines;
        try {
            lines = Files.readAllLines(ignoreFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + ignoreFile, e);
        }

        final List<CompiledPattern> filePatterns = new ArrayList<>();
        final List<CompiledPattern> directoryPatterns = new ArrayList<>();

        for (final String rawLine : lines) {
            final String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            final boolean isDirPattern = line.endsWith("/");
            final String pattern = isDirPattern ? line.substring(0, line.length() - 1) : line;
            final boolean fullPathMatch = pattern.contains("/");
            final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            final CompiledPattern compiled = new CompiledPattern(matcher, fullPathMatch);

            if (isDirPattern) {
                directoryPatterns.add(compiled);
            } else {
                filePatterns.add(compiled);
            }
            log.debug(
                    "Ignore pattern: '{}' (type={}, fullPath={})",
                    pattern,
                    isDirPattern ? "directory" : "file",
                    fullPathMatch);
        }

        log.debug(
                "Loaded {} file pattern(s) and {} directory pattern(s) from {}",
                filePatterns.size(),
                directoryPatterns.size(),
                ignoreFile);

        return new ChangeFileIgnorePatterns(
                Collections.unmodifiableList(filePatterns),
                Collections.unmodifiableList(directoryPatterns));
    }

    /**
     * Returns {@code true} if the given file's relative path should be excluded from discovery.
     *
     * <p>The following files are always excluded, regardless of configured patterns:
     *
     * <ul>
     *   <li>The ignore file itself ({@value #IGNORE_FILE_NAME}).
     *   <li>Any file whose name starts with {@code .} (dot-files / hidden files).
     * </ul>
     *
     * @param relativePath path of the file relative to the changes root
     * @return {@code true} when the file should be excluded from discovery
     */
    public boolean isIgnoredFile(Path relativePath) {
        final Path name = relativePath.getFileName();
        if (name != null && startsWithDot(name.toString())) {
            log.debug("Ignoring dot-file: {}", relativePath);
            return true;
        }
        final boolean ignored = filePatterns.stream().anyMatch(p -> p.matches(relativePath));
        if (ignored) {
            log.debug("Ignoring file: {}", relativePath);
        }
        return ignored;
    }

    /**
     * Returns {@code true} if the given directory's relative path matches any directory ignore
     * pattern.
     *
     * <p>Directories whose name starts with {@code .} (dot-directories / hidden directories) are
     * always excluded, regardless of configured patterns.
     *
     * <p>When a directory is ignored the engine will not recurse into it, effectively excluding all
     * files it contains.
     *
     * @param relativePath path of the directory relative to the changes root
     * @return {@code true} when the directory should be excluded from discovery
     */
    public boolean isIgnoredDirectory(Path relativePath) {
        final Path name = relativePath.getFileName();
        if (name != null && startsWithDot(name.toString())) {
            log.debug("Ignoring dot-directory: {}", relativePath);
            return true;
        }
        final boolean ignored = directoryPatterns.stream().anyMatch(p -> p.matches(relativePath));
        if (ignored) {
            log.debug("Ignoring directory: {}", relativePath);
        }
        return ignored;
    }

    /**
     * Returns {@code true} when {@code name} starts with a dot, indicating a hidden or meta file /
     * directory (e.g. {@code .totalschemaignore}, {@code .git}).
     *
     * @param name the file or directory name to test
     * @return {@code true} if {@code name} begins with {@code .}
     */
    private static boolean startsWithDot(String name) {
        return !name.isEmpty() && name.charAt(0) == '.';
    }
}
