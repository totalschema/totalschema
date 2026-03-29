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

package io.github.totalschema.engine.services.sql;

import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.ComponentFactory;
import io.github.totalschema.spi.sql.SqlDialect;
import java.util.List;

/**
 * ComponentFactory for the default SQL dialect implementation.
 *
 * <p>This factory creates the default SQL dialect which provides standard SQL syntax. The default
 * dialect is used when no specific dialect is configured or when a configured dialect is not
 * available.
 *
 * <p>Usage: {@code context.get(SqlDialect.class, "default")}
 */
public final class DefaultSqlDialectComponentFactory extends ComponentFactory<SqlDialect> {

    @Override
    public boolean isLazy() {
        return false; // Eager initialization - always available
    }

    @Override
    public Class<SqlDialect> getComponentType() {
        return SqlDialect.class;
    }

    @Override
    public String getQualifier() {
        return "default";
    }

    @Override
    public List<Class<?>> getRequiredContextTypes() {
        return List.of();
    }

    @Override
    public SqlDialect newComponent(Context context, Object... arguments) {
        return new DefaultSqlDialect();
    }
}
