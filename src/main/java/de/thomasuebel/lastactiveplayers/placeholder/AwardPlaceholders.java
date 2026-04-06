package de.thomasuebel.lastactiveplayers.placeholder;

import de.thomasuebel.lastactiveplayers.ranking.Awards;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

/**
 * PlaceholderAPI expansion that exposes LastActivePlayers award data as placeholders.
 *
 * <p>Extends {@link PlaceholderExpansion} as required by the PlaceholderAPI framework;
 * this is the sole permitted use of implementation inheritance in the project.
 *
 * <p>Available placeholders:
 * <ul>
 *   <li>{@code %lastactiveplayers_prefix%} -- the configured display-name prefix for
 *       the award the player currently holds ({@code prefix.mvp} or {@code prefix.streak}),
 *       or an empty string if the player holds no active award.</li>
 *   <li>{@code %lastactiveplayers_award%} -- {@code "mvp"}, {@code "streak"}, or {@code ""}.</li>
 * </ul>
 *
 * <p>{@link #persist()} returns {@code true} so the expansion survives PlaceholderAPI
 * reloads. On {@code /lastactive reload} a new instance is registered, replacing this
 * one because the identifier is identical.
 */
public final class AwardPlaceholders extends PlaceholderExpansion {

    private static final String IDENTIFIER = "lastactiveplayers";

    private final Awards awards;
    private final String version;

    /**
     * Constructs the expansion.
     *
     * @param awards   the current award state source; never null
     * @param version  the plugin version string reported to PlaceholderAPI; never null
     */
    public AwardPlaceholders(final Awards awards, final String version) {
        this.awards = awards;
        this.version = version;
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public String getAuthor() {
        return "thomasuebel";
    }

    @Override
    public String getVersion() {
        return this.version;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(final OfflinePlayer player, final String params) {
        if (player == null) {
            return "";
        }
        return switch (params) {
            case "prefix" -> this.awards.currentPrefix(player.getUniqueId());
            case "award" -> this.awards.currentAward(player.getUniqueId());
            default -> null;
        };
    }
}
