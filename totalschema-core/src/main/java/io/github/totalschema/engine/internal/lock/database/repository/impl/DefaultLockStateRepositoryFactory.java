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
import io.github.totalschema.spi.sql.SqlDialect;
import java.util.List;

public final class DefaultLockStateRepositoryFactory
        extends LockingComponentFactory<LockStateRepository> {

    @Override
    public boolean isLazy() {
        return false;
    }

    @Override
    public Class<LockStateRepository> getConstructedClass() {
        return LockStateRepository.class;
    }

    @Override
    public String getQualifier() {
        return null;
    }

    @Override
    public List<Class<?>> getRequiredContextTypes() {
        return List.of(Configuration.class, SqlDialect.class);
    }

    @Override
    public LockStateRepository newComponent(Context context, Object... arguments) {
        Configuration configuration =
                context.get(Configuration.class).getPrefixNamespace("lock.database");

        SqlDialect sqlDialect = context.get(SqlDialect.class);

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
}
