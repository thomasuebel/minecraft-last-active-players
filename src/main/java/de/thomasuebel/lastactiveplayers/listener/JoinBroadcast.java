package de.thomasuebel.lastactiveplayers.listener;

import de.thomasuebel.lastactiveplayers.display.JoinMessage;
import de.thomasuebel.lastactiveplayers.display.RankHint;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Bukkit event listener that sends the last-active player list and rank hint to the
 * joining player.
 *
 * <p>The join list excludes all currently online players so only offline activity is
 * shown. The rank hint excludes all online players except the joiner so that the joiner's
 * own historical play time is visible in the ranking.
 */
public final class JoinBroadcast implements Listener {

    private final JoinMessage joinMessage;
    private final RankHint rankHint;

    /**
     * Constructs a join broadcast listener.
     *
     * @param joinMessage the last-active player list to send to the joining player; never null
     * @param rankHint    the rank hint to send to the joining player; never null
     */
    public JoinBroadcast(final JoinMessage joinMessage, final RankHint rankHint) {
        this.joinMessage = joinMessage;
        this.rankHint = rankHint;
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
        for (final String line : this.joinMessage.lines(online)) {
            player.sendMessage(line);
        }
        this.rankHint.text(player.getUniqueId(), online).ifPresent(player::sendMessage);
    }
}
