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

package io.github.totalschema.engine.internal.shell.direct.ssh.impl;

import io.github.totalschema.config.Configuration;
import io.github.totalschema.engine.cache.NamedConfigKey;
import io.github.totalschema.engine.internal.shell.direct.ssh.spi.SshConnection;
import io.github.totalschema.engine.internal.shell.direct.ssh.spi.SshConnectionFactory;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultSshConnectionFactory implements SshConnectionFactory {

    private final ConcurrentHashMap<NamedConfigKey, SshConnection> connectionCache =
            new ConcurrentHashMap<>();

    @Override
    public SshConnection getSshConnection(String name, Configuration configuration) {
        return connectionCache.computeIfAbsent(
                new NamedConfigKey(name, configuration),
                (key) -> newSshConnection(name, configuration));
    }

    @Override
    public SshConnection newSshConnection(String name, Configuration configuration) {

        return new MinaSshdConnection(name, configuration);
    }
}
