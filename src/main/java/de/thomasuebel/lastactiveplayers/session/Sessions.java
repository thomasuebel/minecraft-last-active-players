package de.thomasuebel.lastactiveplayers.session;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Persistence operations for {@link Session} records.
 */
public interface Sessions {

    /**
     * Opens a new session for the given player and returns its generated ID.
     *
     * @param playerUuid the joining player's UUID; never null
     * @param joinTime   the join timestamp; never null
     * @return the generated session ID; positive
     */
    long open(UUID playerUuid, Instant joinTime);

    /**
     * Closes the session by recording the leave timestamp.
     *
     * @param sessionId the ID of the session to close
     * @param leaveTime the leave timestamp; never null
     */
    void close(long sessionId, Instant leaveTime);

    /**
     * Increments {@code duration_seconds} and updates {@code last_heartbeat}.
     *
     * @param sessionId         the target session ID
     * @param now               the current timestamp; never null
     * @param additionalSeconds seconds to add to the running total; non-negative
     */
    void heartbeat(long sessionId, Instant now, long additionalSeconds);

    /**
     * Returns all sessions that were active at any point within the given window.
     *
     * <p>A session is included if it started at or before {@code end} and either
     * ended at or after {@code start}, or has not ended yet.
     *
     * @param start window start (inclusive); never null
     * @param end   window end (inclusive); never null
     * @return matching sessions; never null, may be empty
     */
    List<Session> activeInWindow(Instant start, Instant end);

    /**
     * Returns all sessions that have no recorded leave time (orphans from a crash).
     *
     * @return orphaned sessions; never null, may be empty
     */
    List<Session> orphaned();

    /**
     * Closes all orphaned sessions by setting their leave time to the given instant.
     *
     * @param effectiveLeaveTime the timestamp to record as leave time; never null
     */
    void closeOrphans(Instant effectiveLeaveTime);
}
