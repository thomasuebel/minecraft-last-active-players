package de.thomasuebel.lastactiveplayers.display;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HumanDurationTest {

    private static final long ZERO_SECONDS = 0L;
    private static final long THIRTY_SECONDS = 30L;
    private static final long ONE_MINUTE_SECONDS = 60L;
    private static final long NINETY_SECONDS = 90L;
    private static final long ONE_HOUR_SECONDS = 3600L;
    private static final long ONE_HOUR_THIRTY_SECONDS = 3630L;
    private static final long ONE_HOUR_ONE_MINUTE_SECONDS = 3660L;
    private static final long TWO_HOURS_SECONDS = 7200L;
    private static final long TWO_HOURS_ONE_MINUTE_SECONDS = 7260L;

    @Test
    void zeroSeconds() {
        assertEquals("0s", new HumanDuration(ZERO_SECONDS).text());
    }

    @Test
    void secondsOnly() {
        assertEquals("30s", new HumanDuration(THIRTY_SECONDS).text());
    }

    @Test
    void minutesOnly() {
        assertEquals("1m", new HumanDuration(ONE_MINUTE_SECONDS).text());
    }

    @Test
    void minutesAndSeconds() {
        assertEquals("1m 30s", new HumanDuration(NINETY_SECONDS).text());
    }

    @Test
    void hoursOnly() {
        assertEquals("1h", new HumanDuration(ONE_HOUR_SECONDS).text());
    }

    @Test
    void hoursWithTrailingSecondsDropped() {
        assertEquals("1h", new HumanDuration(ONE_HOUR_THIRTY_SECONDS).text());
    }

    @Test
    void hoursAndMinutes() {
        assertEquals("1h 1m", new HumanDuration(ONE_HOUR_ONE_MINUTE_SECONDS).text());
    }

    @Test
    void twoHoursOnly() {
        assertEquals("2h", new HumanDuration(TWO_HOURS_SECONDS).text());
    }

    @Test
    void twoHoursAndOneMinute() {
        assertEquals("2h 1m", new HumanDuration(TWO_HOURS_ONE_MINUTE_SECONDS).text());
    }
}
