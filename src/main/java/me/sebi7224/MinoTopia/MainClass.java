package me.sebi7224.MinoTopia;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public class MainClass extends JavaPlugin {

    public static final HashMap<Player, String> PlayersinFight = new HashMap<>();
    public static final HashMap<Player, Location> PlayerslastLocation = new HashMap<>();
    public static final HashMap<Player, Float> PlayerssavedEXP = new HashMap<>();
    public static String prefix = "§6[§a1vs1§6] ";
    private static MainClass instance;

    public static void setWinnerandLooser(Player winner, Player looser, String arena) {
        int taskid = MainCommands.runningTasks.get(arena);
        Bukkit.getScheduler().cancelTask(taskid);
        MainCommands.runningTasks.remove(arena);
        Bukkit.broadcastMessage(prefix + "§a" + winner.getName() + " §7hat gegen §c" + looser.getName() + " §7im 1vs1 gewonnen! (Arena §6" + arena + "§7)");
        PlayersinFight.remove(looser);

        winner.teleport(MainClass.PlayerslastLocation.get(winner));
        winner.getInventory().clear();

        int item = MainClass.instance().getConfig().getInt("item.typeid");
        int amount = MainClass.instance().getConfig().getInt("item.amount");
        ItemStack item1;
        item1 = new ItemStack(Material.getMaterial(Integer.valueOf(item)));

        ItemStackFactory factory = new ItemStackFactory(item1);
        factory.displayName("§b§l1vs1 §6§lBelohnung");
        factory.amount(amount);
        if (MainClass.instance().getConfig().get("item.data") != null) {
            factory.materialData((MaterialData) MainClass.instance().getConfig().get("item.data"));
        }

        winner.setExp(MainClass.PlayerssavedEXP.get(winner));
        winner.getInventory().addItem(factory.produce());
        MainClass.PlayersinFight.remove(winner);
        MainClass.PlayerssavedEXP.remove(winner);
        MainClass.PlayerslastLocation.remove(winner);
        winner.getInventory().setHelmet(new ItemStack(Material.AIR));
        winner.getInventory().setChestplate(new ItemStack(Material.AIR));
        winner.getInventory().setLeggings(new ItemStack(Material.AIR));
        winner.getInventory().setBoots(new ItemStack(Material.AIR));

    }

    public static MainClass instance() {
        return MainClass.instance;
    }

    public void onEnable() {
        instance = this;
        this.getServer().getPluginManager().registerEvents(new MainEvents(), this);
        this.getCommand("1vs1").setExecutor(new MainCommands());
        MainCommands.registerArenaMenu();
    }

    public void onDisable() {


    }
}