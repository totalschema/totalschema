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

package io.github.totalschema.jdbc;

import static org.testng.Assert.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.TimeZone;
import org.testng.annotations.Test;

public class TypeConversionsTest {

    private static final ZoneId PARIS_ZONE = ZoneId.of("Europe/Paris");

    @Test
    public void testToUtcTimestampWithParisTime() {
        // Create TypeConversions instance with Paris timezone
        TypeConversions conversions = new TypeConversions(PARIS_ZONE);

        // Create a ZonedDateTime: January 26, 2025 at 13:30 in Paris
        // Paris is UTC+1 in winter, so 13:30 Paris = 12:30 UTC
        ZonedDateTime parisTime =
                ZonedDateTime.of(2025, 1, 26, 13, 30, 0, 0, ZoneId.of("Europe/Paris"));

        // Convert to UTC timestamp
        Timestamp utcTimestamp = conversions.toUtcTimestamp(parisTime);

        assertNotNull(utcTimestamp);

        // Convert the timestamp back to an Instant and check it represents 12:30 UTC
        Instant instant = utcTimestamp.toInstant();
        ZonedDateTime utcTime = instant.atZone(ZoneOffset.UTC);

        assertEquals(utcTime.getYear(), 2025);
        assertEquals(utcTime.getMonthValue(), 1);
        assertEquals(utcTime.getDayOfMonth(), 26);
        assertEquals(utcTime.getHour(), 12); // 13:30 Paris = 12:30 UTC
        assertEquals(utcTime.getMinute(), 30);
        assertEquals(utcTime.getSecond(), 0);
    }

    @Test
    public void testToUtcTimestampWithParisSummerTime() {
        // Create TypeConversions instance with Paris timezone
        TypeConversions conversions = new TypeConversions(PARIS_ZONE);

        // Create a ZonedDateTime: July 15, 2025 at 13:30 in Paris
        // Paris is UTC+2 in summer (DST), so 13:30 Paris = 11:30 UTC
        ZonedDateTime parisTime =
                ZonedDateTime.of(2025, 7, 15, 13, 30, 0, 0, ZoneId.of("Europe/Paris"));

        // Convert to UTC timestamp
        Timestamp utcTimestamp = conversions.toUtcTimestamp(parisTime);

        assertNotNull(utcTimestamp);

        // Use Calendar API with UTC timezone to extract the actual stored values
        Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCalendar.setTimeInMillis(utcTimestamp.getTime());

        assertEquals(utcCalendar.get(Calendar.YEAR), 2025);
        assertEquals(utcCalendar.get(Calendar.MONTH), Calendar.JULY);
        assertEquals(utcCalendar.get(Calendar.DAY_OF_MONTH), 15);
        assertEquals(utcCalendar.get(Calendar.HOUR_OF_DAY), 11); // 13:30 Paris (DST) = 11:30 UTC
        assertEquals(utcCalendar.get(Calendar.MINUTE), 30);
        assertEquals(utcCalendar.get(Calendar.SECOND), 0);
    }

    @Test
    public void testToZonedDateTimeWithParisZone() {
        // Create TypeConversions instance with Paris timezone
        ZoneOffset parisWinterOffset =
                ZoneId.of("Europe/Paris")
                        .getRules()
                        .getOffset(Instant.parse("2025-01-26T12:30:00Z"));
        TypeConversions conversions = new TypeConversions(parisWinterOffset);

        // Create a UTC timestamp: January 26, 2025 at 12:30 UTC
        Instant instant = Instant.parse("2025-01-26T12:30:00Z");
        Timestamp utcTimestamp = Timestamp.from(instant);

        // Convert to Paris time
        ZonedDateTime parisTime = conversions.toZonedDateTime(utcTimestamp);

        assertNotNull(parisTime);

        // Should be 13:30 in Paris (UTC+1 in winter)
        assertEquals(parisTime.getYear(), 2025);
        assertEquals(parisTime.getMonthValue(), 1);
        assertEquals(parisTime.getDayOfMonth(), 26);
        assertEquals(parisTime.getHour(), 13); // 12:30 UTC = 13:30 Paris
        assertEquals(parisTime.getMinute(), 30);
        assertEquals(parisTime.getSecond(), 0);
    }

