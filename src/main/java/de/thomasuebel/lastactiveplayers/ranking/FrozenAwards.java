package de.thomasuebel.lastactiveplayers.ranking;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable {@link AwardSnapshot} that captures MVP and Streak Leader candidates at
 * the moment of construction.
 *
 * <p>Both lists are stored as-is and returned verbatim on subsequent calls,
 * so this class is safe to use as a previous-state record for change detection.
 */
public final class FrozenAwards implements AwardSnapshot {

    private final List<Nomination> mvpCandidates;
    private final List<Nomination> streakCandidates;

    /**
     * Constructs a frozen snapshot from already-resolved nomination lists.
     *
     * @param mvpCandidates    MVP candidates at election time; never null, may be empty
     * @param streakCandidates streak leader candidates at election time; never null, may be empty
     */
    public FrozenAwards(
        final List<Nomination> mvpCandidates,
        final List<Nomination> streakCandidates
    ) {
        this.mvpCandidates = List.copyOf(mvpCandidates);
        this.streakCandidates = List.copyOf(streakCandidates);
    }

    @Override
    public List<Nomination> mvpCandidates() {
        return this.mvpCandidates;
    }

    @Override
    public List<Nomination> streakCandidates() {
        return this.streakCandidates;
    }

    @Override
    public boolean sameLeaders(final AwardSnapshot other) {
        return uuidsOf(this.mvpCandidates).equals(uuidsOf(other.mvpCandidates()))
            && uuidsOf(this.streakCandidates).equals(uuidsOf(other.streakCandidates()));
    }

    private Set<UUID> uuidsOf(final List<Nomination> nominations) {
        final Set<UUID> result = new HashSet<>();
        for (final Nomination nomination : nominations) {
            result.add(nomination.uuid());
        }
        return result;
    }
}
