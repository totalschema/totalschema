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

package io.github.totalschema.spi.script;

import io.github.totalschema.concurrent.LockTemplate;
import io.github.totalschema.engine.internal.script.SqlScriptExecutorFactory;
import io.github.totalschema.spi.ServiceLoaderFactory;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class ScriptExecutorManager {

    private final LockTemplate lockTemplate =
            new LockTemplate(1, TimeUnit.MINUTES, new ReentrantLock());

    private Map<String, ScriptExecutorFactory> extensionsToScriptExecutor = null;

    private static final ScriptExecutorManager INSTANCE = new ScriptExecutorManager();

    private ScriptExecutorManager() {
        // disallow external instances
    }

    public static ScriptExecutorManager getInstance() {
        return INSTANCE;
    }

    public ScriptExecutorFactory getScriptExecutorFactoryByExtension(String extension) {
        return lockTemplate.withTryLock((() -> getScriptExecutorWithLockHeld(extension)));
    }

    private ScriptExecutorFactory getScriptExecutorWithLockHeld(String extension) {

        if (extensionsToScriptExecutor == null) {

            List<ScriptExecutorFactory> additionalFactories =
                    ServiceLoaderFactory.getAllServices(ScriptExecutorFactory.class);

            LinkedList<ScriptExecutorFactory> scriptExecutorFactories = new LinkedList<>();

            scriptExecutorFactories.add(new SqlScriptExecutorFactory());
            scriptExecutorFactories.addAll(additionalFactories);

            extensionsToScriptExecutor =
                    scriptExecutorFactories.stream()
                            .flatMap(
                                    factory ->
                                            factory.getExtensions().stream()
                                                    .map(it -> it.toLowerCase(Locale.ENGLISH))
                                                    .map(
                                                            factoryExtension ->
                                                                    new AbstractMap.SimpleEntry<>(
                                                                            factoryExtension,
                                                                            factory)))
                            .collect(
                                    Collectors.toMap(
                                            Map.Entry::getKey,
                                            Map.Entry::getValue,
                                            (p1, p2) -> p1));
        }

        String lowerCaseExtension = extension.toLowerCase(Locale.ENGLISH);

        ScriptExecutorFactory scriptExecutorFactory =
                extensionsToScriptExecutor.get(lowerCaseExtension);
        if (scriptExecutorFactory == null) {
            throw new IllegalArgumentException("No ScriptExecutorFactory found for: " + extension);
        }

        return scriptExecutorFactory;
    }
}
