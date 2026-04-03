package de.thomasuebel.lastactiveplayers.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * The initial (V1) schema migration.
 *
 * <p>Creates the {@code players} and {@code sessions} tables if they do not yet exist.
 */
public final class InitialSchema implements Migration {

    private static final int VERSION = 1;

    private static final String CREATE_PLAYERS = """
        CREATE TABLE IF NOT EXISTS players (
            uuid            TEXT PRIMARY KEY,
            username        TEXT NOT NULL,
            streak_days     INTEGER NOT NULL DEFAULT 0,
            streak_last_day TEXT
        )
        """;

    private static final String CREATE_SESSIONS = """
        CREATE TABLE IF NOT EXISTS sessions (
            id               INTEGER PRIMARY KEY AUTOINCREMENT,
            player_uuid      TEXT NOT NULL REFERENCES players(uuid) ON DELETE CASCADE,
            join_time        TEXT NOT NULL,
            leave_time       TEXT,
            last_heartbeat   TEXT NOT NULL,
            duration_seconds INTEGER NOT NULL DEFAULT 0
        )
        """;

    private static final String IDX_SESSIONS_PLAYER =
        "CREATE INDEX IF NOT EXISTS idx_sessions_player_uuid ON sessions(player_uuid)";

    private static final String IDX_SESSIONS_LEAVE =
        "CREATE INDEX IF NOT EXISTS idx_sessions_leave_time ON sessions(leave_time)";

    @Override
    public int version() {
        return VERSION;
    }

    @Override
    public void applyTo(final Connection connection) throws SQLException {
        try (var stmt = connection.createStatement()) {
            stmt.execute(CREATE_PLAYERS);
        }
        try (var stmt = connection.createStatement()) {
            stmt.execute(CREATE_SESSIONS);
        }
        try (var stmt = connection.createStatement()) {
            stmt.execute(IDX_SESSIONS_PLAYER);
        }
        try (var stmt = connection.createStatement()) {
            stmt.execute(IDX_SESSIONS_LEAVE);
        }
    }
}
