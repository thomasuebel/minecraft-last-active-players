package de.thomasuebel.lastactiveplayers.db;

import java.io.Closeable;
import java.sql.Connection;

/**
 * A relational database connection used by the plugin's persistence layer.
 *
 * <p>Implementations are responsible for opening the connection, applying schema
 * migrations, and configuring database-level pragmas. Callers obtain the raw
 * {@link Connection} and should not close it directly; use {@link #close()} instead.
 */
public interface Database extends Closeable {

    /**
     * Returns the underlying JDBC connection.
     *
     * @return the active connection; never null
     */
    Connection connection();
}
