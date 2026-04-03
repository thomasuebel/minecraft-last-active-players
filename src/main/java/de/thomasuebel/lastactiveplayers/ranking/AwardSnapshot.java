package de.thomasuebel.lastactiveplayers.ranking;

import java.util.List;

/**
 * An immutable snapshot of the current MVP and Streak Leader election result.
 *
 * <p>Each category returns a list of {@link Nomination} objects:
 * <ul>
 *   <li>empty list -- no qualifying player exists</li>
 *   <li>one entry -- a single clear winner</li>
 *   <li>two or more entries -- a tie at the top position</li>
 * </ul>
 *
 * <p>When no qualifying player exists, implementations should return an empty list
 * rather than a list containing a {@link NoNomination}.
 */
public interface AwardSnapshot {

    /**
     * Returns all players tied for the MVP title (highest 30-day playtime).
     *
     * @return MVP candidates; never null, may be empty
     */
    List<Nomination> mvpCandidates();

    /**
     * Returns all players tied for the Streak Leader title (highest consecutive
     * daily login streak).
     *
     * @return streak leader candidates; never null, may be empty
     */
    List<Nomination> streakCandidates();

    /**
     * Returns {@code true} if the given snapshot has exactly the same set of MVP and
     * streak leader UUIDs as this one.
     *
     * @param other the snapshot to compare against; never null
     * @return true when both MVP and streak leader sets are identical
     */
    boolean sameLeaders(AwardSnapshot other);
}
