package de.thomasuebel.lastactiveplayers.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteDatabaseTest {

    private static final int SCHEMA_VERSION_ONE = 1;

    private static Database openTestDb(final Path dir) throws IOException {
        return new SqliteDatabase(
            dir.resolve("test.db"),
            new SqliteMigrations(new InitialSchema())
        );
    }

    @Test
    void providesOpenConnectionAfterConstruction(@TempDir final Path dir) throws IOException {
        try (Database db = openTestDb(dir)) {
            assertNotNull(db.connection());
            assertDoesNotThrow(() -> assertFalse(db.connection().isClosed()));
        }
    }

    @Test
    void appliesInitialMigrationAndRecordsVersion(@TempDir final Path dir)
        throws IOException, SQLException {
        try (Database db = openTestDb(dir)) {
            final var rs = db.connection().createStatement()
                .executeQuery("SELECT version FROM schema_version");
            assertTrue(rs.next());
            assertEquals(SCHEMA_VERSION_ONE, rs.getInt("version"));
        }
    }

    @Test
    void createsPlayersTable(@TempDir final Path dir) throws IOException {
        try (Database db = openTestDb(dir)) {
            assertDoesNotThrow(() ->
                db.connection().createStatement().execute(
                    "SELECT uuid, username, streak_days, streak_last_day FROM players"
                )
            );
        }
    }

    @Test
    void createsSessionsTable(@TempDir final Path dir) throws IOException {
        try (Database db = openTestDb(dir)) {
            assertDoesNotThrow(() ->
                db.connection().createStatement().execute(
                    "SELECT id, player_uuid, join_time, leave_time,"
                        + " last_heartbeat, duration_seconds FROM sessions"
                )
            );
        }
    }

    @Test
    void closesConnectionOnClose(@TempDir final Path dir) throws IOException, SQLException {
        final Database db = openTestDb(dir);
        final Connection conn = db.connection();
        db.close();
        assertTrue(conn.isClosed());
    }

    @Test
    void idempotentMigrationOnSecondOpen(@TempDir final Path dir)
        throws IOException, SQLException {
        final Path file = dir.resolve("test.db");
        try (Database db = new SqliteDatabase(file, new SqliteMigrations(new InitialSchema()))) {
            assertFalse(db.connection().isClosed());
        }
        assertDoesNotThrow(() -> {
            try (Database db2 = new SqliteDatabase(
                file, new SqliteMigrations(new InitialSchema())
            )) {
                final var rs = db2.connection().createStatement()
                    .executeQuery("SELECT version FROM schema_version");
                assertTrue(rs.next());
                assertEquals(SCHEMA_VERSION_ONE, rs.getInt("version"));
            }
        });
    }
}
