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

package io.github.totalschema.extensions.groovy;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.cache.AbstractCachedObjectFactory;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.engine.core.event.ChangeEngineCloseEvent;
import io.github.totalschema.engine.core.event.CloseResourceChangeEngineCloseListener;
import io.github.totalschema.engine.core.event.EventDispatcher;
import io.github.totalschema.spi.script.ScriptExecutor;
import io.github.totalschema.spi.script.ScriptExecutorFactory;
import java.io.IOException;
import java.util.List;

public final class GroovyScriptExecutorFactory extends AbstractCachedObjectFactory<ScriptExecutor>
        implements ScriptExecutorFactory {

    @Override
    public List<String> getExtensions() {
        return List.of("groovy");
    }

    @Override
    public ScriptExecutor getScriptExecutor(
            String name, Configuration configuration, CommandContext context) {
        return getMemoizedObject(name, configuration, context);
    }

    @Override
    protected ScriptExecutor createNewObject(
            String name, Configuration configuration, CommandContext context) {

        GroovyScriptExecutor groovyScriptExecutor = new GroovyScriptExecutor(name, configuration);

        EventDispatcher eventDispatcher = context.get(EventDispatcher.class);

        eventDispatcher.subscribe(
                ChangeEngineCloseEvent.class,
                CloseResourceChangeEngineCloseListener.create(groovyScriptExecutor));

        return groovyScriptExecutor;
    }

    @Override
    public void close() throws IOException {
        // no-op

    }
}
