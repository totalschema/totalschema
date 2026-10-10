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

package io.github.totalschema.connector.gcp.bigquery;

import static io.github.totalschema.spi.factory.ArgumentSpecification.*;

import com.google.cloud.bigquery.BigQuery;
import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.database.sql.SQLDatabase;
import io.github.totalschema.spi.factory.ArgumentHandler;
import io.github.totalschema.spi.factory.ArgumentSpecification;
import io.github.totalschema.spi.factory.ComponentFactory;
import java.util.List;
import java.util.Optional;

/**
 * Component factory for creating {@link BigQuerySQLDatabase} instances.
 *
 * <p>This factory creates SQLDatabase implementations backed by Google Cloud BigQuery, allowing
 * direct SQL execution and query operations against BigQuery datasets.
 */
public final class BigQuerySQLDatabaseComponentFactory extends ComponentFactory<SQLDatabase> {

    public static final String QUALIFIER = "bigquery";

    private static final ArgumentSpecification<String> NAME =
            string("name").withConstraint(notBlank());

    private static final ArgumentSpecification<Configuration> CONFIGURATION =
            configuration("configuration");

    private static final ArgumentHandler ARGUMENTS =
            ArgumentHandler.getInstance(
                    BigQuerySQLDatabaseComponentFactory.class, NAME, CONFIGURATION);

    @Override
    public boolean isLazy() {
        return false;
    }

    @Override
    public Class<SQLDatabase> getComponentType() {
        return SQLDatabase.class;
    }

    @Override
    public Optional<String> getQualifier() {
        return Optional.of(QUALIFIER);
    }

    @Override
    public List<Class<?>> getDependencies() {
        return List.of(BigQuery.class);
    }

    @Override
    public List<ArgumentSpecification<?>> getArgumentSpecifications() {
        return ARGUMENTS.getSpecifications();
    }

    @Override
    public SQLDatabase createComponent(Context context, List<Object> arguments) {

        ARGUMENTS.validateStructure(arguments);

        String name = ARGUMENTS.getArgument(NAME, arguments);
        Configuration configuration = ARGUMENTS.getArgument(CONFIGURATION, arguments);

        // Retrieve the BigQuery client from context
        // The client is obtained using the configuration to support multi-project setups
        BigQuery bigQuery = context.get(BigQuery.class, null, configuration);

        return new BigQuerySQLDatabase(name, bigQuery, configuration);
    }
}
