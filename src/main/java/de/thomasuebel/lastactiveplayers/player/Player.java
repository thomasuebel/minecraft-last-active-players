package de.thomasuebel.lastactiveplayers.player;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * A player known to the plugin's persistence layer.
 *
 * <p>The {@link #exists()} method distinguishes a real player record from the
 * Null Object ({@link NoPlayer}) returned when no record is found.
 */
public interface Player {

    /**
     * Returns {@code true} if this player has a record in the database.
     *
     * @return {@code false} for the Null Object, {@code true} otherwise
     */
    boolean exists();

    /**
     * Returns the player's unique identifier.
     *
     * @return the UUID; never null
     */
    UUID uuid();

    /**
     * Returns the player's last known username.
     *
     * @return the username; empty string for the Null Object
     */
    String username();

    /**
     * Returns the player's current consecutive daily login streak in days.
     *
     * @return streak length in days, zero for the Null Object
     */
    int streakDays();

    /**
     * Returns the calendar date of the last day contributing to the streak.
     *
     * @return the date, or empty if no streak has been recorded
     */
    Optional<LocalDate> streakLastDay();
}
