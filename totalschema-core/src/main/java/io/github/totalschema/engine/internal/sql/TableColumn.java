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

package io.github.totalschema.engine.internal.sql;

import java.util.Objects;

/**
 * Represents a column definition in a SQL table. Immutable value object containing the column name
 * and its SQL type expression.
 */
public final class TableColumn {
    private final String name;
    private final String typeExpression;

    /**
     * Creates a new table column definition.
     *
     * @param name the column name (must not be null or blank)
     * @param typeExpression the SQL type expression (e.g., "VARCHAR(255)", "TIMESTAMP")
     * @throws IllegalArgumentException if name is null or blank, or typeExpression is null
     */
    public TableColumn(String name, String typeExpression) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Column name must not be null or blank");
        }
        if (typeExpression == null) {
            throw new IllegalArgumentException("Type expression must not be null");
        }
        this.name = name;
        this.typeExpression = typeExpression;
    }

    /**
     * Gets the column name.
     *
     * @return the column name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the SQL type expression.
     *
     * @return the type expression
     */
    public String getTypeExpression() {
        return typeExpression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableColumn that = (TableColumn) o;
        return Objects.equals(name, that.name)
                && Objects.equals(typeExpression, that.typeExpression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, typeExpression);
    }

    @Override
    public String toString() {
        return "TableColumn{"
                + "name='"
                + name
                + '\''
                + ", typeExpression='"
                + typeExpression
                + '\''
                + '}';
    }
}
