package de.thomasuebel.lastactiveplayers.ranking;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A private hint shown to a player indicating their current rank and how many more minutes
 * of play time they need to reach the next rank up.
 */
public interface RankHint {

    /**
     * Returns the formatted rank hint for the given player, or empty if the player is
     * not in the leaderboard or is already ranked first.
     *
     * @param playerUuid    the player's UUID; never null
     * @param onlinePlayers UUIDs of all currently online players (including the player); never null
     * @return the hint text, or empty; never null
     */
    Optional<String> text(UUID playerUuid, Set<UUID> onlinePlayers);
}
