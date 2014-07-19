package me.sebi7224.onevsone;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ArenaManager {

    public static void createArena(String arena_name) {
        MainClass.instance().getConfig().set("arenas." + arena_name + ".ready", false);
        MainClass.instance().saveConfig();
    }

    public static Set<String> getArenas() {
        if (MainClass.instance().getConfig().getConfigurationSection("arenas.") != null) {
            return MainClass.instance().getConfig().getConfigurationSection("arenas.").getKeys(false);
        } else {
            return null;
        }
    }

    public static void removeArena(String arena_name) {
        MainClass.instance().getConfig().set("arenas." + arena_name, null);
        MainClass.instance().saveConfig();
    }

    public static boolean exists(String arena_name) {
        return MainClass.instance().getConfig().contains("arenas." + arena_name);
    }

    public static boolean isArenaReady(String arena_name) {
        return MainClass.instance().getConfig().getBoolean("arenas." + arena_name + ".ready");
    }

    public static List getReward(String arena_name) {
        if (MainClass.instance().getConfig().contains("arenas." + arena_name + ".rewards")) {
            return MainClass.instance().getConfig().getList("arenas." + arena_name + ".rewards");
        } else {
            return MainClass.instance().getConfig().getList("globalRewards");
        }
    }

    public static void setArenaReady(String arena_name, boolean ready) {
        MainClass.instance().getConfig().set("arenas." + arena_name + ".ready", ready);
        MainClass.instance().saveConfig();
    }

    public static List<String> getReadyArenas() {
        Set<String> arenas = getArenas();
        if (arenas == null) {
            return null;
        }
        List<String> ready_arenas = new ArrayList<>();

        for (String arena : arenas) {
            if (isArenaReady(arena)) ready_arenas.add(arena);
        }

        return ready_arenas;
    }

    public static int getReadyArenasSize() {
        List<String> ready_arenas = getReadyArenas();
        if (ready_arenas == null) {
            return 0;
        } else {
            return getReadyArenas().size();
        }
    }

    public static void setArenaIconItem(String arena_name, Material material) {
        MainClass.instance().getConfig().set("arenas." + arena_name + ".icon", material.name());
        MainClass.instance().saveConfig();
    }

    public static Material getArenaIconItem(String arena_name) {
        return Material.getMaterial(MainClass.instance().getConfig().getString("arenas." + arena_name + ".icon"));
    }

    public static void saveLocation(String path, Location loc) {
        MainClass.instance().getConfig().set(path + ".world", loc.getWorld().getName());
        MainClass.instance().getConfig().set(path + ".x", loc.getX());
        MainClass.instance().getConfig().set(path + ".y", loc.getY());
        MainClass.instance().getConfig().set(path + ".z", loc.getZ());
        MainClass.instance().getConfig().set(path + ".yaw", loc.getYaw());
        MainClass.instance().getConfig().set(path + ".pitch", loc.getPitch());
        MainClass.instance().saveConfig();
    }

    public static Location getLocation(String path) {
        if (MainClass.instance().getConfig().contains(path)) {
            return null;
        }
        String world = MainClass.instance().getConfig().getString(path + ".world");
        double x = MainClass.instance().getConfig().getDouble(path + ".x");
        double y = MainClass.instance().getConfig().getDouble(path + ".y");
        double z = MainClass.instance().getConfig().getDouble(path + ".z");
        float yaw = Float.parseFloat(MainClass.instance().getConfig().getString(path + ".pitch"));
        float pitch = Float.parseFloat(MainClass.instance().getConfig().getString(path + ".pitch"));

        return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
    }
}