package net.Indyuce.mmoitems.util;

import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * mmoitems
 *
 * @author Roch Blondiaux
 * @date 24/10/2022
 */
public class PluginUtils {

    public PluginUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static void isDependencyPresent(@NotNull String name, @NotNull Consumer<Plugin> callback) {
        hookDependencyIfPresent(name, false, callback);
    }

    public static void hookDependencyIfPresent(@NotNull String name, boolean verbose, @NotNull Consumer<Plugin> callback) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
        if (plugin == null) return;

        try {
            callback.accept(plugin);
            if (verbose) MMOItems.plugin.getLogger().log(Level.INFO, String.format("Hooked onto %s", name));
        } catch (Exception exception) {
            MMOItems.plugin.getLogger().log(Level.INFO, String.format("Could not hook onto %s (plugin is out-of-date?): %s", name, exception.getMessage()));
        }
    }
}
