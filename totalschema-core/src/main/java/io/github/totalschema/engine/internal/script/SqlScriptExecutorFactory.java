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

package io.github.totalschema.engine.internal.script;

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

public final class SqlScriptExecutorFactory extends AbstractCachedObjectFactory<ScriptExecutor>
        implements ScriptExecutorFactory {

    @Override
    public ScriptExecutor getScriptExecutor(
            String name, Configuration configuration, CommandContext context) {
        return getMemoizedObject(name, configuration, context);
    }

    @Override
    protected ScriptExecutor createNewObject(
            String name, Configuration configuration, CommandContext context) {

        SqlScriptExecutor sqlScriptExecutor = new SqlScriptExecutor(name, configuration);

        EventDispatcher eventDispatcher = context.get(EventDispatcher.class);

        // A SqlScriptExecutor is created and subscribed to close event - once
        eventDispatcher.subscribe(
                ChangeEngineCloseEvent.class,
                CloseResourceChangeEngineCloseListener.create(sqlScriptExecutor));

        return sqlScriptExecutor;
    }

    @Override
    public List<String> getExtensions() {
        return List.of("sql");
    }

    @Override
    public void close() throws IOException {}
}
