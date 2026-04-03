package de.thomasuebel.lastactiveplayers.listener;

import de.thomasuebel.lastactiveplayers.player.Milestones;
import de.thomasuebel.lastactiveplayers.player.Player;
import de.thomasuebel.lastactiveplayers.player.Players;
import de.thomasuebel.lastactiveplayers.ranking.Leaderboard;
import de.thomasuebel.lastactiveplayers.ranking.LeaderboardEntry;
import de.thomasuebel.lastactiveplayers.ranking.Nomination;
import de.thomasuebel.lastactiveplayers.ranking.NoNomination;
import de.thomasuebel.lastactiveplayers.ranking.StoredNomination;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Bukkit event listener that manages MVP and Streak Leader permissions, display name
 * prefixes, and on-join broadcasts.
 *
 * <p>On every player join (at {@link EventPriority#MONITOR} so streak and session
 * writes are already complete), this listener re-elects the current MVP and Streak
 * Leader, refreshes their permission attachments, updates display name prefixes, and
 * broadcasts the results to all online players.
 *
 * <p>On quit, the departing player's permission attachment is removed.
 */
public final class AwardLifecycle implements Listener {

    private static final String MVP_PERMISSION = "lastactiveplayers.mvp";
    private static final String STREAK_PERMISSION_PREFIX = "lastactiveplayers.streak.";

    private final Leaderboard mvpBoard;
    private final Players players;
    private final Milestones milestones;
    private final Plugin plugin;
    private final String mvpPrefix;
    private final String streakPrefix;
    private final String mvpTemplate;
    private final String streakTemplate;
    private final Map<UUID, PermissionAttachment> attachments;

    /**
     * Constructs the award lifecycle listener.
     *
     * @param mvpBoard       the 30-day playtime leaderboard used to elect the MVP; never null
     * @param players        the player store used to find the streak leader; never null
     * @param milestones     the streak milestone thresholds; never null
     * @param plugin         the owning plugin, used to create permission attachments; never null
     * @param mvpPrefix      display name prefix applied to the current MVP; never null
     * @param streakPrefix   display name prefix applied to the current streak leader; never null
     * @param mvpTemplate    broadcast message template for MVP; use {player} as token; never null
     * @param streakTemplate broadcast message template for streak leader; use {player} and
     *                       {streak} as tokens; never null
     */
    public AwardLifecycle(
        final Leaderboard mvpBoard,
        final Players players,
        final Milestones milestones,
        final Plugin plugin,
        final String mvpPrefix,
        final String streakPrefix,
        final String mvpTemplate,
        final String streakTemplate
    ) {
        this.mvpBoard = mvpBoard;
        this.players = players;
        this.milestones = milestones;
        this.plugin = plugin;
        this.mvpPrefix = mvpPrefix;
        this.streakPrefix = streakPrefix;
        this.mvpTemplate = mvpTemplate;
        this.streakTemplate = streakTemplate;
        this.attachments = new HashMap<>();
    }

    /**
     * Handles a player joining the server.
     *
     * @param event the join event; never null
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(final PlayerJoinEvent event) {
        final org.bukkit.Server server = event.getPlayer().getServer();
        final Nomination mvp = electMvp();
        final Nomination streakLeader = electStreakLeader();
        refreshAttachments(server, mvp, streakLeader);
        broadcast(server, mvp, streakLeader);
    }

    /**
     * Handles a player quitting the server.
     *
     * @param event the quit event; never null
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onQuit(final PlayerQuitEvent event) {
        removeAttachment(event.getPlayer().getUniqueId());
        resetDisplayName(event.getPlayer());
    }

    private Nomination electMvp() {
        final List<LeaderboardEntry> top = this.mvpBoard.top(1, Set.of());
        if (top.isEmpty()) {
            return new NoNomination();
        }
        final LeaderboardEntry entry = top.get(0);
        return new StoredNomination(entry.uuid(), entry.username());
    }

    private Nomination electStreakLeader() {
        final Player leader = this.players.withHighestStreak();
        if (!leader.exists()) {
            return new NoNomination();
        }
        return new StoredNomination(leader.uuid(), leader.username());
    }

    private void refreshAttachments(
        final org.bukkit.Server server,
        final Nomination mvp,
        final Nomination streakLeader
    ) {
        server.getOnlinePlayers().forEach(bukkit -> {
            removeAttachment(bukkit.getUniqueId());
            resetDisplayName(bukkit);

            final PermissionAttachment attachment = bukkit.addAttachment(this.plugin);
            this.attachments.put(bukkit.getUniqueId(), attachment);

            final UUID uuid = bukkit.getUniqueId();
            if (mvp.exists() && uuid.equals(mvp.uuid())) {
                attachment.setPermission(MVP_PERMISSION, true);
                bukkit.setDisplayName(this.mvpPrefix + bukkit.getName());
            }
            if (streakLeader.exists() && uuid.equals(streakLeader.uuid())) {
                final Player stored = this.players.withUuid(uuid);
                final List<Integer> crossed =
                    this.milestones.crossedBy(0, stored.streakDays());
                if (!crossed.isEmpty()) {
                    final int highest = crossed.get(crossed.size() - 1);
                    attachment.setPermission(STREAK_PERMISSION_PREFIX + highest, true);
                }
                bukkit.setDisplayName(this.streakPrefix + bukkit.getName());
            }
        });
    }

    private void broadcast(
        final org.bukkit.Server server,
        final Nomination mvp,
        final Nomination streakLeader
    ) {
        if (mvp.exists()) {
            server.broadcastMessage(
                this.mvpTemplate.replace("{player}", mvp.username())
            );
        }
        if (streakLeader.exists()) {
            final Player leader = this.players.withUuid(streakLeader.uuid());
            server.broadcastMessage(
                this.streakTemplate
                    .replace("{player}", streakLeader.username())
                    .replace("{streak}", String.valueOf(leader.streakDays()))
            );
        }
    }

    private void removeAttachment(final UUID uuid) {
        final PermissionAttachment existing = this.attachments.remove(uuid);
        if (existing != null) {
            existing.remove();
        }
    }

    private void resetDisplayName(final org.bukkit.entity.Player player) {
        player.setDisplayName(player.getName());
    }
}
