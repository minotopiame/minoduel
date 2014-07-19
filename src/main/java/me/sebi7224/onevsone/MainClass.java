package me.sebi7224.onevsone;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;

public class MainClass extends JavaPlugin {

    private static final HashMap<Player, String> playersInQueue = new HashMap<>();
    private static final HashMap<Player, Location> savedLocations = new HashMap<>();
    private static final HashMap<Player, Float> savedExperience = new HashMap<>();
    private static String prefix = "§6[§a1vs1§6] ";
    private static MainClass instance;

    public static void setWinnerandLoser(Player winner, Player loser, String arena) {
        int taskid = MainCommands.runningTasks.get(arena);
        Bukkit.getScheduler().cancelTask(taskid);
        MainCommands.runningTasks.remove(arena);
        Bukkit.broadcastMessage(getPrefix() + "§a" + winner.getName() + " §7hat gegen §c" + loser.getName() + " §7im 1vs1 gewonnen! (Arena §6" + arena + "§7)");
        getPlayersinFight().remove(loser);

        winner.teleport(MainClass.getPlayerslastLocation().get(winner));
        winner.getInventory().clear();
        List listb = ArenaManager.getReward(arena);
        ItemStack[] items = (ItemStack[]) listb.toArray(new ItemStack[listb.size()]);
        winner.getInventory().setContents(items);
        winner.setExp(MainClass.getPlayerssavedEXP().get(winner));
        MainClass.getPlayersinFight().remove(winner);
        MainClass.getPlayerssavedEXP().remove(winner);
        MainClass.getPlayerslastLocation().remove(winner);
        winner.getInventory().setHelmet(new ItemStack(Material.AIR));
        winner.getInventory().setChestplate(new ItemStack(Material.AIR));
        winner.getInventory().setLeggings(new ItemStack(Material.AIR));
        winner.getInventory().setBoots(new ItemStack(Material.AIR));

    }

    public static MainClass instance() {
        return MainClass.getInstance();
    }

    public static HashMap<Player, String> getPlayersinFight() {
        return playersInQueue;
    }

    public static HashMap<Player, Location> getPlayerslastLocation() {
        return savedLocations;
    }

    public static HashMap<Player, Float> getPlayerssavedEXP() {
        return savedExperience;
    }

    public static String getPrefix() {
        return prefix;
    }

    public static MainClass getInstance() {
        return instance;
    }

    public static void setInstance(MainClass instance) {
        MainClass.instance = instance;
    }

    public void onEnable() {
        setInstance(this);
        this.getServer().getPluginManager().registerEvents(new MainEvents(), this);
        this.getCommand("1vs1").setExecutor(new MainCommands());
        MainCommands.registerArenaMenu();
    }

    public void onDisable() {


    }
}