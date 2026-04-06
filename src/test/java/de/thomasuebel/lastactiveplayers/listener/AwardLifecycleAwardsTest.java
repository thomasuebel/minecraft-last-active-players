package de.thomasuebel.lastactiveplayers.listener;

import de.thomasuebel.lastactiveplayers.player.PlayerRecord;
import de.thomasuebel.lastactiveplayers.player.Players;
import de.thomasuebel.lastactiveplayers.player.StreakMilestones;
import de.thomasuebel.lastactiveplayers.ranking.Awards;
import de.thomasuebel.lastactiveplayers.ranking.Leaderboard;
import de.thomasuebel.lastactiveplayers.ranking.LeaderboardEntry;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link AwardLifecycle} as an {@link Awards} implementation,
 * verifying {@code currentPrefix()} and {@code currentAward()} after elections.
 */
class AwardLifecycleAwardsTest {

    private static final String MVP_PREFIX = "[Crown] ";
    private static final String STREAK_PREFIX = "[Fire] ";
    private static final String MVP_TEMPLATE = "MVP: {player}";
    private static final String MVP_TIE_TEMPLATE = "MVP tie: {players}";
    private static final String STREAK_TEMPLATE = "Streak: {player} ({streak})";
    private static final String STREAK_TIE_TEMPLATE = "Streak tie: {players} ({streak})";
    private static final long DELAY_TICKS = 0L;
    private static final int SEVEN_DAY_STREAK = 7;
    private static final int ONE_DAY_STREAK = 1;
    private static final long ONE_HOUR_SECONDS = 3600L;
    private static final UUID MVP_UUID = UUID.randomUUID();
    private static final String MVP_NAME = "MvpPlayer";
    private static final UUID STREAK_UUID = UUID.randomUUID();
    private static final String STREAK_NAME = "StreakPlayer";
    private static final UUID OTHER_UUID = UUID.randomUUID();

    // ---------------------------------------------------------------------
    // Stubs
    // ---------------------------------------------------------------------

    /**
     * Builds a stub leaderboard whose {@code topTied()} returns the given entries.
     *
     * @param tiedEntries entries returned by {@code topTied()}
     * @return a leaderboard stub
     */
    private static Leaderboard stubLeaderboard(
        final List<LeaderboardEntry> tiedEntries
    ) {
        return new Leaderboard() {
            @Override
            public List<LeaderboardEntry> top(
                final int limit, final Set<UUID> exclude
            ) {
                return tiedEntries;
            }

            @Override
            public List<LeaderboardEntry> topTied(final Set<UUID> exclude) {
                return tiedEntries;
            }
        };
    }

    /**
     * Builds a stub {@link Players} whose {@code withTopStreak()} returns the given list.
     *
     * @param topStreak the list returned by {@code withTopStreak()}
     * @return a players stub
     */
    private static Players stubPlayers(final List<PlayerRecord> topStreak) {
        return new Players() {
            @Override
            public void upsert(final UUID uuid, final String username) { }

            @Override
            public void updateStreak(
                final UUID uuid, final int days,
                final Optional<LocalDate> lastDay
            ) { }

            @Override
            public Optional<PlayerRecord> withUuid(final UUID uuid) {
                return Optional.empty();
            }

            @Override
            public Optional<PlayerRecord> withHighestStreak() {
                return Optional.empty();
            }

            @Override
            public List<PlayerRecord> withTopStreak() {
                return topStreak;
            }

            @Override
            public void purgeInactiveBefore(final Instant threshold) { }

            @Override
            public int shields(final UUID uuid) {
                return 0;
            }

            @Override
            public void storeShields(final UUID uuid, final int count) { }
        };
    }

    /**
     * Builds a proxy {@link Server} with empty online players and no-op broadcast.
     *
     * @return a server proxy
     */
    private static Server stubServer() {
        final InvocationHandler handler = (proxy, method, args) -> {
            if ("getOnlinePlayers".equals(method.getName())) {
                return Collections.emptyList();
            }
            if ("broadcastMessage".equals(method.getName())) {
                if (method.getReturnType() == int.class) {
                    return 0;
                }
                return null;
            }
            if (method.getReturnType() == boolean.class) {
                return false;
            }
            if (method.getReturnType() == int.class) {
                return 0;
            }
            return null;
        };
        return (Server) Proxy.newProxyInstance(
            Server.class.getClassLoader(),
            new Class<?>[]{Server.class},
            handler
        );
    }

