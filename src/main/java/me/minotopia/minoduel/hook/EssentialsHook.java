package me.minotopia.minoduel.hook;

import com.earth2me.essentials.User;
import me.minotopia.minoduel.MinoDuelPlugin;
import net.ess3.api.IEssentials;
import org.bukkit.entity.Player;

/**
 * Hooks into Essentials to prevent various forms of bugusing.
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 27.8.14
 */
public class EssentialsHook {
    private MinoDuelPlugin plugin;
    private IEssentials ess;

    public EssentialsHook(MinoDuelPlugin plugin) {
        this.plugin = plugin;
    }

    public EssentialsHook tryHook() {
        try {
            ess = (IEssentials) plugin.getServer().getPluginManager().getPlugin("Essentials");
        } catch (NoClassDefFoundError e) {
            plugin.getLogger().info("Essentials hook failed: Essentials not loaded.");
        }
        return this;
    }

    public void disableGodMode(Player plr) {
        if (ess == null) {
            return;
        }

        User user = ess.getUser(plr);

        if (user != null && user.isGodModeEnabled()) {
            user.setGodModeEnabled(false);
        }
    }
}
