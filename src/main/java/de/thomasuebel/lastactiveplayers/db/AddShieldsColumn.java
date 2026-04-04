package de.thomasuebel.lastactiveplayers.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * V2 schema migration: adds the {@code streak_shields} column to the {@code players} table.
 *
 * <p>Existing rows receive a default value of {@code 0}.
 *
 * <p>Idempotency is guaranteed by {@link SqliteMigrations}: the runner only applies
 * migrations whose version number exceeds the stored {@code schema_version}, so this
 * migration will never be executed twice on the same database.
 */
public final class AddShieldsColumn implements Migration {

    private static final int VERSION = 2;

    private static final String ALTER_PLAYERS =
        "ALTER TABLE players ADD COLUMN streak_shields INTEGER NOT NULL DEFAULT 0";

    @Override
    public int version() {
        return VERSION;
    }

    @Override
    public void applyTo(final Connection connection) throws SQLException {
        try (var stmt = connection.createStatement()) {
            stmt.execute(ALTER_PLAYERS);
        }
    }
}
