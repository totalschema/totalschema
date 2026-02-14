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

package io.github.totalschema.engine.internal.state;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.MisconfigurationException;
import io.github.totalschema.config.MissingConfigurationKeyException;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.engine.internal.state.csv.CsvFileStateRecordRepository;
import io.github.totalschema.engine.internal.state.database.JdbcDatabaseStateRecordRepository;
import io.github.totalschema.spi.state.StateRepository;
import io.github.totalschema.spi.state.StateRepositoryFactory;

public class DefaultStateRepositoryFactory implements StateRepositoryFactory {

    private static final String PROPERTY_NAMESPACE = "stateRepository";

    @Override
    public StateRepository getStateRecordRepository(Context context) {

        Configuration stateConfig =
                context.get(Configuration.class).getPrefixNamespace(PROPERTY_NAMESPACE);

        try {
            String stateType =
                    stateConfig
                            .getString("type")
                            .orElseThrow(
                                    () ->
                                            MissingConfigurationKeyException.forKey(
                                                    PROPERTY_NAMESPACE + ".type"));

            switch (stateType) {
                case "csv":
                    return CsvFileStateRecordRepository.newInstance(
                            context, stateConfig.getPrefixNamespace("csv"));

                case "database":
                    return JdbcDatabaseStateRecordRepository.newInstance(
                            context, stateConfig.getPrefixNamespace("database"));

                default:
                    throw MisconfigurationException.forMessage(
                            "Unknown %.type: '%s'", PROPERTY_NAMESPACE, stateType);
            }

        } catch (RuntimeException ex) {
            throw new RuntimeException(
                    "Failure creating StateRepository from state configuration: " + stateConfig,
                    ex);
        }
    }
}
