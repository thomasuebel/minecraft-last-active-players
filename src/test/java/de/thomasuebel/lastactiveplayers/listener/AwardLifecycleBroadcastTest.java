package de.thomasuebel.lastactiveplayers.listener;

import de.thomasuebel.lastactiveplayers.player.PlayerRecord;
import de.thomasuebel.lastactiveplayers.player.Players;
import de.thomasuebel.lastactiveplayers.player.StreakMilestones;
import de.thomasuebel.lastactiveplayers.ranking.Leaderboard;
import de.thomasuebel.lastactiveplayers.ranking.LeaderboardEntry;
import org.bukkit.Server;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link AwardLifecycle} broadcast behaviour via
 * {@code broadcastIfChanged()} and {@code onJoin()}.
 */
class AwardLifecycleBroadcastTest {

    private static final String MVP_PREFIX = "[Crown] ";
    private static final String STREAK_PREFIX = "[Fire] ";
    private static final String MVP_TEMPLATE = "MVP: {player}";
    private static final String MVP_TIE_TEMPLATE = "MVP tie: {players}";
    private static final String STREAK_TEMPLATE =
        "Streak: {player} ({streak})";
    private static final String STREAK_TIE_TEMPLATE =
        "Streak tie: {players} ({streak})";
    private static final long DELAY_TICKS = 0L;
    private static final int SEVEN_DAY_STREAK = 7;
    private static final long ONE_HOUR_SECONDS = 3600L;
    private static final long TWO_HOURS_SECONDS = 7200L;
    private static final UUID MVP_UUID = UUID.randomUUID();
    private static final String MVP_NAME = "MvpPlayer";
    private static final UUID MVP2_UUID = UUID.randomUUID();
    private static final String MVP2_NAME = "MvpTwo";
    private static final UUID STREAK_UUID = UUID.randomUUID();
    private static final String STREAK_NAME = "StreakPlayer";
    private static final UUID STREAK2_UUID = UUID.randomUUID();
    private static final String STREAK2_NAME = "StreakTwo";

    // ---------------------------------------------------------------------
    // Stubs
    // ---------------------------------------------------------------------

    /**
     * Builds a stub leaderboard whose {@code topTied()} returns the entries.
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
            public List<LeaderboardEntry> topTied(
                final Set<UUID> exclude
            ) {
                return tiedEntries;
            }
        };
    }

    /**
     * Builds a stub {@link Players} whose {@code withTopStreak()} returns
     * the given list.
     *
     * @param topStreak the list returned by {@code withTopStreak()}
     * @return a players stub
     */
    private static Players stubPlayers(
        final List<PlayerRecord> topStreak
    ) {
        return new Players() {
            @Override
            public void upsert(
                final UUID uuid, final String username
            ) { }

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
            public void purgeInactiveBefore(
                final Instant threshold
            ) { }

            @Override
            public int shields(final UUID uuid) {
                return 0;
            }

            @Override
            public void storeShields(
                final UUID uuid, final int count
            ) { }
        };
    }

    /**
     * Builds a proxy {@link Server} with empty online players that
     * captures broadcast messages.
     *
     * @param broadcasts list that captures broadcast messages
     * @return a server proxy
     */
    private static Server stubServer(final List<String> broadcasts) {
        return stubServer(broadcasts, Collections.emptyList());
    }

