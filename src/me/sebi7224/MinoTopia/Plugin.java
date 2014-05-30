package me.sebi7224.MinoTopia;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public class Plugin extends JavaPlugin {

    public static final String prefix = "§6[§a1vs1§6] ";
    public static final HashMap<Player, String> ingame = new HashMap<Player, String>();
    public static final HashMap<Player, Location> lastlocation = new HashMap<Player, Location>();
    public static final HashMap<Player, Float> savedxp = new HashMap<Player, Float>();
    public static FileConfiguration config;
    public static Plugin plugin;

    public static void readyPlayer(Player winner, Player looser) {

        winner.sendMessage(Plugin.prefix + "§6Der Gewinner steht fest: §4" + winner.getName());
        winner.sendMessage(Plugin.prefix + "§6Herzlichen Glückwunsch!!!");
        looser.sendMessage(Plugin.prefix + "§6Der Gewinner steht fest: §4" + winner.getName());

        ingame.remove(looser);

        winner.teleport(Plugin.lastlocation.get(winner));
        winner.getInventory().clear();

        String[] item = Plugin.config.getString("item.typeid").split(":");
        int amount = Plugin.config.getInt("item.amount");
        ItemStack item1;
        if (Plugin.config.getString("item.data") != null) {
            item1 = new ItemStack(Integer.valueOf(item[0]), amount, Byte.valueOf(Plugin.config.getString("item.data")));
        } else {
            item1 = new ItemStack(Integer.valueOf(item[0]), amount);
        }

        ItemMeta item_meta = item1.getItemMeta();
        item_meta.setDisplayName("§6§l1vs1 §b§lBelohnung");
        item1.setItemMeta(item_meta);

        winner.setExp(Plugin.savedxp.get(winner));
        winner.getInventory().addItem(item1);
        Plugin.ingame.remove(winner);
        Plugin.savedxp.remove(winner);
        Plugin.lastlocation.remove(winner);
        winner.getInventory().setHelmet(new ItemStack(Material.AIR));
        winner.getInventory().setChestplate(new ItemStack(Material.AIR));
        winner.getInventory().setLeggings(new ItemStack(Material.AIR));
        winner.getInventory().setBoots(new ItemStack(Material.AIR));
    }

    public void onEnable() {
        config = this.getConfig();
        config.options().copyDefaults(true);
        plugin = this;

        this.getServer().getPluginManager().registerEvents(new Events(), this);
        this.getCommand("1vs1").setExecutor(new Commands());

        Commands.registerArenaMenu();

        loadConfig();
    }

    public void onDisable() {


    }

    private void loadConfig() {
        config.options().copyDefaults(true);
        config.options().header("1vs1 by Sebi7224");
        config.options().copyHeader(true);
        this.saveConfig();

    }
}