package de.thomasuebel.lastactiveplayers.listener;

import de.thomasuebel.lastactiveplayers.ranking.Leaderboard;
import de.thomasuebel.lastactiveplayers.ranking.LeaderboardEntry;
import de.thomasuebel.lastactiveplayers.ranking.OnlineRanks;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Bukkit event listener that feeds {@link OnlineRanks} with join and quit events and
 * exposes a {@link #pulse()} method for the heartbeat scheduler.
 *
 * <p>To avoid a synchronous database query on every join event, the last leaderboard
 * snapshot fetched during {@link #pulse()} is reused to seed joining players. Before
 * the first heartbeat fires the snapshot is empty, so joining players are seeded as
 * {@code UNRANKED} -- no spurious notification fires on the first heartbeat.
 *
 * <p>All methods must be called from the Bukkit main thread.
 */
public final class HeartbeatRankHints implements Listener {

    /** Fetch the full leaderboard so no player is excluded from rank calculation. */
    private static final int MAX_LEADERBOARD_SIZE = Integer.MAX_VALUE;

    private final OnlineRanks onlineRanks;
    private final Leaderboard leaderboard;
    private final Plugin plugin;
    /** Last snapshot used by pulse(); reused to seed joining players without a DB query. */
    private List<LeaderboardEntry> lastSnapshot = new ArrayList<>();

    /**
     * Constructs a heartbeat rank-hints listener.
     *
     * @param onlineRanks the rank tracker to seed and pulse; never null
     * @param leaderboard the leaderboard used for rank snapshots; never null
     * @param plugin      the plugin instance used to resolve online players; never null
     */
    public HeartbeatRankHints(
        final OnlineRanks onlineRanks,
        final Leaderboard leaderboard,
        final Plugin plugin
    ) {
        this.onlineRanks = onlineRanks;
        this.leaderboard = leaderboard;
        this.plugin = plugin;
    }

    /**
     * Seeds the joining player's baseline rank from the cached snapshot.
     * No database query is issued; the snapshot is refreshed by each heartbeat.
     *
     * @param event the join event; never null
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent event) {
        this.onlineRanks.joined(event.getPlayer().getUniqueId(), this.lastSnapshot);
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
     * Called after each heartbeat flush. Fetches the current leaderboard, checks every
     * tracked player's rank against it, and sends a private notification to players
     * whose rank improved. Caches the snapshot for use by {@link #onJoin}.
     */
    public void pulse() {
        this.lastSnapshot = this.leaderboard.top(MAX_LEADERBOARD_SIZE, Set.of());
        this.onlineRanks.pulse(this.lastSnapshot, (uuid, text) -> {
            final Player player = this.plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(text);
            }
        });
    }
}
