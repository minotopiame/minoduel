package me.sebi7224.MinoTopia;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.ArrayList;

public class MainEvents implements Listener {

    @SuppressWarnings({"rawtypes", "deprecation"})
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if (MainClass.PlayersinFight.containsKey(e.getEntity())) {
            Player victim = (Player) e.getEntity();
            if (victim.getHealth() - e.getDamage() <= 0) {
                victim.setHealth(20);
                victim.setFoodLevel(10);
                String arena = MainClass.PlayersinFight.get(e.getEntity());
                ArrayList<Player> PlayerinArena = MainCommands.getArenaPlayers(arena);
                PlayerinArena.remove(victim);
                MainClass.instance().setWinnerandLooser(PlayerinArena.get(0), victim, arena);
                e.getEntity().teleport(MainClass.PlayerslastLocation.get(victim));
                MainClass.PlayerslastLocation.remove(victim);
            }
        }

    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        if (MainClass.PlayersinFight.containsKey(e.getPlayer())) {
            String arena = MainClass.PlayersinFight.get(e.getPlayer());
            MainClass.PlayersinFight.remove(e.getPlayer());
            e.getPlayer().teleport(MainClass.PlayerslastLocation.get(e.getPlayer()));
            e.getPlayer().getInventory().clear();
            e.getPlayer().setExp(MainClass.PlayerssavedEXP.get(e.getPlayer()));
            e.getPlayer().updateInventory();
            MainClass.PlayerssavedEXP.remove(e.getPlayer());
            MainClass.PlayerslastLocation.remove(e.getPlayer());
            ArrayList<Player> player_in_arena = MainCommands.getArenaPlayers(arena);
            player_in_arena.remove(e.getPlayer());
            MainClass.instance().setWinnerandLooser(player_in_arena.get(0), e.getPlayer(), arena);

        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onKick(PlayerKickEvent e) {
        if (MainClass.PlayersinFight.containsKey(e.getPlayer())) {
            String arena = MainClass.PlayersinFight.get(e.getPlayer());
            MainClass.PlayersinFight.remove(e.getPlayer());
            e.getPlayer().teleport(MainClass.PlayerslastLocation.get(e.getPlayer()));
            e.getPlayer().getInventory().clear();
            e.getPlayer().setExp(MainClass.PlayerssavedEXP.get(e.getPlayer()));
            e.getPlayer().updateInventory();
            MainClass.PlayerssavedEXP.remove(e.getPlayer());
            MainClass.PlayerslastLocation.remove(e.getPlayer());
            ArrayList<Player> player_in_arena = MainCommands.getArenaPlayers(arena);
            player_in_arena.remove(e.getPlayer());
            MainClass.instance().setWinnerandLooser(player_in_arena.get(0), e.getPlayer(), arena);

        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (MainClass.PlayersinFight.containsKey(e.getPlayer())) {
            if (!e.getMessage().toLowerCase().startsWith("/1vs1 leave")) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(MainClass.prefix + "§eDu kannst im 1vs1 keine Commands benutzen!");
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (MainClass.PlayersinFight.containsKey(e.getPlayer())) {

            e.getPlayer().getInventory().clear();
            e.getPlayer().setExp(MainClass.PlayerssavedEXP.get(e.getPlayer()));
            MainClass.PlayerssavedEXP.remove(e.getPlayer());
            MainClass.PlayerslastLocation.remove(e.getPlayer());
            e.getPlayer().updateInventory();

        }
    }
}