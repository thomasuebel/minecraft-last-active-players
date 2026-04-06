package de.thomasuebel.lastactiveplayers.session;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * An immutable snapshot of a player session record.
 *
 * <p>A session begins when a player joins the server and ends when they leave or the
 * server stops. Ongoing sessions have an empty {@link #leaveTime()}.
 *
 * @param id              the database-assigned session ID; positive
 * @param playerUuid      the owning player's UUID; never null
 * @param joinTime        the join timestamp; never null
 * @param leaveTime       the leave timestamp, or empty for an ongoing session
 * @param lastHeartbeat   the last heartbeat timestamp; never null
 * @param durationSeconds accumulated play time in seconds; non-negative
 */
public record Session(
    long id,
    UUID playerUuid,
    Instant joinTime,
    Optional<Instant> leaveTime,
    Instant lastHeartbeat,
    long durationSeconds
) {
}
