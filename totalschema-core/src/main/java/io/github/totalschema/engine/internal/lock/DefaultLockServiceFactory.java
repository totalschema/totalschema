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

package io.github.totalschema.engine.internal.lock;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.MisconfigurationException;
import io.github.totalschema.config.MissingConfigurationKeyException;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.engine.internal.lock.database.DefaultDatabaseLockService;
import io.github.totalschema.engine.internal.lock.database.repository.spi.LockStateRepository;
import io.github.totalschema.engine.internal.lock.database.repository.spi.LockStateRepositoryFactory;
import io.github.totalschema.spi.lock.LockService;
import io.github.totalschema.spi.lock.LockServiceFactory;

public class DefaultLockServiceFactory implements LockServiceFactory {

    private static final String PROPERTY_NAMESPACE = "lock";

    @Override
    public LockService getLockService(CommandContext context) {

        Configuration lockConfig =
                context.get(Configuration.class).getPrefixNamespace(PROPERTY_NAMESPACE);

        try {
            String lockType =
                    lockConfig
                            .getString("type")
                            .orElseThrow(
                                    () ->
                                            MissingConfigurationKeyException.forKey(
                                                    PROPERTY_NAMESPACE + ".type"));

            switch (lockType) {
                case "none":
                    return null;

                case DefaultDatabaseLockService.CONFIG_PREFIX:
                    LockStateRepositoryFactory lockStateRepositoryFactory =
                            LockStateRepositoryFactory.getInstance();
                    LockStateRepository lockStateRepository =
                            lockStateRepositoryFactory.getLockStateRepository(context);

                    Configuration configuration =
                            lockConfig.getPrefixNamespace(DefaultDatabaseLockService.CONFIG_PREFIX);

                    return DefaultDatabaseLockService.newInstance(
                            lockStateRepository, configuration);

                default:
                    throw MisconfigurationException.forMessage(
                            "Unknown %.type: '%s'", PROPERTY_NAMESPACE, lockType);
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failure creating LockService from configuration: " + lockConfig, e);
        }
    }
}
