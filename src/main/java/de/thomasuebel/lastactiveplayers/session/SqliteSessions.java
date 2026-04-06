package de.thomasuebel.lastactiveplayers.session;

import de.thomasuebel.lastactiveplayers.db.Database;
import de.thomasuebel.lastactiveplayers.db.DatabaseException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SQLite-backed implementation of {@link Sessions}.
 */
public final class SqliteSessions implements Sessions {

    private static final String INSERT_SESSION = """
        INSERT INTO sessions (player_uuid, join_time, last_heartbeat, duration_seconds)
        VALUES (?, ?, ?, 0)
        """;

    private static final String CLOSE_SESSION =
        "UPDATE sessions SET leave_time = ? WHERE id = ?";

    private static final String HEARTBEAT = """
        UPDATE sessions SET last_heartbeat = ?, duration_seconds = duration_seconds + ?
        WHERE id = ?
        """;

    private static final String ACTIVE_IN_WINDOW = """
        SELECT id, player_uuid, join_time, leave_time, last_heartbeat, duration_seconds
        FROM sessions
        WHERE join_time < ? AND (leave_time > ? OR leave_time IS NULL)
        """;

    private static final String ORPHANED = """
        SELECT id, player_uuid, join_time, leave_time, last_heartbeat, duration_seconds
        FROM sessions WHERE leave_time IS NULL
        """;

    private static final String CLOSE_ORPHANS =
        "UPDATE sessions SET leave_time = ? WHERE leave_time IS NULL";

    private final Database database;

    /**
     * Constructs a repository backed by the given database.
     *
     * @param database the open database; never null
     */
    public SqliteSessions(final Database database) {
        this.database = database;
    }

    @Override
    public long open(final UUID playerUuid, final Instant joinTime) {
        try (PreparedStatement stmt = this.database.connection().prepareStatement(
            INSERT_SESSION, Statement.RETURN_GENERATED_KEYS
        )) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, joinTime.toString());
            stmt.setString(3, joinTime.toString());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (final SQLException exception) {
            throw new DatabaseException(exception);
        }
    }

    @Override
    public void close(final long sessionId, final Instant leaveTime) {
        try (PreparedStatement stmt = this.database.connection().prepareStatement(CLOSE_SESSION)) {
            stmt.setString(1, leaveTime.toString());
            stmt.setLong(2, sessionId);
            stmt.executeUpdate();
        } catch (final SQLException exception) {
            throw new DatabaseException(exception);
        }
    }

    @Override
    public void heartbeat(final long sessionId, final Instant now, final long additionalSeconds) {
        try (PreparedStatement stmt = this.database.connection().prepareStatement(HEARTBEAT)) {
            stmt.setString(1, now.toString());
            stmt.setLong(2, additionalSeconds);
            stmt.setLong(3, sessionId);
            stmt.executeUpdate();
        } catch (final SQLException exception) {
            throw new DatabaseException(exception);
        }
    }

    @Override
    public List<Session> activeInWindow(final Instant start, final Instant end) {
        try (PreparedStatement stmt =
                 this.database.connection().prepareStatement(ACTIVE_IN_WINDOW)) {
            stmt.setString(1, end.toString());
            stmt.setString(2, start.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return mapSessions(rs);
            }
        } catch (final SQLException exception) {
            throw new DatabaseException(exception);
        }
    }

    @Override
    public List<Session> orphaned() {
        try (PreparedStatement stmt = this.database.connection().prepareStatement(ORPHANED)) {
            try (ResultSet rs = stmt.executeQuery()) {
                return mapSessions(rs);
            }
        } catch (final SQLException exception) {
            throw new DatabaseException(exception);
        }
    }

    @Override
    public void closeOrphans(final Instant effectiveLeaveTime) {
        try (PreparedStatement stmt = this.database.connection().prepareStatement(CLOSE_ORPHANS)) {
            stmt.setString(1, effectiveLeaveTime.toString());
            stmt.executeUpdate();
        } catch (final SQLException exception) {
            throw new DatabaseException(exception);
        }
    }

    private List<Session> mapSessions(final ResultSet rs) throws SQLException {
        final List<Session> result = new ArrayList<>();
        while (rs.next()) {
            final String leaveTimeStr = rs.getString("leave_time");
            result.add(new Session(
                rs.getLong("id"),
                UUID.fromString(rs.getString("player_uuid")),
                Instant.parse(rs.getString("join_time")),
                Optional.ofNullable(leaveTimeStr).map(Instant::parse),
                Instant.parse(rs.getString("last_heartbeat")),
                rs.getLong("duration_seconds")
            ));
        }
        return result;
    }
}
