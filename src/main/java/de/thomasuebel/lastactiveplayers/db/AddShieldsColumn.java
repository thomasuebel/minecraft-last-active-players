package de.thomasuebel.lastactiveplayers.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * V2 schema migration: adds the {@code streak_shields} column to the {@code players} table.
 *
 * <p>Existing rows receive a default value of {@code 0}.
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