    @Test
    public void testToZonedDateTimeWithParisSummerTime() {
        // Create TypeConversions instance with Paris timezone for summer
        ZoneOffset parisSummerOffset =
                ZoneId.of("Europe/Paris")
                        .getRules()
                        .getOffset(Instant.parse("2025-07-15T11:30:00Z"));
        TypeConversions conversions = new TypeConversions(parisSummerOffset);

        // Create a UTC timestamp: July 15, 2025 at 11:30 UTC
        Instant instant = Instant.parse("2025-07-15T11:30:00Z");
        Timestamp utcTimestamp = Timestamp.from(instant);

        // Convert to Paris time
        ZonedDateTime parisTime = conversions.toZonedDateTime(utcTimestamp);

        assertNotNull(parisTime);

        // Should be 13:30 in Paris (UTC+2 in summer with DST)
        assertEquals(parisTime.getYear(), 2025);
        assertEquals(parisTime.getMonthValue(), 7);
        assertEquals(parisTime.getDayOfMonth(), 15);
        assertEquals(parisTime.getHour(), 13); // 11:30 UTC = 13:30 Paris (DST)
        assertEquals(parisTime.getMinute(), 30);
        assertEquals(parisTime.getSecond(), 0);
    }

    @Test
    public void testRoundTripConversionParisTime() {
        // Create TypeConversions instance with Paris timezone
        ZoneOffset parisOffset =
                ZoneId.of("Europe/Paris")
                        .getRules()
                        .getOffset(Instant.parse("2025-01-26T12:30:00Z"));
        TypeConversions conversions = new TypeConversions(parisOffset);

        // Original time: January 26, 2025 at 13:30 in Paris
        ZonedDateTime original =
                ZonedDateTime.of(2025, 1, 26, 13, 30, 45, 123456789, ZoneId.of("Europe/Paris"));

        // Convert to UTC timestamp and back
        Timestamp utcTimestamp = conversions.toUtcTimestamp(original);
        ZonedDateTime roundTrip = conversions.toZonedDateTime(utcTimestamp);

        assertNotNull(roundTrip);

        // Should preserve date and time in Paris timezone
        // Note: millisecond precision, so nanoseconds will be truncated
        assertEquals(roundTrip.getYear(), original.getYear());
        assertEquals(roundTrip.getMonthValue(), original.getMonthValue());
        assertEquals(roundTrip.getDayOfMonth(), original.getDayOfMonth());
        assertEquals(roundTrip.getHour(), original.getHour());
        assertEquals(roundTrip.getMinute(), original.getMinute());
        assertEquals(roundTrip.getSecond(), original.getSecond());

        // Verify millisecond precision (nanoseconds truncated to milliseconds)
        long originalMillis = original.toInstant().toEpochMilli();
        long roundTripMillis = roundTrip.toInstant().toEpochMilli();
        assertEquals(roundTripMillis, originalMillis);
    }

    @Test
    public void testToUtcTimestampWithNull() {
        TypeConversions conversions = new TypeConversions(PARIS_ZONE);
        Timestamp timestamp = conversions.toUtcTimestamp(null);
        assertNull(timestamp);
    }

    @Test
    public void testToZonedDateTimeWithNull() {
        TypeConversions conversions = new TypeConversions(PARIS_ZONE);
        ZonedDateTime zonedDateTime = conversions.toZonedDateTime(null);
        assertNull(zonedDateTime);
    }

    @Test
    public void testMillisecondPrecisionTruncation() {
        TypeConversions conversions = new TypeConversions(PARIS_ZONE);

        // Create a time with nanosecond precision
        ZonedDateTime original =
                ZonedDateTime.of(
                        2025,
                        1,
                        26,
                        13,
                        30,
                        45,
                        123456789, // 123.456789 milliseconds
                        ZoneId.of("Europe/Paris"));

        Timestamp timestamp = conversions.toUtcTimestamp(original);

        // Verify that nanoseconds are truncated to milliseconds
        // 123456789 nanoseconds = 123 milliseconds (truncated)
        long nanos = timestamp.toInstant().getNano();
        assertEquals(nanos, 123000000); // Only millisecond precision retained
    }

