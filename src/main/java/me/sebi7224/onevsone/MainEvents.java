package me.sebi7224.onevsone;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.List;

public class MainEvents implements Listener {

    @SuppressWarnings({"rawtypes", "deprecation"})
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if (MainClass.getPlayersinFight().containsKey(e.getEntity())) {
            Player victim = (Player) e.getEntity();
            if (victim.getHealth() - e.getDamage() <= 0) {
                victim.setHealth(victim.getMaxHealth());
                victim.setFoodLevel(10);
                String arena = MainClass.getPlayersinFight().get(e.getEntity());
                List<Player> PlayerinArena = MainCommands.getArenaPlayers(arena);
                PlayerinArena.remove(victim);
                MainClass.setWinnerandLoser(PlayerinArena.get(0), victim, arena);
                victim.teleport(MainClass.getPlayerslastLocation().get(victim));
                MainClass.getPlayerslastLocation().remove(victim);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        if (MainClass.getPlayersinFight().containsKey(e.getPlayer())) {
            String arenaName = MainClass.getPlayersinFight().get(e.getPlayer());
            MainClass.getPlayersinFight().remove(e.getPlayer());
            e.getPlayer().teleport(MainClass.getPlayerslastLocation().get(e.getPlayer()));
            e.getPlayer().getInventory().clear();
            e.getPlayer().setExp(MainClass.getPlayerssavedEXP().get(e.getPlayer()));
            e.getPlayer().updateInventory();
            MainClass.getPlayerssavedEXP().remove(e.getPlayer());
            MainClass.getPlayerslastLocation().remove(e.getPlayer());
            List<Player> playerInArena = MainCommands.getArenaPlayers(arenaName);
            playerInArena.remove(e.getPlayer());
            MainClass.setWinnerandLoser(playerInArena.get(0), e.getPlayer(), arenaName);
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onKick(PlayerKickEvent e) {
        if (MainClass.getPlayersinFight().containsKey(e.getPlayer())) {
            String arenaName = MainClass.getPlayersinFight().get(e.getPlayer());
            MainClass.getPlayersinFight().remove(e.getPlayer());
            e.getPlayer().teleport(MainClass.getPlayerslastLocation().get(e.getPlayer()));
            e.getPlayer().getInventory().clear();
            e.getPlayer().setExp(MainClass.getPlayerssavedEXP().get(e.getPlayer()));
            e.getPlayer().updateInventory();
            MainClass.getPlayerssavedEXP().remove(e.getPlayer());
            MainClass.getPlayerslastLocation().remove(e.getPlayer());
            List<Player> playerInArena = MainCommands.getArenaPlayers(arenaName);
            playerInArena.remove(e.getPlayer());
            MainClass.setWinnerandLoser(playerInArena.get(0), e.getPlayer(), arenaName);
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (MainClass.getPlayersinFight().containsKey(e.getPlayer())) {
            if (!e.getMessage().toLowerCase().startsWith("/1vs1 leave")) {
                if (!e.getPlayer().hasPermission("1vs1.command.bypass")) {
                    e.setCancelled(true);
                    e.getPlayer().sendMessage(MainClass.getPrefix() + "§eDu kannst im 1vs1 keine Commands benutzen!");
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (MainClass.getPlayersinFight().containsKey(e.getPlayer())) {
            e.getPlayer().getInventory().clear();
            e.getPlayer().setExp(MainClass.getPlayerssavedEXP().get(e.getPlayer()));
            MainClass.getPlayerssavedEXP().remove(e.getPlayer());
            MainClass.getPlayerslastLocation().remove(e.getPlayer());
            e.getPlayer().updateInventory();
        }
    }
}