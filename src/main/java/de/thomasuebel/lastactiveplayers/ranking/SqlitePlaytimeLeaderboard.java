package de.thomasuebel.lastactiveplayers.ranking;

import de.thomasuebel.lastactiveplayers.db.Database;
import de.thomasuebel.lastactiveplayers.db.DatabaseException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * SQLite-backed {@link Leaderboard} sorted by total accumulated play time in a rolling
 * window, descending.
 *
 * <p>The window start is computed fresh on each call to {@link #top} using the injected
 * {@link Clock}, so the rolling window always covers the most recent {@code windowDays}
 * days regardless of how long the plugin has been running.
 *
 * <p>Only closed sessions (those with a {@code leave_time}) whose {@code leave_time}
 * falls at or after the window start are counted. This ensures sessions that started
 * before the window but ended inside it are included in the total.
 * Open sessions are excluded because their accumulated time may not yet be fully
 * flushed to {@code duration_seconds}.
 * Online-player exclusion is applied in Java after the query.
 */
public final class SqlitePlaytimeLeaderboard implements Leaderboard {

    private static final String QUERY = """
        SELECT p.uuid, p.username,
               SUM(s.duration_seconds) AS total_seconds,
               MAX(s.leave_time) AS last_leave
        FROM players p
        JOIN sessions s ON s.player_uuid = p.uuid
        WHERE s.leave_time >= ?
        GROUP BY p.uuid, p.username
        HAVING total_seconds > 0
        ORDER BY total_seconds DESC
        """;

    private final Database database;
    private final Clock clock;
    private final long windowDays;

    /**
     * Constructs a leaderboard for the given database, clock, and rolling window length.
     *
     * @param database   the open database; never null
     * @param clock      the clock used to compute the window start on each query; never null
     * @param windowDays the number of days in the rolling window; positive
     */
    public SqlitePlaytimeLeaderboard(
        final Database database, final Clock clock, final long windowDays
    ) {
        this.database = database;
        this.clock = clock;
        this.windowDays = windowDays;
    }

    @Override
    public List<LeaderboardEntry> top(final int limit, final Set<UUID> exclude) {
        final Instant windowStart = Instant.now(this.clock).minus(Duration.ofDays(this.windowDays));
        try (PreparedStatement stmt = this.database.connection().prepareStatement(QUERY)) {
            stmt.setString(1, windowStart.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return mapEntries(rs, limit, exclude);
            }
        } catch (final SQLException exception) {
            throw new DatabaseException(exception);
        }
    }

    @Override
    public List<LeaderboardEntry> topTied(final Set<UUID> exclude) {
        final Instant windowStart = Instant.now(this.clock).minus(Duration.ofDays(this.windowDays));
        try (PreparedStatement stmt = this.database.connection().prepareStatement(QUERY)) {
            stmt.setString(1, windowStart.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return mapTiedEntries(rs, exclude);
            }
        } catch (final SQLException exception) {
            throw new DatabaseException(exception);
        }
    }

    private List<LeaderboardEntry> mapEntries(
        final ResultSet rs, final int limit, final Set<UUID> exclude
    ) throws SQLException {
        final List<LeaderboardEntry> result = new ArrayList<>();
        while (rs.next() && result.size() < limit) {
            final UUID uuid = UUID.fromString(rs.getString("uuid"));
            if (exclude.contains(uuid)) {
                continue;
            }
            final String lastLeaveStr = rs.getString("last_leave");
            result.add(new StoredEntry(
                uuid,
                rs.getString("username"),
                rs.getLong("total_seconds"),
                Optional.ofNullable(lastLeaveStr).map(Instant::parse)
            ));
        }
        return result;
    }

    private List<LeaderboardEntry> mapTiedEntries(
        final ResultSet rs, final Set<UUID> exclude
    ) throws SQLException {
        final List<LeaderboardEntry> result = new ArrayList<>();
        long topScore = -1L;
        while (rs.next()) {
            final UUID uuid = UUID.fromString(rs.getString("uuid"));
            if (exclude.contains(uuid)) {
                continue;
            }
            final long score = rs.getLong("total_seconds");
            if (topScore < 0) {
                topScore = score;
            } else if (score < topScore) {
                break;
            }
            final String lastLeaveStr = rs.getString("last_leave");
            result.add(new StoredEntry(
                uuid,
                rs.getString("username"),
                score,
                Optional.ofNullable(lastLeaveStr).map(Instant::parse)
            ));
        }
        return result;
    }
}
