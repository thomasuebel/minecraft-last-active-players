package de.thomasuebel.lastactiveplayers.ranking;

import java.util.List;

/**
 * Null Object implementation of {@link AwardSnapshot} used as the initial state
 * before any election has been performed.
 *
 * <p>Both candidate lists are always empty, and {@link #sameLeaders} always returns
 * {@code false} so that the first real election always triggers a broadcast.
 */
public final class NoAwards implements AwardSnapshot {

    @Override
    public List<Nomination> mvpCandidates() {
        return List.of();
    }

    @Override
    public List<Nomination> streakCandidates() {
        return List.of();
    }

    /**
     * Always returns {@code false} so that the first real election unconditionally
     * triggers an announcement.
     *
     * @param other the snapshot to compare against; never null
     * @return always {@code false}
     */
    @Override
    public boolean sameLeaders(final AwardSnapshot other) {
        return false;
    }
}
