package de.thomasuebel.lastactiveplayers.listener;

import de.thomasuebel.lastactiveplayers.player.Milestones;
import de.thomasuebel.lastactiveplayers.player.Player;
import de.thomasuebel.lastactiveplayers.player.Players;
import de.thomasuebel.lastactiveplayers.player.Streak;
import de.thomasuebel.lastactiveplayers.player.TodayStreak;
import de.thomasuebel.lastactiveplayers.session.ActiveSessions;
import de.thomasuebel.lastactiveplayers.session.Sessions;
import de.thomasuebel.lastactiveplayers.session.TrackedSession;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Bukkit event listener that opens and closes database sessions on player join and quit,
 * and updates the player's consecutive daily login streak on join.
 *
 * <p>On join: upserts the player record, computes and persists the updated streak, then
 * schedules any newly reached streak milestone broadcasts and the personal title at
 * {@code delayTicks} in the future (first in the join stagger sequence). After that it
 * opens a database session and registers it in the in-memory {@link ActiveSessions}
 * registry.
 *
 * <p>On quit: removes the session from {@link ActiveSessions}, computes the remaining
 * duration since the last heartbeat, persists a final heartbeat, and closes the session.
 */
public final class SessionLifecycle implements Listener {

    private static final int TITLE_FADE_IN_TICKS = 10;
    private static final int TITLE_STAY_TICKS = 70;
    private static final int TITLE_FADE_OUT_TICKS = 20;

    private final Players players;
    private final Sessions sessions;
    private final ActiveSessions activeSessions;
    private final Milestones milestones;
    private final ZoneId serverZone;
    private final String milestoneTemplate;
    private final Plugin plugin;
    private final long delayTicks;
    private final String milestoneTitleTemplate;
    private final String milestoneSubtitleTemplate;

    /**
     * Constructs the lifecycle listener.
     *
     * @param players                   the player persistence store; never null
     * @param sessions                  the session persistence store; never null
     * @param activeSessions            the in-memory active session registry; never null
     * @param milestones                the streak milestone thresholds; never null
     * @param serverZone                the server timezone used to determine the current
     *                                  calendar day for streak computation; never null
     * @param milestoneTemplate         broadcast message template sent to all players;
     *                                  tokens {player} and {streak}; never null
     * @param plugin                    the owning plugin, used for scheduling; never null
     * @param delayTicks                ticks to wait before delivering the milestone
     *                                  broadcast and personal title; non-negative
     * @param milestoneTitleTemplate    full-screen title template sent to the achieving
     *                                  player; tokens {player} and {streak}; empty string
     *                                  suppresses the title line; never null
     * @param milestoneSubtitleTemplate subtitle shown below the title; tokens {player}
     *                                  and {streak}; empty string suppresses it; never null
     */
    public SessionLifecycle(
        final Players players,
        final Sessions sessions,
        final ActiveSessions activeSessions,
        final Milestones milestones,
        final ZoneId serverZone,
        final String milestoneTemplate,
        final Plugin plugin,
        final long delayTicks,
        final String milestoneTitleTemplate,
        final String milestoneSubtitleTemplate
    ) {
        this.players = players;
        this.sessions = sessions;
        this.activeSessions = activeSessions;
        this.milestones = milestones;
        this.serverZone = serverZone;
        this.milestoneTemplate = milestoneTemplate;
        this.plugin = plugin;
        this.delayTicks = delayTicks;
        this.milestoneTitleTemplate = milestoneTitleTemplate;
        this.milestoneSubtitleTemplate = milestoneSubtitleTemplate;
    }

    /**
     * Handles a player joining the server.
     *
     * @param event the join event; never null
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onJoin(final PlayerJoinEvent event) {
        final Instant now = Instant.now();
        final org.bukkit.entity.Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        final String name = player.getName();

        this.players.upsert(uuid, name);

        final Player stored = this.players.withUuid(uuid);
        final LocalDate today = LocalDate.now(this.serverZone);
        final Streak streak = new TodayStreak(stored, today);
        final List<Integer> newMilestones =
            this.milestones.crossedBy(stored.streakDays(), streak.days());
        this.players.updateStreak(uuid, streak.days(), Optional.of(streak.lastDay()));

        for (final int milestone : newMilestones) {
            final String message = this.milestoneTemplate
                .replace("{player}", name)
                .replace("{streak}", String.valueOf(milestone));
            final String title = this.milestoneTitleTemplate
                .replace("{player}", name)
                .replace("{streak}", String.valueOf(milestone));
            final String subtitle = this.milestoneSubtitleTemplate
                .replace("{player}", name)
                .replace("{streak}", String.valueOf(milestone));
            this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                player.getServer().broadcastMessage(message);
                if (player.isOnline() && (!title.isEmpty() || !subtitle.isEmpty())) {
                    player.sendTitle(
                        title, subtitle,
                        TITLE_FADE_IN_TICKS, TITLE_STAY_TICKS, TITLE_FADE_OUT_TICKS
                    );
                }
            }, this.delayTicks);
        }

        final long sessionId = this.sessions.open(uuid, now);
        this.activeSessions.start(uuid, sessionId, now);
    }

    /**
     * Handles a player quitting the server.
     *
     * @param event the quit event; never null
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onQuit(final PlayerQuitEvent event) {
        final Instant now = Instant.now();
        final UUID uuid = event.getPlayer().getUniqueId();
        final Optional<TrackedSession> tracked = this.activeSessions.stop(uuid);
        if (tracked.isEmpty()) {
            return;
        }
        final TrackedSession session = tracked.get();
        final long elapsed =
            Math.max(0L, Duration.between(session.lastHeartbeat(), now).getSeconds());
        this.sessions.heartbeat(session.sessionId(), now, elapsed);
        this.sessions.close(session.sessionId(), now);
    }
}
