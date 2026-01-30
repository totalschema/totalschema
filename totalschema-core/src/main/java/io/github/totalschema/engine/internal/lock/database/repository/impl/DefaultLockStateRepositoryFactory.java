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
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.engine.internal.lock.database.repository.spi.LockStateRepository;
import io.github.totalschema.engine.internal.lock.database.repository.spi.LockStateRepositoryFactory;
import java.sql.SQLException;

public class DefaultLockStateRepositoryFactory implements LockStateRepositoryFactory {

    @Override
    public LockStateRepository getLockStateRepository(CommandContext context) {

        try {
            Configuration configuration =
                    context.get(Configuration.class).getPrefixNamespace("lock.database");

            return DefaultLockStateRepository.newInstance(configuration);

        } catch (SQLException e) {
            throw new RuntimeException(e);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted", e);
        }
    }
}
