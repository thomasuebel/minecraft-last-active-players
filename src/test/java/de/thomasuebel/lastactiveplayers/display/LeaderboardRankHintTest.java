package de.thomasuebel.lastactiveplayers.display;

import de.thomasuebel.lastactiveplayers.ranking.LeaderboardEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardRankHintTest {

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();
    private static final UUID CAROL = UUID.randomUUID();
    private static final long TWO_HOURS_SECONDS = 7200L;
    private static final long ONE_HOUR_SECONDS = 3600L;
    // 7200 - 59 = 7141; a delta of 59s whose ceiling in minutes is 1 (not 0)
    private static final long FIFTY_NINE_SECONDS_BEFORE_TWO_HOURS = 7141L;
    private static final String TEMPLATE = "Rank #{rank}. {minutes}m to #{next_rank}.";

    private static LeaderboardEntry entry(final UUID uuid, final long seconds) {
        return new LeaderboardEntry() {
            @Override
            public UUID uuid() {
                return uuid;
            }
            @Override
            public String username() {
                return "Player";
            }
            @Override
            public long totalSeconds() {
                return seconds;
            }
            @Override
            public Optional<Instant> lastLeave() {
                return Optional.empty();
            }
        };
    }

    @Test
    void returnsEmptyWhenPlayerNotInList() {
        final RankHint hint = new LeaderboardRankHint((l, e) -> List.of(), TEMPLATE);
        assertTrue(hint.text(ALICE, Set.of()).isEmpty());
    }

    @Test
    void returnsEmptyWhenPlayerIsRankOne() {
        final RankHint hint = new LeaderboardRankHint(
            (l, e) -> List.of(entry(ALICE, TWO_HOURS_SECONDS), entry(BOB, ONE_HOUR_SECONDS)),
            TEMPLATE
        );
        assertTrue(hint.text(ALICE, Set.of()).isEmpty());
    }

    @Test
    void returnsHintForRankTwo() {
        final RankHint hint = new LeaderboardRankHint(
            (l, e) -> List.of(entry(BOB, TWO_HOURS_SECONDS), entry(ALICE, ONE_HOUR_SECONDS)),
            TEMPLATE
        );
        final Optional<String> text = hint.text(ALICE, Set.of());
        assertTrue(text.isPresent());
        assertEquals("Rank #2. 60m to #1.", text.get());
    }

    @Test
    void minutesIsCeilingDivision() {
        // Delta of 59 seconds: ceiling gives 1 minute, floor would give 0
        final RankHint hint = new LeaderboardRankHint(
            (l, e) -> List.of(
                entry(BOB, TWO_HOURS_SECONDS),
                entry(ALICE, FIFTY_NINE_SECONDS_BEFORE_TWO_HOURS)
            ),
            TEMPLATE
        );
        final Optional<String> text = hint.text(ALICE, Set.of());
        assertTrue(text.isPresent());
        assertEquals("Rank #2. 1m to #1.", text.get());
    }

    @Test
    void tiedPlayersAtRankOneReturnEmpty() {
        // Both players have identical scores; both share rank #1 -- no hint shown.
        final RankHint hint = new LeaderboardRankHint(
            (l, e) -> List.of(entry(BOB, ONE_HOUR_SECONDS), entry(ALICE, ONE_HOUR_SECONDS)),
            TEMPLATE
        );
        assertTrue(hint.text(ALICE, Set.of()).isEmpty());
    }

    @Test
    void tiedForSecondShowsSharedRankNumber() {
        // Three players: BOB leads; CAROL and ALICE are tied.
        // ALICE's true rank is #2 (one player strictly above), not #3 (index-based).
        final RankHint hint = new LeaderboardRankHint(
            (l, e) -> List.of(
                entry(BOB, TWO_HOURS_SECONDS),
                entry(CAROL, ONE_HOUR_SECONDS),
                entry(ALICE, ONE_HOUR_SECONDS)
            ),
            TEMPLATE
        );
        final Optional<String> text = hint.text(ALICE, Set.of());
        assertTrue(text.isPresent());
        assertEquals("Rank #2. 60m to #1.", text.get());
    }

    @Test
    void gapForTiedSecondIsToLeaderNotTiedPeer() {
        // ALICE is tied with CAROL at rank #2; the gap must be computed against BOB (rank #1),
        // not against CAROL (0 seconds difference would give 0m, which is wrong).
        final RankHint hint = new LeaderboardRankHint(
            (l, e) -> List.of(
                entry(BOB, TWO_HOURS_SECONDS),
                entry(CAROL, ONE_HOUR_SECONDS),
                entry(ALICE, ONE_HOUR_SECONDS)
            ),
            TEMPLATE
        );
        final Optional<String> text = hint.text(ALICE, Set.of());
        assertTrue(text.isPresent());
        assertEquals("Rank #2. 60m to #1.", text.get());
    }

    @Test
    void tiedAtFirstAmongManyReturnsEmpty() {
        // All three players tied -- all share rank #1 -- no hint for any of them.
        final RankHint hint = new LeaderboardRankHint(
            (l, e) -> List.of(
                entry(BOB, ONE_HOUR_SECONDS),
                entry(CAROL, ONE_HOUR_SECONDS),
                entry(ALICE, ONE_HOUR_SECONDS)
            ),
            TEMPLATE
        );
        assertTrue(hint.text(ALICE, Set.of()).isEmpty());
    }

    @Test
    void excludesOtherOnlinePlayersButNotJoiner() {
        // CAROL is online (should be excluded); ALICE is the joiner (should not be excluded)
        final RankHint hint = new LeaderboardRankHint(
            (l, exclude) -> exclude.contains(CAROL) && !exclude.contains(ALICE)
                ? List.of(entry(BOB, TWO_HOURS_SECONDS), entry(ALICE, ONE_HOUR_SECONDS))
                : List.of(),
            TEMPLATE
        );
        final Optional<String> text = hint.text(ALICE, Set.of(ALICE, CAROL));
        assertTrue(text.isPresent());
        assertEquals("Rank #2. 60m to #1.", text.get());
    }
}
