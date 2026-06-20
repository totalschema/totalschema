/*
 * totalschema: tool for managing database versioning and schema changes with ease.
 * Copyright (C) 2025-2026 totalschema development team
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

package io.github.totalschema.engine.internal.changefile.labels;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import org.yaml.snakeyaml.Yaml;

/**
 * Parses and holds the labels defined in a single {@code totalschema-labels.yml} file.
 *
 * <p>A label file has two kinds of entries:
 *
 * <ul>
 *   <li><b>Global labels</b> — top-level {@code KEY: value} (or {@code KEY: [v1, v2]}) pairs that
 *       apply to every change file in the directory and cascade to all descendants.
 *   <li><b>File-pattern entries</b> — declared under the reserved {@code filePatterns} key; each
 *       entry has a {@code match} glob (string or list) and a {@code labels} map. These apply only
 *       to files whose filename matches the glob, within the current directory only (no cascade).
 * </ul>
 */
public final class ChangeFileLabels {

    /** The reserved YAML key that introduces file-level pattern sections. */
    public static final String FILE_PATTERNS_KEY = "filePatterns";

    /** The filename of the label configuration file. */
    public static final String LABEL_FILE_NAME = "totalschema-labels.yml";

    /** Regex for validating label keys: must start and end with alphanumeric. */
    private static final java.util.regex.Pattern VALID_KEY_PATTERN =
            java.util.regex.Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]*[A-Za-z0-9]|[A-Za-z0-9]");

    private static final ChangeFileLabels EMPTY =
            new ChangeFileLabels(Collections.emptyMap(), Collections.emptyList());

    private final Map<String, List<String>> globalLabels;
    private final List<FilePatternEntry> filePatternEntries;

    private ChangeFileLabels(
            Map<String, List<String>> globalLabels, List<FilePatternEntry> filePatternEntries) {
        // these are always unmodifiable, as the constructor is called from
        // internal methods, that guarantee this
        this.globalLabels = globalLabels;
        this.filePatternEntries = filePatternEntries;
    }

    /**
     * Returns an empty label set (no global labels, no pattern entries). Used when no label file is
     * present in a directory.
     */
    public static ChangeFileLabels empty() {
        return EMPTY;
    }

    /**
     * Loads and parses the {@code totalschema-labels.yml} file in the given directory. If no such
     * file exists, returns {@link #empty()}.
     *
     * @param directory the directory to look in
     * @return the parsed labels, or empty if no label file is present
     * @throws IllegalArgumentException if the file is present but malformed
     * @throws IOException if the label fail could not be loaded
     */
    public static ChangeFileLabels load(Path directory) throws IOException {
        Path labelFile = directory.resolve(LABEL_FILE_NAME);
        if (!Files.exists(labelFile)) {
            return empty();
        }

        try (InputStream is = Files.newInputStream(labelFile)) {
            Yaml yaml = new Yaml();
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = yaml.loadAs(is, Map.class);

            if (raw == null || raw.isEmpty()) {
                return empty();
            }

            return parse(raw, labelFile);

        } catch (IOException e) {
            throw new IOException("Failed to read label file: " + labelFile, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static ChangeFileLabels parse(Map<String, Object> raw, Path labelFile) {
        Map<String, List<String>> globalLabels = new LinkedHashMap<>();
        List<FilePatternEntry> filePatternEntries = new ArrayList<>();

        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (FILE_PATTERNS_KEY.equals(key)) {
                if (!(value instanceof List)) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "In label file '%s': '%s' must be a YAML list",
                                    labelFile, FILE_PATTERNS_KEY));
                }
                filePatternEntries.addAll(parseFilePatterns((List<Object>) value, labelFile));
            } else {
                validateKey(key, labelFile);
                globalLabels.put(key, toStringList(value, key, labelFile));
            }
        }

        return new ChangeFileLabels(
                Collections.unmodifiableMap(globalLabels),
                Collections.unmodifiableList(filePatternEntries));
    }

    @SuppressWarnings("unchecked")
    private static List<FilePatternEntry> parseFilePatterns(List<Object> entries, Path labelFile) {
        List<FilePatternEntry> result = new ArrayList<>();
        int index = 0;
        for (Object item : entries) {
            if (!(item instanceof Map)) {
                throw new IllegalArgumentException(
                        String.format(
                                "In label file '%s': each '%s' entry must be a YAML mapping (index %d)",
                                labelFile, FILE_PATTERNS_KEY, index));
            }
            Map<String, Object> entryMap = (Map<String, Object>) item;

            Object matchObj = entryMap.get("match");
            Object labelsObj = entryMap.get("labels");

            if (matchObj == null) {
                throw new IllegalArgumentException(
                        String.format(
                                "In label file '%s': '%s' entry at index %d is missing required 'match' field",
                                labelFile, FILE_PATTERNS_KEY, index));
            }
            if (labelsObj == null) {
                throw new IllegalArgumentException(
                        String.format(
                                "In label file '%s': '%s' entry at index %d is missing required 'labels' field",
                                labelFile, FILE_PATTERNS_KEY, index));
            }
            if (!(labelsObj instanceof Map)) {
                throw new IllegalArgumentException(
                        String.format(
                                "In label file '%s': 'labels' in '%s' entry at index %d must be a YAML mapping",
                                labelFile, FILE_PATTERNS_KEY, index));
            }

            List<String> patterns = parseMatchPatterns(matchObj, labelFile, index);
            Map<String, List<String>> labels =
                    parseLabelsMap((Map<String, Object>) labelsObj, labelFile);
            result.add(new FilePatternEntry(patterns, labels));
            index++;
        }
        return result;
    }

    private static List<String> parseMatchPatterns(Object matchObj, Path labelFile, int index) {
        if (matchObj instanceof String) {
            String pattern = ((String) matchObj).trim();
            if (pattern.isEmpty()) {
                throw new IllegalArgumentException(
                        String.format(
                                "In label file '%s': 'match' in 'filePatterns' entry at index %d must not be empty",
                                labelFile, index));
            }
            return Collections.singletonList(pattern);
        } else if (matchObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> matchList = (List<Object>) matchObj;
            if (matchList.isEmpty()) {
                throw new IllegalArgumentException(
                        String.format(
                                "In label file '%s': 'match' list in 'filePatterns' entry at index %d must not be empty",
                                labelFile, index));
            }
            List<String> patterns = new ArrayList<>();
            for (Object p : matchList) {
                if (p == null || p.toString().trim().isEmpty()) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "In label file '%s': 'match' list in 'filePatterns' entry at index %d contains an empty pattern",
                                    labelFile, index));
                }
                patterns.add(p.toString().trim());
            }
            return Collections.unmodifiableList(patterns);
        } else {
            throw new IllegalArgumentException(
                    String.format(
                            "In label file '%s': 'match' in 'filePatterns' entry at index %d must be a string or list",
                            labelFile, index));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<String>> parseLabelsMap(
            Map<String, Object> labelsMap, Path labelFile) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : labelsMap.entrySet()) {
            validateKey(e.getKey(), labelFile);
            result.put(e.getKey(), toStringList(e.getValue(), e.getKey(), labelFile));
        }
        return Collections.unmodifiableMap(result);
    }

    private static void validateKey(String key, Path labelFile) {
        if (key == null || !VALID_KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException(
                    String.format(
                            "In label file '%s': invalid label key '%s'. Keys must begin and end with an alphanumeric character and may contain alphanumeric, '-', and '_' characters.",
                            labelFile, key));
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> toStringList(Object value, String key, Path labelFile) {
        if (value instanceof List) {
            List<Object> list = (List<Object>) value;
            List<String> result = new ArrayList<>(list.size());
            for (Object item : list) {
                if (item == null) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "In label file '%s': label key '%s' has a null value in its list",
                                    labelFile, key));
                }
                result.add(item.toString());
            }
            return Collections.unmodifiableList(result);
        } else if (value != null) {
            return Collections.singletonList(value.toString());
        } else {
            throw new IllegalArgumentException(
                    String.format(
                            "In label file '%s': label key '%s' has a null value", labelFile, key));
        }
    }

    /**
     * Returns the global labels defined in this file (those outside any {@code filePatterns}
     * entry). These cascade to descendant directories.
     */
    public Map<String, List<String>> getGlobalLabels() {
        return globalLabels;
    }

    /**
     * Computes the effective labels for a file with the given filename. The result is:
     *
     * <ol>
     *   <li>Start with the global labels from this file.
     *   <li>For each {@code filePatterns} entry whose pattern matches the filename, union its
     *       labels on top (pattern-entry labels override global labels for clashing keys).
     * </ol>
     *
     * <p>Note: ancestor cascade labels are handled by {@link ChangeFileLabelsCascade}; this method
     * only contributes the current-directory portion.
     *
     * @param fileName the bare filename (single path element, no directory component) to match
     *     against patterns
     * @return the effective labels contributed by this file for the given filename
     * @throws NullPointerException if {@code fileName} is null
     * @throws IllegalArgumentException if {@code fileName} contains a directory component
     */
    public Map<String, List<String>> getEffectiveLabelsForFile(Path fileName) {
        Objects.requireNonNull(fileName, "fileName must not be null");
        if (fileName.getNameCount() != 1) {
            throw new IllegalArgumentException(
                    "fileName must be a bare filename with no directory component, got: "
                            + fileName);
        }
        Map<String, List<String>> result = new LinkedHashMap<>(globalLabels);

        for (FilePatternEntry entry : filePatternEntries) {
            if (entry.matches(fileName)) {
                // Pattern-entry labels override global for clashing keys
                result.putAll(entry.getLabels());
            }
        }

        return Collections.unmodifiableMap(result);
    }

    /** Returns true if this label set has no global labels and no file-pattern entries. */
    public boolean isEmpty() {
        return globalLabels.isEmpty() && filePatternEntries.isEmpty();
    }

    /** A single entry under the {@code filePatterns} key. */
    static final class FilePatternEntry {

        private final List<String> patterns;
        private final List<PathMatcher> matchers;
        private final Map<String, List<String>> labels;

        FilePatternEntry(List<String> patterns, Map<String, List<String>> labels) {
            List<PathMatcher> pathMatchers = new ArrayList<>(patterns.size());

            // stored for logging/debug purposes only, as the
            // PathMatchers do not show meaningful details in their toString()
            this.patterns = patterns;

            // patterns are compiled to PathMatcher here, once
            FileSystem fileSystem = FileSystems.getDefault();
            for (String pattern : patterns) {
                PathMatcher pathMatcher = fileSystem.getPathMatcher("glob:" + pattern);
                pathMatchers.add(pathMatcher);
            }

            this.matchers = Collections.unmodifiableList(pathMatchers);
            this.labels = labels;
        }

        /**
         * Returns true if the given filename matches any of this entry's glob patterns (OR
         * semantics).
         */
        boolean matches(Path fileName) {
            for (PathMatcher matcher : matchers) {
                if (matcher.matches(fileName)) {
                    return true;
                }
            }
            return false;
        }

        Map<String, List<String>> getLabels() {
            return labels;
        }

        @Override
        public String toString() {
            return "FilePatternEntry{" + "patterns=" + patterns + ", labels=" + labels + '}';
        }
    }
}
