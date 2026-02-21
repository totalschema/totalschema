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

import static io.github.totalschema.spi.state.StateConstants.CONFIG_PROPERTY_NAMESPACE;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.MissingConfigurationKeyException;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.engine.core.event.EventDispatcher;
import io.github.totalschema.spi.ComponentFactory;
import io.github.totalschema.spi.hash.HashService;
import io.github.totalschema.spi.state.StateRepository;
import io.github.totalschema.spi.state.StateService;
import java.util.List;

public class DefaultStateServiceFactory implements ComponentFactory<StateService> {

    @Override
    public boolean isLazy() {
        return true;
    }

    @Override
    public Class<StateService> getConstructedClass() {
        return StateService.class;
    }

    @Override
    public String getQualifier() {
        return null;
    }

    @Override
    public List<Class<?>> getRequiredContextTypes() {
        return List.of(Configuration.class, StateRepository.class, EventDispatcher.class);
    }

    @Override
    public List<Class<?>> getArgumentTypes() {
        return List.of();
    }

    @Override
    public StateService newComponent(Context context, Object... arguments) {
        try {
            Configuration configuration = context.get(Configuration.class);

            String stateType =
                    configuration
                            .getString(CONFIG_PROPERTY_NAMESPACE, "type")
                            .orElseThrow(
                                    () ->
                                            MissingConfigurationKeyException.forKey(
                                                    CONFIG_PROPERTY_NAMESPACE + ".type"));

            StateRepository stateRepository = context.get(StateRepository.class, stateType);

            HashService hashService;
            if (context.has(HashService.class)) {
                hashService = context.get(HashService.class);
            } else {
                hashService = null;
            }

            String overrideAppliedByUserId =
                    context.get(Configuration.class)
                            .getString("state.overrideAppliedByUserId")
                            .orElse(null);

            return new DefaultStateService(stateRepository, hashService, overrideAppliedByUserId);

        } catch (RuntimeException ex) {
            throw new RuntimeException("Failure creating StateService", ex);
        }
    }
}
