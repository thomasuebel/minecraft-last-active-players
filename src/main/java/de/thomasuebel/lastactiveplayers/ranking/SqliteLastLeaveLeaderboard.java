package de.thomasuebel.lastactiveplayers.ranking;

import de.thomasuebel.lastactiveplayers.db.Database;
import de.thomasuebel.lastactiveplayers.db.DatabaseException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * SQLite-backed {@link Leaderboard} sorted by the most recent leave time, descending.
 *
 * <p>Only players who have at least one closed session are included.
 * Online-player exclusion is applied in Java after the query.
 */
public final class SqliteLastLeaveLeaderboard implements Leaderboard {

    private static final String QUERY = """
        SELECT p.uuid, p.username,
               SUM(s.duration_seconds) AS total_seconds,
               MAX(s.leave_time) AS last_leave
        FROM players p
        JOIN sessions s ON s.player_uuid = p.uuid
        WHERE s.leave_time IS NOT NULL
        GROUP BY p.uuid, p.username
        ORDER BY last_leave DESC
        """;

    private final Database database;

    /**
     * Constructs a leaderboard backed by the given database.
     *
     * @param database the open database; never null
     */
    public SqliteLastLeaveLeaderboard(final Database database) {
        this.database = database;
    }

    @Override
    public List<LeaderboardEntry> top(final int limit, final Set<UUID> exclude) {
        try (PreparedStatement stmt = this.database.connection().prepareStatement(QUERY)) {
            try (ResultSet rs = stmt.executeQuery()) {
                return mapEntries(rs, limit, exclude);
            }
        } catch (final SQLException exception) {
            throw new DatabaseException(exception);
        }
    }

    private static List<LeaderboardEntry> mapEntries(
        final ResultSet rs, final int limit, final Set<UUID> exclude
    ) throws SQLException {
        final List<LeaderboardEntry> result = new ArrayList<>();
        while (rs.next() && result.size() < limit) {
            final UUID uuid = UUID.fromString(rs.getString("uuid"));
            if (exclude.contains(uuid)) {
                continue;
            }
            result.add(new StoredEntry(
                uuid,
                rs.getString("username"),
                rs.getLong("total_seconds"),
                Optional.of(Instant.parse(rs.getString("last_leave")))
            ));
        }
        return result;
    }
}
