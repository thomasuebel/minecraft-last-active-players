package de.thomasuebel.lastactiveplayers;

import de.thomasuebel.lastactiveplayers.db.Database;
import de.thomasuebel.lastactiveplayers.db.DatabaseException;
import de.thomasuebel.lastactiveplayers.db.InitialSchema;
import de.thomasuebel.lastactiveplayers.db.SqliteDatabase;
import de.thomasuebel.lastactiveplayers.db.SqliteMigrations;
import de.thomasuebel.lastactiveplayers.listener.AwardLifecycle;
import de.thomasuebel.lastactiveplayers.listener.SessionLifecycle;
import de.thomasuebel.lastactiveplayers.player.Players;
import de.thomasuebel.lastactiveplayers.player.SqlitePlayers;
import de.thomasuebel.lastactiveplayers.player.StreakMilestones;
import de.thomasuebel.lastactiveplayers.ranking.Leaderboard;
import de.thomasuebel.lastactiveplayers.ranking.SqlitePlaytimeLeaderboard;
import de.thomasuebel.lastactiveplayers.session.ActiveSessions;
import de.thomasuebel.lastactiveplayers.session.BukkitHeartbeat;
import de.thomasuebel.lastactiveplayers.session.Heartbeat;
import de.thomasuebel.lastactiveplayers.session.InMemoryActiveSessions;
import de.thomasuebel.lastactiveplayers.session.SessionHeartbeat;
import de.thomasuebel.lastactiveplayers.session.Sessions;
import de.thomasuebel.lastactiveplayers.session.SqliteSessions;
import de.thomasuebel.lastactiveplayers.session.TrackedSession;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Main plugin class for LastActivePlayers.
 *
 * <p>Note: extending JavaPlugin is a framework-imposed constraint and the sole
 * exception to the Elegant Objects rule of no implementation inheritance.
 */
public final class LastActivePlayers extends JavaPlugin {

    /** Server ticks per minute: 20 ticks/s x 60 s. */
    private static final long TICKS_PER_MINUTE = 1200L;
    private static final int THIRTY_DAYS = 30;

    private Database database;
    private Sessions sessions;
    private ActiveSessions activeSessions;
    private BukkitTask heartbeatTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        final long heartbeatMinutes =
            getConfig().getLong("session.heartbeat-interval-minutes", 10L);
        final String milestoneTemplate = getConfig().getString(
            "messages.streak-milestone", "\uD83D\uDD25 {player} has reached a {streak}-day streak!"
        );
        final String mvpTemplate = getConfig().getString(
            "messages.mvp", "\uD83D\uDC51 Most active player (last 30 days): {player}"
        );
        final String streakTemplate = getConfig().getString(
            "messages.streak", "\uD83D\uDD25 Longest daily login streak: {player} ({streak} days)"
        );
        final String mvpPrefix = getConfig().getString("prefix.mvp", "\uD83D\uDC51 ");
        final String streakPrefix = getConfig().getString("prefix.streak", "\uD83D\uDD25 ");

        try {
            this.database = new SqliteDatabase(
                getDataFolder().toPath().resolve("lastactiveplayers.db"),
                new SqliteMigrations(new InitialSchema())
            );
        } catch (final IOException exception) {
            getLogger().severe("Failed to open database: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        final Players players = new SqlitePlayers(this.database);
        this.sessions = new SqliteSessions(this.database);

        this.sessions.closeOrphans(Instant.now());

        this.activeSessions = new InMemoryActiveSessions();
        final Heartbeat heartbeat = new SessionHeartbeat(this.activeSessions, this.sessions);
        final long intervalTicks = heartbeatMinutes * TICKS_PER_MINUTE;
        this.heartbeatTask = new BukkitHeartbeat(heartbeat)
            .runTaskTimer(this, intervalTicks, intervalTicks);

        final StreakMilestones milestones = new StreakMilestones();
        getServer().getPluginManager().registerEvents(
            new SessionLifecycle(
                players, this.sessions, this.activeSessions,
                milestones, ZoneId.systemDefault(), milestoneTemplate
            ),
            this
        );

        final Leaderboard mvpBoard = new SqlitePlaytimeLeaderboard(
            this.database,
            Instant.now().minus(Duration.ofDays(THIRTY_DAYS))
        );
        getServer().getPluginManager().registerEvents(
            new AwardLifecycle(
                mvpBoard, players, milestones, this,
                mvpPrefix, streakPrefix, mvpTemplate, streakTemplate
            ),
            this
        );
    }

    @Override
    public void onDisable() {
        if (this.heartbeatTask != null) {
            this.heartbeatTask.cancel();
        }
        if (this.activeSessions != null && this.sessions != null) {
            final Instant now = Instant.now();
            for (final TrackedSession tracked : this.activeSessions.all()) {
                try {
                    final long elapsed =
                        Math.max(0L, Duration.between(tracked.lastHeartbeat(), now).getSeconds());
                    this.sessions.heartbeat(tracked.sessionId(), now, elapsed);
                    this.sessions.close(tracked.sessionId(), now);
                } catch (final DatabaseException exception) {
                    getLogger().warning(
                        "Failed to close session " + tracked.sessionId()
                        + " on shutdown: " + exception.getMessage()
                    );
                }
            }
        }
        if (this.database != null) {
            try {
                this.database.close();
            } catch (final IOException exception) {
                getLogger().warning("Error closing database: " + exception.getMessage());
            }
        }
    }
}
