package de.thomasuebel.lastactiveplayers.session;

import org.bukkit.scheduler.BukkitRunnable;

import java.time.Instant;

/**
 * Bukkit-scheduler-compatible wrapper around {@link Heartbeat}.
 *
 * <p>Scheduled as a repeating task via {@code BukkitRunnable}. On each tick it calls
 * {@link Heartbeat#pulse(Instant)} with the current wall-clock time, then invokes the
 * {@code afterPulse} callback so callers can react to the updated data (e.g. re-electing
 * award leaders). The actual flush-and-persist logic lives in the pure-Java
 * {@link SessionHeartbeat} delegate, keeping this class free of testable business logic.
 */
public final class BukkitHeartbeat extends BukkitRunnable {

    private final Heartbeat delegate;
    private final Runnable afterPulse;

    /**
     * Constructs a Bukkit runnable backed by the given heartbeat delegate.
     *
     * @param delegate   the heartbeat implementation to invoke on each tick; never null
     * @param afterPulse callback invoked after each pulse completes; never null
     */
    public BukkitHeartbeat(final Heartbeat delegate, final Runnable afterPulse) {
        this.delegate = delegate;
        this.afterPulse = afterPulse;
    }

    @Override
    public void run() {
        this.delegate.pulse(Instant.now());
        this.afterPulse.run();
    }
}
