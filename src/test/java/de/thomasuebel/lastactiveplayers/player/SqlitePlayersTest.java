package de.thomasuebel.lastactiveplayers.player;

import de.thomasuebel.lastactiveplayers.db.Database;
import de.thomasuebel.lastactiveplayers.db.InitialSchema;
import de.thomasuebel.lastactiveplayers.db.SqliteDatabase;
import de.thomasuebel.lastactiveplayers.db.SqliteMigrations;
import de.thomasuebel.lastactiveplayers.session.Sessions;
import de.thomasuebel.lastactiveplayers.session.SqliteSessions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlitePlayersTest {

    private Database db;
    private Players players;
    private Sessions sessions;

    @BeforeEach
    void setUp(@TempDir final Path dir) throws IOException {
        this.db = new SqliteDatabase(dir.resolve("test.db"), new SqliteMigrations(new InitialSchema()));
        this.players = new SqlitePlayers(this.db);
        this.sessions = new SqliteSessions(this.db);
    }

    @AfterEach
    void tearDown() throws IOException {
        this.db.close();
    }

    @Test
    void returnsNoPlayerWhenUuidNotFound() {
        assertFalse(players.withUuid(UUID.randomUUID()).exists());
    }

    @Test
    void upsertCreatesPlayerRecord() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Alice");
        final Player found = players.withUuid(uuid);
        assertTrue(found.exists());
        assertEquals("Alice", found.username());
        assertEquals(uuid, found.uuid());
    }

    @Test
    void upsertUpdatesUsernameOnConflict() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Alice");
        players.upsert(uuid, "AliceRenamed");
        assertEquals("AliceRenamed", players.withUuid(uuid).username());
    }

    @Test
    void newPlayerHasZeroStreak() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Bob");
        final Player found = players.withUuid(uuid);
        assertEquals(0, found.streakDays());
        assertEquals(Optional.empty(), found.streakLastDay());
    }

    @Test
    void updateStreakPersistsStreakAndDate() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Bob");
        final LocalDate date = LocalDate.of(2026, 4, 1);
        players.updateStreak(uuid, 7, Optional.of(date));
        final Player found = players.withUuid(uuid);
        assertEquals(7, found.streakDays());
        assertEquals(Optional.of(date), found.streakLastDay());
    }

    @Test
    void updateStreakClearsDateWhenEmpty() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Bob");
        players.updateStreak(uuid, 5, Optional.of(LocalDate.of(2026, 3, 1)));
        players.updateStreak(uuid, 0, Optional.empty());
        assertEquals(0, players.withUuid(uuid).streakDays());
        assertEquals(Optional.empty(), players.withUuid(uuid).streakLastDay());
    }

    @Test
    void purgesPlayersWithOnlyOldSessions() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Charlie");
        final Instant oldJoin = Instant.now().minus(62, ChronoUnit.DAYS);
        final long sessionId = sessions.open(uuid, oldJoin);
        sessions.close(sessionId, oldJoin.plusSeconds(3600));

        players.purgeInactiveBefore(Instant.now().minus(60, ChronoUnit.DAYS));

        assertFalse(players.withUuid(uuid).exists());
    }

    @Test
    void doesNotPurgePlayersWithRecentSessions() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Dave");
        final long sessionId = sessions.open(uuid, Instant.now().minus(1, ChronoUnit.DAYS));
        sessions.close(sessionId, Instant.now());

        players.purgeInactiveBefore(Instant.now().minus(60, ChronoUnit.DAYS));

        assertTrue(players.withUuid(uuid).exists());
    }

    @Test
    void doesNotPurgePlayersWithOpenSessions() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Eve");
        sessions.open(uuid, Instant.now().minus(90, ChronoUnit.DAYS));

        players.purgeInactiveBefore(Instant.now().minus(60, ChronoUnit.DAYS));

        assertTrue(players.withUuid(uuid).exists());
    }
}
