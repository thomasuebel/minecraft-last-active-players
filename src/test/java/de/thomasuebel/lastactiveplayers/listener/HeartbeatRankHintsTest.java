package de.thomasuebel.lastactiveplayers.listener;

import de.thomasuebel.lastactiveplayers.ranking.Leaderboard;
import de.thomasuebel.lastactiveplayers.ranking.LeaderboardEntry;
import de.thomasuebel.lastactiveplayers.ranking.OnlineRanks;
import org.bukkit.Server;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link HeartbeatRankHints} listener behaviour: seeding on join,
 * rank-up notifications on pulse, and removal on quit.
 */
class HeartbeatRankHintsTest {

    private static final String RANK_TEMPLATE =
        "Rank #{rank}. {minutes}m to #{next_rank}.";
    private static final UUID PLAYER_UUID = UUID.randomUUID();
    private static final String PLAYER_NAME = "RankPlayer";
    private static final UUID TOP_UUID = UUID.randomUUID();
    private static final String TOP_NAME = "TopPlayer";
    private static final UUID MID_UUID = UUID.randomUUID();
    private static final String MID_NAME = "MidPlayer";
    private static final long HIGH_SECONDS = 7200L;
    private static final long MID_SECONDS = 3600L;
    private static final long LOW_SECONDS = 1800L;

    // ---------------------------------------------------------------------
    // Stubs
    // ---------------------------------------------------------------------

    /**
     * Builds a stub {@link Leaderboard} that returns the given snapshot.
     *
     * @param snapshot entries returned by {@code top()}
     * @return a leaderboard stub
     */
    private static Leaderboard stubLeaderboard(
        final List<LeaderboardEntry> snapshot
    ) {
        return (limit, exclude) -> snapshot;
    }

