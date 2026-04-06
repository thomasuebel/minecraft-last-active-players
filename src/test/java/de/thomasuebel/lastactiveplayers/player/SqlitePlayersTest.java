package de.thomasuebel.lastactiveplayers.player;

import de.thomasuebel.lastactiveplayers.db.AddShieldsColumn;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
            dir.resolve("test.db"),
            new SqliteMigrations(new InitialSchema(), new AddShieldsColumn())
        );
        this.players = new SqlitePlayers(this.db);
        this.sessions = new SqliteSessions(this.db);
    }

    @AfterEach
    void tearDown() throws IOException {
        this.db.close();
    }

    @Test
    void returnsEmptyWhenUuidNotFound() {
        assertTrue(players.withUuid(UUID.randomUUID()).isEmpty());
    }

    @Test
    void upsertCreatesPlayerRecord() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Alice");
        final PlayerRecord found = players.withUuid(uuid).orElseThrow();
        assertEquals("Alice", found.username());
        assertEquals(uuid, found.uuid());
    }

    @Test
    void upsertUpdatesUsernameOnConflict() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Alice");
        players.upsert(uuid, "AliceRenamed");
        assertEquals("AliceRenamed", players.withUuid(uuid).orElseThrow().username());
    }

    @Test
    void newPlayerHasZeroStreak() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Bob");
        final PlayerRecord found = players.withUuid(uuid).orElseThrow();
        assertEquals(0, found.streakDays());
        assertEquals(Optional.empty(), found.streakLastDay());
    }

    @Test
    void updateStreakPersistsStreakAndDate() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Bob");
        players.updateStreak(uuid, SEVEN_DAY_STREAK, Optional.of(STREAK_DATE));
        final PlayerRecord found = players.withUuid(uuid).orElseThrow();
        assertEquals(SEVEN_DAY_STREAK, found.streakDays());
        assertEquals(Optional.of(STREAK_DATE), found.streakLastDay());
    }

    @Test
    void updateStreakClearsDateWhenEmpty() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Bob");
        players.updateStreak(uuid, FIVE_DAY_STREAK, Optional.of(EARLIER_STREAK_DATE));
        players.updateStreak(uuid, 0, Optional.empty());
        assertEquals(0, players.withUuid(uuid).orElseThrow().streakDays());
        assertEquals(Optional.empty(), players.withUuid(uuid).orElseThrow().streakLastDay());
    }

    @Test
    void purgesPlayersWithOnlyOldSessions() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Charlie");
        final Instant oldJoin = Instant.now().minus(OLD_SESSION_DAYS, ChronoUnit.DAYS);
        final long sessionId = sessions.open(uuid, oldJoin);
        sessions.close(sessionId, oldJoin.plusSeconds(SESSION_DURATION_SECONDS));

        players.purgeInactiveBefore(Instant.now().minus(PURGE_THRESHOLD_DAYS, ChronoUnit.DAYS));

        assertTrue(players.withUuid(uuid).isEmpty());
    }

    @Test
    void doesNotPurgePlayersWithRecentSessions() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Dave");
        final long sessionId = sessions.open(uuid, Instant.now().minus(1, ChronoUnit.DAYS));
        sessions.close(sessionId, Instant.now());

        players.purgeInactiveBefore(Instant.now().minus(PURGE_THRESHOLD_DAYS, ChronoUnit.DAYS));

        assertTrue(players.withUuid(uuid).isPresent());
    }

    @Test
    void withHighestStreakReturnsEmptyWhenNoneHaveStreak() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Alice");
        assertTrue(players.withHighestStreak().isEmpty());
    }

    @Test
    void withHighestStreakReturnsPlayerWithMostDays() {
        final UUID aliceUuid = UUID.randomUUID();
        final UUID bobUuid = UUID.randomUUID();
        players.upsert(aliceUuid, "Alice");
        players.upsert(bobUuid, "Bob");
        players.updateStreak(aliceUuid, FIVE_DAY_STREAK, Optional.of(STREAK_DATE));
        players.updateStreak(bobUuid, SEVEN_DAY_STREAK, Optional.of(STREAK_DATE));
        final PlayerRecord leader = players.withHighestStreak().orElseThrow();
        assertEquals(bobUuid, leader.uuid());
        assertEquals(SEVEN_DAY_STREAK, leader.streakDays());
    }

    @Test
    void purgesPlayerWhoNeverHadSessions() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Frank");

        players.purgeInactiveBefore(Instant.now().minus(PURGE_THRESHOLD_DAYS, ChronoUnit.DAYS));

        assertTrue(players.withUuid(uuid).isEmpty());
    }

    @Test
    void retainsPlayerWhoseLastSessionEndedAtThreshold() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Grace");
        final Instant threshold = Instant.now().minus(PURGE_THRESHOLD_DAYS, ChronoUnit.DAYS);
        final long sessionId = sessions.open(uuid, threshold.minus(1, ChronoUnit.HOURS));
        sessions.close(sessionId, threshold);

        players.purgeInactiveBefore(threshold);

        assertTrue(players.withUuid(uuid).isPresent());
    }

    @Test
    void doesNotPurgePlayersWithOpenSessions() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Eve");
        sessions.open(uuid, Instant.now().minus(VERY_OLD_SESSION_DAYS, ChronoUnit.DAYS));

        players.purgeInactiveBefore(Instant.now().minus(PURGE_THRESHOLD_DAYS, ChronoUnit.DAYS));

        assertTrue(players.withUuid(uuid).isPresent());
    }

    @Test
    void withTopStreakReturnsEmptyWhenNoPlayerHasStreak() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Alice");

        assertTrue(players.withTopStreak().isEmpty());
    }

    @Test
    void withTopStreakReturnsSingleLeaderWhenUnique() {
        final UUID aliceUuid = UUID.randomUUID();
        final UUID bobUuid = UUID.randomUUID();
        players.upsert(aliceUuid, "Alice");
        players.upsert(bobUuid, "Bob");
        players.updateStreak(aliceUuid, SEVEN_DAY_STREAK, Optional.of(STREAK_DATE));
        players.updateStreak(bobUuid, FIVE_DAY_STREAK, Optional.of(STREAK_DATE));

        final List<PlayerRecord> top = players.withTopStreak();

        assertEquals(1, top.size());
        assertEquals(aliceUuid, top.get(0).uuid());
    }

    @Test
    void withTopStreakReturnsBothPlayersWhenTied() {
        final UUID aliceUuid = UUID.randomUUID();
        final UUID bobUuid = UUID.randomUUID();
        players.upsert(aliceUuid, "Alice");
        players.upsert(bobUuid, "Bob");
        players.updateStreak(aliceUuid, SEVEN_DAY_STREAK, Optional.of(STREAK_DATE));
        players.updateStreak(bobUuid, SEVEN_DAY_STREAK, Optional.of(STREAK_DATE));

        final List<PlayerRecord> top = players.withTopStreak();

        assertEquals(2, top.size());
    }

    @Test
    void shieldsDefaultsToZeroForNewPlayer() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Alice");
        assertEquals(0, players.shields(uuid));
    }

    @Test
    void storeShieldsPersistsCount() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Alice");
        players.storeShields(uuid, 2);
        assertEquals(2, players.shields(uuid));
    }

    @Test
    void storeShieldsOverwritesPreviousValue() {
        final UUID uuid = UUID.randomUUID();
        players.upsert(uuid, "Alice");
        players.storeShields(uuid, 3);
        players.storeShields(uuid, 1);
        assertEquals(1, players.shields(uuid));
    }

    @Test
    void shieldsReturnsZeroForUnknownPlayer() {
        assertEquals(0, players.shields(UUID.randomUUID()));
    }
}
