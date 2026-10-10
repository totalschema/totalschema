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

package io.github.totalschema.engine.internal.state.database;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.engine.internal.changefile.ChangeFileFactory;
import io.github.totalschema.spi.database.sql.SQLDatabase;
import io.github.totalschema.spi.factory.ArgumentSpecification;
import io.github.totalschema.spi.factory.ComponentFactory;
import io.github.totalschema.spi.sql.SqlDialect;
import io.github.totalschema.spi.state.StateConstants;
import io.github.totalschema.spi.state.StateRepository;
import java.util.List;
import java.util.Optional;

public class DatabaseStateRecordRepositoryFactory extends ComponentFactory<StateRepository> {

    @Override
    public boolean isLazy() {
        return true;
    }

    @Override
    public Class<StateRepository> getComponentType() {
        return StateRepository.class;
    }

    @Override
    public Optional<String> getQualifier() {
        return Optional.of("database");
    }

    @Override
    public List<Class<?>> getDependencies() {
        return List.of(Configuration.class, SqlDialect.class, ChangeFileFactory.class);
    }

    @Override
    public List<ArgumentSpecification<?>> getArgumentSpecifications() {
        return List.of();
    }

    @Override
    public StateRepository createComponent(Context context, List<Object> arguments) {

        Configuration stateConfig =
                context.get(Configuration.class)
                        .getPrefixNamespace(StateConstants.CONFIG_PROPERTY_NAMESPACE);

        Configuration dbConfig = stateConfig.getPrefixNamespace("database");

        // Get SQL dialect from configuration, or use "default"
        String dialectType = dbConfig.getString("dialect").orElse("default");
        SqlDialect sqlDialect = getSqlDialect(context, dialectType);

        ChangeFileFactory changeFileFactory = context.get(ChangeFileFactory.class);
        int changeFileNameMaxLength = changeFileFactory.getChangeFileNameMaxLength();

        // Create JdbcDatabase instance with logSql configuration defaulting to false
        Configuration configWithLogSqlSet =
                dbConfig.toBuilder().setIfAbsent("logSql", false).build();

        String databaseType = dbConfig.getString("type").orElse("jdbc");

        SQLDatabase database =
                context.get(SQLDatabase.class, databaseType, "state", configWithLogSqlSet);

        SqlDatabaseStateRecordRepository repository =
                new SqlDatabaseStateRecordRepository(
                        sqlDialect, database, changeFileFactory, changeFileNameMaxLength, dbConfig);

        repository.init();

        return repository;
    }

    private static SqlDialect getSqlDialect(Context context, String dialectType) {
        try {
            return context.get(SqlDialect.class, dialectType);
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    String.format("SQL Dialect '%s' not found", dialectType), e);
        }
    }
}
