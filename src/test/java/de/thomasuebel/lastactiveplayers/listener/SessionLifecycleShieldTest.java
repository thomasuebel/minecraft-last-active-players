package de.thomasuebel.lastactiveplayers.listener;

import de.thomasuebel.lastactiveplayers.player.Milestones;
import de.thomasuebel.lastactiveplayers.player.NoPlayer;
import de.thomasuebel.lastactiveplayers.player.StreakMilestones;
import de.thomasuebel.lastactiveplayers.session.ActiveSessions;
import de.thomasuebel.lastactiveplayers.session.Sessions;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionLifecycleShieldTest {

    private static final UUID PLAYER_UUID = UUID.randomUUID();
    private static final String PLAYER_NAME = "TestPlayer";
    private static final long DELAY_TICKS = 200L;
    private static final int MAX_SHIELDS = 3;
    private static final int TWO_DAYS = 2;
    private static final int THREE_DAYS_AGO = 3;

    // -------------------------------------------------------------------------
    // Players stubs
    // -------------------------------------------------------------------------

    /**
     * Builds a Players stub with controllable streak state and a captured shield store.
     *
     * @param streakDays   current streak days for the player
     * @param lastDayDelta days before today for streakLastDay (1 = yesterday, 2 = 2 days ago)
     * @param initialShields starting shield count
     * @param shieldsStore   mutable holder tracking the latest setShields value
     */
    private static de.thomasuebel.lastactiveplayers.player.Players stubPlayers(
        final int streakDays,
        final int lastDayDelta,
        final int initialShields,
        final AtomicInteger shieldsStore
    ) {
        shieldsStore.set(initialShields);
        return new de.thomasuebel.lastactiveplayers.player.Players() {
            @Override
            public void upsert(final UUID uuid, final String username) { }

            @Override
            public void updateStreak(
                final UUID uuid,
                final int days,
                final Optional<LocalDate> lastDay
            ) { }

            @Override
            public de.thomasuebel.lastactiveplayers.player.Player withUuid(final UUID uuid) {
                return new de.thomasuebel.lastactiveplayers.player.Player() {
                    @Override public boolean exists() { return true; }
                    @Override public UUID uuid() { return uuid; }
                    @Override public String username() { return PLAYER_NAME; }
                    @Override public int streakDays() { return streakDays; }
                    @Override
                    public Optional<LocalDate> streakLastDay() {
                        return Optional.of(LocalDate.now().minusDays(lastDayDelta));
                    }
                };
            }

            @Override
            public de.thomasuebel.lastactiveplayers.player.Player withHighestStreak() {
                return new NoPlayer();
            }

            @Override
            public List<de.thomasuebel.lastactiveplayers.player.Player> withTopStreak() {
                return List.of();
            }

            @Override
            public void purgeInactiveBefore(final Instant threshold) { }

            @Override
            public int shields(final UUID uuid) {
                return shieldsStore.get();
            }

            @Override
            public void setShields(final UUID uuid, final int count) {
                shieldsStore.set(count);
            }
        };
    }

    private static Sessions stubSessions() {
        final InvocationHandler handler = (proxy, method, args) -> {
            if ("open".equals(method.getName())) {
                return 1L;
            }
            if (method.getReturnType() == List.class) {
                return List.of();
            }
            return null;
        };
        return (Sessions) Proxy.newProxyInstance(
            Sessions.class.getClassLoader(), new Class<?>[]{Sessions.class}, handler
        );
    }

    private static ActiveSessions stubActiveSessions() {
        final InvocationHandler handler = (proxy, method, args) -> {
            if ("stop".equals(method.getName())) {
                return Optional.empty();
            }
            if (method.getReturnType() == List.class) {
                return List.of();
            }
            return null;
        };
        return (ActiveSessions) Proxy.newProxyInstance(
            ActiveSessions.class.getClassLoader(), new Class<?>[]{ActiveSessions.class}, handler
        );
    }

    private static org.bukkit.entity.Player stubBukkitPlayer(
        final List<String> messagesCapture
    ) {
        final InvocationHandler taskHandler = (proxy, method, args) -> null;
        final BukkitTask stubTask = (BukkitTask) Proxy.newProxyInstance(
            BukkitTask.class.getClassLoader(), new Class<?>[]{BukkitTask.class}, taskHandler
        );
        final InvocationHandler schedulerHandler = (proxy, method, args) -> {
            if ("runTaskLater".equals(method.getName())
                && args != null && args.length == 3
                && args[1] instanceof Runnable) {
                ((Runnable) args[1]).run();
                return stubTask;
            }
            return null;
        };
        final BukkitScheduler schedulerProxy = (BukkitScheduler) Proxy.newProxyInstance(
            BukkitScheduler.class.getClassLoader(),
            new Class<?>[]{BukkitScheduler.class},
            schedulerHandler
        );
        final InvocationHandler serverHandler = (proxy, method, args) -> {
            if ("getScheduler".equals(method.getName())) {
                return schedulerProxy;
            }
            if ("broadcastMessage".equals(method.getName())) {
                if (method.getReturnType() == int.class) {
                    return 0;
                }
                return null;
            }
            if (method.getReturnType() == boolean.class) {
                return false;
            }
            if (method.getReturnType() == int.class) {
                return 0;
            }
            return null;
        };
        final org.bukkit.Server serverProxy = (org.bukkit.Server) Proxy.newProxyInstance(
            org.bukkit.Server.class.getClassLoader(),
            new Class<?>[]{org.bukkit.Server.class},
            serverHandler
        );
        final InvocationHandler playerHandler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "getUniqueId": return PLAYER_UUID;
                case "getName": return PLAYER_NAME;
                case "getServer": return serverProxy;
                case "isOnline": return true;
                case "sendMessage":
                    if (args != null && args.length == 1 && args[0] instanceof String) {
                        messagesCapture.add((String) args[0]);
                    }
                    return null;
                default:
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    }
                    return null;
            }
        };
        return (org.bukkit.entity.Player) Proxy.newProxyInstance(
            org.bukkit.entity.Player.class.getClassLoader(),
            new Class<?>[]{org.bukkit.entity.Player.class},
            playerHandler
        );
    }

    private static Plugin stubPlugin(final org.bukkit.Server server) {
        final InvocationHandler handler = (proxy, method, args) -> {
            if ("getServer".equals(method.getName())) {
                return server;
            }
            if (method.getReturnType() == boolean.class) {
                return false;
            }
            return null;
        };
        return (Plugin) Proxy.newProxyInstance(
            Plugin.class.getClassLoader(), new Class<?>[]{Plugin.class}, handler
        );
    }

    private static SessionLifecycle lifecycle(
        final de.thomasuebel.lastactiveplayers.player.Players players,
        final Plugin plugin
    ) {
        return new SessionLifecycle(
            players, stubSessions(), stubActiveSessions(),
            new StreakMilestones(), java.time.ZoneId.systemDefault(),
            "Server: {player} hit {streak} days!",
            plugin, DELAY_TICKS,
            "", "",
            MAX_SHIELDS,
            "Shield used! Streak: {streak}. Remaining: {shields_remaining}"
        );
    }

    // -------------------------------------------------------------------------
    // Shield consumption tests
    // -------------------------------------------------------------------------

    @Test
    void shieldConsumedWhenGapIsOneDayAndShieldsAvailable() {
        final AtomicInteger shields = new AtomicInteger();
        // streak = 5, last = 2 days ago (exactly 1 missed day), 2 shields
        final de.thomasuebel.lastactiveplayers.player.Players players =
            stubPlayers(5, TWO_DAYS, 2, shields);
        final List<String> messages = new ArrayList<>();
        final org.bukkit.entity.Player player = stubBukkitPlayer(messages);
        final Plugin plugin = stubPlugin(player.getServer());

        lifecycle(players, plugin).onJoin(new PlayerJoinEvent(player, ""));

        assertEquals(1, shields.get());
    }

    @Test
    void shieldUsedMessageSentToPlayerWhenShieldConsumed() {
        final AtomicInteger shields = new AtomicInteger();
        final de.thomasuebel.lastactiveplayers.player.Players players =
            stubPlayers(5, TWO_DAYS, 2, shields);
        final List<String> messages = new ArrayList<>();
        final org.bukkit.entity.Player player = stubBukkitPlayer(messages);
        final Plugin plugin = stubPlugin(player.getServer());

        lifecycle(players, plugin).onJoin(new PlayerJoinEvent(player, ""));

        assertEquals(1, messages.size());
        assertEquals("Shield used! Streak: 6. Remaining: 1", messages.get(0));
    }

    @Test
    void shieldNotConsumedWhenNoShieldsAvailable() {
        final AtomicInteger shields = new AtomicInteger();
        // streak = 5, last = 2 days ago, 0 shields
        final de.thomasuebel.lastactiveplayers.player.Players players =
            stubPlayers(5, TWO_DAYS, 0, shields);
        final List<String> messages = new ArrayList<>();
        final org.bukkit.entity.Player player = stubBukkitPlayer(messages);
        final Plugin plugin = stubPlugin(player.getServer());

        lifecycle(players, plugin).onJoin(new PlayerJoinEvent(player, ""));

        assertEquals(0, shields.get());
        assertTrue(messages.isEmpty());
    }

    @Test
    void shieldNotConsumedWhenGapIsThreeDays() {
        // last = 3 days ago → epoch gap of 3; shield only bridges a gap of 2 (one missed day)
        final AtomicInteger shields = new AtomicInteger();
        final de.thomasuebel.lastactiveplayers.player.Players players =
            stubPlayers(5, THREE_DAYS_AGO, 2, shields);
        final List<String> messages = new ArrayList<>();
        final org.bukkit.entity.Player player = stubBukkitPlayer(messages);
        final Plugin plugin = stubPlugin(player.getServer());

        lifecycle(players, plugin).onJoin(new PlayerJoinEvent(player, ""));

        assertEquals(2, shields.get());
        assertTrue(messages.isEmpty());
    }

    // -------------------------------------------------------------------------
    // Shield award tests
    // -------------------------------------------------------------------------

    @Test
    void shieldAwardedWhenMilestoneCrossed() {
        final AtomicInteger shields = new AtomicInteger();
        // streak = 2, last = yesterday → new streak = 3 → crosses milestone 3 → +1 shield
        final de.thomasuebel.lastactiveplayers.player.Players players =
            stubPlayers(2, 1, 0, shields);
        final List<String> messages = new ArrayList<>();
        final org.bukkit.entity.Player player = stubBukkitPlayer(messages);
        final Plugin plugin = stubPlugin(player.getServer());

        lifecycle(players, plugin).onJoin(new PlayerJoinEvent(player, ""));

        assertEquals(1, shields.get());
    }

    @Test
    void shieldsNotExceedingMaxOnAward() {
        final AtomicInteger shields = new AtomicInteger();
        // Already at max (3), crossing milestone 7 should not increase beyond 3
        final de.thomasuebel.lastactiveplayers.player.Players players =
            stubPlayers(2, 1, MAX_SHIELDS, shields);
        final List<String> messages = new ArrayList<>();
        final org.bukkit.entity.Player player = stubBukkitPlayer(messages);
        final Plugin plugin = stubPlugin(player.getServer());

        lifecycle(players, plugin).onJoin(new PlayerJoinEvent(player, ""));

        assertEquals(MAX_SHIELDS, shields.get());
    }
}
