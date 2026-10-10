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

import org.testng.annotations.Test;

/** Tests for {@link DurationParser} utility class. */
public class DurationParserTest {

    // ========== Days Tests ==========

    @Test
    public void testParseDays_ShortForm() {
        // Given: a duration string with days (short form)
        String duration = "7d";

        // When: parsing the duration
        org.threeten.bp.Duration result = DurationParser.parse(duration);

        // Then: should return 7 days
        assertNotNull(result);
        assertEquals(result.toDays(), 7L);
    }

    @Test
    public void testParseDays_SingularForm() {
        // Given: a duration string with day (singular)
        String duration = "1day";

        // When: parsing the duration
        org.threeten.bp.Duration result = DurationParser.parse(duration);

        // Then: should return 1 day
        assertNotNull(result);
        assertEquals(result.toDays(), 1L);
    }

    @Test
    public void testParseDays_PluralForm() {
        // Given: a duration string with days (plural)
        String duration = "30days";

        // When: parsing the duration
        org.threeten.bp.Duration result = DurationParser.parse(duration);

        // Then: should return 30 days
        assertNotNull(result);
        assertEquals(result.toDays(), 30L);
    }

    // ========== Hours Tests ==========

    @Test
    public void testParseHours_ShortForm() {
        // Given: a duration string with hours (short form)
        String duration = "24h";

        // When: parsing the duration
        org.threeten.bp.Duration result = DurationParser.parse(duration);

        // Then: should return 24 hours
        assertNotNull(result);
        assertEquals(result.toHours(), 24L);
    }

    @Test
    public void testParseHours_SingularForm() {
        // Given: a duration string with hour (singular)
        String duration = "1hour";

        // When: parsing the duration
        org.threeten.bp.Duration result = DurationParser.parse(duration);

        // Then: should return 1 hour
        assertNotNull(result);
        assertEquals(result.toHours(), 1L);
    }

    @Test
    public void testParseHours_PluralForm() {
        // Given: a duration string with hours (plural)
        String duration = "48hours";

        // When: parsing the duration
        org.threeten.bp.Duration result = DurationParser.parse(duration);

        // Then: should return 48 hours
        assertNotNull(result);
        assertEquals(result.toHours(), 48L);
    }

    // ========== Minutes Tests ==========

    @Test
    public void testParseMinutes_ShortForm() {
        // Given: a duration string with minutes (short form)
        String duration = "30m";

        // When: parsing the duration
        org.threeten.bp.Duration result = DurationParser.parse(duration);

        // Then: should return 30 minutes
        assertNotNull(result);
        assertEquals(result.toMinutes(), 30L);
    }

    @Test
    public void testParseMinutes_MinForm() {
        // Given: a duration string with min
        String duration = "15min";

        // When: parsing the duration
        org.threeten.bp.Duration result = DurationParser.parse(duration);

        // Then: should return 15 minutes
        assertNotNull(result);
        assertEquals(result.toMinutes(), 15L);
    }

    @Test
    public void testParseMinutes_SingularForm() {
        // Given: a duration string with minute (singular)
        String duration = "1minute";

        // When: parsing the duration
        org.threeten.bp.Duration result = DurationParser.parse(duration);

        // Then: should return 1 minute
        assertNotNull(result);
        assertEquals(result.toMinutes(), 1L);
    }

    @Test
    public void testParseMinutes_PluralForm() {
        // Given: a duration string with minutes (plural)
        String duration = "90minutes";

        // When: parsing the duration
        org.threeten.bp.Duration result = DurationParser.parse(duration);

        // Then: should return 90 minutes
        assertNotNull(result);
        assertEquals(result.toMinutes(), 90L);
    }

    // ========== Seconds Tests ==========

    @Test
    public void testParseSeconds_ShortForm() {
        // Given: a duration string with seconds (short form)
        String duration = "3600s";

        // When: parsing the duration
        org.threeten.bp.Duration result = DurationParser.parse(duration);

        // Then: should return 3600 seconds
        assertNotNull(result);
        assertEquals(result.getSeconds(), 3600L);
    }

