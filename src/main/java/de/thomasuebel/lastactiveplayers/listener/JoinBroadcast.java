package de.thomasuebel.lastactiveplayers.listener;

import de.thomasuebel.lastactiveplayers.display.JoinMessage;
import de.thomasuebel.lastactiveplayers.display.RankHint;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Bukkit event listener that sends the last-active player list and rank hint to the
 * joining player.
 *
 * <p>The join list excludes all currently online players so only offline activity is
 * shown. The rank hint excludes all online players except the joiner so that the joiner's
 * own historical play time is visible in the ranking.
 *
 * <p>The message is delivered after a configurable delay (in server ticks) so that it
 * appears after the initial join noise has settled. If the player disconnects before the
 * delay expires, no message is sent.
 */
public final class JoinBroadcast implements Listener {

    private final JoinMessage joinMessage;
    private final RankHint rankHint;
    private final Plugin plugin;
    private final long delayTicks;

    /**
     * Constructs a join broadcast listener.
     *
     * @param joinMessage the last-active player list to send to the joining player; never null
     * @param rankHint    the rank hint to send to the joining player; never null
     * @param plugin      the plugin used to schedule the delayed send; never null
     * @param delayTicks  server ticks to wait before sending (0 schedules for the next tick)
     */
    public JoinBroadcast(
        final JoinMessage joinMessage,
        final RankHint rankHint,
        final Plugin plugin,
        final long delayTicks
    ) {
        this.joinMessage = joinMessage;
        this.rankHint = rankHint;
        this.plugin = plugin;
        this.delayTicks = delayTicks;
    }

    /**
     * Handles a player joining the server.
     *
     * @param event the join event; never null
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final Set<UUID> online = new HashSet<>();
        for (final Player p : player.getServer().getOnlinePlayers()) {
            online.add(p.getUniqueId());
        }
        final List<String> lines = this.joinMessage.lines(online);
        final Optional<String> hint = this.rankHint.text(player.getUniqueId(), online);
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            for (final String line : lines) {
                player.sendMessage(line);
            }
            hint.ifPresent(player::sendMessage);
        }, this.delayTicks);
    }
}
