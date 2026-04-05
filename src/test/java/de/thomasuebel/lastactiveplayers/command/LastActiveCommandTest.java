package de.thomasuebel.lastactiveplayers.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LastActiveCommandTest {

    private static final String PERM_ADMIN = "lastactiveplayers.admin";

    private static CommandSender stubSender(
        final boolean admin, final List<String> captured
    ) {
        final InvocationHandler handler = (proxy, method, args) -> {
            if ("sendMessage".equals(method.getName())
                && args != null && args.length == 1
                && args[0] instanceof String) {
                captured.add((String) args[0]);
                return null;
            }
            if ("hasPermission".equals(method.getName())
                && args != null && args.length == 1) {
                return admin && PERM_ADMIN.equals(args[0]);
            }
            final Class<?> ret = method.getReturnType();
            if (ret == boolean.class) {
                return false;
            }
            return null;
        };
        return (CommandSender) Proxy.newProxyInstance(
            CommandSender.class.getClassLoader(),
            new Class<?>[]{CommandSender.class},
            handler
        );
    }

    private static Command stubCommand() {
        return new Command("lastactive") {
            @Override
            public boolean execute(
                final CommandSender sender,
                final String commandLabel,
                final String[] args
            ) {
                return false;
            }
        };
    }

    private static LastActiveCommand command(
        final CommandLines list,
        final CommandLines mvp,
        final CommandLines streak,
        final CommandLines preview
    ) {
        return new LastActiveCommand(list, mvp, streak, preview, sender -> { }, Set::of);
    }

    @Test
    void sendsListLinesForBaseCommand() {
        final List<String> captured = new ArrayList<>();
        final boolean result = command(
            online -> List.of("line1", "line2"),
            online -> List.of(),
            online -> List.of(),
            online -> List.of()
        ).onCommand(stubSender(false, captured), stubCommand(), "lastactive", new String[0]);
        assertTrue(result);
        assertEquals(List.of("line1", "line2"), captured);
    }

    @Test
    void sendsMvpLinesForMvpSubcommand() {
        final List<String> captured = new ArrayList<>();
        final boolean result = command(
            online -> List.of(),
            online -> List.of("mvp-line"),
            online -> List.of(),
            online -> List.of()
        ).onCommand(
            stubSender(false, captured), stubCommand(), "lastactive", new String[]{"mvp"}
        );
        assertTrue(result);
        assertEquals(List.of("mvp-line"), captured);
    }

    @Test
    void sendsStreakLinesForStreakSubcommand() {
        final List<String> captured = new ArrayList<>();
        final boolean result = command(
            online -> List.of(),
            online -> List.of(),
            online -> List.of("streak-line"),
            online -> List.of()
        ).onCommand(
            stubSender(false, captured), stubCommand(), "lastactive", new String[]{"streak"}
        );
        assertTrue(result);
        assertEquals(List.of("streak-line"), captured);
    }

    @Test
    void sendsPreviewLinesForTestSubcommandWithPermission() {
        final List<String> captured = new ArrayList<>();
        final boolean result = command(
            online -> List.of(),
            online -> List.of(),
            online -> List.of(),
            online -> List.of("preview1", "preview2")
        ).onCommand(
            stubSender(true, captured), stubCommand(), "lastactive", new String[]{"test"}
        );
        assertTrue(result);
        assertEquals(List.of("preview1", "preview2"), captured);
    }

    @Test
    void sendsPermissionDeniedForTestSubcommandWithoutPermission() {
        final List<String> captured = new ArrayList<>();
        final boolean result = command(
            online -> List.of(),
            online -> List.of(),
            online -> List.of(),
            online -> List.of()
        ).onCommand(
            stubSender(false, captured), stubCommand(), "lastactive", new String[]{"test"}
        );
        assertTrue(result);
        assertEquals(1, captured.size());
        assertTrue(captured.get(0).contains("permission"));
    }

    @Test
    void mvpSubcommandRequiresNoPermission() {
        final List<String> captured = new ArrayList<>();
        command(
            online -> List.of(),
            online -> List.of("mvp"),
            online -> List.of(),
            online -> List.of()
        ).onCommand(
            stubSender(false, captured), stubCommand(), "lastactive", new String[]{"mvp"}
        );
        assertEquals(List.of("mvp"), captured);
    }

    @Test
    void streakSubcommandRequiresNoPermission() {
        final List<String> captured = new ArrayList<>();
        command(
            online -> List.of(),
            online -> List.of(),
            online -> List.of("streak"),
            online -> List.of()
        ).onCommand(
            stubSender(false, captured), stubCommand(), "lastactive", new String[]{"streak"}
        );
        assertEquals(List.of("streak"), captured);
    }

    @Test
    void returnsHelpUsageForHelpSubcommand() {
        final List<String> captured = new ArrayList<>();
        final boolean result = command(
            online -> List.of("list"),
            online -> List.of(),
            online -> List.of(),
            online -> List.of()
        ).onCommand(
            stubSender(false, captured), stubCommand(), "lastactive", new String[]{"help"}
        );
        assertFalse(result);
        assertTrue(captured.isEmpty());
    }

    @Test
    void reloadSubcommandInvokesReloadActionWithPermission() {
        final AtomicBoolean reloadCalled = new AtomicBoolean(false);
        final List<String> captured = new ArrayList<>();
        final LastActiveCommand cmd = new LastActiveCommand(
            online -> List.of(),
            online -> List.of(),
            online -> List.of(),
            online -> List.of(),
            sender -> reloadCalled.set(true),
            Set::of
        );
        final boolean result = cmd.onCommand(
            stubSender(true, captured), stubCommand(), "lastactive", new String[]{"reload"}
        );
        assertTrue(result);
        assertTrue(reloadCalled.get());
        assertTrue(captured.isEmpty());
    }

    @Test
    void reloadSubcommandSendsPermissionDeniedWithoutPermission() {
        final AtomicBoolean reloadCalled = new AtomicBoolean(false);
        final List<String> captured = new ArrayList<>();
        final LastActiveCommand cmd = new LastActiveCommand(
            online -> List.of(),
            online -> List.of(),
            online -> List.of(),
            online -> List.of(),
            sender -> reloadCalled.set(true),
            Set::of
        );
        final boolean result = cmd.onCommand(
            stubSender(false, captured), stubCommand(), "lastactive", new String[]{"reload"}
        );
        assertTrue(result);
        assertFalse(reloadCalled.get());
        assertEquals(1, captured.size());
        assertTrue(captured.get(0).contains("permission"));
    }
}
