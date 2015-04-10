package me.minotopia.minoduel.hook;

import me.minotopia.minoduel.MinoDuelPlugin;
import org.bukkit.plugin.Plugin;

import io.github.xxyy.mtc.api.PlayerGameManager;

import java.util.UUID;

/**
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 27.8.14
 */
public class MTCHook {
    private MinoDuelPlugin plugin;
    private PlayerGameManager manager;

    public MTCHook(MinoDuelPlugin plugin) {
        this.plugin = plugin;
    }

    public MTCHook tryHook() {
        try {
            manager = plugin.getServer().getServicesManager().load(PlayerGameManager.class);
        } catch (NoClassDefFoundError e) {
            plugin.getLogger().info("MTC hook failed: MTC not loaded.");
        }
        return this;
    }

    public boolean isInOtherGame(UUID uuid) {
        return manager != null && manager.isInGame(uuid) && !plugin.equals(manager.getProvidingPlugin(uuid));
    }

    public Plugin getBlockingPlugin(UUID uuid) {
        if (manager == null) {
            return null;
        }
        return manager.getProvidingPlugin(uuid);
    }

    public void setInGame(boolean inGame, UUID uuid) {
        if (manager != null) {
            manager.setInGame(inGame, uuid, plugin);
        }
    }
}
