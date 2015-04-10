package me.minotopia.minoduel.listener;

import me.minotopia.minoduel.MinoDuelPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Handles teleporting players back if xLogin is not installed.
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 27.8.14
 */
public class DefaultLocationListener implements Listener {
    private final MinoDuelPlugin plugin;

    public DefaultLocationListener(MinoDuelPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent evt) {
        if (plugin.getLocationSaver().hasLocation(evt.getPlayer())) {
            plugin.getLocationSaver().loadLocationWithMessage(evt.getPlayer());
        }
    }
}
