package de.thomasuebel.lastactiveplayers.stats;

import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * {@link Statistics} implementation backed by bStats.
 *
 * <p>Calling {@link #register()} registers the plugin with the bStats metrics platform.
 * bStats manages its own background reporting; no further interaction is required.
 */
public final class BStatsStatistics implements Statistics {

    private final JavaPlugin plugin;
    private final int pluginId;

    /**
     * Constructs a bStats statistics reporter.
     *
     * @param plugin   the plugin instance; never null
     * @param pluginId the positive bStats plugin ID matching the registered plugin
     */
    public BStatsStatistics(final JavaPlugin plugin, final int pluginId) {
        this.plugin = plugin;
        this.pluginId = pluginId;
    }

    @Override
    public void register() {
        new Metrics(this.plugin, this.pluginId);
    }
}
