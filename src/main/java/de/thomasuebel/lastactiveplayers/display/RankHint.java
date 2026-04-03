package de.thomasuebel.lastactiveplayers.display;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A private hint shown to the joining player indicating their current rank and how many
 * more minutes of play time they need to reach the next rank up.
 */
public interface RankHint {

    /**
     * Returns the formatted rank hint for the given player, or empty if the player is
     * not in the leaderboard or is already ranked first.
     *
     * @param playerUuid    the joining player's UUID; never null
     * @param onlinePlayers UUIDs of all currently online players (including the joiner); never null
     * @return the hint text, or empty; never null
     */
    Optional<String> text(UUID playerUuid, Set<UUID> onlinePlayers);
}
