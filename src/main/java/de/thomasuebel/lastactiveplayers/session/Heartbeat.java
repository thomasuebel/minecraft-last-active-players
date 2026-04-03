package de.thomasuebel.lastactiveplayers.session;

import java.time.Instant;

/**
 * Triggers a periodic session heartbeat flush.
 *
 * <p>Implementors flush all active sessions and persist the accumulated duration
 * to the database. Intended to be called on a fixed schedule.
 */
public interface Heartbeat {

    /**
     * Flushes all active sessions and writes heartbeat records to persistent storage.
     *
     * @param now the current timestamp used as the flush baseline; never null
     */
    void pulse(Instant now);
}
