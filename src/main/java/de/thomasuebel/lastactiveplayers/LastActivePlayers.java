package de.thomasuebel.lastactiveplayers;

import de.thomasuebel.lastactiveplayers.db.Database;
import de.thomasuebel.lastactiveplayers.db.InitialSchema;
import de.thomasuebel.lastactiveplayers.db.SqliteDatabase;
import de.thomasuebel.lastactiveplayers.db.SqliteMigrations;
import de.thomasuebel.lastactiveplayers.listener.SessionLifecycle;
import de.thomasuebel.lastactiveplayers.player.Players;
import de.thomasuebel.lastactiveplayers.player.SqlitePlayers;
import de.thomasuebel.lastactiveplayers.session.ActiveSessions;
import de.thomasuebel.lastactiveplayers.session.BukkitHeartbeat;
import de.thomasuebel.lastactiveplayers.session.Heartbeat;
import de.thomasuebel.lastactiveplayers.session.InMemoryActiveSessions;
import de.thomasuebel.lastactiveplayers.session.SessionHeartbeat;
import de.thomasuebel.lastactiveplayers.session.Sessions;
import de.thomasuebel.lastactiveplayers.session.SqliteSessions;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * Main plugin class for LastActivePlayers.
 *
 * <p>Note: extending JavaPlugin is a framework-imposed constraint and the sole
 * exception to the Elegant Objects rule of no implementation inheritance.
 */
public final class LastActivePlayers extends JavaPlugin {

    private static final long TICKS_PER_MINUTE = 1200L;

    private Database database;
    private ActiveSessions activeSessions;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        final long heartbeatMinutes =
            getConfig().getLong("session.heartbeat-interval-minutes", 10L);

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
        final Sessions sessions = new SqliteSessions(this.database);

        sessions.closeOrphans(Instant.now());

        this.activeSessions = new InMemoryActiveSessions();
        final Heartbeat heartbeat = new SessionHeartbeat(this.activeSessions, sessions);
        final BukkitHeartbeat task = new BukkitHeartbeat(heartbeat);
        task.runTaskTimerAsynchronously(
            this, heartbeatMinutes * TICKS_PER_MINUTE, heartbeatMinutes * TICKS_PER_MINUTE
        );

        getServer().getPluginManager().registerEvents(
            new SessionLifecycle(players, sessions, this.activeSessions), this
        );
    }

    @Override
    public void onDisable() {
        if (this.activeSessions != null && this.database != null) {
            final Instant now = Instant.now();
            final Sessions sessions = new SqliteSessions(this.database);
            for (final de.thomasuebel.lastactiveplayers.session.TrackedSession tracked
                : this.activeSessions.all()) {
                final long elapsed =
                    Duration.between(tracked.lastHeartbeat(), now).getSeconds();
                sessions.heartbeat(tracked.sessionId(), now, elapsed);
                sessions.close(tracked.sessionId(), now);
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
