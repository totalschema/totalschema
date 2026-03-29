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

package io.github.totalschema.integrations.bigquery.sql;

import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.factory.ArgumentSpecification;
import io.github.totalschema.spi.factory.ComponentFactory;
import io.github.totalschema.spi.sql.SqlDialect;
import java.util.List;
import java.util.Optional;

/**
 * ComponentFactory for BigQuery SQL dialect implementation.
 *
 * <p>This factory creates the BigQuery-specific SQL dialect which handles BigQuery's unique syntax
 * requirements such as STRING type instead of VARCHAR.
 *
 * <p>Usage: {@code context.get(SqlDialect.class, "bigquery")}
 */
public final class BigQuerySqlDialectComponentFactory extends ComponentFactory<SqlDialect> {

    @Override
    public boolean isLazy() {
        return false; // Eager initialization
    }

    @Override
    public Class<SqlDialect> getComponentType() {
        return SqlDialect.class;
    }

    @Override
    public Optional<String> getQualifier() {
        return Optional.of("bigquery");
    }

    @Override
    public List<Class<?>> getRequiredContextTypes() {
        return List.of();
    }

    @Override
    public List<ArgumentSpecification<?>> getArgumentSpecifications() {
        return List.of();
    }

    @Override
    public SqlDialect createComponent(Context context, List<Object> arguments) {
        return new BigQuerySqlDialect();
    }
}
