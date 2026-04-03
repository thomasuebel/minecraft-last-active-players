package de.thomasuebel.lastactiveplayers.player;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence operations for {@link Player} records.
 *
 * <p>Implementations must never return {@code null}; use {@link NoPlayer} when a
 * requested player is not found.
 */
public interface Players {

    /**
     * Inserts the player if new, or updates the username if the UUID already exists.
     *
     * @param uuid     the player's unique identifier; never null
     * @param username the player's current username; never null
     */
    void upsert(UUID uuid, String username);

    /**
     * Updates the streak counters for an existing player.
     *
     * @param uuid          the player's unique identifier; never null
     * @param streakDays    the new streak length in days; non-negative
     * @param streakLastDay the date of the last streak day, or empty to clear it
     */
    void updateStreak(UUID uuid, int streakDays, Optional<LocalDate> streakLastDay);

    /**
     * Returns the player record for the given UUID, or {@link NoPlayer} if not found.
     *
     * @param uuid the player's unique identifier; never null
     * @return the player record; never null
     */
    Player withUuid(UUID uuid);

    /**
     * Returns the player with the highest {@code streak_days}, or {@link NoPlayer} if
     * no player has a streak greater than zero.
     *
     * @return the streak leader; never null
     */
    Player withHighestStreak();

    /**
     * Deletes players whose last session ended before the given threshold.
     *
     * <p>Players with open sessions (currently online) are never purged.
     *
     * @param threshold sessions ending before this instant mark a player as inactive
     */
    void purgeInactiveBefore(Instant threshold);
}
