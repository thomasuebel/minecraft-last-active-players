package de.thomasuebel.lastactiveplayers.session;

import de.thomasuebel.lastactiveplayers.db.Database;
import de.thomasuebel.lastactiveplayers.db.InitialSchema;
import de.thomasuebel.lastactiveplayers.db.SqliteDatabase;
import de.thomasuebel.lastactiveplayers.db.SqliteMigrations;
import de.thomasuebel.lastactiveplayers.player.Players;
import de.thomasuebel.lastactiveplayers.player.SqlitePlayers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteSessionsTest {

    private Database db;
    private Players players;
    private Sessions sessions;
    private UUID playerUuid;

    @BeforeEach
    void setUp(@TempDir final Path dir) throws IOException {
        this.db = new SqliteDatabase(dir.resolve("test.db"), new SqliteMigrations(new InitialSchema()));
        this.players = new SqlitePlayers(this.db);
        this.sessions = new SqliteSessions(this.db);
        this.playerUuid = UUID.randomUUID();
        this.players.upsert(this.playerUuid, "Alice");
    }

    @AfterEach
    void tearDown() throws IOException {
        this.db.close();
    }

    @Test
    void openReturnsPositiveSessionId() {
        assertTrue(sessions.open(playerUuid, Instant.now()) > 0);
    }

    @Test
    void openedSessionAppearsInOrphaned() {
        final long id = sessions.open(playerUuid, Instant.now());
        final List<Session> orphans = sessions.orphaned();
        assertEquals(1, orphans.size());
        assertEquals(id, orphans.get(0).id());
    }

    @Test
    void closedSessionDisappearsFromOrphaned() {
        final long id = sessions.open(playerUuid, Instant.now());
        sessions.close(id, Instant.now().plusSeconds(60));
        assertTrue(sessions.orphaned().isEmpty());
    }

    @Test
    void closedSessionHasLeaveTime() {
        final Instant join = Instant.parse("2026-04-01T10:00:00Z");
        final Instant leave = Instant.parse("2026-04-01T11:00:00Z");
        final long id = sessions.open(playerUuid, join);
        sessions.close(id, leave);

        final List<Session> found = sessions.activeInWindow(
            Instant.parse("2026-04-01T00:00:00Z"),
            Instant.parse("2026-04-02T00:00:00Z")
        );
        assertEquals(1, found.size());
        assertEquals(Optional.of(leave), found.get(0).leaveTime());
    }

    @Test
    void heartbeatAccumulatesDurationSeconds() {
        final long id = sessions.open(playerUuid, Instant.now());
        sessions.heartbeat(id, Instant.now(), 600L);
        sessions.heartbeat(id, Instant.now(), 600L);
        final Session session = sessions.orphaned().get(0);
        assertEquals(1200L, session.durationSeconds());
    }

    @Test
    void heartbeatUpdatesLastHeartbeat() {
        final Instant join = Instant.parse("2026-04-01T10:00:00Z");
        final Instant beat = Instant.parse("2026-04-01T10:10:00Z");
        final long id = sessions.open(playerUuid, join);
        sessions.heartbeat(id, beat, 600L);
        assertEquals(beat, sessions.orphaned().get(0).lastHeartbeat());
    }

    @Test
    void activeInWindowIncludesSessionsInsideRange() {
        final Instant join = Instant.parse("2026-03-01T10:00:00Z");
        final long id = sessions.open(playerUuid, join);
        sessions.close(id, Instant.parse("2026-03-01T11:00:00Z"));

        final List<Session> found = sessions.activeInWindow(
            Instant.parse("2026-02-01T00:00:00Z"),
            Instant.parse("2026-04-01T00:00:00Z")
        );
        assertEquals(1, found.size());
    }

    @Test
    void activeInWindowExcludesSessionsOutsideRange() {
        final Instant join = Instant.parse("2025-01-01T10:00:00Z");
        final long id = sessions.open(playerUuid, join);
        sessions.close(id, Instant.parse("2025-01-01T11:00:00Z"));

        final List<Session> found = sessions.activeInWindow(
            Instant.parse("2026-02-01T00:00:00Z"),
            Instant.parse("2026-04-01T00:00:00Z")
        );
        assertTrue(found.isEmpty());
    }

    @Test
    void activeInWindowIncludesOpenSessionsStartedBeforeEnd() {
        sessions.open(playerUuid, Instant.parse("2026-03-15T10:00:00Z"));

        final List<Session> found = sessions.activeInWindow(
            Instant.parse("2026-03-01T00:00:00Z"),
            Instant.parse("2026-04-01T00:00:00Z")
        );
        assertEquals(1, found.size());
        assertEquals(Optional.empty(), found.get(0).leaveTime());
    }

    @Test
    void closeOrphansClosesAllOpenSessions() {
        sessions.open(playerUuid, Instant.now());
        sessions.open(playerUuid, Instant.now().minusSeconds(60));
        assertEquals(2, sessions.orphaned().size());

        sessions.closeOrphans(Instant.now());

        assertTrue(sessions.orphaned().isEmpty());
    }

    @Test
    void closeOrphansDoesNotAffectAlreadyClosedSessions() {
        final long closedId = sessions.open(playerUuid, Instant.parse("2026-03-01T10:00:00Z"));
        sessions.close(closedId, Instant.parse("2026-03-01T11:00:00Z"));
        sessions.open(playerUuid, Instant.parse("2026-04-01T10:00:00Z"));

        sessions.closeOrphans(Instant.now());

        final List<Session> window = sessions.activeInWindow(
            Instant.parse("2026-03-01T00:00:00Z"),
            Instant.parse("2026-03-02T00:00:00Z")
        );
        assertEquals(1, window.size());
        assertEquals(Optional.of(Instant.parse("2026-03-01T11:00:00Z")), window.get(0).leaveTime());
    }
}
