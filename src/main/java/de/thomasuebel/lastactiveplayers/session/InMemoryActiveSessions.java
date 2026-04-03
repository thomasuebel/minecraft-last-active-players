package de.thomasuebel.lastactiveplayers.session;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Main-thread in-memory implementation of {@link ActiveSessions}.
 *
 * <p>Backed by a {@link HashMap} keyed on player UUID. All methods must be called from
 * the server's main thread. The {@link #flush(Instant)} result may be consumed off-thread
 * by {@link SessionHeartbeat}, but the flush call itself must be main-thread.
 */
public final class InMemoryActiveSessions implements ActiveSessions {

    private final Map<UUID, Entry> entries;

    /**
     * Constructs an empty in-memory session registry.
     */
    public InMemoryActiveSessions() {
        this.entries = new HashMap<>();
    }

    @Override
    public void start(final UUID playerUuid, final long sessionId, final Instant joinTime) {
        this.entries.put(playerUuid, new Entry(sessionId, joinTime));
    }

    @Override
    public Optional<TrackedSession> stop(final UUID playerUuid) {
        final Entry removed = this.entries.remove(playerUuid);
        if (removed == null) {
            return Optional.empty();
        }
        return Optional.of(new TrackedSession(removed.sessionId, removed.lastHeartbeat));
    }

    @Override
    public List<HeartbeatEntry> flush(final Instant now) {
        final List<HeartbeatEntry> result = new ArrayList<>(this.entries.size());
        for (final Map.Entry<UUID, Entry> mapEntry : this.entries.entrySet()) {
            final Entry entry = mapEntry.getValue();
            final long elapsed = Duration.between(entry.lastHeartbeat, now).getSeconds();
            result.add(new HeartbeatEntry(entry.sessionId, elapsed));
            mapEntry.setValue(new Entry(entry.sessionId, now));
        }
        return result;
    }

    @Override
    public List<TrackedSession> all() {
        final List<TrackedSession> result = new ArrayList<>(this.entries.size());
        for (final Entry entry : this.entries.values()) {
            result.add(new TrackedSession(entry.sessionId, entry.lastHeartbeat));
        }
        return result;
    }

    private static final class Entry {

        private final long sessionId;
        private final Instant lastHeartbeat;

        private Entry(final long sessionId, final Instant lastHeartbeat) {
            this.sessionId = sessionId;
            this.lastHeartbeat = lastHeartbeat;
        }
    }
}
