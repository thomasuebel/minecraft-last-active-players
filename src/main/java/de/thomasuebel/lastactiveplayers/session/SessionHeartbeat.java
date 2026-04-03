package de.thomasuebel.lastactiveplayers.session;

import java.time.Instant;
import java.util.List;

/**
 * Pure-Java implementation of {@link Heartbeat}.
 *
 * <p>Delegates to {@link ActiveSessions#flush(Instant)} to obtain elapsed-second deltas
 * for every in-flight session, then persists each delta via {@link Sessions#heartbeat}.
 * Contains no Bukkit dependencies so it can be unit-tested without a server environment.
 */
public final class SessionHeartbeat implements Heartbeat {

    private final ActiveSessions activeSessions;
    private final Sessions sessions;

    /**
     * Constructs a heartbeat backed by the given active-session registry and session store.
     *
     * @param activeSessions the in-memory active session registry; never null
     * @param sessions       the persistent session store; never null
     */
    public SessionHeartbeat(final ActiveSessions activeSessions, final Sessions sessions) {
        this.activeSessions = activeSessions;
        this.sessions = sessions;
    }

    @Override
    public void pulse(final Instant now) {
        final List<HeartbeatEntry> entries = this.activeSessions.flush(now);
        for (final HeartbeatEntry entry : entries) {
            this.sessions.heartbeat(entry.sessionId(), now, entry.additionalSeconds());
        }
    }
}
