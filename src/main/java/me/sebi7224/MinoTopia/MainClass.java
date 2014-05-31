package main.java.me.sebi7224.MinoTopia;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
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
    public static FileConfiguration config;
    public static MainClass plugin;

    public static void setWinnerandLooser(Player winner, Player looser) {

        String winnermessage = ChatColor.translateAlternateColorCodes('&', config.getString("message.winner"));
        winnermessage += winnermessage.replace("%name%", winner.getName());
        String loosermessage = ChatColor.translateAlternateColorCodes('&', config.getString("message.looser"));
        winnermessage += winnermessage.replace("%name%", looser.getName());
        winner.sendMessage(MainClass.prefix + winnermessage);
        looser.sendMessage(MainClass.prefix + loosermessage);

        PlayersinFight.remove(looser);

        winner.teleport(MainClass.PlayerslastLocation.get(winner));
        winner.getInventory().clear();

        int item = config.getInt("item.typeid");
        int amount = config.getInt("item.amount");
        ItemStack item1;
        item1 = new ItemStack(Material.getMaterial(Integer.valueOf(item)));

        ItemStackFactory factory = new ItemStackFactory(item1);
        factory.displayName("§b§l1vs1 §6§lBelohnung");
        factory.amount(amount);
        if (config.get("item.data") != null) {
            factory.materialData((MaterialData) config.get("item.data"));
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

    public void onEnable() {
        config = this.getConfig();
        config.options().copyDefaults(true);
        plugin = this;
        this.getServer().getPluginManager().registerEvents(new MainEvents(), this);
        this.getCommand("1vs1").setExecutor(new MainCommands());

        MainCommands.registerArenaMenu();

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