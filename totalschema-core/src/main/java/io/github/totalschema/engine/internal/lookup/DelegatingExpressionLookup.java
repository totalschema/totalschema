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

package io.github.totalschema.engine.internal.lookup;

import io.github.totalschema.spi.lookup.ExpressionLookup;
import java.util.function.Function;

public final class DelegatingExpressionLookup implements ExpressionLookup {

    private final String key;

    private final Function<String, String> delegate;

    private final String description;

    public DelegatingExpressionLookup(
            String key, Function<String, String> delegate, String description) {
        this.key = key;
        this.delegate = delegate;
        this.description = description;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String apply(String value) {
        return delegate.apply(value);
    }

    @Override
    public String toString() {
        return String.format("%s-->%s", key, description);
    }
}
