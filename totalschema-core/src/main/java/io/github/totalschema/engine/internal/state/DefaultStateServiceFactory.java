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
import io.github.totalschema.spi.factory.ArgumentSpecification;
import io.github.totalschema.spi.factory.ComponentFactory;
import io.github.totalschema.spi.hash.HashService;
import io.github.totalschema.spi.state.StateRepository;
import io.github.totalschema.spi.state.StateService;
import java.util.List;
import java.util.Optional;

public class DefaultStateServiceFactory extends ComponentFactory<StateService> {

    @Override
    public boolean isLazy() {
        return false;
    }

    @Override
    public Class<StateService> getComponentType() {
        return StateService.class;
    }

    @Override
    public Optional<String> getQualifier() {
        return Optional.empty();
    }

    @Override
    public List<Class<?>> getRequiredContextTypes() {
        return List.of(Configuration.class, StateRepository.class, EventDispatcher.class);
    }

    @Override
    public List<ArgumentSpecification<?>> getArgumentSpecifications() {
        return List.of();
    }

    @Override
    public StateService createComponent(Context context, List<Object> arguments) {
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

            HashService hashService = context.getOptional(HashService.class).orElse(null);

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
