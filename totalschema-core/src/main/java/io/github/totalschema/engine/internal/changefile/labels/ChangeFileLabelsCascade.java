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

package io.github.totalschema.engine.internal.changefile.labels;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Carries the accumulated cascaded labels from the root of the change tree down to a specific
 * directory. Immutable — each call to {@link #withDirectory} returns a new instance.
 *
 * <p>Usage during directory traversal:
 *
 * <pre>{@code
 * ChangeFileLabelsCascade cascade = ChangeFileLabelsCascade.empty();
 * cascade = cascade.withDirectory(rootDir, mode);   // picks up root-level labels
 * cascade = cascade.withDirectory(subDir, mode);    // inherits and extends
 * Map<String, List<String>> effective = cascade.resolve(changeFileLabels, filePath);
 * }</pre>
 */
public final class ChangeFileLabelsCascade {

    private static final ChangeFileLabelsCascade EMPTY =
            new ChangeFileLabelsCascade(Collections.emptyMap());

    /** The accumulated global labels from all ancestor directories visited so far. */
    private final Map<String, List<String>> accumulatedLabels;

    private ChangeFileLabelsCascade(Map<String, List<String>> accumulatedLabels) {
        this.accumulatedLabels = accumulatedLabels;
    }

    /** Returns an empty cascade with no inherited labels. */
    public static ChangeFileLabelsCascade empty() {
        return EMPTY;
    }

    /**
     * Returns a new cascade that incorporates the global labels from a {@link ChangeFileLabels}
     * instance (i.e. the labels loaded from a specific directory). The {@code filePatterns} labels
     * defined in that directory are <em>not</em> included in the cascade — they only affect files
     * directly in that directory.
     *
     * @param directoryLabels the labels loaded from the directory being entered
     * @param mode the inheritance mode governing key conflicts
     * @return a new cascade with the directory's global labels applied
     */
    public ChangeFileLabelsCascade withDirectory(
            ChangeFileLabels directoryLabels, LabelInheritanceMode mode) {

        if (directoryLabels.isEmpty()) {
            return this;
        }

        Map<String, List<String>> globalLabels = directoryLabels.getGlobalLabels();
        if (globalLabels.isEmpty()) {
            return this;
        }

        Map<String, List<String>> merged = new LinkedHashMap<>(accumulatedLabels);

        for (Map.Entry<String, List<String>> entry : globalLabels.entrySet()) {
            String key = entry.getKey();
            List<String> newValues = entry.getValue();

            if (mode == LabelInheritanceMode.MERGE && merged.containsKey(key)) {
                // Merge: union of parent values + child values
                List<String> combined = new ArrayList<>(merged.get(key));
                combined.addAll(newValues);
                merged.put(key, Collections.unmodifiableList(combined));
            } else {
                // Override: child replaces parent (also the default for keys not yet seen)
                merged.put(key, newValues);
            }
        }

        return new ChangeFileLabelsCascade(Collections.unmodifiableMap(merged));
    }

    /**
     * Loads {@link ChangeFileLabels} for the given directory and returns a new cascade with those
     * global labels applied.
     *
     * @param directory the directory whose label file should be loaded
     * @param mode the inheritance mode
     * @return a new cascade incorporating the directory's global labels
     * @throws IOException if loading of label file fails
     */
    public ChangeFileLabelsCascade withDirectory(Path directory, LabelInheritanceMode mode)
            throws IOException {
        return withDirectory(ChangeFileLabels.load(directory), mode);
    }

    /**
     * Resolves the effective labels for a specific change file. Combines:
     *
     * <ol>
     *   <li>The accumulated cascaded labels from ancestor directories (this cascade).
     *   <li>The current directory's global labels (already in this cascade if applied via {@link
     *       #withDirectory}).
     *   <li>The current directory's file-pattern labels for the given file (via {@code
     *       currentDirLabels.getEffectiveLabelsForFile}).
     * </ol>
     *
     * <p>File-pattern labels override cascaded labels for clashing keys.
     *
     * @param currentDirLabels the labels loaded from the file's immediate parent directory
     * @param filePath the absolute or relative path of the change file; must have a non-null
     *     filename component
     * @return the fully resolved effective labels
     * @throws NullPointerException if {@code filePath.getFileName()} is null
     */
    public Map<String, List<String>> resolve(ChangeFileLabels currentDirLabels, Path filePath) {
        Path fileName = filePath.getFileName();
        if (fileName == null) {
            throw new NullPointerException("File name cannot be null for: " + filePath);
        }

        // Start with accumulated cascade (includes ancestor globals + current dir globals
        // if withDirectory was already called for the current directory).
        Map<String, List<String>> result = new LinkedHashMap<>(accumulatedLabels);

        // Apply file-pattern entries from the current directory (these override cascade labels).
        // Note: only the filePatterns portion matters here since global labels from currentDir
        // are already in accumulatedLabels after withDirectory() was called.
        Map<String, List<String>> filePatternLabels =
                currentDirLabels.getEffectiveLabelsForFile(fileName);

        // filePatternLabels contains globals + matched patterns; we only want to overlay
        // the additional labels that patterns contribute on top of what is already cascaded.
        // Since patterns override globals in the same file, and globals are already cascaded,
        // we put all of filePatternLabels — this correctly overrides cascade for pattern matches.
        result.putAll(filePatternLabels);

        return Collections.unmodifiableMap(result);
    }
}
