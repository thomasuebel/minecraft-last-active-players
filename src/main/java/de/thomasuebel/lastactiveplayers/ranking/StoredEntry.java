package de.thomasuebel.lastactiveplayers.ranking;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Package-private immutable implementation of {@link LeaderboardEntry} backed by
 * values read from a database result set.
 */
final class StoredEntry implements LeaderboardEntry {

    private final UUID uuid;
    private final String username;
    private final long totalSeconds;
    private final Optional<Instant> lastLeave;

    StoredEntry(
        final UUID uuid,
        final String username,
        final long totalSeconds,
        final Optional<Instant> lastLeave
    ) {
        this.uuid = uuid;
        this.username = username;
        this.totalSeconds = totalSeconds;
        this.lastLeave = lastLeave;
    }

    @Override
    public UUID uuid() {
        return this.uuid;
    }

    @Override
    public String username() {
        return this.username;
    }

    @Override
    public long totalSeconds() {
        return this.totalSeconds;
    }

    @Override
    public Optional<Instant> lastLeave() {
        return this.lastLeave;
    }
}
