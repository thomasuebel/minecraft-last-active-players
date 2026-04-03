package de.thomasuebel.lastactiveplayers.display;

import de.thomasuebel.lastactiveplayers.ranking.LeaderboardEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardJoinMessageTest {

    private static final long TWO_HOURS_SECONDS = 7200L;
    private static final long ONE_HOUR_SECONDS = 3600L;
    private static final Instant LAST_LEAVE = Instant.parse("2026-03-10T12:00:00Z");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static LeaderboardEntry entry(final String username, final long seconds) {
        return new LeaderboardEntry() {
            @Override public UUID uuid() { return UUID.randomUUID(); }
            @Override public String username() { return username; }
            @Override public long totalSeconds() { return seconds; }
            @Override public Optional<Instant> lastLeave() { return Optional.of(LAST_LEAVE); }
        };
    }

    @Test
    void returnsOneLinePerEntry() {
        final List<LeaderboardEntry> entries = List.of(
            entry("Alice", TWO_HOURS_SECONDS),
            entry("Bob", ONE_HOUR_SECONDS)
        );
        final JoinMessage msg = new LeaderboardJoinMessage(
            (limit, exclude) -> entries, entries.size(), "{n}. {player}", DATE_FMT, ZoneOffset.UTC
        );
        final List<String> lines = msg.lines(Set.of());
        assertEquals(2, lines.size());
        assertEquals("1. Alice", lines.get(0));
        assertEquals("2. Bob", lines.get(1));
    }

    @Test
    void respectsListSizeLimit() {
        final List<LeaderboardEntry> entries = List.of(
            entry("Alice", TWO_HOURS_SECONDS),
            entry("Bob", ONE_HOUR_SECONDS)
        );
        final JoinMessage msg = new LeaderboardJoinMessage(
            (limit, exclude) -> entries.subList(0, limit),
            1, "{n}. {player}", DATE_FMT, ZoneOffset.UTC
        );
        assertEquals(1, msg.lines(Set.of()).size());
    }

    @Test
    void returnsEmptyListWhenNoEntries() {
        final JoinMessage msg = new LeaderboardJoinMessage(
            (limit, exclude) -> List.of(), 3, "{n}. {player}", DATE_FMT, ZoneOffset.UTC
        );
        assertTrue(msg.lines(Set.of()).isEmpty());
    }

    @Test
    void passesExcludeSetToLeaderboard() {
        final UUID excluded = UUID.randomUUID();
        final JoinMessage msg = new LeaderboardJoinMessage(
            (limit, exclude) -> exclude.isEmpty()
                ? List.of(entry("Alice", ONE_HOUR_SECONDS))
                : List.of(),
            3, "{n}. {player}", DATE_FMT, ZoneOffset.UTC
        );
        assertTrue(msg.lines(Set.of(excluded)).isEmpty());
        assertEquals(1, msg.lines(Set.of()).size());
    }
}
