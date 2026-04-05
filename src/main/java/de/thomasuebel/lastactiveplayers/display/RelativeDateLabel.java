package de.thomasuebel.lastactiveplayers.display;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * {@link DateLabel} that returns "today", "yesterday", or "N days ago" for dates within
 * the last six days, falling back to a formatted date string for anything older.
 *
 * <p>All three relative strings are configurable so operators can localise them.
 * The {@code {days}} token in the days-ago template is replaced with the actual count.
 */
public final class RelativeDateLabel implements DateLabel {

    private static final long RELATIVE_THRESHOLD_DAYS = 7L;
    private static final String TOKEN_DAYS = "{days}";

    private final Clock clock;
    private final ZoneId zone;
    private final String today;
    private final String yesterday;
    private final String daysAgo;
    private final DateTimeFormatter fallback;

    /**
     * Constructs a relative date label.
     *
     * @param clock     used to determine the current date on each call; never null
     * @param zone      time zone for converting instants to local dates; never null
     * @param today     label for a date matching today; never null
     * @param yesterday label for a date one day before today; never null
     * @param daysAgo   template for 2-6 days ago; use {@code {days}} for the count; never null
     * @param fallback  formatter used for dates seven or more days ago; never null
     */
    public RelativeDateLabel(
        final Clock clock,
        final ZoneId zone,
        final String today,
        final String yesterday,
        final String daysAgo,
        final DateTimeFormatter fallback
    ) {
        this.clock = clock;
        this.zone = zone;
        this.today = today;
        this.yesterday = yesterday;
        this.daysAgo = daysAgo;
        this.fallback = fallback;
    }

    @Override
    public String text(final Instant instant) {
        final LocalDate date = LocalDate.ofInstant(instant, this.zone);
        final LocalDate now = LocalDate.ofInstant(Instant.now(this.clock), this.zone);
        final long days = ChronoUnit.DAYS.between(date, now);
        if (days <= 0) {
            return this.today;
        }
        if (days == 1) {
            return this.yesterday;
        }
        if (days < RELATIVE_THRESHOLD_DAYS) {
            return this.daysAgo.replace(TOKEN_DAYS, String.valueOf(days));
        }
        return date.format(this.fallback);
    }
}
