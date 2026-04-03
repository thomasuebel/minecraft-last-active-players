package de.thomasuebel.lastactiveplayers.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionHeartbeatTest {

    private static final long SESSION_A = 1L;
    private static final long SESSION_B = 2L;
    private static final long HEARTBEAT_INTERVAL_SECONDS = 600L;

    private static final Instant JOIN = Instant.parse("2026-04-01T10:00:00Z");
    private static final Instant FLUSH = Instant.parse("2026-04-01T10:10:00Z");

    private FakeSessions fakeSessions;
    private ActiveSessions activeSessions;
    private Heartbeat heartbeat;

    @BeforeEach
    void setUp() {
        this.fakeSessions = new FakeSessions();
        this.activeSessions = new InMemoryActiveSessions();
        this.heartbeat = new SessionHeartbeat(this.activeSessions, this.fakeSessions);
    }

    @Test
    void pulseOnEmptyActiveSessionsWritesNoHeartbeats() {
        heartbeat.pulse(FLUSH);
        assertTrue(fakeSessions.heartbeatCalls().isEmpty());
    }

    @Test
    void pulseWritesHeartbeatForEachActiveSession() {
        activeSessions.start(UUID.randomUUID(), SESSION_A, JOIN);
        activeSessions.start(UUID.randomUUID(), SESSION_B, JOIN);
        heartbeat.pulse(FLUSH);
        assertEquals(2, fakeSessions.heartbeatCalls().size());
    }

    @Test
    void pulseWritesCorrectSessionId() {
        activeSessions.start(UUID.randomUUID(), SESSION_A, JOIN);
        heartbeat.pulse(FLUSH);
        assertEquals(SESSION_A, fakeSessions.heartbeatCalls().get(0).sessionId());
    }

    @Test
    void pulseWritesElapsedSeconds() {
        activeSessions.start(UUID.randomUUID(), SESSION_A, JOIN);
        heartbeat.pulse(FLUSH);
        assertEquals(
            HEARTBEAT_INTERVAL_SECONDS, fakeSessions.heartbeatCalls().get(0).additionalSeconds()
        );
    }

    @Test
    void pulseWritesNowAsHeartbeatTimestamp() {
        activeSessions.start(UUID.randomUUID(), SESSION_A, JOIN);
        heartbeat.pulse(FLUSH);
        assertEquals(FLUSH, fakeSessions.heartbeatCalls().get(0).now());
    }

    // ---- Test double ----

    static final class FakeSessions implements Sessions {

        private final List<HeartbeatCall> heartbeatCalls = new ArrayList<>();

        List<HeartbeatCall> heartbeatCalls() {
            return this.heartbeatCalls;
        }

        @Override
        public long open(final UUID playerUuid, final Instant joinTime) {
            return 0L;
        }

        @Override
        public void close(final long sessionId, final Instant leaveTime) {
        }

        @Override
        public void heartbeat(
            final long sessionId, final Instant now, final long additionalSeconds
        ) {
            this.heartbeatCalls.add(new HeartbeatCall(sessionId, now, additionalSeconds));
        }

        @Override
        public List<Session> activeInWindow(final Instant start, final Instant end) {
            return List.of();
        }

        @Override
        public List<Session> orphaned() {
            return List.of();
        }

        @Override
        public void closeOrphans(final Instant effectiveLeaveTime) {
        }

        static final class HeartbeatCall {

            private final long sessionId;
            private final Instant now;
            private final long additionalSeconds;

            HeartbeatCall(
                final long sessionId, final Instant now, final long additionalSeconds
            ) {
                this.sessionId = sessionId;
                this.now = now;
                this.additionalSeconds = additionalSeconds;
            }

            long sessionId() {
                return this.sessionId;
            }

            Instant now() {
                return this.now;
            }

            long additionalSeconds() {
                return this.additionalSeconds;
            }
        }
    }
}
