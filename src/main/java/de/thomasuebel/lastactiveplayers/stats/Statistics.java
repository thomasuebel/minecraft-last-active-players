package de.thomasuebel.lastactiveplayers.stats;

/**
 * Represents a plugin statistics reporter.
 *
 * <p>Callers invoke {@link #register()} once during plugin startup to begin reporting.
 * {@link NoStatistics} is the null-object that performs no registration.
 */
public interface Statistics {

    /**
     * Registers this reporter with its underlying platform.
     *
     * <p>Implementations may perform I/O; callers should invoke this exactly once.
     */
    void register();
}
