package de.thomasuebel.lastactiveplayers.session;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * An immutable {@link Session} record loaded from the database.
 */
final class StoredSession implements Session {

    private final long id;
    private final UUID playerUuid;
    private final Instant joinTime;
    private final Optional<Instant> leaveTime;
    private final Instant lastHeartbeat;
    private final long durationSeconds;

    /**
     * Constructs a stored session from its raw field values.
     *
     * @param id              the database-assigned session ID; positive
     * @param playerUuid      the owning player's UUID; never null
     * @param joinTime        the join timestamp; never null
     * @param leaveTime       the leave timestamp, or empty for an ongoing session
     * @param lastHeartbeat   the last heartbeat timestamp; never null
     * @param durationSeconds accumulated play time in seconds; non-negative
     */
    StoredSession(
        final long id,
        final UUID playerUuid,
        final Instant joinTime,
        final Optional<Instant> leaveTime,
        final Instant lastHeartbeat,
        final long durationSeconds
    ) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.joinTime = joinTime;
        this.leaveTime = leaveTime;
        this.lastHeartbeat = lastHeartbeat;
        this.durationSeconds = durationSeconds;
    }

    @Override
    public long id() {
        return this.id;
    }

    @Override
    public UUID playerUuid() {
        return this.playerUuid;
    }

    @Override
    public Instant joinTime() {
        return this.joinTime;
    }

    @Override
    public Optional<Instant> leaveTime() {
        return this.leaveTime;
    }

    @Override
    public Instant lastHeartbeat() {
        return this.lastHeartbeat;
    }

    @Override
    public long durationSeconds() {
        return this.durationSeconds;
    }
}
