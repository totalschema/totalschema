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

package io.github.totalschema.model;

/** Enumeration of change file types supported by the system. */
public enum ChangeType {
    /** Standard apply change executed once. */
    APPLY,
    /** Change executed every time, regardless of previous execution. */
    APPLY_ALWAYS,
    /** Change executed when file content has changed since last execution. */
    APPLY_ON_CHANGE,
    /** Revert change to rollback previously applied changes. */
    REVERT;

    /**
     * Converts a string to a ChangeType enum value.
     *
     * @param string the string to convert (case-insensitive)
     * @return the corresponding ChangeType
     * @throws IllegalStateException if the string cannot be mapped to a ChangeType
     */
    public static ChangeType getChangeType(String string) {

        for (ChangeType ct : values()) {
            if (ct.name().equalsIgnoreCase(string)) {
                return ct;
            }
        }

        throw new IllegalStateException(String.format("Could not map '%s' to ChangeType", string));
    }
}