    /**
     * Builds a proxy Bukkit {@link org.bukkit.entity.Player} for join/quit
     * events. Only supports {@code getUniqueId()} and {@code getName()}.
     *
     * @param uuid the player UUID
     * @param name the player name
     * @return a player proxy
     */
    private static org.bukkit.entity.Player stubEventPlayer(
        final UUID uuid, final String name
    ) {
        final InvocationHandler handler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "getUniqueId":
                    return uuid;
                case "getName":
                    return name;
                case "isOnline":
                    return true;
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
     * Builds a proxy Bukkit {@link org.bukkit.entity.Player} that captures
     * {@code sendMessage()} calls. Used as the player returned by
     * {@code Server.getPlayer(UUID)}.
     *
     * @param uuid     the player UUID
     * @param name     the player name
     * @param messages list that captures sent messages
     * @return a player proxy
     */
    private static org.bukkit.entity.Player stubMessagePlayer(
        final UUID uuid,
        final String name,
        final List<String> messages
    ) {
        final InvocationHandler handler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "getUniqueId":
                    return uuid;
                case "getName":
                    return name;
                case "isOnline":
                    return true;
                case "sendMessage":
                    if (args != null && args.length == 1
                        && args[0] instanceof String) {
                        messages.add((String) args[0]);
                    }
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
     * Builds a proxy {@link Server} whose {@code getPlayer(UUID)} returns
     * the given player proxy for matching UUIDs.
     *
     * @param tracked the player proxy returned for matching UUID
     * @return a server proxy
     */
    private static Server stubServer(
        final org.bukkit.entity.Player tracked
    ) {
        final InvocationHandler handler = (proxy, method, args) -> {
            if ("getPlayer".equals(method.getName())
                && args != null && args.length == 1
                && args[0] instanceof UUID) {
                final UUID requested = (UUID) args[0];
                if (tracked != null
                    && requested.equals(tracked.getUniqueId())) {
                    return tracked;
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

    // ---------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------

    @Test
    void seedsJoiningPlayerFromCachedSnapshot() {
        final List<String> messages = new ArrayList<>();
        final org.bukkit.entity.Player msgPlayer = stubMessagePlayer(
            PLAYER_UUID, PLAYER_NAME, messages
        );
        final List<LeaderboardEntry> snapshot = List.of(
            new LeaderboardEntry(
                TOP_UUID, TOP_NAME,
                HIGH_SECONDS, Optional.empty()
            ),
            new LeaderboardEntry(
                PLAYER_UUID, PLAYER_NAME,
                LOW_SECONDS, Optional.empty()
            )
        );
        final Leaderboard leaderboard = stubLeaderboard(snapshot);
        final OnlineRanks onlineRanks = new OnlineRanks(RANK_TEMPLATE);
        final Plugin plugin = stubPlugin(stubServer(msgPlayer));
        final HeartbeatRankHints hints =
            new HeartbeatRankHints(onlineRanks, leaderboard, plugin);

        final org.bukkit.entity.Player eventPlayer = stubEventPlayer(
            PLAYER_UUID, PLAYER_NAME
        );
        hints.onJoin(new PlayerJoinEvent(eventPlayer, "joined"));
        hints.pulse();
        assertTrue(messages.isEmpty());
    }

    @Test
    void sendsRankUpMessageAfterPulse() {
        final List<String> messages = new ArrayList<>();
        final org.bukkit.entity.Player msgPlayer = stubMessagePlayer(
            PLAYER_UUID, PLAYER_NAME, messages
        );
        // Initial snapshot: player at rank 3.
        final List<LeaderboardEntry> initialSnapshot = List.of(
            new LeaderboardEntry(
                TOP_UUID, TOP_NAME,
                HIGH_SECONDS, Optional.empty()
            ),
            new LeaderboardEntry(
                MID_UUID, MID_NAME,
                MID_SECONDS, Optional.empty()
            ),
            new LeaderboardEntry(
                PLAYER_UUID, PLAYER_NAME,
                LOW_SECONDS, Optional.empty()
            )
        );
        // Updated snapshot: player at rank 2.
        final List<LeaderboardEntry> updatedSnapshot = List.of(
            new LeaderboardEntry(
                TOP_UUID, TOP_NAME,
                HIGH_SECONDS, Optional.empty()
            ),
            new LeaderboardEntry(
                PLAYER_UUID, PLAYER_NAME,
                MID_SECONDS, Optional.empty()
            ),
            new LeaderboardEntry(
                MID_UUID, MID_NAME,
                LOW_SECONDS, Optional.empty()
            )
        );
        final OnlineRanks onlineRanks = new OnlineRanks(RANK_TEMPLATE);
        // First pulse seeds with initial snapshot.
        final HeartbeatRankHints hints = new HeartbeatRankHints(
            onlineRanks, stubLeaderboard(initialSnapshot),
            stubPlugin(stubServer(msgPlayer))
        );
        final org.bukkit.entity.Player eventPlayer = stubEventPlayer(
            PLAYER_UUID, PLAYER_NAME
        );
        hints.onJoin(new PlayerJoinEvent(eventPlayer, "joined"));
        hints.pulse();
        messages.clear();
        // Second pulse with updated snapshot triggers rank-up.
        final HeartbeatRankHints updated = new HeartbeatRankHints(
            onlineRanks, stubLeaderboard(updatedSnapshot),
            stubPlugin(stubServer(msgPlayer))
        );
        updated.pulse();
        assertEquals(1, messages.size());
        assertEquals("Rank #2. 60m to #1.", messages.get(0));
    }

    @Test
    void noMessageWhenRankUnchanged() {
        final List<String> messages = new ArrayList<>();
        final org.bukkit.entity.Player msgPlayer = stubMessagePlayer(
            PLAYER_UUID, PLAYER_NAME, messages
        );
        final List<LeaderboardEntry> snapshot = List.of(
            new LeaderboardEntry(
                TOP_UUID, TOP_NAME,
                HIGH_SECONDS, Optional.empty()
            ),
            new LeaderboardEntry(
                PLAYER_UUID, PLAYER_NAME,
                LOW_SECONDS, Optional.empty()
            )
        );
        final Leaderboard leaderboard = stubLeaderboard(snapshot);
        final OnlineRanks onlineRanks = new OnlineRanks(RANK_TEMPLATE);
        final Plugin plugin = stubPlugin(stubServer(msgPlayer));
        final HeartbeatRankHints hints =
            new HeartbeatRankHints(onlineRanks, leaderboard, plugin);

        final org.bukkit.entity.Player eventPlayer = stubEventPlayer(
            PLAYER_UUID, PLAYER_NAME
        );
        hints.onJoin(new PlayerJoinEvent(eventPlayer, "joined"));
        hints.pulse();
        messages.clear();
        hints.pulse();
        assertTrue(messages.isEmpty());
    }

    @Test
    void removesPlayerOnQuit() {
        final List<String> messages = new ArrayList<>();
        final org.bukkit.entity.Player msgPlayer = stubMessagePlayer(
            PLAYER_UUID, PLAYER_NAME, messages
        );
        final List<LeaderboardEntry> snapshot = List.of(
            new LeaderboardEntry(
                PLAYER_UUID, PLAYER_NAME,
                HIGH_SECONDS, Optional.empty()
            )
        );
        final Leaderboard leaderboard = stubLeaderboard(snapshot);
        final OnlineRanks onlineRanks = new OnlineRanks(RANK_TEMPLATE);
        final Plugin plugin = stubPlugin(stubServer(msgPlayer));
        final HeartbeatRankHints hints =
            new HeartbeatRankHints(onlineRanks, leaderboard, plugin);

        final org.bukkit.entity.Player eventPlayer = stubEventPlayer(
            PLAYER_UUID, PLAYER_NAME
        );
        hints.onJoin(new PlayerJoinEvent(eventPlayer, "joined"));
        hints.onQuit(new PlayerQuitEvent(eventPlayer, "left"));
        hints.pulse();
        assertTrue(messages.isEmpty());
    }

    @Test
    void noMessageForRankOne() {
        final List<String> messages = new ArrayList<>();
        final org.bukkit.entity.Player msgPlayer = stubMessagePlayer(
            PLAYER_UUID, PLAYER_NAME, messages
        );
        // Initial: player at rank 2.
        final List<LeaderboardEntry> initialSnapshot = List.of(
            new LeaderboardEntry(
                TOP_UUID, TOP_NAME,
                HIGH_SECONDS, Optional.empty()
            ),
            new LeaderboardEntry(
                PLAYER_UUID, PLAYER_NAME,
                LOW_SECONDS, Optional.empty()
            )
        );
        // Updated: player at rank 1.
        final List<LeaderboardEntry> updatedSnapshot = List.of(
            new LeaderboardEntry(
                PLAYER_UUID, PLAYER_NAME,
                HIGH_SECONDS, Optional.empty()
            ),
            new LeaderboardEntry(
                TOP_UUID, TOP_NAME,
                LOW_SECONDS, Optional.empty()
            )
        );
        final OnlineRanks onlineRanks = new OnlineRanks(RANK_TEMPLATE);
        final HeartbeatRankHints hints = new HeartbeatRankHints(
            onlineRanks, stubLeaderboard(initialSnapshot),
            stubPlugin(stubServer(msgPlayer))
        );
        final org.bukkit.entity.Player eventPlayer = stubEventPlayer(
            PLAYER_UUID, PLAYER_NAME
        );
        hints.onJoin(new PlayerJoinEvent(eventPlayer, "joined"));
        hints.pulse();
        messages.clear();
        // Pulse with updated data where player is now rank 1.
        final HeartbeatRankHints updated = new HeartbeatRankHints(
            onlineRanks, stubLeaderboard(updatedSnapshot),
            stubPlugin(stubServer(msgPlayer))
        );
        updated.pulse();
        assertTrue(messages.isEmpty());
    }
}