    @Test
    public void testParseSeconds_SecForm() {
        // Given: a duration string with sec
        String duration = "60sec";

        // When: parsing the duration
        org.threeten.bp.Duration result = DurationParser.parse(duration);

        // Then: should return 60 seconds
        assertNotNull(result);
        assertEquals(result.getSeconds(), 60L);
    }

    @Test
    public void testParseSeconds_SingularForm() {
        // Given: a duration string with second (singular)
        String duration = "1second";

        // When: parsing the duration
        org.threeten.bp.Duration result = DurationParser.parse(duration);

        // Then: should return 1 second
        assertNotNull(result);
        assertEquals(result.getSeconds(), 1L);
    }

    @Test
    public void testParseSeconds_PluralForm() {
        // Given: a duration string with seconds (plural)
        String duration = "120seconds";

        // When: parsing the duration
        org.threeten.bp.Duration result = DurationParser.parse(duration);

        // Then: should return 120 seconds
        assertNotNull(result);
        assertEquals(result.getSeconds(), 120L);
    }

    // ========== Milliseconds Tests ==========

    @Test
    public void testParseMilliseconds_ShortForm() {
        // Given: a duration string with milliseconds (short form)
        String duration = "1000ms";

        // When: parsing the duration
        org.threeten.bp.Duration result = DurationParser.parse(duration);

        // Then: should return 1000 milliseconds
        assertNotNull(result);
        assertEquals(result.toMillis(), 1000L);
    }

    @Test
    public void testParseMilliseconds_MilliForm() {
        // Given: a duration string with milli
        String duration = "500milli";

        // When: parsing the duration
        org.threeten.bp.Duration result = DurationParser.parse(duration);

        // Then: should return 500 milliseconds
        assertNotNull(result);
        assertEquals(result.toMillis(), 500L);
    }

    @Test
    public void testParseMilliseconds_MillisForm() {
        // Given: a duration string with millis
        String duration = "2000millis";

        // When: parsing the duration
        org.threeten.bp.Duration result = DurationParser.parse(duration);

        // Then: should return 2000 milliseconds
        assertNotNull(result);
        assertEquals(result.toMillis(), 2000L);
    }

    @Test
    public void testParseMilliseconds_SingularForm() {
        // Given: a duration string with millisecond (singular)
        String duration = "1millisecond";

        // When: parsing the duration
        org.threeten.bp.Duration result = DurationParser.parse(duration);

        // Then: should return 1 millisecond
        assertNotNull(result);
        assertEquals(result.toMillis(), 1L);
    }

    @Test
    public void testParseMilliseconds_PluralForm() {
        // Given: a duration string with milliseconds (plural)
        String duration = "5000milliseconds";

        // When: parsing the duration
        org.threeten.bp.Duration result = DurationParser.parse(duration);

        // Then: should return 5000 milliseconds
        assertNotNull(result);
        assertEquals(result.toMillis(), 5000L);
    }

    // ========== Edge Cases and Whitespace Tests ==========

    @Test
    public void testParse_WithLeadingWhitespace() {
        // Given: a duration string with leading whitespace
        String duration = "  7d";

        // When: parsing the duration
        org.threeten.bp.Duration result = DurationParser.parse(duration);

        // Then: should return 7 days
        assertNotNull(result);
        assertEquals(result.toDays(), 7L);
    }

    @Test
    public void testParse_WithTrailingWhitespace() {
        // Given: a duration string with trailing whitespace
        String duration = "7d  ";

        // When: parsing the duration
        org.threeten.bp.Duration result = DurationParser.parse(duration);

        // Then: should return 7 days
        assertNotNull(result);
        assertEquals(result.toDays(), 7L);
    }

