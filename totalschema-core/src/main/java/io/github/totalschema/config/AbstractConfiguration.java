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

package io.github.totalschema.config;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Abstract base implementation of the Configuration interface.
 *
 * <p>Provides default implementations for common configuration operations.
 */
public abstract class AbstractConfiguration implements Configuration {

    @Override
    public Optional<Boolean> getBoolean(String key) {
        return getString(key)
                .map(
                        value -> {
                            String lowerCaseValue = value.toLowerCase(Locale.ENGLISH);
                            if (lowerCaseValue.equalsIgnoreCase("true")) {
                                return true;
                            } else if (lowerCaseValue.equalsIgnoreCase("false")) {
                                return false;
                            } else {
                                throw new IllegalStateException(
                                        String.format(
                                                "Value [%s] cannot be mapped to boolean", value));
                            }
                        });
    }

    @Override
    public final Optional<List<String>> getList(String key) {
        return getString(key)
                .map(value -> value.split(","))
                .map(
                        values ->
                                Arrays.stream(values)
                                        .map(String::trim)
                                        .collect(Collectors.toList()));
    }

    @Override
    public final <E extends Enum<E>> Optional<E> getEnumValue(
            Class<E> elementType, String... keys) {
        Objects.requireNonNull(elementType);

        return getString(keys)
                .map(
                        enumName ->
                                EnumSet.allOf(elementType).stream()
                                        .filter(it -> it.name().equalsIgnoreCase(enumName))
                                        .findFirst()
                                        .orElseThrow(
                                                () ->
                                                        MisconfigurationException.forMessage(
                                                                "Value '%s' of '%s' in configuration %s cannot be parsed to a %s enum value",
                                                                enumName,
                                                                String.join(".", keys),
                                                                this,
                                                                elementType.getName())));
    }

    @Override
    public final Optional<String> getString(String... keys) {
        return getString(String.join(".", keys));
    }

    @Override
    public Optional<Integer> getInt(String... key) {
        return getString(key)
                .map(
                        stringValue -> {
                            try {
                                return Integer.parseInt(stringValue);
                            } catch (NumberFormatException nfe) {
                                throw MisconfigurationException.forMessage(
                                        "Value '%s' of '%s' in configuration %s cannot be parsed to an integer",
                                        stringValue, String.join(".", key), this);
                            }
                        });
    }

    @Override
    public Optional<Long> getLong(String... key) {
        return getString(key)
                .map(
                        stringValue -> {
                            try {
                                return Long.parseLong(stringValue);
                            } catch (NumberFormatException nfe) {
                                throw MisconfigurationException.forMessage(
                                        "Value '%s' of '%s' in configuration %s cannot be parsed to a long",
                                        stringValue, String.join(".", key), this);
                            }
                        });
    }

    @Override
    public final Configuration getPrefixNamespace(String... prefixes) {

        String prefixNamesSpace = String.join(".", prefixes);

        Map<String, String> map =
                getKeys().stream()
                        .filter(it -> it.startsWith(prefixNamesSpace))
                        .collect(
                                Collectors.toMap(
                                        key -> key.replaceFirst(prefixNamesSpace + "\\.", ""),
                                        key ->
                                                getString(key)
                                                        .orElseThrow(
                                                                () ->
                                                                        new IllegalStateException(
                                                                                "Key not found: "
                                                                                        + String
                                                                                                .join(
                                                                                                        ".",
                                                                                                        key)))));

        return new MapConfiguration(map);
    }

    @Override
    public final Configuration addAll(Configuration otherConfiguration) {

        Map<String, String> thisMap = this.asMap().orElseGet(HashMap::new);

        Map<String, String> thatMap = otherConfiguration.asMap().orElseGet(HashMap::new);

        thisMap.putAll(thatMap);

        return new MapConfiguration(thisMap);
    }

    @Override
    public final Optional<Properties> asProperties() {
        return asMap().map(
                        map -> {
                            Properties properties = new Properties();
                            properties.putAll(map);
                            return properties;
                        });
    }

    @Override
    public final Configuration withEntry(String key, String value) {

        Map<String, String> configMap =
                asMap().map(
                                it -> {
                                    Map<String, String> map = new HashMap<>(it);
                                    map.put(key, value);

                                    return map;
                                })
                        .orElseGet(() -> Collections.singletonMap(key, value));

        return new MapConfiguration(configMap);
    }

    @Override
    public final boolean isEmpty() {
        return asMap().isEmpty();
    }

    @Override
    public final Optional<Map<String, String>> asMap() {

        Map<String, String> map =
                getKeys().stream()
                        .collect(
                                Collectors.toMap(
                                        Function.identity(),
                                        key ->
                                                getString(key)
                                                        .orElseThrow(
                                                                () ->
                                                                        new IllegalStateException(
                                                                                "Key not found: "
                                                                                        + String
                                                                                                .join(
                                                                                                        ".",
                                                                                                        key)))));

        if (map.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(map);
        }
    }

    @Override
    public abstract String toString();
}
