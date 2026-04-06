package de.thomasuebel.lastactiveplayers.player;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence operations for {@link PlayerRecord} entries.
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
     * Returns the player record for the given UUID, or empty if not found.
     *
     * @param uuid the player's unique identifier; never null
     * @return the player record, or empty; never null
     */
    Optional<PlayerRecord> withUuid(UUID uuid);

    /**
     * Returns the player with the highest {@code streak_days}, or empty if
     * no player has a streak greater than zero.
     *
     * @return the streak leader, or empty; never null
     */
    Optional<PlayerRecord> withHighestStreak();

    /**
     * Returns all players sharing the highest {@code streak_days} value.
     *
     * <p>If no player has a streak greater than zero, the returned list is empty.
     * If exactly one player leads, the list contains that single player.
     * If two or more players share the maximum streak, all are returned.
     *
     * @return all tied streak leaders; never null, may be empty
     */
    List<PlayerRecord> withTopStreak();

    /**
     * Deletes players whose last session ended before the given threshold.
     *
     * <p>Players with open sessions (currently online) are never purged.
     *
     * @param threshold sessions ending before this instant mark a player as inactive
     */
    void purgeInactiveBefore(Instant threshold);

    /**
     * Returns the current streak shield count for the given player.
     *
     * <p>Returns {@code 0} if the player is not found.
     *
     * @param uuid the player's unique identifier; never null
     * @return non-negative shield count
     */
    int shields(UUID uuid);

    /**
     * Persists a new streak shield count for the given player.
     *
     * @param uuid  the player's unique identifier; never null
     * @param count the new shield count; non-negative
     */
    void storeShields(UUID uuid, int count);
}