    @Test
    public void testParse_WithBothWhitespace() {
        // Given: a duration string with both leading and trailing whitespace
        String duration = "  24h  ";

        // When: parsing the duration
        org.threeten.bp.Duration result = DurationParser.parse(duration);

        // Then: should return 24 hours
        assertNotNull(result);
        assertEquals(result.toHours(), 24L);
    }

    @Test
    public void testParse_UpperCase() {
        // Given: a duration string in uppercase
        String duration = "7D";

        // When: parsing the duration
        org.threeten.bp.Duration result = DurationParser.parse(duration);

        // Then: should return 7 days (case-insensitive)
        assertNotNull(result);
        assertEquals(result.toDays(), 7L);
    }

    @Test
    public void testParse_MixedCase() {
        // Given: a duration string in mixed case
        String duration = "30DaYs";

        // When: parsing the duration
        org.threeten.bp.Duration result = DurationParser.parse(duration);

        // Then: should return 30 days (case-insensitive)
        assertNotNull(result);
        assertEquals(result.toDays(), 30L);
    }

    @Test
    public void testParse_LargeValue() {
        // Given: a duration string with a large value
        String duration = "365d";

        // When: parsing the duration
        org.threeten.bp.Duration result = DurationParser.parse(duration);

        // Then: should return 365 days
        assertNotNull(result);
        assertEquals(result.toDays(), 365L);
    }

    @Test
    public void testParse_ZeroValue() {
        // Given: a duration string with zero value
        String duration = "0d";

        // When: parsing the duration
        org.threeten.bp.Duration result = DurationParser.parse(duration);

        // Then: should return 0 days
        assertNotNull(result);
        assertEquals(result.toDays(), 0L);
    }

    // ========== Error Cases Tests ==========

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Duration cannot be null or empty")
    public void testParse_NullInput() {
        // Given: a null duration string
        String duration = null;

        // When/Then: should throw IllegalArgumentException
        DurationParser.parse(duration);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Duration cannot be null or empty")
    public void testParse_EmptyInput() {
        // Given: an empty duration string
        String duration = "";

        // When/Then: should throw IllegalArgumentException
        DurationParser.parse(duration);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*Duration must have a unit.*")
    public void testParse_WhitespaceOnlyInput() {
        // Given: a whitespace-only duration string
        String duration = "   ";

        // When/Then: should throw IllegalArgumentException (empty after trim)
        DurationParser.parse(duration);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*Duration must have a unit.*")
    public void testParse_NoUnit() {
        // Given: a duration string without unit
        String duration = "7";

        // When/Then: should throw IllegalArgumentException
        DurationParser.parse(duration);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*Unknown duration unit.*")
    public void testParse_UnknownUnit() {
        // Given: a duration string with unknown unit
        String duration = "7w";

        // When/Then: should throw IllegalArgumentException
        DurationParser.parse(duration);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*Unknown duration unit.*")
    public void testParse_InvalidUnit() {
        // Given: a duration string with invalid unit
        String duration = "10years";

        // When/Then: should throw IllegalArgumentException
        DurationParser.parse(duration);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*Invalid duration format.*")
    public void testParse_InvalidNumber() {
        // Given: a duration string with invalid number
        String duration = "abcd";

        // When/Then: should throw IllegalArgumentException
        DurationParser.parse(duration);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*Invalid duration format.*")
    public void testParse_InvalidNumberWithValidUnit() {
        // Given: a duration string with invalid number but valid unit
        String duration = "abc123d";

        // When/Then: should throw IllegalArgumentException
        DurationParser.parse(duration);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*Invalid duration format.*")
    public void testParse_OnlyUnit() {
        // Given: a duration string with only unit
        String duration = "d";

        // When/Then: should throw IllegalArgumentException (empty number part)
        DurationParser.parse(duration);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*Invalid duration format.*")
    public void testParse_NegativeNumber() {
        // Given: a duration string with negative number
        String duration = "-5d";

        // When/Then: should throw IllegalArgumentException
        DurationParser.parse(duration);
    }
}
