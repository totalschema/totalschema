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

package io.github.totalschema.engine.internal.lock.database.repository.impl;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.engine.internal.lock.database.LockingComponentFactory;
import io.github.totalschema.engine.internal.lock.database.repository.spi.LockStateRepository;
import io.github.totalschema.jdbc.JdbcDatabase;
import io.github.totalschema.spi.factory.ArgumentSpecification;
import io.github.totalschema.spi.sql.SqlDialect;
import java.util.List;
import java.util.Optional;

public final class DefaultLockStateRepositoryFactory
        extends LockingComponentFactory<LockStateRepository> {

    @Override
    public boolean isLazy() {
        return false;
    }

    @Override
    public Class<LockStateRepository> getComponentType() {
        return LockStateRepository.class;
    }

    @Override
    public Optional<String> getQualifier() {
        return Optional.empty();
    }

    @Override
    public List<Class<?>> getDependencies() {
        return List.of(Configuration.class, SqlDialect.class);
    }

    @Override
    public List<ArgumentSpecification<?>> getArgumentSpecifications() {
        return List.of();
    }

    @Override
    public LockStateRepository createComponent(Context context, List<Object> arguments) {
        Configuration configuration =
                context.get(Configuration.class).getPrefixNamespace("lock.database");

        // Get SQL dialect from configuration, or use "default"
        String dialectType = configuration.getString("dialect").orElse("default");

        SqlDialect sqlDialect = getSqlDialect(context, dialectType);

        // Create JdbcDatabase instance with logSql configuration
        Configuration configWithLogSqlSet =
                configuration.toBuilder().setIfAbsent("logSql", false).build();

        JdbcDatabase jdbcDatabase =
                context.get(JdbcDatabase.class, null, "lock", configWithLogSqlSet);

        DefaultLockStateRepository repository =
                new DefaultLockStateRepository(sqlDialect, jdbcDatabase, configuration);

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
