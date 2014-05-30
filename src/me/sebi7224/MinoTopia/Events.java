package me.sebi7224.MinoTopia;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class Events implements Listener {

    @SuppressWarnings({"rawtypes", "deprecation"})
    @EventHandler
    public void onDeath(final PlayerDeathEvent e) {
        if (Plugin.ingame.containsKey(e.getEntity())) {
            String arena = Plugin.ingame.get(e.getEntity());
            ArrayList<Player> player_in_arena = Commands.getArenaPlayer(arena);
            player_in_arena.remove(e.getEntity());
            Plugin.readyPlayer(player_in_arena.get(0), e.getEntity());

            new BukkitRunnable() {
                public void run() {
                    try {
                        Object nmsPlayer = e.getEntity().getClass().getMethod("getHandle").invoke(e.getEntity());
                        Object con = nmsPlayer.getClass().getDeclaredField("playerConnection").get(nmsPlayer);

                        Class<?> EntityPlayer = Class.forName(nmsPlayer.getClass().getPackage().getName() + ".EntityPlayer");

                        Field minecraftServer = con.getClass().getDeclaredField("minecraftServer");
                        minecraftServer.setAccessible(true);
                        Object mcserver = minecraftServer.get(con);

                        Object playerlist = mcserver.getClass().getDeclaredMethod("getPlayerList").invoke(mcserver);
                        Method moveToWorld = playerlist.getClass().getMethod("moveToWorld", EntityPlayer, int.class, boolean.class);
                        moveToWorld.invoke(playerlist, nmsPlayer, 0, false);

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                }
            }.runTaskLater(Plugin.plugin, 2);
            e.getEntity().teleport(Plugin.lastlocation.get(e.getEntity()));
            Plugin.lastlocation.remove(e.getEntity());
            e.getDrops().clear();
        }
    }

    @SuppressWarnings({"deprecation", "rawtypes"})
    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        if (Plugin.ingame.containsKey(e.getPlayer())) {
            String arena = Plugin.ingame.get(e.getPlayer());
            Plugin.ingame.remove(e.getPlayer());
            e.getPlayer().teleport(Plugin.lastlocation.get(e.getPlayer()));
            e.getPlayer().getInventory().clear();
            e.getPlayer().setExp(Plugin.savedxp.get(e.getPlayer()));
            e.getPlayer().updateInventory();
            Plugin.savedxp.remove(e.getPlayer());
            Plugin.lastlocation.remove(e.getPlayer());
            ArrayList<Player> player_in_arena = Commands.getArenaPlayer(arena);
            player_in_arena.remove(e.getPlayer());
            Plugin.readyPlayer(player_in_arena.get(0), e.getPlayer());

        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (Plugin.ingame.containsKey(e.getPlayer())) {
            if (Commands.getArenaPlayer(Plugin.ingame.get(e.getPlayer())).size() == 2) {
                if (e.getMessage().startsWith("/")) {
                    e.setCancelled(true);
                    e.getPlayer().sendMessage(Plugin.prefix + "§eDu kannst im 1vs1 keine Commands benutzen!");
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (Plugin.ingame.containsKey(e.getPlayer())) {

            e.getPlayer().getInventory().clear();
            e.getPlayer().setExp(Plugin.savedxp.get(e.getPlayer()));
            Plugin.savedxp.remove(e.getPlayer());
            Plugin.lastlocation.remove(e.getPlayer());
            e.getPlayer().updateInventory();

        }
    }
}