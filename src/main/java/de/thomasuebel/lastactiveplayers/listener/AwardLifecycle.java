package de.thomasuebel.lastactiveplayers.listener;

import de.thomasuebel.lastactiveplayers.player.Milestones;
import de.thomasuebel.lastactiveplayers.player.PlayerRecord;
import de.thomasuebel.lastactiveplayers.player.Players;
import de.thomasuebel.lastactiveplayers.ranking.Awards;
import de.thomasuebel.lastactiveplayers.ranking.Leaderboard;
import de.thomasuebel.lastactiveplayers.ranking.LeaderboardEntry;
import de.thomasuebel.lastactiveplayers.ranking.Nomination;
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
 * Bukkit event listener that manages MVP and Streak Leader permissions
 * and on-join and on-change broadcasts.
 *
 * <p>On every player join ({@link EventPriority#MONITOR} so streak and session writes
 * are already complete), this listener re-elects the current MVP and Streak Leader,
 * refreshes their permission attachments immediately, then broadcasts both results
 * after a configurable delay so the message appears after the initial join noise
 * has settled.
 *
 * <p>On quit, and after each heartbeat flush (via {@link #broadcastIfChanged}), the
 * election is repeated and the results are broadcast only when the set of leaders changed.
 *
 * <p>On quit, the departing player's permission attachment is removed.
 */
public final class AwardLifecycle implements Listener, Awards {

    private static final String MVP_PERMISSION = "lastactiveplayers.mvp";
    private static final String STREAK_PERMISSION_PREFIX = "lastactiveplayers.streak.";

    /**
     * Immutable snapshot of the current MVP and Streak Leader election result.
     * Stored in an {@link AtomicReference} so PlaceholderAPI reads from any thread
     * see a consistent pair of lists.
     */
    private record ElectionResult(List<Nomination> mvp, List<Nomination> streak) {
        ElectionResult(final List<Nomination> mvp, final List<Nomination> streak) {
            this.mvp = List.copyOf(mvp);
            this.streak = List.copyOf(streak);
        }
    }

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
    private final long delayTicks;
    private final Map<UUID, PermissionAttachment> attachments;
    private final AtomicReference<ElectionResult> previousResult;

    /**
     * Constructs the award lifecycle listener.
     *
     * @param mvpBoard          the 30-day playtime leaderboard used to elect the MVP; never null
     * @param players           the player store used to find the streak leader; never null
     * @param milestones        the streak milestone thresholds; never null
     * @param plugin            owning plugin, used to create permission attachments; never null
     * @param mvpPrefix         prefix for the current MVP(s), exposed via PAPI; never null
     * @param streakPrefix      prefix for streak leader(s), exposed via PAPI; never null
     * @param mvpTemplate       broadcast template for a sole MVP; use {player}; never null
     * @param mvpTieTemplate    broadcast template for tied MVPs; use {players}; never null
     * @param streakTemplate    broadcast template for a sole streak leader;
     *                          use {player} and {streak}; never null
     * @param streakTieTemplate broadcast template for tied streak leaders;
     *                          use {players} and {streak}; never null
     * @param delayTicks        server ticks to wait before broadcasting on join
     *                          (0 schedules for the next tick)
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
        final String streakTieTemplate,
        final long delayTicks
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
        this.delayTicks = delayTicks;
        this.attachments = new ConcurrentHashMap<>();
        // null initial state: first election always triggers a broadcast.
        this.previousResult = new AtomicReference<>(null);
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
        this.previousResult.set(new ElectionResult(mvp, streak));
        final Server server = this.plugin.getServer();
        refreshAttachments(server, mvp, streak);
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            broadcastMvp(server, mvp);
            broadcastStreak(server, streak);
        }, this.delayTicks);
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
        broadcastIfChanged();
    }

    @Override
    public String currentPrefix(final UUID uuid) {
        final ElectionResult result = this.previousResult.get();
        if (result == null) {
            return "";
        }
        if (uuidsOf(result.mvp()).contains(uuid)) {
            return this.mvpPrefix;
        }
        final List<Nomination> streakCandidates = result.streak();
        if (!streakCandidates.isEmpty() && uuidsOf(streakCandidates).contains(uuid)) {
            final int days = streakCandidates.get(0).streakDays();
            if (!this.milestones.crossedBy(0, days).isEmpty()) {
                return this.streakPrefix;
            }
        }
        return "";
    }

    @Override
    public String currentAward(final UUID uuid) {
        final ElectionResult result = this.previousResult.get();
        if (result == null) {
            return "";
        }
        if (uuidsOf(result.mvp()).contains(uuid)) {
            return "mvp";
        }
        final List<Nomination> streakCandidates = result.streak();
        if (!streakCandidates.isEmpty() && uuidsOf(streakCandidates).contains(uuid)) {
            final int days = streakCandidates.get(0).streakDays();
            if (!this.milestones.crossedBy(0, days).isEmpty()) {
                return "streak";
            }
        }
        return "";
    }

    /**
     * Removes all permission attachments held by this instance from every online player.
     * Must be called before this listener is unregistered (e.g. on {@code /lastactive reload})
     * so that attachment objects do not leak from players who remain online after the old
     * listener is replaced by a new one.
     */
    public void cleanup() {
        final Server server = this.plugin.getServer();
        for (final org.bukkit.entity.Player bukkit : server.getOnlinePlayers()) {
            final PermissionAttachment attachment = this.attachments.remove(bukkit.getUniqueId());
            if (attachment != null) {
                attachment.remove();
            }
        }
    }

    /**
     * Re-elects MVP and Streak Leader and broadcasts only when the set of leaders has
     * changed since the last election. Intended to be called after each heartbeat flush.
     */
    public void broadcastIfChanged() {
        final List<Nomination> currMvp = electMvp();
        final List<Nomination> currStreak = electStreak();
        final ElectionResult previous = this.previousResult.get();
        if (previous != null && sameLeaders(previous, currMvp, currStreak)) {
            return;
        }
        this.previousResult.set(new ElectionResult(currMvp, currStreak));
        final Server server = this.plugin.getServer();
        refreshAttachments(server, currMvp, currStreak);
        final List<Nomination> prevMvp = previous != null ? previous.mvp() : List.of();
        final List<Nomination> prevStreak = previous != null ? previous.streak() : List.of();
        if (!uuidsOf(prevMvp).equals(uuidsOf(currMvp))) {
            broadcastMvp(server, currMvp);
        }
        if (!uuidsOf(prevStreak).equals(uuidsOf(currStreak))) {
            broadcastStreak(server, currStreak);
        }
    }

    private boolean sameLeaders(
        final ElectionResult previous,
        final List<Nomination> currMvp,
        final List<Nomination> currStreak
    ) {
        return uuidsOf(previous.mvp()).equals(uuidsOf(currMvp))
            && uuidsOf(previous.streak()).equals(uuidsOf(currStreak));
    }

    private List<Nomination> electMvp() {
        final List<LeaderboardEntry> entries = this.mvpBoard.topTied(Set.of());
        final List<Nomination> result = new ArrayList<>();
        for (final LeaderboardEntry entry : entries) {
            result.add(new Nomination(entry.uuid(), entry.username(), 0));
        }
        return result;
    }

    private List<Nomination> electStreak() {
        final List<PlayerRecord> leaders = this.players.withTopStreak();
        final List<Nomination> result = new ArrayList<>();
        for (final PlayerRecord p : leaders) {
            result.add(new Nomination(p.uuid(), p.username(), p.streakDays()));
        }
        return result;
    }

    private void refreshAttachments(
        final Server server,
        final List<Nomination> mvpCandidates,
        final List<Nomination> streakCandidates
    ) {
        final Set<UUID> mvpUuids = uuidsOf(mvpCandidates);
        final Set<UUID> streakUuids = uuidsOf(streakCandidates);
        final int streakDays = streakCandidates.isEmpty()
            ? 0 : streakCandidates.get(0).streakDays();

        for (final org.bukkit.entity.Player bukkit : server.getOnlinePlayers()) {
            final PermissionAttachment old = this.attachments.remove(bukkit.getUniqueId());
            if (old != null) {
                old.remove();
            }
            final PermissionAttachment attachment = bukkit.addAttachment(this.plugin);
            this.attachments.put(bukkit.getUniqueId(), attachment);

            final UUID uuid = bukkit.getUniqueId();
            if (mvpUuids.contains(uuid)) {
                attachment.setPermission(MVP_PERMISSION, true);
            }
            if (streakUuids.contains(uuid) && streakDays > 0) {
                final List<Integer> crossed = this.milestones.crossedBy(0, streakDays);
                if (!crossed.isEmpty()) {
                    final int highest = crossed.get(crossed.size() - 1);
                    attachment.setPermission(STREAK_PERMISSION_PREFIX + highest, true);
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
