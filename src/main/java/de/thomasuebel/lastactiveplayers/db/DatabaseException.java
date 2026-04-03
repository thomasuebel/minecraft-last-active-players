package de.thomasuebel.lastactiveplayers.db;

/**
 * Unchecked wrapper for {@link java.sql.SQLException} thrown by repository operations.
 *
 * <p>Bukkit event handlers cannot propagate checked exceptions, so SQL failures are
 * wrapped here to allow them to surface without forcing every caller to declare
 * {@code throws SQLException}.
 */
public final class DatabaseException extends RuntimeException {

    /**
     * Constructs a new exception wrapping the given cause.
     *
     * @param cause the underlying SQL failure; never null
     */
    public DatabaseException(final Throwable cause) {
        super(cause);
    }
}
