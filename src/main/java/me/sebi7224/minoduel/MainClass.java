package me.sebi7224.minoduel;

import me.sebi7224.minoduel.arena.Arena;
import me.sebi7224.minoduel.arena.Arenas;
import me.sebi7224.minoduel.cmd.CommandPlayer;
import me.sebi7224.minoduel.util.IconMenu;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.xxyy.common.util.inventory.InventoryHelper;

public class MainClass extends JavaPlugin {

    public static final String PREFIX = "§6[§a1vs1§6] ";
    private static MainClass instance;
    private long teleportDelayTicks;
    private IconMenu arenaMenu;

    @Override
    public void onEnable() {
        instance = this;

        //Initialize config
        saveDefaultConfig(); //Doesn't save it it exists
        getConfig().options().header("MinoDuel config file! Any changes in here will get overridden on reload - Use the ingame config editing commands.");
        getConfig().options().copyHeader(true);
        teleportDelayTicks = getConfig().getLong("tp-delay-seconds") * 20L;

        //Load arenas
        Arenas.reloadArenas(getConfig());

        //Register Bukkit API stuffs
        getServer().getPluginManager().registerEvents(new MainListener(), this);
        getCommand("1vs1").setExecutor(new CommandPlayer(this));

        //Automagically save config every 5 minutes to minimize data-loss on crash
        getServer().getScheduler().runTaskTimer(this, this::saveConfig, 5L * 60L * 20L, 5L * 60L * 20L); //And yes, the compiler does actually optimize that calculation away so quit complaining kthnx
    }

    @Override
    public void onDisable() {
        //Make sure we save the config and make it impossible to override by editing manually.
        saveConfig();
    }


    private void initArenaMenu() {
        arenaMenu = new IconMenu("§8Wähle eine Arena!", //Title
                InventoryHelper.validateInventorySize(Arenas.all().size() + 1), //Round arena amount up to next valid inv size - Need +1 so that at least one "any arena" option is included
                event -> {
                    Player player = event.getPlayer();
                    Arena arena = event.getArena();

                    player.playSound(player.getLocation(), Sound.NOTE_PIANO, 1, 1);
                    WaitingQueueManager.enqueue(player, arena); //This takes care of teleportation etc if a match is found
                    player.sendMessage(getPrefix() + "Du bist nun in der Warteschlange" +
                            (arena == null ? "" : " für die Arena §e" + arena.getName() + "§6") +
                            "!");
                }, this);

        int i = 0;
        for (Arena arena : Arenas.all()) {
            arenaMenu.setArena(i, arena);
            i++;
        }
    }

    public long getTeleportDelayTicks() {
        return teleportDelayTicks;
    }

    public static MainClass instance() {
        return MainClass.getInstance();
    }

    public static String getPrefix() {
        return PREFIX;
    }

    public static MainClass getInstance() {
        return instance;
    }

    public IconMenu getArenaMenu() {
        return arenaMenu;
    }
}
