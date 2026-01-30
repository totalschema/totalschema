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

package io.github.totalschema.engine.core.command.interceptor;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.config.environment.Environment;
import io.github.totalschema.config.environment.EnvironmentFactory;
import io.github.totalschema.connector.ConnectorManager;
import io.github.totalschema.engine.core.command.api.CommandContext;
import io.github.totalschema.engine.core.command.api.CommandExecutor;
import io.github.totalschema.engine.core.command.api.ContextInitializerInterceptor;
import io.github.totalschema.engine.internal.changefile.ChangeFileFactory;
import io.github.totalschema.spi.change.ChangeService;
import io.github.totalschema.spi.change.ChangeServiceFactory;
import io.github.totalschema.spi.hash.HashService;
import io.github.totalschema.spi.hash.HashServiceFactory;
import io.github.totalschema.spi.lock.LockService;
import io.github.totalschema.spi.lock.LockServiceFactory;
import io.github.totalschema.spi.script.ScriptExecutorManager;
import io.github.totalschema.spi.sql.SqlDialect;
import io.github.totalschema.spi.sql.SqlDialectFactory;
import io.github.totalschema.spi.state.StateService;
import io.github.totalschema.spi.state.StateServiceFactory;

public final class ServiceInitializerInterceptor extends ContextInitializerInterceptor {

    private SqlDialect sqlDialect;

    private ScriptExecutorManager scriptExecutorManager;

    private ChangeFileFactory changeFileFactory;

    private ConnectorManager connectorManager;

    private StateService stateService;

    private ChangeService changeService;

    private LockService lockService;
    private HashService hashService;

    private EnvironmentFactory environmentFactory;

    public ServiceInitializerInterceptor(CommandExecutor next) {
        super(next);
    }

    @Override
    protected void initializeFromContext(CommandContext context) {

        initializeHashService(context);

        initializeSqlDialect(context);

        initializeScriptExecutorManager(context);

        initializeChangeFileIdFactory(context);

        initializeConnectorManager(context);

        initializeEnvironmentFactory(context);

        if (context.has(Environment.class)) {
            initializeStateService(context);
            initializeChangeService(context);
        }

        initializeLockService(context);
    }

    private void initializeChangeFileIdFactory(CommandContext context) {
        if (changeFileFactory == null) {
            changeFileFactory = new ChangeFileFactory(context);
        }
        context.setValue(ChangeFileFactory.class, changeFileFactory);
    }

    private void initializeScriptExecutorManager(CommandContext context) {

        if (scriptExecutorManager == null) {
            scriptExecutorManager = ScriptExecutorManager.getInstance();
        }

        context.setValue(ScriptExecutorManager.class, scriptExecutorManager);
    }

    private void initializeConnectorManager(CommandContext context) {
        if (connectorManager == null) {
            connectorManager = ConnectorManager.getInstance();
        }
        context.setValue(ConnectorManager.class, connectorManager);
    }

    private void initializeEnvironmentFactory(CommandContext context) {
        if (environmentFactory == null) {
            environmentFactory = EnvironmentFactory.getInstance();
        }

        context.setValue(EnvironmentFactory.class, environmentFactory);
    }

    private void initializeStateService(CommandContext context) {

        if (stateService == null) {
            StateServiceFactory stateServiceFactory = StateServiceFactory.getInstance();
            stateService = stateServiceFactory.getStateService(context);
        }

        context.setValue(StateService.class, stateService);
    }

    private void initializeChangeService(CommandContext context) {

        if (changeService == null) {
            ChangeServiceFactory changeServiceFactory = ChangeServiceFactory.getInstance();
            changeService = changeServiceFactory.getChangeService(context);
        }

        context.setValue(ChangeService.class, changeService);
    }

    private void initializeSqlDialect(CommandContext context) {

        if (sqlDialect == null) {
            SqlDialectFactory sqlDialectFactory = SqlDialectFactory.getInstance();
            sqlDialect = sqlDialectFactory.getSqlDialect(context);
        }

        context.setValue(SqlDialect.class, sqlDialect);
    }

    private void initializeLockService(CommandContext context) {

        if (lockService == null) {
            LockServiceFactory lockServiceFactory = LockServiceFactory.getInstance();
            lockService = lockServiceFactory.getLockService(context);
        }

        if (lockService != null) {
            context.setValue(LockService.class, lockService);
        }
    }

    private void initializeHashService(CommandContext context) {

        Configuration configuration = context.get(Configuration.class);

        if ("contentHash"
                .equalsIgnoreCase(configuration.getString("validation.type").orElse(null))) {

            if (hashService == null) {
                HashServiceFactory hashServiceFactory = HashServiceFactory.getInstance();
                hashService = hashServiceFactory.getHashService(context);
            }

            context.setValue(HashService.class, hashService);
        }
    }
}