    /**
     * Builds a proxy {@link Plugin} backed by the given server.
     *
     * @param server the server returned by {@code getServer()}
     * @return a plugin proxy
     */
    private static Plugin stubPlugin(final Server server) {
        final InvocationHandler handler = (proxy, method, args) -> {
            if ("getServer".equals(method.getName())) {
                return server;
            }
            if (method.getReturnType() == boolean.class) {
                return false;
            }
            return null;
        };
        return (Plugin) Proxy.newProxyInstance(
            Plugin.class.getClassLoader(),
            new Class<?>[]{Plugin.class},
            handler
        );
    }

    /**
     * Builds an {@link AwardLifecycle} wired to the given stubs.
     *
     * @param leaderboard the leaderboard stub
     * @param players     the players stub
     * @return a fully wired award lifecycle
     */
    private static AwardLifecycle lifecycle(
        final Leaderboard leaderboard, final Players players
    ) {
        return new AwardLifecycle(
            leaderboard, players, new StreakMilestones(),
            stubPlugin(stubServer()),
            MVP_PREFIX, STREAK_PREFIX,
            MVP_TEMPLATE, MVP_TIE_TEMPLATE,
            STREAK_TEMPLATE, STREAK_TIE_TEMPLATE,
            DELAY_TICKS
        );
    }

    // ---------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------

    @Test
    void returnsEmptyPrefixBeforeFirstElection() {
        final AwardLifecycle awards = lifecycle(
            stubLeaderboard(List.of()),
            stubPlayers(List.of())
        );
        assertEquals("", awards.currentPrefix(MVP_UUID));
    }

    @Test
    void returnsEmptyAwardBeforeFirstElection() {
        final AwardLifecycle awards = lifecycle(
            stubLeaderboard(List.of()),
            stubPlayers(List.of())
        );
        assertEquals("", awards.currentAward(MVP_UUID));
    }

    @Test
    void returnsMvpPrefixForElectedMvp() {
        final AwardLifecycle awards = lifecycle(
            stubLeaderboard(List.of(new LeaderboardEntry(
                MVP_UUID, MVP_NAME, ONE_HOUR_SECONDS, Optional.empty()
            ))),
            stubPlayers(List.of())
        );
        awards.broadcastIfChanged();
        assertEquals(MVP_PREFIX, awards.currentPrefix(MVP_UUID));
    }

    @Test
    void returnsMvpAwardForElectedMvp() {
        final AwardLifecycle awards = lifecycle(
            stubLeaderboard(List.of(new LeaderboardEntry(
                MVP_UUID, MVP_NAME, ONE_HOUR_SECONDS, Optional.empty()
            ))),
            stubPlayers(List.of())
        );
        awards.broadcastIfChanged();
        assertEquals("mvp", awards.currentAward(MVP_UUID));
    }

    @Test
    void returnsStreakPrefixForStreakLeaderAboveMilestone() {
        final AwardLifecycle awards = lifecycle(
            stubLeaderboard(List.of()),
            stubPlayers(List.of(new PlayerRecord(
                STREAK_UUID, STREAK_NAME,
                SEVEN_DAY_STREAK, Optional.empty()
            )))
        );
        awards.broadcastIfChanged();
        assertEquals(STREAK_PREFIX, awards.currentPrefix(STREAK_UUID));
    }

    @Test
    void returnsStreakAwardForStreakLeader() {
        final AwardLifecycle awards = lifecycle(
            stubLeaderboard(List.of()),
            stubPlayers(List.of(new PlayerRecord(
                STREAK_UUID, STREAK_NAME,
                SEVEN_DAY_STREAK, Optional.empty()
            )))
        );
        awards.broadcastIfChanged();
        assertEquals("streak", awards.currentAward(STREAK_UUID));
    }

    @Test
    void returnsEmptyPrefixForNonLeader() {
        final AwardLifecycle awards = lifecycle(
            stubLeaderboard(List.of(new LeaderboardEntry(
                MVP_UUID, MVP_NAME, ONE_HOUR_SECONDS, Optional.empty()
            ))),
            stubPlayers(List.of(new PlayerRecord(
                STREAK_UUID, STREAK_NAME,
                SEVEN_DAY_STREAK, Optional.empty()
            )))
        );
        awards.broadcastIfChanged();
        assertEquals("", awards.currentPrefix(OTHER_UUID));
    }

    @Test
    void returnsEmptyPrefixWhenStreakBelowMilestone() {
        final AwardLifecycle awards = lifecycle(
            stubLeaderboard(List.of()),
            stubPlayers(List.of(new PlayerRecord(
                STREAK_UUID, STREAK_NAME,
                ONE_DAY_STREAK, Optional.empty()
            )))
        );
        awards.broadcastIfChanged();
        assertEquals("", awards.currentPrefix(STREAK_UUID));
    }
}
