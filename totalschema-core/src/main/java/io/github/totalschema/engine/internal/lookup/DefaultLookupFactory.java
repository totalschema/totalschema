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

package io.github.totalschema.engine.internal.lookup;

import io.github.totalschema.spi.ServiceLoaderFactory;
import io.github.totalschema.spi.lookup.ExpressionLookup;
import io.github.totalschema.spi.lookup.LookupFactory;
import io.github.totalschema.spi.secrets.SecretsManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DefaultLookupFactory implements LookupFactory {

    @Override
    public List<ExpressionLookup> getLookups(SecretsManager secretsManager) {

        List<ExpressionLookup> builtInLookups =
                List.of(
                        new DelegatingExpressionLookup(
                                "secret", secretsManager::decode, "SecretsManager.decode(String)"),
                        new DelegatingExpressionLookup(
                                "secretFileContent",
                                secretsManager::decodedFileContent,
                                "SecretsManager.decodedFileContent(String)"),
                        new DelegatingExpressionLookup(
                                "decodedFilePath",
                                secretsManager::decodedFilePath,
                                "SecretsManager.decodedFilePath(String)"));

        List<ExpressionLookup> additionalLookups =
                ServiceLoaderFactory.getAllServices(ExpressionLookup.class);

        LinkedHashMap<String, ExpressionLookup> nameToLookup = new LinkedHashMap<>();

        addToMap(builtInLookups, nameToLookup);

        addToMap(additionalLookups, nameToLookup);

        return List.copyOf(nameToLookup.values());
    }

    private void addToMap(
            List<ExpressionLookup> builtInLookups, Map<String, ExpressionLookup> nameToLookup) {

        for (ExpressionLookup el : builtInLookups) {
            nameToLookup.put(el.getKey(), el);
        }
    }
}
