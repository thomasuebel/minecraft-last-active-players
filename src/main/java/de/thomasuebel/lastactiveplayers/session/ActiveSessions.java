package de.thomasuebel.lastactiveplayers.session;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * In-memory registry of sessions that are currently open on the server.
 *
 * <p>All mutating methods must be called from the server's main thread.
 * {@link #flush(Instant)} is the only method safe to call off-thread after retrieving
 * a snapshot of active sessions.
 */
public interface ActiveSessions {

    /**
     * Registers a newly opened session.
     *
     * @param playerUuid the joining player's UUID; never null
     * @param sessionId  the database-assigned session ID; positive
     * @param joinTime   the join timestamp used as the initial heartbeat baseline; never null
     */
    void start(UUID playerUuid, long sessionId, Instant joinTime);

    /**
     * Removes the session for the given player and returns it.
     *
     * <p>Returns empty if no session is registered for that player (e.g. the player was
     * never recorded, or the session was already stopped).
     *
     * @param playerUuid the leaving player's UUID; never null
     * @return the tracked session, or empty if none was found
     */
    Optional<TrackedSession> stop(UUID playerUuid);

    /**
     * Computes a heartbeat delta for every active session and advances each session's
     * internal {@code lastHeartbeat} to {@code now}.
     *
     * <p>Returns one {@link HeartbeatEntry} per active session, containing the number of
     * seconds elapsed since its previous heartbeat.
     *
     * @param now the current timestamp; never null
     * @return heartbeat entries for each active session; never null, may be empty
     */
    List<HeartbeatEntry> flush(Instant now);

    /**
     * Returns a snapshot of all currently tracked sessions.
     *
     * @return tracked sessions; never null, may be empty
     */
    List<TrackedSession> all();
}
