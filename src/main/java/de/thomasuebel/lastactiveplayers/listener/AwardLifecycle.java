package de.thomasuebel.lastactiveplayers.listener;

import de.thomasuebel.lastactiveplayers.player.Milestones;
import de.thomasuebel.lastactiveplayers.player.Player;
import de.thomasuebel.lastactiveplayers.player.Players;
import de.thomasuebel.lastactiveplayers.ranking.AwardSnapshot;
import de.thomasuebel.lastactiveplayers.ranking.FrozenAwards;
import de.thomasuebel.lastactiveplayers.ranking.Leaderboard;
import de.thomasuebel.lastactiveplayers.ranking.LeaderboardEntry;
import de.thomasuebel.lastactiveplayers.ranking.NoAwards;
import de.thomasuebel.lastactiveplayers.ranking.Nomination;
import de.thomasuebel.lastactiveplayers.ranking.StoredNomination;
import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Bukkit event listener that manages MVP and Streak Leader permissions, display name
 * prefixes, and on-join and on-change broadcasts.
 *
 * <p>On every player join ({@link EventPriority#MONITOR} so streak and session writes
 * are already complete), this listener re-elects the current MVP and Streak Leader,
 * refreshes their permission attachments, updates display name prefixes, and broadcasts
 * both results unconditionally.
 *
 * <p>On quit, and after each heartbeat flush (via {@link #broadcastIfChanged}), the
 * election is repeated and the results are broadcast only when the set of leaders changed.
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
    private final String mvpTieTemplate;
    private final String streakTemplate;
    private final String streakTieTemplate;
    private final Map<UUID, PermissionAttachment> attachments;
    private final AtomicReference<AwardSnapshot> previousSnapshot;

    /**
     * Constructs the award lifecycle listener.
     *
     * @param mvpBoard          the 30-day playtime leaderboard used to elect the MVP; never null
     * @param players           the player store used to find the streak leader; never null
     * @param milestones        the streak milestone thresholds; never null
     * @param plugin            owning plugin, used to create permission attachments; never null
     * @param mvpPrefix         display name prefix applied to the current MVP(s); never null
     * @param streakPrefix      display name prefix applied to streak leader(s); never null
     * @param mvpTemplate       broadcast template for a sole MVP; use {player}; never null
     * @param mvpTieTemplate    broadcast template for tied MVPs; use {players}; never null
     * @param streakTemplate    broadcast template for a sole streak leader;
     *                          use {player} and {streak}; never null
     * @param streakTieTemplate broadcast template for tied streak leaders;
     *                          use {players} and {streak}; never null
     */
    public AwardLifecycle(
        final Leaderboard mvpBoard,
        final Players players,
        final Milestones milestones,
        final Plugin plugin,
        final String mvpPrefix,
        final String streakPrefix,
        final String mvpTemplate,
        final String mvpTieTemplate,
        final String streakTemplate,
        final String streakTieTemplate
    ) {
        this.mvpBoard = mvpBoard;
        this.players = players;
        this.milestones = milestones;
        this.plugin = plugin;
        this.mvpPrefix = mvpPrefix;
        this.streakPrefix = streakPrefix;
        this.mvpTemplate = mvpTemplate;
        this.mvpTieTemplate = mvpTieTemplate;
        this.streakTemplate = streakTemplate;
        this.streakTieTemplate = streakTieTemplate;
        this.attachments = new ConcurrentHashMap<>();
        this.previousSnapshot = new AtomicReference<>(new NoAwards());
    }

    /**
     * Handles a player joining the server.
     *
     * @param event the join event; never null
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(final PlayerJoinEvent event) {
        final List<Nomination> mvp = electMvp();
        final List<Nomination> streak = electStreak();
        final AwardSnapshot current = new FrozenAwards(mvp, streak);
        this.previousSnapshot.set(current);
        final Server server = this.plugin.getServer();
        refreshAttachments(server, current);
        broadcastMvp(server, mvp);
        broadcastStreak(server, streak);
    }

    /**
     * Handles a player quitting the server.
     *
     * @param event the quit event; never null
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onQuit(final PlayerQuitEvent event) {
        final org.bukkit.entity.Player player = event.getPlayer();
        final PermissionAttachment existing = this.attachments.remove(player.getUniqueId());
        if (existing != null) {
            existing.remove();
        }
        player.setDisplayName(player.getName());
        broadcastIfChanged();
    }

    /**
     * Re-elects MVP and Streak Leader and broadcasts only when the set of leaders has
     * changed since the last election. Intended to be called after each heartbeat flush.
     */
    public void broadcastIfChanged() {
        final List<Nomination> currMvp = electMvp();
        final List<Nomination> currStreak = electStreak();
        final AwardSnapshot current = new FrozenAwards(currMvp, currStreak);
        final AwardSnapshot previous = this.previousSnapshot.get();
        if (previous.sameLeaders(current)) {
            return;
        }
        this.previousSnapshot.set(current);
        final Server server = this.plugin.getServer();
        refreshAttachments(server, current);
        if (!uuidsOf(previous.mvpCandidates()).equals(uuidsOf(currMvp))) {
            broadcastMvp(server, currMvp);
        }
        if (!uuidsOf(previous.streakCandidates()).equals(uuidsOf(currStreak))) {
            broadcastStreak(server, currStreak);
        }
    }

    private List<Nomination> electMvp() {
        final List<LeaderboardEntry> entries = this.mvpBoard.topTied(Set.of());
        final List<Nomination> result = new ArrayList<>();
        for (final LeaderboardEntry entry : entries) {
            result.add(new StoredNomination(entry.uuid(), entry.username(), 0));
        }
        return result;
    }

    private List<Nomination> electStreak() {
        final List<Player> leaders = this.players.withTopStreak();
        final List<Nomination> result = new ArrayList<>();
        for (final Player p : leaders) {
            result.add(new StoredNomination(p.uuid(), p.username(), p.streakDays()));
        }
        return result;
    }

    private void refreshAttachments(final Server server, final AwardSnapshot snapshot) {
        final List<Nomination> mvpCandidates = snapshot.mvpCandidates();
        final List<Nomination> streakCandidates = snapshot.streakCandidates();
        final Set<UUID> mvpUuids = uuidsOf(mvpCandidates);
        final Set<UUID> streakUuids = uuidsOf(streakCandidates);
        final int streakDays = streakCandidates.isEmpty()
            ? 0 : streakCandidates.get(0).streakDays();

        for (final org.bukkit.entity.Player bukkit : server.getOnlinePlayers()) {
            final PermissionAttachment old = this.attachments.remove(bukkit.getUniqueId());
            if (old != null) {
                old.remove();
            }
            bukkit.setDisplayName(bukkit.getName());

            final PermissionAttachment attachment = bukkit.addAttachment(this.plugin);
            this.attachments.put(bukkit.getUniqueId(), attachment);

            final UUID uuid = bukkit.getUniqueId();
            if (mvpUuids.contains(uuid)) {
                attachment.setPermission(MVP_PERMISSION, true);
                bukkit.setDisplayName(this.mvpPrefix + bukkit.getName());
            }
            if (streakUuids.contains(uuid) && streakDays > 0) {
                final List<Integer> crossed = this.milestones.crossedBy(0, streakDays);
                if (!crossed.isEmpty()) {
                    final int highest = crossed.get(crossed.size() - 1);
                    attachment.setPermission(STREAK_PERMISSION_PREFIX + highest, true);
                    bukkit.setDisplayName(this.streakPrefix + bukkit.getName());
                }
            }
        }
    }

    private void broadcastMvp(final Server server, final List<Nomination> candidates) {
        if (candidates.isEmpty()) {
            return;
        }
        if (candidates.size() == 1) {
            server.broadcastMessage(
                this.mvpTemplate.replace("{player}", candidates.get(0).username())
            );
        } else {
            server.broadcastMessage(
                this.mvpTieTemplate.replace("{players}", joinNames(candidates))
            );
        }
    }

    private void broadcastStreak(final Server server, final List<Nomination> candidates) {
        if (candidates.isEmpty()) {
            return;
        }
        final String days = String.valueOf(candidates.get(0).streakDays());
        if (candidates.size() == 1) {
            server.broadcastMessage(
                this.streakTemplate
                    .replace("{player}", candidates.get(0).username())
                    .replace("{streak}", days)
            );
        } else {
            server.broadcastMessage(
                this.streakTieTemplate
                    .replace("{players}", joinNames(candidates))
                    .replace("{streak}", days)
            );
        }
    }

    private String joinNames(final List<Nomination> nominations) {
        final List<String> names = new ArrayList<>();
        for (final Nomination nomination : nominations) {
            names.add(nomination.username());
        }
        return String.join(", ", names);
    }

    private Set<UUID> uuidsOf(final List<Nomination> nominations) {
        final Set<UUID> result = new HashSet<>();
        for (final Nomination nomination : nominations) {
            result.add(nomination.uuid());
        }
        return result;
    }
}
