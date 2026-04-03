package de.thomasuebel.lastactiveplayers.session;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * An immutable snapshot of a player session record.
 *
 * <p>A session begins when a player joins the server and ends when they leave or the
 * server stops. Ongoing sessions have an empty {@link #leaveTime()}.
 */
public interface Session {

    /**
     * Returns the database-assigned session identifier.
     *
     * @return positive session ID
     */
    long id();

    /**
     * Returns the UUID of the player this session belongs to.
     *
     * @return the player UUID; never null
     */
    UUID playerUuid();

    /**
     * Returns the instant the player joined.
     *
     * @return join timestamp; never null
     */
    Instant joinTime();

    /**
     * Returns the instant the player left, or empty if the session is still open.
     *
     * @return leave timestamp, or empty for an ongoing session
     */
    Optional<Instant> leaveTime();

    /**
     * Returns the timestamp of the last heartbeat flush for this session.
     *
     * @return last heartbeat instant; never null
     */
    Instant lastHeartbeat();

    /**
     * Returns the total accumulated play time for this session in seconds.
     *
     * @return duration in seconds; non-negative
     */
    long durationSeconds();
}
