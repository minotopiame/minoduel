package me.sebi7224.onevsone;

import me.sebi7224.onevsone.arena.Arena;
import me.sebi7224.onevsone.arena.Arenas;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MainListener implements Listener {

    @EventHandler
    public void onEntityDamage(EntityDamageEvent evt) {
        if (evt.getEntityType() != EntityType.PLAYER) {
            return;
        }

        Player victim = (Player) evt.getEntity();
        Arena arena = Arenas.getPlayerArena(victim);

        if (arena != null && victim.getHealth() - evt.getDamage() <= 0) {
            arena.endGame(arena.getOther(victim));
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent evt) {
        Arena arena = Arenas.getPlayerArena(evt.getPlayer());

        if (arena != null) {
            arena.endGame(arena.getOther(evt.getPlayer()));
        }
    }

    @EventHandler
    public void onKick(PlayerKickEvent evt) {
        Arena arena = Arenas.getPlayerArena(evt.getPlayer());

        if (arena != null) {
            arena.endGame(arena.getOther(evt.getPlayer()));
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent evt) {
        if (Arenas.isInGame(evt.getPlayer()) &&
                !evt.getPlayer().hasPermission("1vs1.command.bypass") &&
                !evt.getMessage().toLowerCase().startsWith("/1vs1 leave")) {
            evt.setCancelled(true);
            evt.getPlayer().sendMessage(MainClass.getPrefix() + "§eDu kannst im 1vs1 keine Befehle benutzen!");
        }
    }
}
