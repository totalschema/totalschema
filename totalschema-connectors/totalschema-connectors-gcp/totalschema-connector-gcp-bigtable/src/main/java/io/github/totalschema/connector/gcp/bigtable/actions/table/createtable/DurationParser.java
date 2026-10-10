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

package io.github.totalschema.connector.gcp.bigtable.actions.table.createtable;

import java.util.Locale;

/**
 * Utility class for parsing human-readable duration strings into {@link org.threeten.bp.Duration}
 * objects.
 *
 * <p>This parser supports the following duration formats:
 *
 * <ul>
 *   <li><strong>Days:</strong> "d", "day", "days" (e.g., "7d" = 7 days)
 *   <li><strong>Hours:</strong> "h", "hour", "hours" (e.g., "24h" = 24 hours)
 *   <li><strong>Minutes:</strong> "m", "min", "minute", "minutes" (e.g., "30m" = 30 minutes)
 *   <li><strong>Seconds:</strong> "s", "sec", "second", "seconds" (e.g., "3600s" = 3600 seconds)
 *   <li><strong>Milliseconds:</strong> "ms", "milli", "millis", "millisecond", "milliseconds"
 *       (e.g., "1000ms" = 1000 milliseconds)
 * </ul>
 *
 * <p>Examples:
 *
 * <pre>
 * DurationParser.parse("7d")     → 7 days
 * DurationParser.parse("24h")    → 24 hours
 * DurationParser.parse("90days") → 90 days
 * DurationParser.parse("30m")    → 30 minutes
 * </pre>
 */
public final class DurationParser {

    /** Private constructor to prevent instantiation. */
    private DurationParser() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Parses a duration string into an {@link org.threeten.bp.Duration}.
     *
     * @param duration the duration string (e.g., "7d", "24h", "30m")
     * @return the parsed Duration
     * @throws IllegalArgumentException if the duration is null, empty, or has an invalid format
     */
    public static org.threeten.bp.Duration parse(String duration) {
        if (duration == null || duration.isEmpty()) {
            throw new IllegalArgumentException("Duration cannot be null or empty");
        }

        String trimmed = duration.trim().toLowerCase(Locale.ENGLISH);

        try {
            // Extract number and unit
            int unitIndex = -1;
            for (int i = 0; i < trimmed.length(); i++) {
                if (!Character.isDigit(trimmed.charAt(i)) && trimmed.charAt(i) != '.') {
                    unitIndex = i;
                    break;
                }
            }

            if (unitIndex == -1) {
                throw new IllegalArgumentException(
                        "Duration must have a unit (d, h, m, s, ms): " + duration);
            }

            String numberPart = trimmed.substring(0, unitIndex);
            String unitPart = trimmed.substring(unitIndex);

            long value = Long.parseLong(numberPart);

            switch (unitPart) {
                case "d":
                case "day":
                case "days":
                    return org.threeten.bp.Duration.ofDays(value);
                case "h":
                case "hour":
                case "hours":
                    return org.threeten.bp.Duration.ofHours(value);
                case "m":
                case "min":
                case "minute":
                case "minutes":
                    return org.threeten.bp.Duration.ofMinutes(value);
                case "s":
                case "sec":
                case "second":
                case "seconds":
                    return org.threeten.bp.Duration.ofSeconds(value);
                case "ms":
                case "milli":
                case "millis":
                case "millisecond":
                case "milliseconds":
                    return org.threeten.bp.Duration.ofMillis(value);
                default:
                    throw new IllegalArgumentException(
                            "Unknown duration unit: " + unitPart + " (use d, h, m, s, ms)");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid duration format: " + duration, e);
        }
    }
}
