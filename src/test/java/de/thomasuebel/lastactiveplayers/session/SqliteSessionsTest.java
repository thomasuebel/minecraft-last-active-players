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
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteSessionsTest {

    private static final long HEARTBEAT_SECONDS = 600L;
    private static final long TWO_HEARTBEATS_SECONDS = 1200L;
    private static final int SIXTY_SECONDS = 60;

    private static final Instant MARCH_JOIN = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant MARCH_LEAVE = Instant.parse("2026-03-01T11:00:00Z");
    private static final Instant MARCH_BEAT = Instant.parse("2026-04-01T10:10:00Z");
    private static final Instant WINDOW_FEB = Instant.parse("2026-02-01T00:00:00Z");
    private static final Instant WINDOW_APR = Instant.parse("2026-04-01T00:00:00Z");
    private static final Instant WINDOW_MAR_START = Instant.parse("2026-03-01T00:00:00Z");
    private static final Instant WINDOW_MAR_END = Instant.parse("2026-03-02T00:00:00Z");
    private static final Instant OLD_JOIN = Instant.parse("2025-01-01T10:00:00Z");
    private static final Instant OLD_LEAVE = Instant.parse("2025-01-01T11:00:00Z");
    private static final Instant JOIN_BEAT = Instant.parse("2026-04-01T10:00:00Z");

    private Database db;
    private Players players;
    private Sessions sessions;
    private UUID playerUuid;

    @BeforeEach
    void setUp(@TempDir final Path dir) throws IOException {
        this.db = new SqliteDatabase(
            dir.resolve("test.db"), new SqliteMigrations(new InitialSchema())
        );
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
        sessions.close(id, Instant.now().plusSeconds(SIXTY_SECONDS));
        assertTrue(sessions.orphaned().isEmpty());
    }

    @Test
    void closedSessionHasLeaveTime() {
        final long id = sessions.open(playerUuid, MARCH_JOIN);
        sessions.close(id, MARCH_LEAVE);

        final List<Session> found = sessions.activeInWindow(WINDOW_FEB, WINDOW_APR);
        assertEquals(1, found.size());
        assertEquals(Optional.of(MARCH_LEAVE), found.get(0).leaveTime());
    }

    @Test
    void heartbeatAccumulatesDurationSeconds() {
        final long id = sessions.open(playerUuid, Instant.now());
        sessions.heartbeat(id, Instant.now(), HEARTBEAT_SECONDS);
        sessions.heartbeat(id, Instant.now(), HEARTBEAT_SECONDS);
        final Session session = sessions.orphaned().get(0);
        assertEquals(TWO_HEARTBEATS_SECONDS, session.durationSeconds());
    }

    @Test
    void heartbeatUpdatesLastHeartbeat() {
        final long id = sessions.open(playerUuid, JOIN_BEAT);
        sessions.heartbeat(id, MARCH_BEAT, HEARTBEAT_SECONDS);
        assertEquals(MARCH_BEAT, sessions.orphaned().get(0).lastHeartbeat());
    }

    @Test
    void activeInWindowIncludesSessionsInsideRange() {
        final long id = sessions.open(playerUuid, MARCH_JOIN);
        sessions.close(id, MARCH_LEAVE);

        final List<Session> found = sessions.activeInWindow(WINDOW_FEB, WINDOW_APR);
        assertEquals(1, found.size());
    }

    @Test
    void activeInWindowExcludesSessionsOutsideRange() {
        final long id = sessions.open(playerUuid, OLD_JOIN);
        sessions.close(id, OLD_LEAVE);

        final List<Session> found = sessions.activeInWindow(WINDOW_FEB, WINDOW_APR);
        assertTrue(found.isEmpty());
    }

    @Test
    void activeInWindowIncludesOpenSessionsStartedBeforeEnd() {
        sessions.open(playerUuid, MARCH_JOIN);

        final List<Session> found = sessions.activeInWindow(WINDOW_FEB, WINDOW_APR);
        assertEquals(1, found.size());
        assertEquals(Optional.empty(), found.get(0).leaveTime());
    }

    @Test
    void closeOrphansClosesAllOpenSessions() {
        sessions.open(playerUuid, Instant.now());
        sessions.open(playerUuid, Instant.now().minusSeconds(SIXTY_SECONDS));
        assertEquals(2, sessions.orphaned().size());

        sessions.closeOrphans(Instant.now());

        assertTrue(sessions.orphaned().isEmpty());
    }

    @Test
    void closeOrphansDoesNotAffectAlreadyClosedSessions() {
        final long closedId = sessions.open(playerUuid, MARCH_JOIN);
        sessions.close(closedId, MARCH_LEAVE);
        sessions.open(playerUuid, JOIN_BEAT);

        sessions.closeOrphans(Instant.now());

        final List<Session> window = sessions.activeInWindow(WINDOW_MAR_START, WINDOW_MAR_END);
        assertEquals(1, window.size());
        assertEquals(Optional.of(MARCH_LEAVE), window.get(0).leaveTime());
    }
}
