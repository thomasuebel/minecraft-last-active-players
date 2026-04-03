package de.thomasuebel.lastactiveplayers.display;

/**
 * Package-private value object that formats a duration given in seconds as a
 * human-readable string.
 *
 * <p>Format rules (seconds are always omitted when hours are non-zero):
 * <ul>
 *   <li>Hours and minutes: {@code "Xh Ym"}.</li>
 *   <li>Hours only (minutes = 0): {@code "Xh"}.</li>
 *   <li>Minutes and seconds: {@code "Xm Ys"}.</li>
 *   <li>Minutes only (seconds = 0): {@code "Xm"}.</li>
 *   <li>Seconds only (including zero): {@code "Xs"}.</li>
 * </ul>
 */
final class HumanDuration {

    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long SECONDS_PER_HOUR = 3600L;

    private final long seconds;

    /**
     * @param seconds non-negative duration in seconds
     */
    HumanDuration(final long seconds) {
        this.seconds = seconds;
    }

    String text() {
        final long hours = this.seconds / SECONDS_PER_HOUR;
        final long minutes = (this.seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE;
        final long secs = this.seconds % SECONDS_PER_MINUTE;
        if (hours > 0 && minutes > 0) {
            return hours + "h " + minutes + "m";
        }
        if (hours > 0) {
            return hours + "h";
        }
        if (minutes > 0 && secs > 0) {
            return minutes + "m " + secs + "s";
        }
        if (minutes > 0) {
            return minutes + "m";
        }
        return secs + "s";
    }
}
