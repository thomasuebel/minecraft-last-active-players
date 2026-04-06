package de.thomasuebel.lastactiveplayers.listener;

import de.thomasuebel.lastactiveplayers.ranking.Leaderboard;
import de.thomasuebel.lastactiveplayers.ranking.LeaderboardEntry;
import de.thomasuebel.lastactiveplayers.ranking.TrackedRanks;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Bukkit event listener that feeds {@link OnlineRanks} with join and quit events and
 * exposes a {@link #pulse()} method for the heartbeat scheduler.
 *
 * <p>On each player join the listener seeds the player's baseline rank from the
 * current leaderboard. On quit it removes the player so no stale rank is retained.
 *
 * <p>The leaderboard is queried without any UUID exclusions. All players -- online and
 * offline -- appear based on their past closed sessions, giving a consistent snapshot
 * for rank-change detection.
 */
public final class HeartbeatRankHints implements Listener {

    /** Fetch the full leaderboard so no player is excluded from rank calculation. */
    private static final int MAX_LEADERBOARD_SIZE = Integer.MAX_VALUE;

    private final TrackedRanks onlineRanks;
    private final Leaderboard leaderboard;
    private final Plugin plugin;

    /**
     * Constructs a heartbeat rank-hints listener.
     *
     * @param onlineRanks the rank tracker to seed and pulse; never null
     * @param leaderboard the leaderboard used for rank snapshots; never null
     * @param plugin      the plugin instance used to resolve online players; never null
     */
    public HeartbeatRankHints(
        final TrackedRanks onlineRanks,
        final Leaderboard leaderboard,
        final Plugin plugin
    ) {
        this.onlineRanks = onlineRanks;
        this.leaderboard = leaderboard;
        this.plugin = plugin;
    }

    /**
     * Seeds the joining player's baseline rank.
     *
     * @param event the join event; never null
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final List<LeaderboardEntry> ranked = this.leaderboard.top(MAX_LEADERBOARD_SIZE, Set.of());
        this.onlineRanks.joined(uuid, ranked);
    }

    /**
     * Removes the quitting player from rank tracking.
     *
     * @param event the quit event; never null
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(final PlayerQuitEvent event) {
        this.onlineRanks.quit(event.getPlayer().getUniqueId());
    }

    /**
     * Called after each heartbeat flush. Checks every tracked player's rank against the
     * current leaderboard and sends a private notification to players whose rank improved.
     */
    public void pulse() {
        final List<LeaderboardEntry> ranked = this.leaderboard.top(MAX_LEADERBOARD_SIZE, Set.of());
        this.onlineRanks.pulse(ranked, (uuid, text) -> {
            final Player player = this.plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(text);
            }
        });
    }
}