    /**
     * Builds a proxy {@link Server} with configurable online players
     * that captures broadcast messages.
     *
     * @param broadcasts    list that captures broadcast messages
     * @param onlinePlayers collection of online players
     * @return a server proxy
     */
    private static Server stubServer(
        final List<String> broadcasts,
        final Collection<org.bukkit.entity.Player> onlinePlayers
    ) {
        final InvocationHandler handler = (proxy, method, args) -> {
            if ("getOnlinePlayers".equals(method.getName())) {
                return onlinePlayers;
            }
            if ("broadcastMessage".equals(method.getName())) {
                if (args != null && args.length == 1
                    && args[0] instanceof String) {
                    broadcasts.add((String) args[0]);
                }
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
     * {@code isEnabled()} returns true so that
     * {@link PermissionAttachment} construction succeeds.
     *
     * @param server the server returned by {@code getServer()}
     * @return a plugin proxy
     */
    private static Plugin stubPlugin(final Server server) {
        final InvocationHandler handler = (proxy, method, args) -> {
            if ("getServer".equals(method.getName())) {
                return server;
            }
            if ("isEnabled".equals(method.getName())) {
                return true;
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
     * Builds a proxy Bukkit {@link org.bukkit.entity.Player} that captures
     * permission operations. The player creates a real
     * {@link PermissionAttachment} on {@code addAttachment()}, and captures
     * {@code setPermission()} calls made on any returned attachment.
     *
     * @param uuid           the player UUID
     * @param name           the player name
     * @param permissions    list that captures setPermission calls
     * @param plugin         plugin used for addAttachment
     * @return a player proxy
     */
    private static org.bukkit.entity.Player stubOnlinePlayer(
        final UUID uuid,
        final String name,
        final List<String> permissions,
        final Plugin plugin
    ) {
        final InvocationHandler handler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "getUniqueId":
                    return uuid;
                case "getName":
                    return name;
                case "addAttachment":
                    return new PermissionAttachment(
                        plugin,
                        (org.bukkit.entity.Player) proxy
                    ) {
                        @Override
                        public void setPermission(
                            final String perm, final boolean value
                        ) {
                            permissions.add(perm);
                        }

                        @Override
                        public boolean remove() {
                            return true;
                        }
                    };
                case "isOnline":
                    return true;
                case "removeAttachment":
                    return null;
                case "recalculatePermissions":
                    return null;
                default:
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    }
                    return null;
            }
        };
        return (org.bukkit.entity.Player) Proxy.newProxyInstance(
            org.bukkit.entity.Player.class.getClassLoader(),
            new Class<?>[]{org.bukkit.entity.Player.class},
            handler
        );
    }

    /**
     * Builds an {@link AwardLifecycle} wired to the given stubs.
     *
     * @param leaderboard the leaderboard stub
     * @param players     the players stub
     * @param plugin      the plugin proxy
     * @return a fully wired award lifecycle
     */
    private static AwardLifecycle lifecycle(
        final Leaderboard leaderboard,
        final Players players,
        final Plugin plugin
    ) {
        return lifecycle(leaderboard, players, plugin, AwardPermissions.NONE);
    }

    private static AwardLifecycle lifecycle(
        final Leaderboard leaderboard,
        final Players players,
        final Plugin plugin,
        final AwardPermissions extraPermissions
    ) {
        return new AwardLifecycle(
            leaderboard, players, new StreakMilestones(),
            plugin,
            MVP_PREFIX, STREAK_PREFIX,
            MVP_TEMPLATE, MVP_TIE_TEMPLATE,
            STREAK_TEMPLATE, STREAK_TIE_TEMPLATE,
            DELAY_TICKS,
            extraPermissions
        );
    }

    // ---------------------------------------------------------------------
    // Broadcast tests
    // ---------------------------------------------------------------------

    @Test
    void broadcastsMvpOnFirstElection() {
        final List<String> broadcasts = new ArrayList<>();
        final Server server = stubServer(broadcasts);
        final AwardLifecycle awards = lifecycle(
            stubLeaderboard(List.of(new LeaderboardEntry(
                MVP_UUID, MVP_NAME,
                ONE_HOUR_SECONDS, Optional.empty()
            ))),
            stubPlayers(List.of()),
            stubPlugin(server)
        );
        awards.broadcastIfChanged();
        assertTrue(broadcasts.contains("MVP: MvpPlayer"));
    }

    @Test
    void broadcastsStreakOnFirstElection() {
        final List<String> broadcasts = new ArrayList<>();
        final Server server = stubServer(broadcasts);
        final AwardLifecycle awards = lifecycle(
            stubLeaderboard(List.of()),
            stubPlayers(List.of(new PlayerRecord(
                STREAK_UUID, STREAK_NAME,
                SEVEN_DAY_STREAK, Optional.empty()
            ))),
            stubPlugin(server)
        );
        awards.broadcastIfChanged();
        assertTrue(broadcasts.contains("Streak: StreakPlayer (7)"));
    }

    @Test
    void doesNotBroadcastWhenLeadersUnchanged() {
        final List<String> broadcasts = new ArrayList<>();
        final Server server = stubServer(broadcasts);
        final AwardLifecycle awards = lifecycle(
            stubLeaderboard(List.of(new LeaderboardEntry(
                MVP_UUID, MVP_NAME,
                ONE_HOUR_SECONDS, Optional.empty()
            ))),
            stubPlayers(List.of(new PlayerRecord(
                STREAK_UUID, STREAK_NAME,
                SEVEN_DAY_STREAK, Optional.empty()
            ))),
            stubPlugin(server)
        );
        awards.broadcastIfChanged();
        final int firstCount = broadcasts.size();
        broadcasts.clear();
        awards.broadcastIfChanged();
        assertEquals(0, broadcasts.size());
    }

    @Test
    void broadcastsMvpTieMessage() {
        final List<String> broadcasts = new ArrayList<>();
        final Server server = stubServer(broadcasts);
        final AwardLifecycle awards = lifecycle(
            stubLeaderboard(List.of(
                new LeaderboardEntry(
                    MVP_UUID, MVP_NAME,
                    ONE_HOUR_SECONDS, Optional.empty()
                ),
                new LeaderboardEntry(
                    MVP2_UUID, MVP2_NAME,
                    ONE_HOUR_SECONDS, Optional.empty()
                )
            )),
            stubPlayers(List.of()),
            stubPlugin(server)
        );
        awards.broadcastIfChanged();
        assertTrue(broadcasts.contains(
            "MVP tie: MvpPlayer, MvpTwo"
        ));
    }

    @Test
    void broadcastsStreakTieMessage() {
        final List<String> broadcasts = new ArrayList<>();
        final Server server = stubServer(broadcasts);
        final AwardLifecycle awards = lifecycle(
            stubLeaderboard(List.of()),
            stubPlayers(List.of(
                new PlayerRecord(
                    STREAK_UUID, STREAK_NAME,
                    SEVEN_DAY_STREAK, Optional.empty()
                ),
                new PlayerRecord(
                    STREAK2_UUID, STREAK2_NAME,
                    SEVEN_DAY_STREAK, Optional.empty()
                )
            )),
            stubPlugin(server)
        );
        awards.broadcastIfChanged();
        assertTrue(broadcasts.contains(
            "Streak tie: StreakPlayer, StreakTwo (7)"
        ));
    }

    @Test
    void broadcastsOnlyChangedCategory() {
        final UUID stableMvp = UUID.randomUUID();
        final List<String> broadcasts = new ArrayList<>();
        final Server server = stubServer(broadcasts);
        // First election: stableMvp as MVP, STREAK_UUID as streak leader.
        final AwardLifecycle awards = lifecycle(
            stubLeaderboard(List.of(new LeaderboardEntry(
                stableMvp, "StableMvp",
                TWO_HOURS_SECONDS, Optional.empty()
            ))),
            stubPlayers(List.of(new PlayerRecord(
                STREAK_UUID, STREAK_NAME,
                SEVEN_DAY_STREAK, Optional.empty()
            ))),
            stubPlugin(server)
        );
        awards.broadcastIfChanged();
        broadcasts.clear();
        // Second election: same data, nothing should broadcast.
        awards.broadcastIfChanged();
        assertTrue(broadcasts.isEmpty());
    }

    @Test
    void grantsMvpPermissionToOnlinePlayer() {
        final List<String> broadcasts = new ArrayList<>();
        final List<String> permissions = new ArrayList<>();
        final Server serverForPlugin = stubServer(broadcasts);
        final Plugin plugin = stubPlugin(serverForPlugin);
        final org.bukkit.entity.Player onlinePlayer = stubOnlinePlayer(
            MVP_UUID, MVP_NAME, permissions, plugin
        );
        final Server serverWithPlayer = stubServer(
            broadcasts, List.of(onlinePlayer)
        );
        final Plugin pluginWithPlayer = stubPlugin(serverWithPlayer);
        final AwardLifecycle awards = lifecycle(
            stubLeaderboard(List.of(new LeaderboardEntry(
                MVP_UUID, MVP_NAME,
                ONE_HOUR_SECONDS, Optional.empty()
            ))),
            stubPlayers(List.of()),
            pluginWithPlayer
        );
        awards.broadcastIfChanged();
        assertTrue(permissions.contains("lastactiveplayers.mvp"));
    }

    @Test
    void grantsMvpExtraPermissionsToOnlinePlayer() {
        final List<String> broadcasts = new ArrayList<>();
        final List<String> permissions = new ArrayList<>();
        final Server serverForPlugin = stubServer(broadcasts);
        final Plugin plugin = stubPlugin(serverForPlugin);
        final org.bukkit.entity.Player onlinePlayer = stubOnlinePlayer(
            MVP_UUID, MVP_NAME, permissions, plugin
        );
        final Server serverWithPlayer = stubServer(
            broadcasts, List.of(onlinePlayer)
        );
        final Plugin pluginWithPlayer = stubPlugin(serverWithPlayer);
        final AwardPermissions extra = new AwardPermissions(
            List.of("essentials.kits.mvp-daily"),
            Map.of()
        );
        final AwardLifecycle awards = lifecycle(
            stubLeaderboard(List.of(new LeaderboardEntry(
                MVP_UUID, MVP_NAME,
                ONE_HOUR_SECONDS, Optional.empty()
            ))),
            stubPlayers(List.of()),
            pluginWithPlayer,
            extra
        );
        awards.broadcastIfChanged();
        assertTrue(permissions.contains("lastactiveplayers.mvp"));
        assertTrue(permissions.contains("essentials.kits.mvp-daily"));
    }

    @Test
    void grantsStreakExtraPermissionsToOnlinePlayer() {
        final List<String> broadcasts = new ArrayList<>();
        final List<String> permissions = new ArrayList<>();
        final Server serverForPlugin = stubServer(broadcasts);
        final Plugin plugin = stubPlugin(serverForPlugin);
        final org.bukkit.entity.Player onlinePlayer = stubOnlinePlayer(
            STREAK_UUID, STREAK_NAME, permissions, plugin
        );
        final Server serverWithPlayer = stubServer(
            broadcasts, List.of(onlinePlayer)
        );
        final Plugin pluginWithPlayer = stubPlugin(serverWithPlayer);
        final AwardPermissions extra = new AwardPermissions(
            List.of(),
            Map.of(SEVEN_DAY_STREAK, List.of("essentials.kits.streak-7"))
        );
        final AwardLifecycle awards = lifecycle(
            stubLeaderboard(List.of()),
            stubPlayers(List.of(new PlayerRecord(
                STREAK_UUID, STREAK_NAME,
                SEVEN_DAY_STREAK, Optional.empty()
            ))),
            pluginWithPlayer,
            extra
        );
        awards.broadcastIfChanged();
        assertTrue(permissions.contains("lastactiveplayers.streak.7"));
        assertTrue(permissions.contains("essentials.kits.streak-7"));
    }

    @Test
    void doesNotGrantStreakExtraForUnmatchedMilestone() {
        final int fourteen = 14;
        final List<String> broadcasts = new ArrayList<>();
        final List<String> permissions = new ArrayList<>();
        final Server serverForPlugin = stubServer(broadcasts);
        final Plugin plugin = stubPlugin(serverForPlugin);
        final org.bukkit.entity.Player onlinePlayer = stubOnlinePlayer(
            STREAK_UUID, STREAK_NAME, permissions, plugin
        );
        final Server serverWithPlayer = stubServer(
            broadcasts, List.of(onlinePlayer)
        );
        final Plugin pluginWithPlayer = stubPlugin(serverWithPlayer);
        final AwardPermissions extra = new AwardPermissions(
            List.of(),
            Map.of(fourteen, List.of("essentials.kits.streak-14"))
        );
        final AwardLifecycle awards = lifecycle(
            stubLeaderboard(List.of()),
            stubPlayers(List.of(new PlayerRecord(
                STREAK_UUID, STREAK_NAME,
                SEVEN_DAY_STREAK, Optional.empty()
            ))),
            pluginWithPlayer,
            extra
        );
        awards.broadcastIfChanged();
        assertTrue(permissions.contains("lastactiveplayers.streak.7"));
        assertFalse(permissions.contains("essentials.kits.streak-14"));
    }

}
