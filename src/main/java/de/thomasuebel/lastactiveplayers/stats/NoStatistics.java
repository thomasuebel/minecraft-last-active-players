package de.thomasuebel.lastactiveplayers.stats;

/**
 * Null-object {@link Statistics} that performs no registration.
 *
 * <p>Used when statistics reporting is unavailable or not configured.
 */
public final class NoStatistics implements Statistics {

    /** Constructs a no-op statistics sentinel. */
    public NoStatistics() {
    }

    @Override
    public void register() {
    }
}