    @Test
    public void testDifferentTimezones() {
        // Test with different timezones to ensure UTC storage works correctly
        ZoneOffset tokyoOffset =
                ZoneId.of("Asia/Tokyo").getRules().getOffset(Instant.parse("2025-01-26T03:30:00Z"));
        ZoneOffset newYorkOffset =
                ZoneId.of("America/New_York")
                        .getRules()
                        .getOffset(Instant.parse("2025-01-26T17:30:00Z"));

        TypeConversions tokyoConversions = new TypeConversions(tokyoOffset);
        TypeConversions newYorkConversions = new TypeConversions(newYorkOffset);

        // Same instant in time: 12:30 UTC
        Instant instant = Instant.parse("2025-01-26T12:30:00Z");

        // Tokyo: UTC+9 -> 21:30
        ZonedDateTime tokyoTime =
                ZonedDateTime.of(2025, 1, 26, 21, 30, 0, 0, ZoneId.of("Asia/Tokyo"));

        // New York: UTC-5 -> 07:30
        ZonedDateTime newYorkTime =
                ZonedDateTime.of(2025, 1, 26, 7, 30, 0, 0, ZoneId.of("America/New_York"));

        // Both should produce the same UTC timestamp
        Timestamp tokyoTimestamp = tokyoConversions.toUtcTimestamp(tokyoTime);
        Timestamp newYorkTimestamp = newYorkConversions.toUtcTimestamp(newYorkTime);

        assertEquals(tokyoTimestamp.toInstant(), instant);
        assertEquals(newYorkTimestamp.toInstant(), instant);
        assertEquals(tokyoTimestamp, newYorkTimestamp);
    }

    @Test
    public void testTimestampFieldValuesDirectly() {
        // This test directly verifies that Timestamp stores UTC values correctly
        // by using Calendar API with UTC timezone to extract the stored values

        TypeConversions conversions =
                new TypeConversions(
                        ZoneId.of("Europe/Paris")
                                .getRules()
                                .getOffset(Instant.parse("2025-01-26T12:30:00Z")));

        // Create a ZonedDateTime: January 26, 2025 at 13:30 in Paris (UTC+1)
        // This should be stored as 2025-01-26 12:30:00 UTC in the Timestamp
        ZonedDateTime parisTime =
                ZonedDateTime.of(2025, 1, 26, 13, 30, 0, 0, ZoneId.of("Europe/Paris"));

        // Convert to UTC timestamp
        Timestamp actualTimestamp = conversions.toUtcTimestamp(parisTime);

        // Create the expected UTC timestamp for comparison
        Instant expectedInstant = Instant.parse("2025-01-26T12:30:00Z");
        Timestamp expectedTimestamp = Timestamp.from(expectedInstant);

        // Use Calendar API with UTC timezone to extract values from both timestamps
        Calendar actualCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        actualCalendar.setTimeInMillis(actualTimestamp.getTime());

        Calendar expectedCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        expectedCalendar.setTimeInMillis(expectedTimestamp.getTime());

        // Compare internal field values directly - these should match
        assertEquals(
                actualCalendar.get(Calendar.YEAR),
                expectedCalendar.get(Calendar.YEAR),
                "Year mismatch");
        assertEquals(
                actualCalendar.get(Calendar.MONTH),
                expectedCalendar.get(Calendar.MONTH),
                "Month mismatch");
        assertEquals(
                actualCalendar.get(Calendar.DAY_OF_MONTH),
                expectedCalendar.get(Calendar.DAY_OF_MONTH),
                "Day mismatch");
        assertEquals(
                actualCalendar.get(Calendar.HOUR_OF_DAY),
                expectedCalendar.get(Calendar.HOUR_OF_DAY),
                "Hours mismatch - should be 12 (UTC)");
        assertEquals(
                actualCalendar.get(Calendar.MINUTE),
                expectedCalendar.get(Calendar.MINUTE),
                "Minutes mismatch");
        assertEquals(
                actualCalendar.get(Calendar.SECOND),
                expectedCalendar.get(Calendar.SECOND),
                "Seconds mismatch");

        // Verify the actual values are UTC (12:30, not Paris time 13:30)
        assertEquals(
                actualCalendar.get(Calendar.HOUR_OF_DAY),
                12,
                "Timestamp should store UTC hour 12, not Paris hour 13");
        assertEquals(actualCalendar.get(Calendar.MINUTE), 30);

        // Compare timestamps directly
        assertEquals(actualTimestamp, expectedTimestamp, "Timestamps should be equal");
    }
}
