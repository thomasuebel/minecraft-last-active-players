package de.thomasuebel.lastactiveplayers.display;

import de.thomasuebel.lastactiveplayers.ranking.LeaderboardEntry;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Package-private value object that formats a single last-active player entry line by
 * replacing template tokens with values from a {@link LeaderboardEntry}.
 *
 * <p>Supported tokens: {@code {n}} (1-based rank), {@code {player}}, {@code {date}},
 * {@code {duration}}.
 */
final class JoinEntryLine {

    private static final String TOKEN_N = "{n}";
    private static final String TOKEN_PLAYER = "{player}";
    private static final String TOKEN_DATE = "{date}";
    private static final String TOKEN_DURATION = "{duration}";

    private final LeaderboardEntry entry;
    private final int rank;
    private final String template;
    private final DateTimeFormatter formatter;
    private final ZoneId zone;

    JoinEntryLine(
        final LeaderboardEntry entry,
        final int rank,
        final String template,
        final DateTimeFormatter formatter,
        final ZoneId zone
    ) {
        this.entry = entry;
        this.rank = rank;
        this.template = template;
        this.formatter = formatter;
        this.zone = zone;
    }

    String text() {
        final String date = this.entry.lastLeave()
            .map(instant -> LocalDate.ofInstant(instant, this.zone).format(this.formatter))
            .orElse("");
        return this.template
            .replace(TOKEN_N, String.valueOf(this.rank))
            .replace(TOKEN_PLAYER, this.entry.username())
            .replace(TOKEN_DATE, date)
            .replace(TOKEN_DURATION, new HumanDuration(this.entry.totalSeconds()).text());
    }
}
