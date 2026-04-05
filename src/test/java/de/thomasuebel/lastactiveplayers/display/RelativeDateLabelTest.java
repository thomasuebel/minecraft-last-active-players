package de.thomasuebel.lastactiveplayers.display;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelativeDateLabelTest {

    // "now" is 2026-04-05 12:00 UTC for all tests
    private static final Instant NOW = Instant.parse("2026-04-05T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final DateTimeFormatter FALLBACK =
        DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private static final String TODAY = "today";
    private static final String YESTERDAY = "yesterday";
    private static final String DAYS_AGO = "{days} days ago";

    private DateLabel label() {
        return new RelativeDateLabel(CLOCK, ZoneOffset.UTC, TODAY, YESTERDAY, DAYS_AGO, FALLBACK);
    }

    @Test
    void returnsToday() {
        assertEquals(TODAY, label().text(Instant.parse("2026-04-05T06:00:00Z")));
    }

    @Test
    void returnsYesterday() {
        assertEquals(YESTERDAY, label().text(Instant.parse("2026-04-04T06:00:00Z")));
    }

    @Test
    void returnsTwoDaysAgo() {
        assertEquals("2 days ago", label().text(Instant.parse("2026-04-03T06:00:00Z")));
    }

    @Test
    void returnsSixDaysAgo() {
        assertEquals("6 days ago", label().text(Instant.parse("2026-03-30T06:00:00Z")));
    }

    @Test
    void returnsFallbackAtSevenDays() {
        assertEquals("2026-03-29", label().text(Instant.parse("2026-03-29T06:00:00Z")));
    }

    @Test
    void returnsFallbackBeyondSevenDays() {
        assertEquals("2026-03-01", label().text(Instant.parse("2026-03-01T06:00:00Z")));
    }

    @Test
    void returnsTodayForFutureInstant() {
        // Clock skew or a session heartbeat written after a server clock correction
        // could produce an instant in the future; must not silently return a future date.
        assertEquals(TODAY, label().text(Instant.parse("2026-04-06T06:00:00Z")));
    }

    @Test
    void daysBoundaryRespectsTimezone() {
        // 23:30 UTC on the 4th is still "yesterday" in UTC
        assertEquals(YESTERDAY, label().text(Instant.parse("2026-04-04T23:30:00Z")));
    }
}
