package de.thomasuebel.lastactiveplayers.display;

/**
 * Package-private value object that formats a duration given in seconds as a
 * human-readable string.
 *
 * <p>Format rules:
 * <ul>
 *   <li>Hours present: {@code "Xh"} or {@code "Xh Ym"} (seconds omitted for brevity).</li>
 *   <li>Minutes only: {@code "Xm"} or {@code "Xm Ys"}.</li>
 *   <li>Seconds only (including zero): {@code "Xs"}.</li>
 * </ul>
 */
final class HumanDuration {

    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long SECONDS_PER_HOUR = 3600L;

    private final long seconds;

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
