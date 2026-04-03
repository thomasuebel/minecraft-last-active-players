package de.thomasuebel.lastactiveplayers.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A single, versioned schema migration step.
 *
 * <p>Migrations are applied in ascending version order by {@link Migrations}.
 * Each migration is applied at most once; the applied version is tracked in
 * a {@code schema_version} table.
 */
public interface Migration {

    /**
     * Returns the version number of this migration.
     *
     * <p>Version numbers must be positive and unique across all migrations.
     *
     * @return positive migration version
     */
    int version();

    /**
     * Applies this migration to the given connection.
     *
     * @param connection the open database connection; never null
     * @throws SQLException if any SQL statement fails
     */
    void applyTo(Connection connection) throws SQLException;
}
