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
    private static final long TWO_HOURS_SECONDS = 7200L;
    private static final long ONE_HOUR_SECONDS = 3600L;
    // 7200 - 59 = 7141; a delta of 59s whose ceiling in minutes is 1 (not 0)
    private static final long FIFTY_NINE_SECONDS_BEFORE_TWO_HOURS = 7141L;
    private static final String TEMPLATE = "Rank #{rank}. {minutes}m to #{next_rank}.";

    private static LeaderboardEntry entry(final UUID uuid, final long seconds) {
        return new LeaderboardEntry() {
            @Override public UUID uuid() { return uuid; }
            @Override public String username() { return "Player"; }
            @Override public long totalSeconds() { return seconds; }
            @Override public Optional<Instant> lastLeave() { return Optional.empty(); }
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
    void excludesOtherOnlinePlayersButNotJoiner() {
        final UUID carol = UUID.randomUUID();
        // Carol is online (should be excluded); Alice is the joiner (should not be excluded)
        final RankHint hint = new LeaderboardRankHint(
            (l, exclude) -> exclude.contains(carol) && !exclude.contains(ALICE)
                ? List.of(entry(BOB, TWO_HOURS_SECONDS), entry(ALICE, ONE_HOUR_SECONDS))
                : List.of(),
            TEMPLATE
        );
        final Optional<String> text = hint.text(ALICE, Set.of(ALICE, carol));
        assertTrue(text.isPresent());
        assertEquals("Rank #2. 60m to #1.", text.get());
    }
}
