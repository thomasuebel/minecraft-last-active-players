package de.thomasuebel.lastactiveplayers.listener;

import de.thomasuebel.lastactiveplayers.player.Players;
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
import java.util.Optional;

/**
 * Bukkit event listener that opens and closes database sessions on player join and quit.
 *
 * <p>On join: upserts the player record, opens a database session, and registers the
 * session in the in-memory {@link ActiveSessions} registry.
 *
 * <p>On quit: removes the session from {@link ActiveSessions}, computes the remaining
 * duration since the last heartbeat, persists a final heartbeat, and closes the session.
 */
public final class SessionLifecycle implements Listener {

    private final Players players;
    private final Sessions sessions;
    private final ActiveSessions activeSessions;

    /**
     * Constructs the lifecycle listener.
     *
     * @param players        the player persistence store; never null
     * @param sessions       the session persistence store; never null
     * @param activeSessions the in-memory active session registry; never null
     */
    public SessionLifecycle(
        final Players players,
        final Sessions sessions,
        final ActiveSessions activeSessions
    ) {
        this.players = players;
        this.sessions = sessions;
        this.activeSessions = activeSessions;
    }

    /**
     * Handles a player joining the server.
     *
     * @param event the join event; never null
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(final PlayerJoinEvent event) {
        final Instant now = Instant.now();
        final java.util.UUID uuid = event.getPlayer().getUniqueId();
        final String name = event.getPlayer().getName();
        this.players.upsert(uuid, name);
        final long sessionId = this.sessions.open(uuid, now);
        this.activeSessions.start(uuid, sessionId, now);
    }

    /**
     * Handles a player quitting the server.
     *
     * @param event the quit event; never null
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onQuit(final PlayerQuitEvent event) {
        final Instant now = Instant.now();
        final java.util.UUID uuid = event.getPlayer().getUniqueId();
        final Optional<TrackedSession> tracked = this.activeSessions.stop(uuid);
        if (tracked.isEmpty()) {
            return;
        }
        final TrackedSession session = tracked.get();
        final long elapsed = Duration.between(session.lastHeartbeat(), now).getSeconds();
        this.sessions.heartbeat(session.sessionId(), now, elapsed);
        this.sessions.close(session.sessionId(), now);
    }
}
