package me.sebi7224.onevsone;

import io.github.xxyy.common.util.inventory.InventoryHelper;
import me.sebi7224.onevsone.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public class MainClass extends JavaPlugin {

    public static final String PREFIX = "§6[§a1vs1§6] ";
    private static final HashMap<Player, String> playersInQueue = new HashMap<>();
    private static final HashMap<Player, Location> savedLocations = new HashMap<>();
    private static final HashMap<Player, Float> savedExperience = new HashMap<>();
    private static MainClass instance;

    @Override
    public void onEnable() {
        instance = this;

        //Initialize config
        saveDefaultConfig(); //Doesn't save it it exists
        getConfig().options().header("1vs1 config file! Any changes in here will get overridden on reload - Use the ingame config editing commands.");
        getConfig().options().copyHeader(true);

        //Load arenas
        Arena.reloadArenas(getConfig());

        //Register Bukkit API stuffs
        getServer().getPluginManager().registerEvents(new MainListener(), this);
        getCommand("1vs1").setExecutor(new Command1vs1());
        Command1vs1.registerArenaMenu();

        //Automagically save config every 5 minutes to minimize data-loss on crash
        getServer().getScheduler().runTaskTimer(this, this::saveConfig, 5L * 60L * 20L, 5L * 60L * 20L); //And yes, the compiler does actually optimize that calculation away so quit complaining kthnx
    }

    @Override
    public void onDisable() {
        //Make sure we save the config and make it impossible to override by editing manually.
        saveConfig();
    }

    public static MainClass instance() {
        return MainClass.getInstance();
    }

    public static HashMap<Player, String> getPlayersinFight() { //FIXME: Leaks Player ref TODO: This is the wrong way to implement a Queue
        return playersInQueue;
    }

    @Deprecated //PlayerInfo
    public static HashMap<Player, Location> getSavedLocations() { //FIXME: Leaks Player ref
        return savedLocations;
    }

    @Deprecated //PlayerInfo
    public static HashMap<Player, Float> getSavedExperience() { //FIXME: Leaks Player ref
        return savedExperience;
    }

    public static String getPrefix() {
        return PREFIX;
    }

    public static MainClass getInstance() {
        return instance;
    }

    public static void setGameResult(Player winner, Player loser, Arena arena) { //formerly setWinnerAndLoser() TODO: stats saving to database
        //Clean up
        cleanUpPlayer(winner);
        cleanUpPlayer(loser);
        Bukkit.getScheduler().cancelTask(Command1vs1.runningTasks.remove(arena.getName())); //remove() returns the previous value TODO: the Arena class should take care of this

        Bukkit.broadcastMessage(getPrefix() + "§a" + winner.getName() + " §7hat gegen §c" + loser.getName() + " §7 gewonnen! (Arena §6" + arena + "§7)");
        // ^^^^ TODO: winners and losers could get random (fun) messages like in vanilla

        //Treat winner nicely
        arena.getRewards().stream() //Add reward to inventory TODO: should be more random (Class RewardSet or so)
                .forEach(winner.getInventory()::addItem);
    }
}
