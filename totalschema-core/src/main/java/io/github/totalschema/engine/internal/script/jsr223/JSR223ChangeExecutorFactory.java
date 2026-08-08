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

package io.github.totalschema.engine.internal.script.jsr223;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.api.Context;
import io.github.totalschema.spi.change.executor.ChangeExecutor;
import io.github.totalschema.spi.factory.ArgumentSpecification;
import io.github.totalschema.spi.factory.ComponentFactory;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

/**
 * Factory for creating {@link JSR223ChangeExecutor} instances backed by the Java Scripting API
 * (JSR-223).
 *
 * <p>At runtime, this factory discovers all {@link javax.script.ScriptEngineFactory}
 * implementations available on the classpath via {@link ScriptEngineManager} and produces one
 * {@link JSR223ChangeExecutor} per registered file extension. JSR-223 support must be explicitly
 * opted in via the {@code scripting.jsr223.enabled} configuration flag.
 */
public final class JSR223ChangeExecutorFactory extends ComponentFactory<ChangeExecutor> {

    private final String extension;

    public JSR223ChangeExecutorFactory(String extension) {
        this.extension = extension;
    }

    /**
     * Returns {@code true} if JSR-223 scripting is enabled in the supplied configuration.
     *
     * <p>The feature is controlled by the {@code scripting.jsr223.enabled} boolean property. When
     * the property is absent it defaults to {@code false}.
     *
     * @param configuration the runtime configuration to inspect; must not be {@code null}
     * @return {@code true} if JSR-223 scripting is enabled, {@code false} otherwise
     */
    public static boolean isEnabled(Configuration configuration) {
        return configuration.getBoolean("scripting.jsr223.enabled").orElse(false);
    }

    /**
     * Creates a {@link JSR223ChangeExecutorFactory} for every file extension advertised by the
     * {@link ScriptEngineFactory} instances discovered on the current classpath.
     *
     * <p>The discovery is performed via {@link ScriptEngineManager#getEngineFactories()}. Factories
     * that report a {@code null} or empty extension list are silently skipped.
     *
     * @return an immutable list of {@link JSR223ChangeExecutorFactory} instances, one per supported
     *     file extension; never {@code null}, may be empty if no JSR-223 engines are present
     */
    public static List<JSR223ChangeExecutorFactory> createFactories() {

        List<JSR223ChangeExecutorFactory> results = new LinkedList<>();

        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

        for (ScriptEngineFactory scriptEngineFactory : scriptEngineManager.getEngineFactories()) {
            List<String> extensions = scriptEngineFactory.getExtensions();
            if (extensions != null) {
                for (String extension : extensions) {
                    results.add(new JSR223ChangeExecutorFactory(extension));
                }
            }
        }

        return List.copyOf(results);
    }

    @Override
    public boolean isLazy() {
        return true;
    }

    @Override
    public Class<ChangeExecutor> getComponentType() {
        return ChangeExecutor.class;
    }

    @Override
    public Optional<String> getQualifier() {
        return Optional.of(extension);
    }

    @Override
    public List<Class<?>> getDependencies() {
        return List.of();
    }

    @Override
    public List<ArgumentSpecification<?>> getArgumentSpecifications() {
        return List.of();
    }

    @Override
    public ChangeExecutor createComponent(Context context, List<Object> arguments) {
        return JSR223ChangeExecutor.newInstance(extension, context);
    }
}
