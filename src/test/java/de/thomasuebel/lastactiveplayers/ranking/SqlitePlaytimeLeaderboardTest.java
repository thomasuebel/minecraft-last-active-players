package de.thomasuebel.lastactiveplayers.ranking;

import de.thomasuebel.lastactiveplayers.db.Database;
import de.thomasuebel.lastactiveplayers.db.InitialSchema;
import de.thomasuebel.lastactiveplayers.db.SqliteDatabase;
import de.thomasuebel.lastactiveplayers.db.SqliteMigrations;
import de.thomasuebel.lastactiveplayers.player.Players;
import de.thomasuebel.lastactiveplayers.player.SqlitePlayers;
import de.thomasuebel.lastactiveplayers.session.Sessions;
import de.thomasuebel.lastactiveplayers.session.SqliteSessions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlitePlaytimeLeaderboardTest {

    private static final long TWO_HOURS_SECONDS = 7200L;
    private static final long ONE_HOUR_SECONDS = 3600L;
    private static final int SIXTY_SECONDS = 60;
    private static final long THIRTY_DAYS = 30L;

    // "now" is April 1; window start = now - 30 days = March 2
    private static final Instant NOW = Instant.parse("2026-04-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static final Instant JOIN_ALICE = Instant.parse("2026-03-10T10:00:00Z");
    private static final Instant LEAVE_ALICE = Instant.parse("2026-03-10T12:00:00Z");
    private static final Instant JOIN_BOB = Instant.parse("2026-03-11T10:00:00Z");
    private static final Instant LEAVE_BOB = Instant.parse("2026-03-11T11:00:00Z");
    private static final Instant OLD_JOIN = Instant.parse("2026-02-01T10:00:00Z");
    private static final Instant OLD_LEAVE = Instant.parse("2026-02-01T11:00:00Z");
    private static final Instant PRE_WINDOW_JOIN = Instant.parse("2026-03-01T23:00:00Z");
    private static final Instant POST_WINDOW_LEAVE = Instant.parse("2026-03-02T01:00:00Z");

    private Database db;
    private Players players;
    private Sessions sessions;
    private UUID aliceUuid;
    private UUID bobUuid;

    @BeforeEach
    void setUp(@TempDir final Path dir) throws IOException {
        this.db = new SqliteDatabase(
            dir.resolve("test.db"), new SqliteMigrations(new InitialSchema())
        );
        this.players = new SqlitePlayers(this.db);
        this.sessions = new SqliteSessions(this.db);
        this.aliceUuid = UUID.randomUUID();
        this.bobUuid = UUID.randomUUID();
        this.players.upsert(aliceUuid, "Alice");
        this.players.upsert(bobUuid, "Bob");
    }

    @AfterEach
    void tearDown() throws IOException {
        this.db.close();
    }

    @Test
    void returnsPlayersOrderedByTotalSecondsDescending() {
        final long aliceId = sessions.open(aliceUuid, JOIN_ALICE);
        sessions.heartbeat(aliceId, LEAVE_ALICE, TWO_HOURS_SECONDS);
        sessions.close(aliceId, LEAVE_ALICE);
        final long bobId = sessions.open(bobUuid, JOIN_BOB);
        sessions.heartbeat(bobId, LEAVE_BOB, ONE_HOUR_SECONDS);
        sessions.close(bobId, LEAVE_BOB);

        final Leaderboard board = new SqlitePlaytimeLeaderboard(this.db, CLOCK, THIRTY_DAYS);
        final List<LeaderboardEntry> top = board.top(10, Set.of());

        assertEquals(2, top.size());
        assertEquals(aliceUuid, top.get(0).uuid());
        assertEquals(bobUuid, top.get(1).uuid());
    }

    @Test
    void totalSecondsReflectsAccumulatedHeartbeats() {
        final long aliceId = sessions.open(aliceUuid, JOIN_ALICE);
        sessions.heartbeat(aliceId, LEAVE_ALICE, TWO_HOURS_SECONDS);
        sessions.close(aliceId, LEAVE_ALICE);

        final Leaderboard board = new SqlitePlaytimeLeaderboard(this.db, CLOCK, THIRTY_DAYS);
        final List<LeaderboardEntry> top = board.top(10, Set.of());

        assertEquals(TWO_HOURS_SECONDS, top.get(0).totalSeconds());
    }

    @Test
    void excludesSessionsThatEndedBeforeWindowStart() {
        final long oldId = sessions.open(aliceUuid, OLD_JOIN);
        sessions.heartbeat(oldId, OLD_LEAVE, ONE_HOUR_SECONDS);
        sessions.close(oldId, OLD_LEAVE);

        final Leaderboard board = new SqlitePlaytimeLeaderboard(this.db, CLOCK, THIRTY_DAYS);
        final List<LeaderboardEntry> top = board.top(10, Set.of());

        assertTrue(top.isEmpty());
    }

    @Test
    void includesSessionThatStartedBeforeWindowButEndedAfter() {
        // Session spans the window boundary: join before, leave after window start.
        // Should be included because leave_time >= windowStart.
        final long crossId = sessions.open(aliceUuid, PRE_WINDOW_JOIN);
        sessions.heartbeat(crossId, POST_WINDOW_LEAVE, ONE_HOUR_SECONDS);
        sessions.close(crossId, POST_WINDOW_LEAVE);

        final Leaderboard board = new SqlitePlaytimeLeaderboard(this.db, CLOCK, THIRTY_DAYS);
        final List<LeaderboardEntry> top = board.top(10, Set.of());

        assertEquals(1, top.size());
        assertEquals(ONE_HOUR_SECONDS, top.get(0).totalSeconds());
    }

    @Test
    void excludesOnlinePlayers() {
        final long aliceId = sessions.open(aliceUuid, JOIN_ALICE);
        sessions.heartbeat(aliceId, LEAVE_ALICE, TWO_HOURS_SECONDS);
        sessions.close(aliceId, LEAVE_ALICE);

        final Leaderboard board = new SqlitePlaytimeLeaderboard(this.db, CLOCK, THIRTY_DAYS);
        final List<LeaderboardEntry> top = board.top(10, Set.of(aliceUuid));

        assertTrue(top.isEmpty());
    }

    @Test
    void respectsLimit() {
        final long aliceId = sessions.open(aliceUuid, JOIN_ALICE);
        sessions.heartbeat(aliceId, LEAVE_ALICE, TWO_HOURS_SECONDS);
        sessions.close(aliceId, LEAVE_ALICE);
        final long bobId = sessions.open(bobUuid, JOIN_BOB);
        sessions.heartbeat(bobId, LEAVE_BOB, ONE_HOUR_SECONDS);
        sessions.close(bobId, LEAVE_BOB);

        final Leaderboard board = new SqlitePlaytimeLeaderboard(this.db, CLOCK, THIRTY_DAYS);
        final List<LeaderboardEntry> top = board.top(1, Set.of());

        assertEquals(1, top.size());
    }

    @Test
    void sumsMultipleSessionsForSamePlayer() {
        final long session1 = sessions.open(aliceUuid, JOIN_ALICE);
        sessions.heartbeat(session1, LEAVE_ALICE, ONE_HOUR_SECONDS);
        sessions.close(session1, LEAVE_ALICE);
        final Instant join2 = LEAVE_ALICE.plusSeconds(SIXTY_SECONDS);
        final Instant leave2 = join2.plusSeconds(ONE_HOUR_SECONDS);
        final long session2 = sessions.open(aliceUuid, join2);
        sessions.heartbeat(session2, leave2, ONE_HOUR_SECONDS);
        sessions.close(session2, leave2);

        final Leaderboard board = new SqlitePlaytimeLeaderboard(this.db, CLOCK, THIRTY_DAYS);
        final List<LeaderboardEntry> top = board.top(10, Set.of());

        assertEquals(1, top.size());
        assertEquals(TWO_HOURS_SECONDS, top.get(0).totalSeconds());
    }

    @Test
    void entryUsernameMatchesPlayerRecord() {
        final long aliceId = sessions.open(aliceUuid, JOIN_ALICE);
        sessions.heartbeat(aliceId, LEAVE_ALICE, ONE_HOUR_SECONDS);
        sessions.close(aliceId, LEAVE_ALICE);

        final Leaderboard board = new SqlitePlaytimeLeaderboard(this.db, CLOCK, THIRTY_DAYS);
        final List<LeaderboardEntry> top = board.top(10, Set.of());

        assertEquals("Alice", top.get(0).username());
    }

    @Test
    void topTiedReturnsBothPlayersWhenScoresAreEqual() {
        final long aliceId = sessions.open(aliceUuid, JOIN_ALICE);
        sessions.heartbeat(aliceId, LEAVE_ALICE, ONE_HOUR_SECONDS);
        sessions.close(aliceId, LEAVE_ALICE);
        final long bobId = sessions.open(bobUuid, JOIN_BOB);
        sessions.heartbeat(bobId, LEAVE_BOB, ONE_HOUR_SECONDS);
        sessions.close(bobId, LEAVE_BOB);

        final Leaderboard board = new SqlitePlaytimeLeaderboard(this.db, CLOCK, THIRTY_DAYS);
        final List<LeaderboardEntry> tied = board.topTied(Set.of());

        assertEquals(2, tied.size());
    }

    @Test
    void topTiedReturnsSingleEntryWhenOnlyOneLeads() {
        final long aliceId = sessions.open(aliceUuid, JOIN_ALICE);
        sessions.heartbeat(aliceId, LEAVE_ALICE, TWO_HOURS_SECONDS);
        sessions.close(aliceId, LEAVE_ALICE);
        final long bobId = sessions.open(bobUuid, JOIN_BOB);
        sessions.heartbeat(bobId, LEAVE_BOB, ONE_HOUR_SECONDS);
        sessions.close(bobId, LEAVE_BOB);

        final Leaderboard board = new SqlitePlaytimeLeaderboard(this.db, CLOCK, THIRTY_DAYS);
        final List<LeaderboardEntry> tied = board.topTied(Set.of());

        assertEquals(1, tied.size());
        assertEquals(aliceUuid, tied.get(0).uuid());
    }

    @Test
    void topTiedReturnsEmptyWhenNoQualifyingPlayers() {
        final Leaderboard board = new SqlitePlaytimeLeaderboard(this.db, CLOCK, THIRTY_DAYS);
        final List<LeaderboardEntry> tied = board.topTied(Set.of());

        assertTrue(tied.isEmpty());
    }
}
