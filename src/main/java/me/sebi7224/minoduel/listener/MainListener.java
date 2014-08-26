package me.sebi7224.minoduel.listener;

import me.sebi7224.minoduel.MinoDuelPlugin;
import me.sebi7224.minoduel.arena.Arena;
import me.sebi7224.minoduel.arena.ArenaPlayerInfo;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MainListener implements Listener {
    private final MinoDuelPlugin plugin;

    public MainListener(MinoDuelPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent evt) {
        plugin.getInventorySaver().loadInventoryWithMessage(evt.getPlayer());
        if (plugin.getLocationSaver().hasLocation(evt.getPlayer())) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> plugin.getLocationSaver().loadLocationWithMessage(evt.getPlayer()), 20L); //xLogin last locations wait 10t
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent evt) {
        if (evt.getEntityType() != EntityType.PLAYER) {
            return;
        }

        Player victim = (Player) evt.getEntity();
        Arena arena = plugin.getArenaManager().getPlayerArena(victim);

        if (arena != null && victim.getHealth() - evt.getFinalDamage() <= 0) {
            arena.endGame(arena.getOther(victim));
            evt.setCancelled(true);
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
            ArenaPlayerInfo opponent = arena.getOther(plr);
            if (opponent.getPlayer() != null) {
                opponent.getPlayer().sendMessage(plugin.getPrefix() + "Dein Gegner hat das Spiel verlassen, daher bist du der Gewinner!");
            }
            arena.endGame(opponent, false, false);
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

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent evt) {
        if (plugin.getArenaManager().isInGame(evt.getPlayer())) {
            evt.setCancelled(true);
            evt.getPlayer().sendMessage(plugin.getPrefix() + "Du kannst im Kampf keine Items droppen!");
        }
    }
}
