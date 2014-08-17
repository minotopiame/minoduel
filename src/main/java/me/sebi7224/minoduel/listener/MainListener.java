package me.sebi7224.minoduel.listener;

import me.sebi7224.minoduel.MinoDuelPlugin;
import me.sebi7224.minoduel.arena.Arena;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MainListener implements Listener {
    private final MinoDuelPlugin plugin;

    public MainListener(MinoDuelPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent evt) {
        if (evt.getEntityType() != EntityType.PLAYER) {
            return;
        }

        Player victim = (Player) evt.getEntity();
        Arena arena = plugin.getArenaManager().getPlayerArena(victim);

        if (arena != null && victim.getHealth() - evt.getDamage() <= 0) {
            arena.endGame(arena.getOther(victim));
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent evt) {
        onDisconnect(evt.getPlayer());
    }

    @EventHandler
    public void onKick(PlayerKickEvent evt) {
        onDisconnect(evt.getPlayer());
    }

    private void onDisconnect(Player plr) {
        Arena arena = plugin.getArenaManager().getPlayerArena(plr);

        if (arena != null) {
            arena.endGame(arena.getOther(plr));
        } else {
            plugin.getQueueManager().remove(plr);
        }

        plugin.getRequestManager().removeAll(plr);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent evt) {
        if (plugin.getArenaManager().isInGame(evt.getPlayer()) &&
                !evt.getPlayer().hasPermission("minoduel.command.bypass") &&
                !evt.getMessage().toLowerCase().startsWith("/1vs1 leave") &&
                !evt.getMessage().toLowerCase().startsWith("/mdu leave")) {
            evt.setCancelled(true);
            evt.getPlayer().sendMessage(MinoDuelPlugin.PREFIX + "§eDu kannst im 1vs1 keine Befehle benutzen!");
        }
    }
}
