package de.thomasuebel.lastactiveplayers.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryActiveSessionsTest {

    private static final long SESSION_ID = 42L;
    private static final long HEARTBEAT_INTERVAL_SECONDS = 600L;

    private static final Instant JOIN = Instant.parse("2026-04-01T10:00:00Z");
    private static final Instant AFTER_ONE_BEAT = Instant.parse("2026-04-01T10:10:00Z");
    private static final Instant AFTER_TWO_BEATS = Instant.parse("2026-04-01T10:20:00Z");

    private ActiveSessions sessions;
    private UUID playerUuid;

    @BeforeEach
    void setUp() {
        this.sessions = new InMemoryActiveSessions();
        this.playerUuid = UUID.randomUUID();
    }

    @Test
    void startRegistersSession() {
        sessions.start(playerUuid, SESSION_ID, JOIN);
        assertEquals(1, sessions.all().size());
    }

    @Test
    void stopRemovesSession() {
        sessions.start(playerUuid, SESSION_ID, JOIN);
        sessions.stop(playerUuid);
        assertTrue(sessions.all().isEmpty());
    }

    @Test
    void stopReturnsPresentForKnownPlayer() {
        sessions.start(playerUuid, SESSION_ID, JOIN);
        final Optional<TrackedSession> result = sessions.stop(playerUuid);
        assertTrue(result.isPresent());
    }

    @Test
    void stopReturnsSessionId() {
        sessions.start(playerUuid, SESSION_ID, JOIN);
        final Optional<TrackedSession> result = sessions.stop(playerUuid);
        assertEquals(SESSION_ID, result.get().sessionId());
    }

    @Test
    void stopReturnsLastHeartbeat() {
        sessions.start(playerUuid, SESSION_ID, JOIN);
        final Optional<TrackedSession> result = sessions.stop(playerUuid);
        assertEquals(JOIN, result.get().lastHeartbeat());
    }

    @Test
    void stopUnknownUuidReturnsEmpty() {
        final Optional<TrackedSession> result = sessions.stop(UUID.randomUUID());
        assertTrue(result.isEmpty());
    }

    @Test
    void flushOnEmptyReturnsEmptyList() {
        final List<HeartbeatEntry> entries = sessions.flush(AFTER_ONE_BEAT);
        assertTrue(entries.isEmpty());
    }

    @Test
    void flushReturnsOneEntryPerActiveSession() {
        sessions.start(playerUuid, SESSION_ID, JOIN);
        final List<HeartbeatEntry> entries = sessions.flush(AFTER_ONE_BEAT);
        assertEquals(1, entries.size());
    }

    @Test
    void flushEntryHasCorrectSessionId() {
        sessions.start(playerUuid, SESSION_ID, JOIN);
        final List<HeartbeatEntry> entries = sessions.flush(AFTER_ONE_BEAT);
        assertEquals(SESSION_ID, entries.get(0).sessionId());
    }

    @Test
    void flushEntryHasElapsedSeconds() {
        sessions.start(playerUuid, SESSION_ID, JOIN);
        final List<HeartbeatEntry> entries = sessions.flush(AFTER_ONE_BEAT);
        assertEquals(HEARTBEAT_INTERVAL_SECONDS, entries.get(0).additionalSeconds());
    }

    @Test
    void secondFlushAccumulatesFromPreviousFlush() {
        sessions.start(playerUuid, SESSION_ID, JOIN);
        sessions.flush(AFTER_ONE_BEAT);
        final List<HeartbeatEntry> entries = sessions.flush(AFTER_TWO_BEATS);
        assertEquals(HEARTBEAT_INTERVAL_SECONDS, entries.get(0).additionalSeconds());
    }

    @Test
    void stopAfterFlushReturnsUpdatedHeartbeat() {
        sessions.start(playerUuid, SESSION_ID, JOIN);
        sessions.flush(AFTER_ONE_BEAT);
        final Optional<TrackedSession> stopped = sessions.stop(playerUuid);
        assertEquals(AFTER_ONE_BEAT, stopped.get().lastHeartbeat());
    }
}
