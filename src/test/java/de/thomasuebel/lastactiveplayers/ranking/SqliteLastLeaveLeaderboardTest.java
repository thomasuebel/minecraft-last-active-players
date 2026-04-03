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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteLastLeaveLeaderboardTest {

    private static final long ONE_HOUR_SECONDS = 3600L;

    private static final Instant JOIN_ALICE = Instant.parse("2026-03-10T10:00:00Z");
    private static final Instant LEAVE_ALICE = Instant.parse("2026-03-10T11:00:00Z");
    private static final Instant JOIN_BOB = Instant.parse("2026-03-09T10:00:00Z");
    private static final Instant LEAVE_BOB = Instant.parse("2026-03-09T11:00:00Z");

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
    void returnsPlayersOrderedByMostRecentLeaveFirst() {
        final long aliceId = sessions.open(aliceUuid, JOIN_ALICE);
        sessions.heartbeat(aliceId, LEAVE_ALICE, ONE_HOUR_SECONDS);
        sessions.close(aliceId, LEAVE_ALICE);
        final long bobId = sessions.open(bobUuid, JOIN_BOB);
        sessions.heartbeat(bobId, LEAVE_BOB, ONE_HOUR_SECONDS);
        sessions.close(bobId, LEAVE_BOB);

        final Leaderboard board = new SqliteLastLeaveLeaderboard(this.db);
        final List<LeaderboardEntry> top = board.top(10, Set.of());

        assertEquals(2, top.size());
        assertEquals(aliceUuid, top.get(0).uuid());
        assertEquals(bobUuid, top.get(1).uuid());
    }

    @Test
    void lastLeaveIsPresent() {
        final long aliceId = sessions.open(aliceUuid, JOIN_ALICE);
        sessions.heartbeat(aliceId, LEAVE_ALICE, ONE_HOUR_SECONDS);
        sessions.close(aliceId, LEAVE_ALICE);

        final Leaderboard board = new SqliteLastLeaveLeaderboard(this.db);
        final List<LeaderboardEntry> top = board.top(10, Set.of());

        assertEquals(Optional.of(LEAVE_ALICE), top.get(0).lastLeave());
    }

    @Test
    void excludesOnlinePlayers() {
        final long aliceId = sessions.open(aliceUuid, JOIN_ALICE);
        sessions.heartbeat(aliceId, LEAVE_ALICE, ONE_HOUR_SECONDS);
        sessions.close(aliceId, LEAVE_ALICE);

        final Leaderboard board = new SqliteLastLeaveLeaderboard(this.db);
        final List<LeaderboardEntry> top = board.top(10, Set.of(aliceUuid));

        assertTrue(top.isEmpty());
    }

    @Test
    void playersWithOnlyOpenSessionsAreExcluded() {
        sessions.open(aliceUuid, JOIN_ALICE);

        final Leaderboard board = new SqliteLastLeaveLeaderboard(this.db);
        final List<LeaderboardEntry> top = board.top(10, Set.of());

        assertTrue(top.isEmpty());
    }

    @Test
    void respectsLimit() {
        final long aliceId = sessions.open(aliceUuid, JOIN_ALICE);
        sessions.heartbeat(aliceId, LEAVE_ALICE, ONE_HOUR_SECONDS);
        sessions.close(aliceId, LEAVE_ALICE);
        final long bobId = sessions.open(bobUuid, JOIN_BOB);
        sessions.heartbeat(bobId, LEAVE_BOB, ONE_HOUR_SECONDS);
        sessions.close(bobId, LEAVE_BOB);

        final Leaderboard board = new SqliteLastLeaveLeaderboard(this.db);
        final List<LeaderboardEntry> top = board.top(1, Set.of());

        assertEquals(1, top.size());
    }
}
