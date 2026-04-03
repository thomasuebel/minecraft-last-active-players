package de.thomasuebel.lastactiveplayers.listener;

import de.thomasuebel.lastactiveplayers.player.Milestones;
import de.thomasuebel.lastactiveplayers.player.Player;
import de.thomasuebel.lastactiveplayers.player.Players;
import de.thomasuebel.lastactiveplayers.player.Streak;
import de.thomasuebel.lastactiveplayers.player.TodayStreak;
import de.thomasuebel.lastactiveplayers.session.ActiveSessions;
import de.thomasuebel.lastactiveplayers.session.Sessions;
import de.thomasuebel.lastactiveplayers.session.TrackedSession;
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
 * <p>On join: upserts the player record, computes and persists the updated streak,
 * broadcasts any newly reached streak milestones, then opens a database session and
 * registers it in the in-memory {@link ActiveSessions} registry.
 *
 * <p>On quit: removes the session from {@link ActiveSessions}, computes the remaining
 * duration since the last heartbeat, persists a final heartbeat, and closes the session.
 */
public final class SessionLifecycle implements Listener {

    private final Players players;
    private final Sessions sessions;
    private final ActiveSessions activeSessions;
    private final Milestones milestones;
    private final ZoneId serverZone;
    private final String milestoneTemplate;

    /**
     * Constructs the lifecycle listener.
     *
     * @param players           the player persistence store; never null
     * @param sessions          the session persistence store; never null
     * @param activeSessions    the in-memory active session registry; never null
     * @param milestones        the streak milestone thresholds; never null
     * @param serverZone        the server timezone used to determine the current calendar
     *                          day for streak computation; never null
     * @param milestoneTemplate broadcast message template; use {player} and {streak} as
     *                          tokens; never null
     */
    public SessionLifecycle(
        final Players players,
        final Sessions sessions,
        final ActiveSessions activeSessions,
        final Milestones milestones,
        final ZoneId serverZone,
        final String milestoneTemplate
    ) {
        this.players = players;
        this.sessions = sessions;
        this.activeSessions = activeSessions;
        this.milestones = milestones;
        this.serverZone = serverZone;
        this.milestoneTemplate = milestoneTemplate;
    }

    /**
     * Handles a player joining the server.
     *
     * @param event the join event; never null
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onJoin(final PlayerJoinEvent event) {
        final Instant now = Instant.now();
        final UUID uuid = event.getPlayer().getUniqueId();
        final String name = event.getPlayer().getName();

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
            event.getPlayer().getServer().broadcastMessage(message);
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
