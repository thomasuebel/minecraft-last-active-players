package de.thomasuebel.lastactiveplayers.stats;

import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * {@link Statistics} implementation backed by bStats.
 *
 * <p>Registers the plugin with the bStats metrics platform on construction.
 * bStats manages its own background reporting; no further interaction is required.
 */
public final class BStatsStatistics implements Statistics {

    /**
     * Registers the plugin with bStats.
     *
     * @param plugin   the plugin instance; never null
     * @param pluginId the bStats plugin ID
     */
    @SuppressWarnings("PMD.UnusedLocalVariable")
    public BStatsStatistics(final JavaPlugin plugin, final int pluginId) {
        new Metrics(plugin, pluginId);
    }
}
