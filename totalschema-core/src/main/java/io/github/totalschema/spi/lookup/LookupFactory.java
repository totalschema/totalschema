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

package io.github.totalschema.spi.lookup;

import io.github.totalschema.engine.internal.lookup.DefaultLookupFactory;
import io.github.totalschema.spi.ServiceLoaderFactory;
import io.github.totalschema.spi.secrets.SecretsManager;
import java.util.List;

public interface LookupFactory {

    static LookupFactory getInstance() {
        return ServiceLoaderFactory.getSingleService(LookupFactory.class)
                .orElseGet(DefaultLookupFactory::new);
    }

    List<ExpressionLookup> getLookups(SecretsManager secretsManager);
}
