package de.thomasuebel.lastactiveplayers.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * Applies a sequence of {@link Migration} steps to a SQLite database.
 *
 * <p>A {@code schema_version} table (single integer row) tracks the highest applied
 * version. On each call to {@link #applyTo(Connection)}, only migrations with a version
 * greater than the current value are executed, making the operation idempotent.
 */
public final class SqliteMigrations implements Migrations {

    private static final String CREATE_VERSION_TABLE =
        "CREATE TABLE IF NOT EXISTS schema_version (version INTEGER NOT NULL DEFAULT 0)";

    private static final String ENSURE_VERSION_ROW =
        "INSERT OR IGNORE INTO schema_version (version) VALUES (0)";

    private static final String GET_VERSION =
        "SELECT version FROM schema_version";

    private static final String SET_VERSION =
        "UPDATE schema_version SET version = ?";

    private final List<Migration> migrations;

    /**
     * Constructs a migration runner for the given steps.
     *
     * @param migrations the ordered migration steps to apply; never null
     */
    public SqliteMigrations(final Migration... migrations) {
        this.migrations = Arrays.asList(migrations);
    }

    @Override
    public void applyTo(final Connection connection) throws SQLException {
        try (var stmt = connection.createStatement()) {
            stmt.execute(CREATE_VERSION_TABLE);
        }
        try (var stmt = connection.createStatement()) {
            stmt.execute(ENSURE_VERSION_ROW);
        }
        final int current = currentVersion(connection);
        for (final Migration migration : this.migrations) {
            if (migration.version() > current) {
                connection.setAutoCommit(false);
                try {
                    migration.applyTo(connection);
                    setVersion(connection, migration.version());
                    connection.commit();
                } catch (final SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        }
    }

    private int currentVersion(final Connection connection) throws SQLException {
        try (var stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(GET_VERSION)) {
            return rs.next() ? rs.getInt("version") : 0;
        }
    }

    private void setVersion(final Connection connection, final int version) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(SET_VERSION)) {
            stmt.setInt(1, version);
            stmt.executeUpdate();
        }
    }
}
