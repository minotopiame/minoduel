package me.minotopia.minoduel.hook;

import me.minotopia.minoduel.MinoDuelPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import io.github.xxyy.xlogin.common.PreferencesHolder;
import io.github.xxyy.xlogin.common.api.spigot.event.AuthenticationEvent;

/**
 * Hooks into xLogin to properly delay teleporting players back.
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 27.8.14
 */
public class XLoginHook implements Listener {
    private final MinoDuelPlugin plugin;
    private boolean hooked;

    public XLoginHook(MinoDuelPlugin plugin) {
        this.plugin = plugin;
    }

    public XLoginHook tryHook() {
        try {
            plugin.getLogger().info("Hooking into xLogin using " + PreferencesHolder.getConsumer().getClass() + "!");
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            hooked = true;
        } catch (NoClassDefFoundError e) {
            plugin.getLogger().info("xLogin hook failed: xLogin not loaded.");
        }
        return this;
    }

    @EventHandler
    public void onAuth(AuthenticationEvent evt) {
        if (plugin.getLocationSaver().hasLocation(evt.getPlayer())) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> plugin.getLocationSaver().loadLocationWithMessage(evt.getPlayer()), 10L);
        }
    }

    public boolean isHooked() {
        return hooked;
    }
}
