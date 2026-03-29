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

package io.github.totalschema.engine.core.container;

import io.github.totalschema.spi.factory.ArgumentSpecification;
import io.github.totalschema.spi.factory.ComponentFactory;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A builder for creating and configuring a {@link ComponentContainer}. This builder provides a
 * fluent API for registering components and factories before building the container.
 */
public final class ComponentContainerBuilder {

    private final Logger logger = LoggerFactory.getLogger(ComponentContainerBuilder.class);

    private final Map<ObjectSpecification, Object> objects = new LinkedHashMap<>();
    private final Map<FactorySpecification, ComponentFactory<?>> factories = new LinkedHashMap<>();

    private boolean allowUnqualifiedAccessToSingleComponents = false;

    /**
     * Registers a component instance of a specific type.
     *
     * @param type The class type of the component to register.
     * @param object The component instance.
     * @param <T> The type of the component.
     * @return This builder for chaining.
     */
    public <T> ComponentContainerBuilder withComponent(Class<T> type, T object) {
        objects.put(ObjectSpecification.from(type, null), object);
        return this;
    }

    /**
     * Registers a {@link ComponentFactory} that can create component instances.
     *
     * @param componentFactory The factory to register.
     * @return This builder for chaining.
     */
    public ComponentContainerBuilder withFactory(ComponentFactory<?> componentFactory) {
        factories.put(FactorySpecification.from(componentFactory), componentFactory);
        return this;
    }

    public ComponentContainerBuilder allowUnqualifiedAccessToSingleComponents(boolean newValue) {
        this.allowUnqualifiedAccessToSingleComponents = newValue;

        return this;
    }

    /**
     * Builds the {@link ComponentContainer}, initializing all registered components.
     *
     * @return The fully configured and initialized {@link ComponentContainer}.
     */
    public ComponentContainer build() {

        if (allowUnqualifiedAccessToSingleComponents) {
            initializeUnqualifiedAccess();
        }

        ComponentContainer componentContainer = new ComponentContainer();
        objects.forEach(componentContainer::registerComponent);

        Map<FactorySpecification, ComponentFactory<?>> enabledFactories = filterEnabledFactories();
        enabledFactories.forEach(componentContainer::registerComponentFactory);

        createObjectsFromFactories(componentContainer, enabledFactories);

        return componentContainer;
    }

    private void initializeUnqualifiedAccess() {

        Set<ObjectSpecification> objectSpecificationsSnapshot = new HashSet<>(objects.keySet());

        objectSpecificationsSnapshot.stream()
                .collect(Collectors.groupingBy(ObjectSpecification::getType))
                .values()
                .stream()
                .filter(specs -> specs.size() == 1)
                .map(specs -> specs.get(0))
                .filter(spec -> spec.getQualifier() != null) // Still using String internally
                .forEach(
                        spec -> {
                            ObjectSpecification defaultSpec = spec.withQualifier(null);
                            objects.computeIfAbsent(defaultSpec, k -> objects.get(spec));
                        });

        Set<FactorySpecification> factorySpecifications = new HashSet<>(factories.keySet());

        factorySpecifications.stream()
                .collect(Collectors.groupingBy(FactorySpecification::getConstructedClass))
                .values()
                .stream()
                .filter(specs -> specs.size() == 1)
                .map(specs -> specs.get(0))
                .filter(spec -> spec.getQualifier() != null) // Still using String internally
                .forEach(
                        spec -> {
                            FactorySpecification defaultSpec = spec.withQualifier(null);
                            factories.computeIfAbsent(defaultSpec, k -> factories.get(spec));
                        });
    }

    private Map<FactorySpecification, ComponentFactory<?>> filterEnabledFactories() {

        Map<ObjectSpecification, Object> unmodifiableViewOfObjects =
                Collections.unmodifiableMap(objects);
        Map<FactorySpecification, ComponentFactory<?>> unmodifiableViewOfFactories =
                Collections.unmodifiableMap(factories);

        // Filter factories based on whether their required context types are available
        return factories.entrySet().stream()
                .filter(
                        entry -> {
                            ComponentFactory<?> factory = entry.getValue();

                            boolean enabled =
                                    factory.isEnabled(
                                            unmodifiableViewOfObjects, unmodifiableViewOfFactories);

                            logger.debug(
                                    "Factory {} is {}.",
                                    factory.getClass().getName(),
                                    enabled ? "enabled" : "NOT enabled");

                            return enabled;
                        })
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (a, b) -> a,
                                LinkedHashMap::new));
    }

    private void createObjectsFromFactories(
            ComponentContainer componentContainer,
            Map<FactorySpecification, ComponentFactory<?>> enabledFactories) {

        for (Map.Entry<FactorySpecification, ComponentFactory<?>> entry :
                enabledFactories.entrySet()) {

            ComponentFactory<?> factory = entry.getValue();

            if (!factory.isLazy()) {
                List<ArgumentSpecification<?>> argumentTypes = factory.getArgumentSpecifications();
                if (argumentTypes == null || argumentTypes.isEmpty()) {
                    FactorySpecification factorySpecification = entry.getKey();

                    ObjectSpecification objectSpecification =
                            ObjectSpecification.from(factorySpecification);

                    Object createdObject =
                            componentContainer.get(
                                    objectSpecification.getType(),
                                    objectSpecification.getQualifier());
                    logger.debug(
                            "Created object {} for {} using factory {}",
                            createdObject,
                            factorySpecification,
                            factory);
                }
            }
        }
    }
}
