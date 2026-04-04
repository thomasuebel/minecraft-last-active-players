package de.thomasuebel.lastactiveplayers.command;

import de.thomasuebel.lastactiveplayers.ranking.Leaderboard;
import de.thomasuebel.lastactiveplayers.ranking.LeaderboardEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MvpLinesTest {

    private static final long ONE_HOUR = 3600L;
    private static final String MVP_TEMPLATE = "MVP: {player}";
    private static final String TIE_TEMPLATE = "Tied MVPs: {players}";

    private static LeaderboardEntry entry(final String username) {
        return new LeaderboardEntry() {
            @Override
            public UUID uuid() {
                return UUID.randomUUID();
            }

            @Override
            public String username() {
                return username;
            }

            @Override
            public long totalSeconds() {
                return ONE_HOUR;
            }

            @Override
            public Optional<Instant> lastLeave() {
                return Optional.empty();
            }
        };
    }

    private static Leaderboard boardWithTied(final List<LeaderboardEntry> tied) {
        return new Leaderboard() {
            @Override
            public List<LeaderboardEntry> top(final int limit, final Set<UUID> exclude) {
                return List.of();
            }

            @Override
            public List<LeaderboardEntry> topTied(final Set<UUID> exclude) {
                return tied;
            }
        };
    }

    @Test
    void returnsSingleMvpLine() {
        final CommandLines lines = new MvpLines(
            boardWithTied(List.of(entry("Alice"))),
            MVP_TEMPLATE, TIE_TEMPLATE
        );
        assertEquals(List.of("MVP: Alice"), lines.lines(Set.of()));
    }

    @Test
    void returnsTieLineWhenMultipleMvps() {
        final CommandLines lines = new MvpLines(
            boardWithTied(List.of(entry("Alice"), entry("Bob"))),
            MVP_TEMPLATE, TIE_TEMPLATE
        );
        assertEquals(List.of("Tied MVPs: Alice, Bob"), lines.lines(Set.of()));
    }

    @Test
    void returnsEmptyWhenNoMvp() {
        final CommandLines lines = new MvpLines(
            boardWithTied(List.of()),
            MVP_TEMPLATE, TIE_TEMPLATE
        );
        assertTrue(lines.lines(Set.of()).isEmpty());
    }

    @Test
    void ignoresOnlinePlayers() {
        final CommandLines lines = new MvpLines(
            boardWithTied(List.of(entry("Alice"))),
            MVP_TEMPLATE, TIE_TEMPLATE
        );
        assertEquals(List.of("MVP: Alice"), lines.lines(Set.of(UUID.randomUUID())));
    }
}
