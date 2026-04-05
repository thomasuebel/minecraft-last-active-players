package de.thomasuebel.lastactiveplayers.display;

import java.time.Instant;

/**
 * Produces a human-readable date string for a given {@link Instant}.
 */
public interface DateLabel {

    /**
     * Returns a formatted date string for the given instant.
     *
     * @param instant the point in time to format; never null
     * @return the date string; never null
     */
    String text(Instant instant);
}
