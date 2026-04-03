package de.thomasuebel.lastactiveplayers.session;

import java.time.Instant;

/**
 * An immutable snapshot of an in-memory active session.
 *
 * <p>Holds the database session ID and the timestamp of the most recent heartbeat flush.
 * Returned by {@link ActiveSessions#stop(java.util.UUID)} so callers can compute the
 * remaining duration before closing the session in the database.
 */
public final class TrackedSession {

    private final long sessionId;
    private final Instant lastHeartbeat;

    /**
     * Constructs a tracked-session snapshot.
     *
     * @param sessionId     the database-assigned session ID; positive
     * @param lastHeartbeat the timestamp of the last heartbeat or join; never null
     */
    public TrackedSession(final long sessionId, final Instant lastHeartbeat) {
        this.sessionId = sessionId;
        this.lastHeartbeat = lastHeartbeat;
    }

    /**
     * Returns the database session ID.
     *
     * @return positive session ID
     */
    public long sessionId() {
        return this.sessionId;
    }

    /**
     * Returns the timestamp of the last heartbeat flush for this session.
     *
     * @return last heartbeat instant; never null
     */
    public Instant lastHeartbeat() {
        return this.lastHeartbeat;
    }
}
