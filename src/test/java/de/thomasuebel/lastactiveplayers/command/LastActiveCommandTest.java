package de.thomasuebel.lastactiveplayers.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LastActiveCommandTest {

    private static final String PERM_ADMIN = "lastactiveplayers.admin";

    @SuppressWarnings("unchecked")
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

    @Test
    void sendsListLinesForBaseCommand() {
        final List<String> captured = new ArrayList<>();
        final LastActiveCommand cmd = new LastActiveCommand(
            online -> List.of("line1", "line2"),
            online -> List.of("preview"),
            Set::of
        );
        final boolean result = cmd.onCommand(
            stubSender(false, captured), stubCommand(), "lastactive", new String[0]
        );
        assertTrue(result);
        assertEquals(List.of("line1", "line2"), captured);
    }

    @Test
    void sendsPreviewLinesForTestSubcommandWithPermission() {
        final List<String> captured = new ArrayList<>();
        final LastActiveCommand cmd = new LastActiveCommand(
            online -> List.of("list"),
            online -> List.of("preview1", "preview2"),
            Set::of
        );
        final boolean result = cmd.onCommand(
            stubSender(true, captured), stubCommand(), "lastactive", new String[]{"test"}
        );
        assertTrue(result);
        assertEquals(List.of("preview1", "preview2"), captured);
    }

    @Test
    void sendsPermissionDeniedForTestSubcommandWithoutPermission() {
        final List<String> captured = new ArrayList<>();
        final LastActiveCommand cmd = new LastActiveCommand(
            online -> List.of(),
            online -> List.of(),
            Set::of
        );
        final boolean result = cmd.onCommand(
            stubSender(false, captured), stubCommand(), "lastactive", new String[]{"test"}
        );
        assertTrue(result);
        assertEquals(1, captured.size());
        assertTrue(captured.get(0).contains("permission"));
    }
}
