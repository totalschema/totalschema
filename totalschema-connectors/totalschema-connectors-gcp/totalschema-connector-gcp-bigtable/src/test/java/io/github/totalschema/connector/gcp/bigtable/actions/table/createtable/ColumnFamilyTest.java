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

package io.github.totalschema.connector.gcp.bigtable.actions.table.createtable;

import static org.testng.Assert.*;

import com.google.cloud.bigtable.admin.v2.models.GCRules;
import java.util.Arrays;
import org.testng.annotations.Test;

/** Tests for {@link ColumnFamily} GC rule building. */
public class ColumnFamilyTest {

    @Test
    public void testNoGcRules() {
        // Given: a family with no GC rules
        ColumnFamily family = new ColumnFamily();
        family.setName("simple");

        // Then: hasGcRules should be false
        assertFalse(family.hasGcRules());
        assertNull(family.buildGcRule());
    }

    @Test
    public void testMaxVersionsRule() {
        // Given: a family with maxVersions rule
        ColumnFamily family = new ColumnFamily();
        family.setName("versioned");
        family.setMaxVersions(5);

        // Then: should build maxVersions rule
        assertTrue(family.hasGcRules());
        GCRules.GCRule rule = family.buildGcRule();
        assertNotNull(rule);
    }

    @Test
    public void testMaxAgeRule() {
        // Given: a family with maxAge rule
        ColumnFamily family = new ColumnFamily();
        family.setName("aged");
        family.setMaxAge("7d");

        // Then: should build maxAge rule
        assertTrue(family.hasGcRules());
        GCRules.GCRule rule = family.buildGcRule();
        assertNotNull(rule);
    }

    @Test
    public void testImplicitIntersectionRule() {
        // Given: a family with both maxVersions and maxAge (implicit intersection)
        ColumnFamily family = new ColumnFamily();
        family.setName("intersection");
        family.setMaxVersions(10);
        family.setMaxAge("90d");

        // Then: should build intersection rule
        assertTrue(family.hasGcRules());
        GCRules.GCRule rule = family.buildGcRule();
        assertNotNull(rule);
    }

    @Test
    public void testUnionRule() {
        // Given: a family with explicit union rule
        ColumnFamily family = new ColumnFamily();
        family.setName("union");

        GcRule rule1 = new GcRule();
        rule1.setMaxVersions(3);

        GcRule rule2 = new GcRule();
        rule2.setMaxAge("24h");

        family.setUnion(Arrays.asList(rule1, rule2));

        // Then: should build union rule
        assertTrue(family.hasGcRules());
        GCRules.GCRule rule = family.buildGcRule();
        assertNotNull(rule);
    }

    @Test
    public void testIntersectionRuleExplicit() {
        // Given: a family with explicit intersection rule
        ColumnFamily family = new ColumnFamily();
        family.setName("explicit-intersection");

        GcRule rule1 = new GcRule();
        rule1.setMaxVersions(100);

        GcRule rule2 = new GcRule();
        rule2.setMaxAge("365d");

        family.setIntersection(Arrays.asList(rule1, rule2));

        // Then: should build intersection rule
        assertTrue(family.hasGcRules());
        GCRules.GCRule rule = family.buildGcRule();
        assertNotNull(rule);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*both 'union' and 'intersection'.*")
    public void testUnionAndIntersectionConflict() {
        // Given: a family with both union and intersection (invalid)
        ColumnFamily family = new ColumnFamily();
        family.setName("invalid");

        GcRule rule1 = new GcRule();
        rule1.setMaxVersions(3);

        family.setUnion(Arrays.asList(rule1));
        family.setIntersection(Arrays.asList(rule1));

        // When/Then: should throw exception
        family.buildGcRule();
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*must have at least one rule.*")
    public void testEmptyUnionRule() {
        // Given: a family with empty union list
        ColumnFamily family = new ColumnFamily();
        family.setName("empty-union");
        family.setUnion(Arrays.asList());

        // When/Then: should throw exception
        family.buildGcRule();
    }

    @Test
    public void testDurationParsingDays() {
        // Given: various day formats
        ColumnFamily family = new ColumnFamily();
        family.setName("test");

        family.setMaxAge("7d");
        assertNotNull(family.buildGcRule());

        family.setMaxAge("30d");
        assertNotNull(family.buildGcRule());
    }

    @Test
    public void testDurationParsingHours() {
        // Given: hour format
        ColumnFamily family = new ColumnFamily();
        family.setName("test");
        family.setMaxAge("24h");

        assertNotNull(family.buildGcRule());
    }

    @Test
    public void testDurationParsingMinutes() {
        // Given: minute format
        ColumnFamily family = new ColumnFamily();
        family.setName("test");
        family.setMaxAge("60m");

        assertNotNull(family.buildGcRule());
    }

    @Test
    public void testDurationParsingSeconds() {
        // Given: second format
        ColumnFamily family = new ColumnFamily();
        family.setName("test");
        family.setMaxAge("3600s");

        assertNotNull(family.buildGcRule());
    }

    @Test
    public void testDurationParsingMilliseconds() {
        // Given: millisecond format
        ColumnFamily family = new ColumnFamily();
        family.setName("test");
        family.setMaxAge("1000ms");

        assertNotNull(family.buildGcRule());
    }

    @Test
    public void testDurationParsingMillisecondsFullWord() {
        // Given: millisecond full word format
        ColumnFamily family = new ColumnFamily();
        family.setName("test");
        family.setMaxAge("5000milliseconds");

        assertNotNull(family.buildGcRule());
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*Duration must have a unit.*")
    public void testDurationParsingNoUnit() {
        // Given: duration without unit
        ColumnFamily family = new ColumnFamily();
        family.setName("test");
        family.setMaxAge("7");

        // When/Then: should throw exception
        family.buildGcRule();
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*Unknown duration unit.*")
    public void testDurationParsingInvalidUnit() {
        // Given: duration with invalid unit
        ColumnFamily family = new ColumnFamily();
        family.setName("test");
        family.setMaxAge("7w");

        // When/Then: should throw exception
        family.buildGcRule();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testDurationParsingInvalidNumber() {
        // Given: duration with invalid number
        ColumnFamily family = new ColumnFamily();
        family.setName("test");
        family.setMaxAge("abcd");

        // When/Then: should throw exception
        family.buildGcRule();
    }
}
