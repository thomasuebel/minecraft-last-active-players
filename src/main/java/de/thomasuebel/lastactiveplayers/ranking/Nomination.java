package de.thomasuebel.lastactiveplayers.ranking;

import java.util.UUID;

/**
 * The identity and streak data for a player nominated as MVP or Streak Leader.
 *
 * @param uuid       the nominated player's UUID; never null
 * @param username   the nominated player's last-known username; never null
 * @param streakDays the player's current streak in days; 0 for MVP nominations
 */
public record Nomination(UUID uuid, String username, int streakDays) {
}
