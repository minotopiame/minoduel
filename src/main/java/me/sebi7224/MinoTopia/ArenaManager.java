package main.java.me.sebi7224.MinoTopia;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ArenaManager {

    public static void createArena(String arena_name) {
        MainClass.config.set("arenas." + arena_name + ".ready", false);
        MainClass.plugin.saveConfig();
    }

    public static Set<String> getArenas() {
        if (MainClass.config.getConfigurationSection("arenas.") != null) {
            return MainClass.config.getConfigurationSection("arenas.").getKeys(false);
        } else {
            return null;
        }
    }

    public static void removeArena(String arena_name) {
        MainClass.config.set("arenas." + arena_name, null);
        MainClass.plugin.saveConfig();
    }

    public static boolean Arenaexists(String arena_name) {
        return MainClass.config.contains("arenas." + arena_name);
    }

    public static boolean isArenaReady(String arena_name) {
        return MainClass.config.getBoolean("arenas." + arena_name + ".ready");
    }

    public static void setArenaReady(String arena_name, boolean ready) {
        MainClass.config.set("arenas." + arena_name + ".ready", ready);
        MainClass.plugin.saveConfig();
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

    public static void setArenaItem(String arena_name, Material material) {
        MainClass.config.set("arenas." + arena_name + ".icon", material.name());
        MainClass.plugin.saveConfig();
    }

    public static Material getArenaItem(String arena_name) {
        return Material.getMaterial(MainClass.config.getString("arenas." + arena_name + ".icon"));
    }

    public static void saveLocation(String path, Location loc) {
        MainClass.config.set(path + ".world", loc.getWorld().getName());
        MainClass.config.set(path + ".x", loc.getX());
        MainClass.config.set(path + ".y", loc.getY());
        MainClass.config.set(path + ".z", loc.getZ());
        MainClass.config.set(path + ".yaw", loc.getYaw());
        MainClass.config.set(path + ".pitch", loc.getPitch());
        MainClass.plugin.saveConfig();
    }

    public static Location getLocation(String path) {
        if (MainClass.config.get(path) == null) {
            return null;
        }
        String world = MainClass.config.getString(path + ".world");
        double x = MainClass.config.getDouble(path + ".x");
        double y = MainClass.config.getDouble(path + ".y");
        double z = MainClass.config.getDouble(path + ".z");
        float yaw = Float.parseFloat(MainClass.config.getString(path + ".pitch"));
        float pitch = Float.parseFloat(MainClass.config.getString(path + ".pitch"));

        return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
    }
}