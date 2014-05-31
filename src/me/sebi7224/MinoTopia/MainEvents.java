package me.sebi7224.MinoTopia;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.ArrayList;

public class MainEvents implements Listener {

    @SuppressWarnings({"rawtypes", "deprecation"})
    @EventHandler
    public void onDamage(final EntityDamageByEntityEvent e) {
        if (MainClass.PlayersinFight.containsKey(e.getEntity())) {
            Player player = (Player) e.getEntity();
            if (player.getHealth() == 0) {
                player.setHealth(20);
                player.setFoodLevel(10);
                String arena = MainClass.PlayersinFight.get(e.getEntity());
                ArrayList<Player> PlayerinArena = MainCommands.getArenaPlayers(arena);
                PlayerinArena.remove(e.getEntity());
                MainClass.setWinnerandLooser(PlayerinArena.get(0), (Player) e.getEntity());

                e.getEntity().teleport(MainClass.PlayerslastLocation.get(e.getEntity()));
                MainClass.PlayerslastLocation.remove(e.getEntity());
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
            MainClass.setWinnerandLooser(player_in_arena.get(0), e.getPlayer());

        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (MainClass.PlayersinFight.containsKey(e.getPlayer())) {
            if (MainCommands.getArenaPlayers(MainClass.PlayersinFight.get(e.getPlayer())).size() == 2) {
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