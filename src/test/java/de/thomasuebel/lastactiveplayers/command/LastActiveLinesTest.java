package de.thomasuebel.lastactiveplayers.command;

import de.thomasuebel.lastactiveplayers.player.Player;
import de.thomasuebel.lastactiveplayers.player.Players;
import de.thomasuebel.lastactiveplayers.ranking.LeaderboardEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LastActiveLinesTest {

    private static final long ONE_HOUR_SECONDS = 3600L;
    private static final int SEVEN_DAYS = 7;
    private static final String MVP_TEMPLATE = "MVP: {player}";
    private static final String STREAK_TEMPLATE = "Streak: {player} ({streak} days)";

    private static LeaderboardEntry leaderboardEntry(final String username) {
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
                return ONE_HOUR_SECONDS;
            }
            @Override
            public Optional<Instant> lastLeave() {
                return Optional.empty();
            }
        };
    }

    private static Player existingPlayer(final String username, final int streak) {
        return new Player() {
            @Override
            public boolean exists() {
                return true;
            }
            @Override
            public UUID uuid() {
                return UUID.randomUUID();
            }
            @Override
            public String username() {
                return username;
            }
            @Override
            public int streakDays() {
                return streak;
            }
            @Override
            public Optional<LocalDate> streakLastDay() {
                return Optional.empty();
            }
        };
    }

    private static Player noPlayer() {
        return new Player() {
            @Override
            public boolean exists() {
                return false;
            }
            @Override
            public UUID uuid() {
                return new UUID(0L, 0L);
            }
            @Override
            public String username() {
                return "";
            }
            @Override
            public int streakDays() {
                return 0;
            }
            @Override
            public Optional<LocalDate> streakLastDay() {
                return Optional.empty();
            }
        };
    }

    private static Players withStreakLeader(final Player leader) {
        return new Players() {
            @Override
            public void upsert(final UUID uuid, final String username) {
            }
            @Override
            public void updateStreak(
                final UUID uuid, final int days, final Optional<LocalDate> last
            ) {
            }
            @Override
            public Player withUuid(final UUID uuid) {
                return noPlayer();
            }
            @Override
            public Player withHighestStreak() {
                return leader;
            }
            @Override
            public List<Player> withTopStreak() {
                return List.of();
            }
            @Override
            public void purgeInactiveBefore(final Instant threshold) {
            }

            @Override
            public int shields(final UUID uuid) {
                return 0;
            }

            @Override
            public void setShields(final UUID uuid, final int count) {
            }
        };
    }

    @Test
    void includesJoinListLines() {
        final CommandLines lines = new LastActiveLines(
            (exclude) -> List.of("line1", "line2"),
            (limit, exclude) -> List.of(),
            withStreakLeader(noPlayer()),
            MVP_TEMPLATE, STREAK_TEMPLATE
        );
        final List<String> result = lines.lines(Set.of());
        assertTrue(result.contains("line1"));
        assertTrue(result.contains("line2"));
    }

    @Test
    void includesMvpLineWhenMvpExists() {
        final CommandLines lines = new LastActiveLines(
            (exclude) -> List.of(),
            (limit, exclude) -> List.of(leaderboardEntry("Alice")),
            withStreakLeader(noPlayer()),
            MVP_TEMPLATE, STREAK_TEMPLATE
        );
        assertTrue(lines.lines(Set.of()).contains("MVP: Alice"));
    }

    @Test
    void skipsMvpLineWhenNoMvp() {
        final CommandLines lines = new LastActiveLines(
            (exclude) -> List.of(),
            (limit, exclude) -> List.of(),
            withStreakLeader(noPlayer()),
            MVP_TEMPLATE, STREAK_TEMPLATE
        );
        assertFalse(lines.lines(Set.of()).stream().anyMatch(l -> l.startsWith("MVP:")));
    }

    @Test
    void includesStreakLineWhenStreakLeaderExists() {
        final CommandLines lines = new LastActiveLines(
            (exclude) -> List.of(),
            (limit, exclude) -> List.of(),
            withStreakLeader(existingPlayer("Bob", SEVEN_DAYS)),
            MVP_TEMPLATE, STREAK_TEMPLATE
        );
        assertTrue(lines.lines(Set.of()).contains("Streak: Bob (7 days)"));
    }

    @Test
    void skipsStreakLineWhenNoStreakLeader() {
        final CommandLines lines = new LastActiveLines(
            (exclude) -> List.of(),
            (limit, exclude) -> List.of(),
            withStreakLeader(noPlayer()),
            MVP_TEMPLATE, STREAK_TEMPLATE
        );
        assertFalse(lines.lines(Set.of()).stream().anyMatch(l -> l.startsWith("Streak:")));
    }

    @Test
    void mvpNotExcludedByOnlineSet() {
        final UUID onlineUuid = UUID.randomUUID();
        final CommandLines lines = new LastActiveLines(
            (exclude) -> List.of(),
            (limit, exclude) -> exclude.isEmpty()
                ? List.of(leaderboardEntry("Alice"))
                : List.of(),
            withStreakLeader(noPlayer()),
            MVP_TEMPLATE, STREAK_TEMPLATE
        );
        assertTrue(lines.lines(Set.of(onlineUuid)).contains("MVP: Alice"));
    }

    @Test
    void passesOnlinePlayersToJoinMessage() {
        final UUID online = UUID.randomUUID();
        final CommandLines lines = new LastActiveLines(
            (exclude) -> exclude.contains(online) ? List.of("excluded") : List.of("visible"),
            (limit, exclude) -> List.of(),
            withStreakLeader(noPlayer()),
            MVP_TEMPLATE, STREAK_TEMPLATE
        );
        assertTrue(lines.lines(Set.of(online)).contains("excluded"));
    }
}
