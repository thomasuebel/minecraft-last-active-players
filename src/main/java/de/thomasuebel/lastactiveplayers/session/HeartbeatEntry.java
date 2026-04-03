package de.thomasuebel.lastactiveplayers.session;

/**
 * An immutable pair of (sessionId, additionalSeconds) produced by a heartbeat flush.
 *
 * <p>Each entry represents the number of seconds that should be added to a session's
 * {@code duration_seconds} column and the session it belongs to.
 */
public final class HeartbeatEntry {

    private final long sessionId;
    private final long additionalSeconds;

    /**
     * Constructs a heartbeat entry.
     *
     * @param sessionId         the target session ID; positive
     * @param additionalSeconds seconds elapsed since the last heartbeat; non-negative
     */
    public HeartbeatEntry(final long sessionId, final long additionalSeconds) {
        this.sessionId = sessionId;
        this.additionalSeconds = additionalSeconds;
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
     * Returns the number of seconds to add to the session's duration.
     *
     * @return non-negative additional seconds
     */
    public long additionalSeconds() {
        return this.additionalSeconds;
    }
}
