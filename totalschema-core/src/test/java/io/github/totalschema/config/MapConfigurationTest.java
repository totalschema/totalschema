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

import static org.testng.Assert.*;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.testng.annotations.Test;

public class MapConfigurationTest {

    @Test
    public void testKeys() {

        MapConfiguration configuration =
                new MapConfiguration(
                        Map.of(
                                "foo", "fooValue",
                                "bar", "barValue",
                                "baz", "bazValue"));

        assertEquals(configuration.getKeys(), Set.of("foo", "bar", "baz"));
    }

    @Test
    public void testMissingValue() {

        MapConfiguration configuration = new MapConfiguration(Map.of("foo", "fooValue"));

        assertEquals(configuration.getString("bar"), Optional.empty());
    }

    @Test
    public void testValidIntValue() {

        int fooValue = 42;

        MapConfiguration configuration =
                new MapConfiguration(Map.of("foo", Integer.toString(fooValue)));

        assertEquals(configuration.getInt("foo").orElse(null), (Integer) fooValue);
    }

    @Test
    public void testInvalidIntValue() {

        MapConfiguration configuration = new MapConfiguration(Map.of("foo", "fooValue"));

        MisconfigurationException misconfigurationException =
                expectThrows(
                        MisconfigurationException.class,
                        () -> {
                            configuration.getInt("foo").orElse(null);
                        });

        assertEquals(
                misconfigurationException.getMessage(),
                "Value 'fooValue' of 'foo' in configuration Configuration "
                        + "{{foo=fooValue}} cannot be parsed to an integer");
    }

    @Test
    public void testValidEnumValue() {

        TimeUnit value = TimeUnit.MINUTES;

        MapConfiguration configuration = new MapConfiguration(Map.of("foo", value.toString()));

        assertEquals(configuration.getEnumValue(TimeUnit.class, "foo").orElse(null), value);
    }

    @Test
    public void testInvalidEnumValue() {

        MapConfiguration configuration = new MapConfiguration(Map.of("foo", "fooValue"));

        MisconfigurationException misconfigurationException =
                expectThrows(
                        MisconfigurationException.class,
                        () -> {
                            configuration.getEnumValue(TimeUnit.class, "foo").orElse(null);
                        });

        assertEquals(
                misconfigurationException.getMessage(),
                "Value 'fooValue' of 'foo' in configuration Configuration "
                        + "{{foo=fooValue}} cannot be parsed to a java.util.concurrent.TimeUnit enum value");
    }

    @Test
    public void testIntValueOutIfRange() {

        long aValueTooLarge = ((long) Integer.MAX_VALUE) + 10;

        MapConfiguration configuration =
                new MapConfiguration(Map.of("foo", Long.toString(aValueTooLarge)));

        MisconfigurationException misconfigurationException =
                expectThrows(
                        MisconfigurationException.class,
                        () -> {
                            configuration.getInt("foo").orElse(null);
                        });

        assertEquals(
                misconfigurationException.getMessage(),
                "Value '"
                        + aValueTooLarge
                        + "' of 'foo' in configuration Configuration "
                        + "{{foo="
                        + aValueTooLarge
                        + "}} cannot be parsed to an integer");
    }
}
