package de.thomasuebel.lastactiveplayers.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * An ordered collection of {@link Migration} steps that can be applied to a database.
 *
 * <p>Implementations track which migrations have already been applied and skip them
 * on subsequent calls, making {@link #applyTo(Connection)} idempotent.
 */
public interface Migrations {

    /**
     * Applies any pending migrations to the given connection.
     *
     * <p>Already-applied migrations (as tracked by the {@code schema_version} table)
     * are skipped. This method is safe to call on every plugin startup.
     *
     * @param connection the open database connection; never null
     * @throws SQLException if any SQL statement fails
     */
    void applyTo(Connection connection) throws SQLException;
}
