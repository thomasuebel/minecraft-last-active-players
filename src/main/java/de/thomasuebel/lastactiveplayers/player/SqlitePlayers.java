package de.thomasuebel.lastactiveplayers.player;

import de.thomasuebel.lastactiveplayers.db.Database;
import de.thomasuebel.lastactiveplayers.db.DatabaseException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SQLite-backed implementation of {@link Players}.
 */
public final class SqlitePlayers implements Players {

    private static final String UPSERT = """
        INSERT INTO players (uuid, username) VALUES (?, ?)
        ON CONFLICT(uuid) DO UPDATE SET username = excluded.username
        """;

    private static final String UPDATE_STREAK = """
        UPDATE players SET streak_days = ?, streak_last_day = ? WHERE uuid = ?
        """;

    private static final String SELECT_BY_UUID = """
        SELECT uuid, username, streak_days, streak_last_day FROM players WHERE uuid = ?
        """;

    private static final String SELECT_HIGHEST_STREAK = """
        SELECT uuid, username, streak_days, streak_last_day FROM players
        WHERE streak_days > 0
        ORDER BY streak_days DESC
        LIMIT 1
        """;

    private static final String SELECT_TOP_STREAK = """
        SELECT uuid, username, streak_days, streak_last_day FROM players
        WHERE streak_days = (SELECT MAX(streak_days) FROM players WHERE streak_days > 0)
        ORDER BY uuid
        """;

    private static final String PURGE = """
        DELETE FROM players
        WHERE uuid NOT IN (
            SELECT DISTINCT player_uuid FROM sessions
            WHERE leave_time >= ? OR leave_time IS NULL
        )
        """;

    private static final String SELECT_SHIELDS =
        "SELECT streak_shields FROM players WHERE uuid = ?";

    private static final String UPDATE_SHIELDS =
        "UPDATE players SET streak_shields = ? WHERE uuid = ?";

    private final Database database;

    /**
     * Constructs a repository backed by the given database.
     *
     * @param database the open database; never null
     */
    public SqlitePlayers(final Database database) {
        this.database = database;
    }

    @Override
    public void upsert(final UUID uuid, final String username) {
        try (PreparedStatement stmt = this.database.connection().prepareStatement(UPSERT)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, username);
            stmt.executeUpdate();
        } catch (final SQLException exception) {
            throw new DatabaseException(exception);
        }
    }

    @Override
    public void updateStreak(
        final UUID uuid,
        final int streakDays,
        final Optional<LocalDate> streakLastDay
    ) {
        try (PreparedStatement stmt = this.database.connection().prepareStatement(UPDATE_STREAK)) {
            stmt.setInt(1, streakDays);
            if (streakLastDay.isPresent()) {
                stmt.setString(2, streakLastDay.get().toString());
            } else {
                stmt.setNull(2, Types.VARCHAR);
            }
            stmt.setString(3, uuid.toString());
            stmt.executeUpdate();
        } catch (final SQLException exception) {
            throw new DatabaseException(exception);
        }
    }

    @Override
    public Player withUuid(final UUID uuid) {
        try (PreparedStatement stmt = this.database.connection().prepareStatement(SELECT_BY_UUID)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    final String dateStr = rs.getString("streak_last_day");
                    return new StoredPlayer(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("username"),
                        rs.getInt("streak_days"),
                        Optional.ofNullable(dateStr).map(LocalDate::parse)
                    );
                }
                return new NoPlayer();
            }
        } catch (final SQLException exception) {
            throw new DatabaseException(exception);
        }
    }

    @Override
    public Player withHighestStreak() {
        try (PreparedStatement stmt =
                 this.database.connection().prepareStatement(SELECT_HIGHEST_STREAK)) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    final String dateStr = rs.getString("streak_last_day");
                    return new StoredPlayer(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("username"),
                        rs.getInt("streak_days"),
                        Optional.ofNullable(dateStr).map(LocalDate::parse)
                    );
                }
                return new NoPlayer();
            }
        } catch (final SQLException exception) {
            throw new DatabaseException(exception);
        }
    }

    @Override
    public List<Player> withTopStreak() {
        try (PreparedStatement stmt =
                 this.database.connection().prepareStatement(SELECT_TOP_STREAK)) {
            try (ResultSet rs = stmt.executeQuery()) {
                final List<Player> result = new ArrayList<>();
                while (rs.next()) {
                    final String dateStr = rs.getString("streak_last_day");
                    result.add(new StoredPlayer(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("username"),
                        rs.getInt("streak_days"),
                        Optional.ofNullable(dateStr).map(LocalDate::parse)
                    ));
                }
                return result;
            }
        } catch (final SQLException exception) {
            throw new DatabaseException(exception);
        }
    }

    @Override
    public void purgeInactiveBefore(final Instant threshold) {
        try (PreparedStatement stmt = this.database.connection().prepareStatement(PURGE)) {
            stmt.setString(1, threshold.toString());
            stmt.executeUpdate();
        } catch (final SQLException exception) {
            throw new DatabaseException(exception);
        }
    }

    @Override
    public int shields(final UUID uuid) {
        try (PreparedStatement stmt =
                 this.database.connection().prepareStatement(SELECT_SHIELDS)) {
            stmt.setString(1, uuid.toString());
            try (var rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("streak_shields") : 0;
            }
        } catch (final SQLException exception) {
            throw new DatabaseException(exception);
        }
    }

    @Override
    public void setShields(final UUID uuid, final int count) {
        try (PreparedStatement stmt =
                 this.database.connection().prepareStatement(UPDATE_SHIELDS)) {
            stmt.setInt(1, count);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
        } catch (final SQLException exception) {
            throw new DatabaseException(exception);
        }
    }
}
