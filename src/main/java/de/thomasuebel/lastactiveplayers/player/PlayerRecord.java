package de.thomasuebel.lastactiveplayers.player;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * An immutable snapshot of a player known to the plugin's persistence layer.
 *
 * @param uuid          the player's unique identifier; never null
 * @param username      the player's last known username; never null
 * @param streakDays    current consecutive daily login streak in days; non-negative
 * @param streakLastDay calendar date of the last day contributing to the streak, or empty
 */
public record PlayerRecord(
    UUID uuid,
    String username,
    int streakDays,
    Optional<LocalDate> streakLastDay
) {

    /**
     * Returns a copy of this record with the streak last day replaced.
     * Used to simulate a shield-protected streak where the effective last day
     * is shifted back by one calendar day.
     *
     * @param day the effective streak last day; never null
     * @return a new record with the modified streak last day
     */
    public PlayerRecord withStreakLastDay(final LocalDate day) {
        return new PlayerRecord(this.uuid, this.username, this.streakDays, Optional.of(day));
    }
}
