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

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Evaluates whether a change file's effective labels satisfy a set of label filter expressions.
 *
 * <p>Each expression is a {@code KEY=VALUE} string. When multiple expressions are supplied, all
 * must match (AND semantics). An empty filter list matches everything (backward-compatible no-op).
 */
public final class LabelFilter {

    private static final LabelFilter EMPTY = new LabelFilter(Collections.emptyList());

    private final List<LabelExpression> expressions;

    private LabelFilter(List<LabelExpression> expressions) {
        this.expressions = expressions;
    }

    /**
     * Creates a {@link LabelFilter} from a list of {@code KEY=VALUE} string expressions.
     *
     * @param rawExpressions the raw expressions; may be null or empty (treated as no filter)
     * @return a new filter, or {@link #empty()} if the list is null or empty
     * @throws IllegalArgumentException if any expression does not contain {@code =}
     */
    public static LabelFilter of(List<String> rawExpressions) {
        if (rawExpressions == null || rawExpressions.isEmpty()) {
            return EMPTY;
        }

        List<LabelExpression> parsed = new java.util.ArrayList<>(rawExpressions.size());
        for (String raw : rawExpressions) {
            int eq = raw.indexOf('=');
            if (eq < 1) {
                throw new IllegalArgumentException(
                        String.format(
                                "Invalid label filter expression '%s': must be in KEY=VALUE format",
                                raw));
            }
            String key = raw.substring(0, eq);
            String value = raw.substring(eq + 1);
            parsed.add(new LabelExpression(key, value));
        }

        return new LabelFilter(Collections.unmodifiableList(parsed));
    }

    /** Returns a filter that matches everything (no restrictions). */
    public static LabelFilter empty() {
        return EMPTY;
    }

    /**
     * Returns {@code true} if the given effective label set satisfies all filter expressions.
     *
     * <p>An empty filter always returns {@code true}.
     *
     * @param effectiveLabels the resolved labels for a change file
     * @return {@code true} if all expressions are satisfied
     */
    public boolean matches(Map<String, List<String>> effectiveLabels) {
        if (expressions.isEmpty()) {
            return true;
        }
        for (LabelExpression expr : expressions) {
            List<String> values = effectiveLabels.get(expr.key);
            if (values == null || !values.contains(expr.value)) {
                return false;
            }
        }
        return true;
    }

    /** Returns {@code true} if this filter has no expressions (matches everything). */
    public boolean isEmpty() {
        return expressions.isEmpty();
    }

    private static final class LabelExpression {
        final String key;
        final String value;

        LabelExpression(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}
