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
 * SQLite-backed {@link Leaderboard} sorted by the most recent leave time, descending.
 *
 * <p>{@code totalSeconds} in each entry reflects only sessions whose {@code leave_time}
 * falls within the rolling window, so callers display the same 30-day playtime figure
 * used by the MVP leaderboard.
 *
 * <p>Players whose last session predates the window still appear in the list (sorted by
 * {@code last_leave}) but their {@code totalSeconds} will be zero.
 *
 * <p>Only players who have at least one closed session are included.
 * Online-player exclusion is applied in Java after the query.
 */
public final class SqliteLastLeaveLeaderboard implements Leaderboard {

    private static final String QUERY = """
        SELECT p.uuid, p.username,
               SUM(CASE WHEN s.leave_time >= ? THEN s.duration_seconds ELSE 0 END)
                   AS total_seconds,
               MAX(s.leave_time) AS last_leave
        FROM players p
        JOIN sessions s ON s.player_uuid = p.uuid
        WHERE s.leave_time IS NOT NULL
        GROUP BY p.uuid, p.username
        ORDER BY last_leave DESC
        """;

    private final Database database;
    private final Clock clock;
    private final long windowDays;

    /**
     * Constructs a leaderboard backed by the given database.
     *
     * @param database   the open database; never null
     * @param clock      used to compute the rolling window start on each query; never null
     * @param windowDays length of the rolling window in days; positive
     */
    public SqliteLastLeaveLeaderboard(
        final Database database, final Clock clock, final long windowDays
    ) {
        this.database = database;
        this.clock = clock;
        this.windowDays = windowDays;
    }

    @Override
    public List<LeaderboardEntry> top(final int limit, final Set<UUID> exclude) {
        final String windowStart = windowStart();
        try (PreparedStatement stmt = this.database.connection().prepareStatement(QUERY)) {
            stmt.setString(1, windowStart);
            try (ResultSet rs = stmt.executeQuery()) {
                return mapEntries(rs, limit, exclude);
            }
        } catch (final SQLException exception) {
            throw new DatabaseException(exception);
        }
    }

    @Override
    public List<LeaderboardEntry> topTied(final Set<UUID> exclude) {
        final String windowStart = windowStart();
        try (PreparedStatement stmt = this.database.connection().prepareStatement(QUERY)) {
            stmt.setString(1, windowStart);
            try (ResultSet rs = stmt.executeQuery()) {
                return mapTiedEntries(rs, exclude);
            }
        } catch (final SQLException exception) {
            throw new DatabaseException(exception);
        }
    }

    private String windowStart() {
        return Instant.now(this.clock).minus(Duration.ofDays(this.windowDays)).toString();
    }

    private List<LeaderboardEntry> mapEntries(
        final ResultSet rs, final int limit, final Set<UUID> exclude
    ) throws SQLException {
        final List<LeaderboardEntry> result = new ArrayList<>();
        while (rs.next()) {
            final UUID uuid = UUID.fromString(rs.getString("uuid"));
            if (exclude.contains(uuid)) {
                continue;
            }
            if (result.size() >= limit) {
                break;
            }
            result.add(new LeaderboardEntry(
                uuid,
                rs.getString("username"),
                rs.getLong("total_seconds"),
                Optional.of(Instant.parse(rs.getString("last_leave")))
            ));
        }
        return result;
    }

    private List<LeaderboardEntry> mapTiedEntries(
        final ResultSet rs, final Set<UUID> exclude
    ) throws SQLException {
        final List<LeaderboardEntry> result = new ArrayList<>();
        Instant topLeave = null;
        while (rs.next()) {
            final UUID uuid = UUID.fromString(rs.getString("uuid"));
            if (exclude.contains(uuid)) {
                continue;
            }
            final String lastLeaveStr = rs.getString("last_leave");
            if (lastLeaveStr == null) {
                continue;
            }
            final Instant lastLeave = Instant.parse(lastLeaveStr);
            if (topLeave == null) {
                topLeave = lastLeave;
            } else if (!lastLeave.equals(topLeave)) {
                break;
            }
            result.add(new LeaderboardEntry(
                uuid,
                rs.getString("username"),
                rs.getLong("total_seconds"),
                Optional.of(lastLeave)
            ));
        }
        return result;
    }
}
