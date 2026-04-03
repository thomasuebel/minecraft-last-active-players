package de.thomasuebel.lastactiveplayers.db;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.JournalMode;
import org.sqlite.SQLiteConfig.SynchronousMode;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * A SQLite-backed {@link Database} that opens a JDBC connection on construction,
 * configures WAL journaling via {@link SQLiteConfig}, and applies schema migrations.
 *
 * <p>Using {@link SQLiteConfig} to set pragmas before opening the connection avoids
 * JDBC driver issues with executing result-set-returning pragmas after connection open.
 *
 * <p>Note: opening a database connection in the constructor is a deliberate exception
 * to the Elegant Objects "constructor only assigns" rule. The object IS the connection;
 * lazy initialisation would require mutable state and add unnecessary complexity.
 */
public final class SqliteDatabase implements Database {

    private final Connection connection;

    /**
     * Opens a SQLite database at the given file path and applies pending migrations.
     *
     * @param file       path to the {@code .db} file; created if it does not exist
     * @param migrations the migration runner to apply on first open; never null
     * @throws IOException if the connection or migrations fail
     */
    public SqliteDatabase(final Path file, final Migrations migrations) throws IOException {
        try {
            final SQLiteConfig config = new SQLiteConfig();
            config.setJournalMode(JournalMode.WAL);
            config.setSynchronous(SynchronousMode.NORMAL);
            config.enforceForeignKeys(true);
            this.connection = config.createConnection("jdbc:sqlite:" + file.toAbsolutePath());
            migrations.applyTo(this.connection);
        } catch (final SQLException exception) {
            throw new IOException(exception);
        }
    }

    @Override
    public Connection connection() {
        return this.connection;
    }

    @Override
    public void close() throws IOException {
        try {
            this.connection.close();
        } catch (final SQLException exception) {
            throw new IOException(exception);
        }
    }
}
