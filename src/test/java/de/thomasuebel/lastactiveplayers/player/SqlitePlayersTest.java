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
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlitePlayersTest {

    private static final int SEVEN_DAY_STREAK = 7;
    private static final int FIVE_DAY_STREAK = 5;
    private static final int PURGE_THRESHOLD_DAYS = 60;
    private static final int OLD_SESSION_DAYS = 62;
    private static final int VERY_OLD_SESSION_DAYS = 90;
    private static final int SESSION_DURATION_SECONDS = 3600;
    private static final LocalDate STREAK_DATE = LocalDate.of(2026, Month.APRIL, 1);
    private static final LocalDate EARLIER_STREAK_DATE = LocalDate.of(2026, Month.MARCH, 1);

    private Database db;
    private Players players;
    private Sessions sessions;

    @BeforeEach
    void setUp(@TempDir final Path dir) throws IOException {
        this.db = new SqliteDatabase(
            dir.resolve("test.db"), new SqliteMigrations(new InitialSchema())
        );
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
        players.updateStreak(uuid, SEVEN_DAY_STREAK, Optional.of(STREAK_DATE));
        final Player found = players.withUuid(uuid);
        assertEquals(SEVEN_DAY_STREAK, found.streakDays());
        assertEquals(Optional.of(STREAK_DATE), found.streakLastDay());
    }

    @Test
    void updateStreakClearsDateWhenEmpty() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Bob");
        players.updateStreak(uuid, FIVE_DAY_STREAK, Optional.of(EARLIER_STREAK_DATE));
        players.updateStreak(uuid, 0, Optional.empty());
        assertEquals(0, players.withUuid(uuid).streakDays());
        assertEquals(Optional.empty(), players.withUuid(uuid).streakLastDay());
    }

    @Test
    void purgesPlayersWithOnlyOldSessions() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Charlie");
        final Instant oldJoin = Instant.now().minus(OLD_SESSION_DAYS, ChronoUnit.DAYS);
        final long sessionId = sessions.open(uuid, oldJoin);
        sessions.close(sessionId, oldJoin.plusSeconds(SESSION_DURATION_SECONDS));

        players.purgeInactiveBefore(Instant.now().minus(PURGE_THRESHOLD_DAYS, ChronoUnit.DAYS));

        assertFalse(players.withUuid(uuid).exists());
    }

    @Test
    void doesNotPurgePlayersWithRecentSessions() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Dave");
        final long sessionId = sessions.open(uuid, Instant.now().minus(1, ChronoUnit.DAYS));
        sessions.close(sessionId, Instant.now());

        players.purgeInactiveBefore(Instant.now().minus(PURGE_THRESHOLD_DAYS, ChronoUnit.DAYS));

        assertTrue(players.withUuid(uuid).exists());
    }

    @Test
    void withHighestStreakReturnsNoPlayerWhenNoneHaveStreak() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Alice");
        assertFalse(players.withHighestStreak().exists());
    }

    @Test
    void withHighestStreakReturnsPlayerWithMostDays() {
        final UUID aliceUuid = UUID.randomUUID();
        final UUID bobUuid = UUID.randomUUID();
        players.upsert(aliceUuid, "Alice");
        players.upsert(bobUuid, "Bob");
        players.updateStreak(aliceUuid, FIVE_DAY_STREAK, Optional.of(STREAK_DATE));
        players.updateStreak(bobUuid, SEVEN_DAY_STREAK, Optional.of(STREAK_DATE));
        final Player leader = players.withHighestStreak();
        assertTrue(leader.exists());
        assertEquals(bobUuid, leader.uuid());
        assertEquals(SEVEN_DAY_STREAK, leader.streakDays());
    }

    @Test
    void purgesPlayerWhoNeverHadSessions() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Frank");

        players.purgeInactiveBefore(Instant.now().minus(PURGE_THRESHOLD_DAYS, ChronoUnit.DAYS));

        assertFalse(players.withUuid(uuid).exists());
    }

    @Test
    void retainsPlayerWhoseLastSessionEndedAtThreshold() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Grace");
        final Instant threshold = Instant.now().minus(PURGE_THRESHOLD_DAYS, ChronoUnit.DAYS);
        final long sessionId = sessions.open(uuid, threshold.minus(1, ChronoUnit.HOURS));
        sessions.close(sessionId, threshold);

        players.purgeInactiveBefore(threshold);

        assertTrue(players.withUuid(uuid).exists());
    }

    @Test
    void doesNotPurgePlayersWithOpenSessions() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Eve");
        sessions.open(uuid, Instant.now().minus(VERY_OLD_SESSION_DAYS, ChronoUnit.DAYS));

        players.purgeInactiveBefore(Instant.now().minus(PURGE_THRESHOLD_DAYS, ChronoUnit.DAYS));

        assertTrue(players.withUuid(uuid).exists());
    }
}
