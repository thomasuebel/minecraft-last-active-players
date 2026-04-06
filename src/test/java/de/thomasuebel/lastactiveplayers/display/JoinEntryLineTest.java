package de.thomasuebel.lastactiveplayers.display;

import de.thomasuebel.lastactiveplayers.ranking.LeaderboardEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JoinEntryLineTest {

    private static final int RANK_ONE = 1;
    private static final int RANK_THREE = 3;
    private static final long ONE_HOUR_SECONDS = 3600L;
    private static final Instant LAST_LEAVE = Instant.parse("2026-03-10T12:00:00Z");
    private static final DateLabel DATE_LABEL = instant -> "2026-03-10";

    private static LeaderboardEntry entry(
        final String username, final long seconds, final Optional<Instant> leave
    ) {
        return new LeaderboardEntry(UUID.randomUUID(), username, seconds, leave);
    }

    @Test
    void replacesAllTokens() {
        final LeaderboardEntry e = entry("Alice", ONE_HOUR_SECONDS, Optional.of(LAST_LEAVE));
        final String line = new JoinEntryLine(
            e, RANK_ONE, "{n}. {player} - {date} ({duration})", DATE_LABEL
        ).text();
        assertEquals("1. Alice - 2026-03-10 (1h)", line);
    }

    @Test
    void partialTokenTemplate() {
        final LeaderboardEntry e = entry("Dave", ONE_HOUR_SECONDS, Optional.of(LAST_LEAVE));
        final String line = new JoinEntryLine(
            e, RANK_ONE, "Hello {player}!", DATE_LABEL
        ).text();
        assertEquals("Hello Dave!", line);
    }

    @Test
    void rankTokenSubstituted() {
        final LeaderboardEntry e = entry("Bob", ONE_HOUR_SECONDS, Optional.of(LAST_LEAVE));
        final String line = new JoinEntryLine(
            e, RANK_THREE, "{n}", DATE_LABEL
        ).text();
        assertEquals("3", line);
    }

    @Test
    void emptyLastLeaveGivesEmptyDate() {
        final LeaderboardEntry e = entry("Carol", ONE_HOUR_SECONDS, Optional.empty());
        final String line = new JoinEntryLine(
            e, RANK_ONE, "{date}", DATE_LABEL
        ).text();
        assertEquals("", line);
    }
}
