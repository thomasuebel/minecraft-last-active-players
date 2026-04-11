package de.thomasuebel.lastactiveplayers.listener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extra permission nodes that ride along with award permissions.
 *
 * <p>Operators configure these in {@code config.yml} under {@code awards.*}.
 * When an award permission is granted (e.g. {@code lastactiveplayers.mvp}),
 * all extra nodes listed for that award are granted on the same
 * {@link org.bukkit.permissions.PermissionAttachment}.
 *
 * @param mvpExtra    extra nodes granted alongside the MVP permission
 * @param streakExtra extra nodes per milestone, keyed by threshold
 *                    (e.g. 7 for the 7-day milestone)
 */
public record AwardPermissions(
    List<String> mvpExtra,
    Map<Integer, List<String>> streakExtra
) {

    /** No extra permissions -- the default when nothing is configured. */
    static final AwardPermissions NONE =
        new AwardPermissions(List.of(), Map.of());

    /**
     * Compact constructor that makes defensive copies.
     */
    public AwardPermissions {
        mvpExtra = List.copyOf(mvpExtra);
        final Map<Integer, List<String>> copy = new HashMap<>();
        for (final var entry : streakExtra.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        streakExtra = Map.copyOf(copy);
    }
}
