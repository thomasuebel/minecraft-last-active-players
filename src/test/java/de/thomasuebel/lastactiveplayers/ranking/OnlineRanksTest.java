package de.thomasuebel.lastactiveplayers.ranking;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnlineRanksTest {

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();
    private static final long TWO_HOURS_SECONDS = 7200L;
    private static final long ONE_HOUR_SECONDS = 3600L;
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
    void pulseDoesNotFireOnFirstRankAppearance() {
        // Player joins with no sessions (not in leaderboard); first heartbeat puts them at rank 2.
        // This is their first recorded rank -- no improvement, so no notification.
        final OnlineRanks ranks = new OnlineRanks(TEMPLATE);
        ranks.joined(ALICE, List.of());

        final List<String> notifications = new ArrayList<>();
        ranks.pulse(
            List.of(entry(BOB, TWO_HOURS_SECONDS), entry(ALICE, ONE_HOUR_SECONDS)),
            (uuid, text) -> notifications.add(text)
        );

        assertTrue(notifications.isEmpty());
    }

    @Test
    void pulseFiresWhenRankImproves() {
        // Alice joins at rank 3; next heartbeat she moves to rank 2.
        final UUID carol = UUID.randomUUID();
        final OnlineRanks ranks = new OnlineRanks(TEMPLATE);
        ranks.joined(ALICE, List.of(
            entry(BOB, TWO_HOURS_SECONDS),
            entry(carol, ONE_HOUR_SECONDS + 1L),
            entry(ALICE, ONE_HOUR_SECONDS)
        ));

        final List<String> notifications = new ArrayList<>();
        ranks.pulse(
            List.of(entry(BOB, TWO_HOURS_SECONDS), entry(ALICE, ONE_HOUR_SECONDS)),
            (uuid, text) -> notifications.add(text)
        );

        assertEquals(1, notifications.size());
        assertEquals("Rank #2. 60m to #1.", notifications.get(0));
    }

    @Test
    void pulseDoesNotFireWhenRankUnchanged() {
        final OnlineRanks ranks = new OnlineRanks(TEMPLATE);
        ranks.joined(ALICE, List.of(entry(BOB, TWO_HOURS_SECONDS), entry(ALICE, ONE_HOUR_SECONDS)));

        final List<String> notifications = new ArrayList<>();
        ranks.pulse(
            List.of(entry(BOB, TWO_HOURS_SECONDS), entry(ALICE, ONE_HOUR_SECONDS)),
            (uuid, text) -> notifications.add(text)
        );

        assertTrue(notifications.isEmpty());
    }

    @Test
    void pulseDoesNotFireWhenRankDeclines() {
        // Alice joins at rank 2; drops to rank 3 -- no notification for rank loss.
        final UUID carol = UUID.randomUUID();
        final OnlineRanks ranks = new OnlineRanks(TEMPLATE);
        ranks.joined(ALICE, List.of(entry(BOB, TWO_HOURS_SECONDS), entry(ALICE, ONE_HOUR_SECONDS)));

        final List<String> notifications = new ArrayList<>();
        ranks.pulse(
            List.of(
                entry(BOB, TWO_HOURS_SECONDS),
                entry(carol, ONE_HOUR_SECONDS + 1L),
                entry(ALICE, ONE_HOUR_SECONDS)
            ),
            (uuid, text) -> notifications.add(text)
        );

        assertTrue(notifications.isEmpty());
    }

    @Test
    void pulseDoesNotFireForRankOne() {
        // MVP broadcast covers rank #1 -- OnlineRanks must stay silent for that case.
        final OnlineRanks ranks = new OnlineRanks(TEMPLATE);
        ranks.joined(ALICE, List.of(entry(BOB, TWO_HOURS_SECONDS), entry(ALICE, ONE_HOUR_SECONDS)));

        final List<String> notifications = new ArrayList<>();
        ranks.pulse(
            List.of(entry(ALICE, TWO_HOURS_SECONDS)),
            (uuid, text) -> notifications.add(text)
        );

        assertTrue(notifications.isEmpty());
    }

    @Test
    void quitRemovesFromTracking() {
        final OnlineRanks ranks = new OnlineRanks(TEMPLATE);
        ranks.joined(ALICE, List.of(entry(BOB, TWO_HOURS_SECONDS), entry(ALICE, ONE_HOUR_SECONDS)));
        ranks.quit(ALICE);

        final List<String> notifications = new ArrayList<>();
        ranks.pulse(
            List.of(entry(ALICE, TWO_HOURS_SECONDS)),
            (uuid, text) -> notifications.add(text)
        );

        assertTrue(notifications.isEmpty());
    }

    @Test
    void resetClearsAllTracking() {
        final OnlineRanks ranks = new OnlineRanks(TEMPLATE);
        ranks.joined(ALICE, List.of(entry(BOB, TWO_HOURS_SECONDS), entry(ALICE, ONE_HOUR_SECONDS)));
        ranks.joined(BOB, List.of(entry(BOB, TWO_HOURS_SECONDS), entry(ALICE, ONE_HOUR_SECONDS)));
        ranks.reset();

        final List<String> notifications = new ArrayList<>();
        ranks.pulse(
            List.of(entry(ALICE, TWO_HOURS_SECONDS), entry(BOB, ONE_HOUR_SECONDS)),
            (uuid, text) -> notifications.add(text)
        );

        assertTrue(notifications.isEmpty());
    }
}
