package de.thomasuebel.lastactiveplayers.ranking;

import java.util.UUID;

/**
 * The current holder of a server title (MVP or Streak Leader).
 *
 * <p>When no qualifying player exists, implementations should return {@link NoNomination}
 * rather than {@code null}.
 */
public interface Nomination {

    /**
     * Returns {@code true} if a title holder was found.
     *
     * @return true when this nomination represents a real player
     */
    boolean exists();

    /**
     * Returns the title holder's UUID.
     *
     * @return the UUID; never null (returns zero UUID for {@link NoNomination})
     */
    UUID uuid();

    /**
     * Returns the title holder's last-known username.
     *
     * @return username; never null, empty for {@link NoNomination}
     */
    String username();
}
