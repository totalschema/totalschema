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

package io.github.totalschema.engine.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable value object that encapsulates both a path-based filter expression and a set of change
 * file group label filters.
 *
 * <p>A {@code null} or absent {@code filterExpression} means no path restriction. An empty {@code
 * labelFilters} list means no label restriction. When both are present, a change file must satisfy
 * both (AND semantics).
 *
 * <p>Use the {@link Builder} to construct instances, or convenience factory methods:
 *
 * <pre>{@code
 * // No restrictions — selects all files
 * ChangeFileSelector.empty()
 *
 * // Path filter only (backward-compatible)
 * ChangeFileSelector.of("postgresql/.*")
 *
 * // Label filter(s) only
 * ChangeFileSelector.builder().label("targetRelease=2027-Q1-01").build()
 *
 * // Combined
 * ChangeFileSelector.builder()
 *     .filterExpression("postgresql/.*")
 *     .label("targetRelease=2027-Q1-01")
 *     .label("JIRA=FOOBAR-1234")
 *     .build()
 * }</pre>
 */
public final class ChangeFileSelector {

    private static final ChangeFileSelector EMPTY =
            new ChangeFileSelector(null, Collections.emptyList());

    private final String filterExpression;
    private final List<String> labelFilters;

    private ChangeFileSelector(String filterExpression, List<String> labelFilters) {
        Objects.requireNonNull(labelFilters, "labelFilters must not be null");

        this.filterExpression = filterExpression;
        this.labelFilters = List.copyOf(labelFilters);
    }

    /**
     * Returns a selector that imposes no restrictions (selects all change files). This is the
     * backward-compatible default.
     */
    public static ChangeFileSelector empty() {
        return EMPTY;
    }

    /**
     * Returns a selector with only a path filter expression, equivalent to the legacy {@code
     * filterExpression} parameter.
     *
     * @param filterExpression the regex pattern matched against the relative path; may be null
     */
    public static ChangeFileSelector of(String filterExpression) {
        if (filterExpression == null) {
            return EMPTY;
        }
        return new ChangeFileSelector(filterExpression, Collections.emptyList());
    }

    /** Returns a new builder. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns {@code true} if this selector imposes no restrictions, i.e. it selects all change
     * files.
     *
     * <p>A selector is considered empty when its path filter expression is {@code null} and its
     * label filter list is empty.
     *
     * @return {@code true} when this instance is equivalent to {@link #empty()}
     */
    public boolean isEmpty() {
        return filterExpression == null && labelFilters.isEmpty();
    }

    /**
     * Gets the path filter expression.
     *
     * @return the expression, or {@code null} if none
     */
    public String getFilterExpression() {
        return filterExpression;
    }

    /**
     * Gets the label filter expressions ({@code KEY=VALUE} strings).
     *
     * @return an unmodifiable list; never null; empty means no label restriction
     */
    public List<String> getLabelFilters() {
        return labelFilters;
    }

    /**
     * Returns a human-readable description of the restrictions imposed by this selector.
     *
     * <p>Examples:
     *
     * <ul>
     *   <li>{@code "all change files"} — no restrictions
     *   <li>{@code "path matching 'postgresql/.*'"} — path filter only
     *   <li>{@code "labels [targetRelease=2027-Q1-01, JIRA=FOOBAR-1234]"} — label filters only
     *   <li>{@code "path matching 'postgresql/.*' and labels [targetRelease=2027-Q1-01]"} — both
     * </ul>
     *
     * @return a non-null, non-empty human-readable description
     */
    public String getDescription() {
        if (isEmpty()) {
            return "all change files";
        }
        final StringBuilder sb = new StringBuilder();
        if (filterExpression != null) {
            sb.append("path matching '").append(filterExpression).append('\'');
        }
        if (!labelFilters.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" and ");
            }
            if (labelFilters.size() == 1) {
                sb.append("label: ").append(labelFilters);
            } else {
                sb.append("labels: ").append(labelFilters);
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "ChangeFileSelector{"
                + "filterExpression='"
                + filterExpression
                + '\''
                + ", labelFilters="
                + labelFilters
                + '}';
    }

    /** Builder for {@link ChangeFileSelector}. */
    public static final class Builder {

        private String filterExpression;
        private final List<String> labelFilters = new ArrayList<>();

        private Builder() {}

        /**
         * Sets the path filter expression.
         *
         * @param filterExpression regex matched against the relative file path; null means no path
         *     restriction
         */
        public Builder filterExpression(String filterExpression) {
            this.filterExpression = filterExpression;
            return this;
        }

        /**
         * Adds a label filter expression. Multiple calls result in AND semantics (all must match).
         *
         * @param labelExpression a {@code KEY=VALUE} string
         */
        public Builder label(String labelExpression) {
            this.labelFilters.add(labelExpression);
            return this;
        }

        /**
         * Adds all label filter expressions from a list.
         *
         * @param labelExpressions list of {@code KEY=VALUE} strings; may be null or empty
         */
        public Builder labels(List<String> labelExpressions) {
            if (labelExpressions != null) {
                this.labelFilters.addAll(labelExpressions);
            }
            return this;
        }

        /** Builds the {@link ChangeFileSelector}. */
        public ChangeFileSelector build() {
            // the constructor creates a defensive copy of labelFilters,
            // so we can safely pass the mutable list
            return new ChangeFileSelector(filterExpression, labelFilters);
        }
    }
}
