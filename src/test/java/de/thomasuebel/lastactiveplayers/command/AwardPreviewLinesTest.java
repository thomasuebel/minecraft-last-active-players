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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AwardPreviewLinesTest {

    private static final long ONE_HOUR_SECONDS = 3600L;
    private static final int SEVEN_DAYS = 7;
    private static final String MVP_PREFIX = "[MVP] ";
    private static final String STREAK_PREFIX = "[Streak] ";

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
            public void purgeInactiveBefore(final Instant threshold) {
            }
        };
    }

    @Test
    void showsMvpWithPrefix() {
        final CommandLines preview = new AwardPreviewLines(
            (limit, exclude) -> List.of(leaderboardEntry("Alice")),
            withStreakLeader(noPlayer()),
            MVP_PREFIX, STREAK_PREFIX
        );
        assertEquals(List.of("[MVP] Alice"), preview.lines(Set.of()));
    }

    @Test
    void showsStreakWithPrefix() {
        final CommandLines preview = new AwardPreviewLines(
            (limit, exclude) -> List.of(),
            withStreakLeader(existingPlayer("Bob", SEVEN_DAYS)),
            MVP_PREFIX, STREAK_PREFIX
        );
        assertEquals(List.of("[Streak] Bob (7 days)"), preview.lines(Set.of()));
    }

    @Test
    void showsBothWhenBothExist() {
        final CommandLines preview = new AwardPreviewLines(
            (limit, exclude) -> List.of(leaderboardEntry("Alice")),
            withStreakLeader(existingPlayer("Bob", SEVEN_DAYS)),
            MVP_PREFIX, STREAK_PREFIX
        );
        final List<String> lines = preview.lines(Set.of());
        assertEquals(2, lines.size());
        assertEquals("[MVP] Alice", lines.get(0));
        assertEquals("[Streak] Bob (7 days)", lines.get(1));
    }

    @Test
    void emptyWhenNeitherExists() {
        final CommandLines preview = new AwardPreviewLines(
            (limit, exclude) -> List.of(),
            withStreakLeader(noPlayer()),
            MVP_PREFIX, STREAK_PREFIX
        );
        assertTrue(preview.lines(Set.of()).isEmpty());
    }
}
