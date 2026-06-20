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

/**
 * Controls how label keys from ancestor directories interact with the same keys defined in
 * descendant directories.
 *
 * <ul>
 *   <li>{@link #OVERRIDE} — the descendant's values replace the ancestor's values for the same key.
 *       This is the default.
 *   <li>{@link #MERGE} — the descendant's values are added to the ancestor's values for the same
 *       key (multi-value union).
 * </ul>
 *
 * <p>Configured globally via {@code labels.inheritance} in {@code totalschema.yml}.
 */
public enum LabelInheritanceMode {

    /** Descendant label values replace ancestor label values for the same key. Default mode. */
    OVERRIDE,

    /** Descendant label values are added to ancestor label values for the same key. */
    MERGE;

    /** The default mode used when no configuration is present. */
    public static final LabelInheritanceMode DEFAULT = OVERRIDE;

    /**
     * Parses a string value from configuration.
     *
     * @param value the string value; may be null
     * @return the matching mode, or {@link #DEFAULT} if the value is null or blank
     * @throws IllegalArgumentException if the value is non-blank but not a recognised mode name
     */
    public static LabelInheritanceMode fromConfig(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT;
        }
        try {
            return valueOf(value.toUpperCase(java.util.Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    String.format(
                            "Invalid value for 'labels.inheritance': '%s'. Expected 'override' or 'merge'.",
                            value));
        }
    }
}
