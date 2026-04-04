package de.thomasuebel.lastactiveplayers.command;

import de.thomasuebel.lastactiveplayers.player.Player;
import de.thomasuebel.lastactiveplayers.player.Players;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreakLinesTest {

    private static final int SEVEN_DAYS = 7;
    private static final String STREAK_TEMPLATE = "Streak: {player} ({streak} days)";
    private static final String TIE_TEMPLATE = "Tied: {players} ({streak} days)";

    private static Player player(final String username, final int streak) {
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

    private static Players withTopStreak(final List<Player> leaders) {
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
                return leaders.isEmpty() ? noPlayer() : leaders.get(0);
            }

            @Override
            public Player withHighestStreak() {
                return leaders.isEmpty() ? noPlayer() : leaders.get(0);
            }

            @Override
            public List<Player> withTopStreak() {
                return leaders;
            }

            @Override
            public void purgeInactiveBefore(final Instant threshold) {
            }

            @Override
            public int shields(final UUID uuid) {
                return 0;
            }

            @Override
            public void storeShields(final UUID uuid, final int count) {
            }

            private Player noPlayer() {
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
        };
    }

    @Test
    void returnsSingleStreakLine() {
        final CommandLines lines = new StreakLines(
            withTopStreak(List.of(player("Alice", SEVEN_DAYS))),
            STREAK_TEMPLATE, TIE_TEMPLATE
        );
        assertEquals(List.of("Streak: Alice (7 days)"), lines.lines(Set.of()));
    }

    @Test
    void returnsTieLineWhenMultipleLeaders() {
        final CommandLines lines = new StreakLines(
            withTopStreak(List.of(player("Alice", SEVEN_DAYS), player("Bob", SEVEN_DAYS))),
            STREAK_TEMPLATE, TIE_TEMPLATE
        );
        assertEquals(List.of("Tied: Alice, Bob (7 days)"), lines.lines(Set.of()));
    }

    @Test
    void returnsEmptyWhenNoStreakLeader() {
        final CommandLines lines = new StreakLines(
            withTopStreak(List.of()),
            STREAK_TEMPLATE, TIE_TEMPLATE
        );
        assertTrue(lines.lines(Set.of()).isEmpty());
    }

    @Test
    void ignoresOnlinePlayers() {
        final CommandLines lines = new StreakLines(
            withTopStreak(List.of(player("Alice", SEVEN_DAYS))),
            STREAK_TEMPLATE, TIE_TEMPLATE
        );
        assertEquals(List.of("Streak: Alice (7 days)"), lines.lines(Set.of(UUID.randomUUID())));
    }
}
