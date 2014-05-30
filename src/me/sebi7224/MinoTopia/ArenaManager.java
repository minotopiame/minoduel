package me.sebi7224.MinoTopia;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ArenaManager {

    public static void createArena(String arena_name) {
        Plugin.config.set("arenas." + arena_name + ".ready", false);
        Plugin.plugin.saveConfig();
    }

    public static Set<String> getArenas() {
        try {
            return Plugin.config.getConfigurationSection("arenas.").getKeys(false);
        } catch (NullPointerException NPE) {
            return null;
        }
    }

    public static void removeArena(String arena_name) {
        Plugin.config.set("arenas." + arena_name, null);
        Plugin.plugin.saveConfig();
    }

    public static boolean Arena_exists(String arena_name) {
        return Plugin.config.contains("arenas." + arena_name);
    }

    public static boolean isArenaReady(String arena_name) {
        return Plugin.config.getBoolean("arenas." + arena_name + ".ready");

    }

    public static void setArenaReady(String arena_name, boolean ready) {
        Plugin.config.set("arenas." + arena_name + ".ready", ready);
        Plugin.plugin.saveConfig();
    }

    public static List<String> getReadyArenas() {
        Set<String> arenas = getArenas();
        if (arenas == null) return null;
        List<String> ready_arenas = new ArrayList<String>();

        for (String arena : arenas) {
            if (isArenaReady(arena)) ready_arenas.add(arena);
        }

        return ready_arenas;
    }

    public static int ready_arenas() {
        List<String> ready_arenas = getReadyArenas();
        if (ready_arenas == null) return 0;
        else return getReadyArenas().size();
    }

    public static void setItem(String arena_name, Material material) {
        Plugin.config.set("arenas." + arena_name + ".icon", material.name());
        Plugin.plugin.saveConfig();
    }

    public static Material getItem(String arena_name) {
        return Material.getMaterial(Plugin.config.getString("arenas." + arena_name + ".icon"));
    }

    public static void saveLocation(String path, Location loc) {
        Plugin.config.set(path, loc.getWorld().getName() + "_" + loc.getX() + "_" + loc.getY() + "_" + loc.getZ() + "_" + loc.getYaw() + "_" + loc.getPitch());
        Plugin.plugin.saveConfig();
    }

    public static Location getLocation(String path) {
        if (Plugin.config.get(path) == null) return null;
        String[] split = Plugin.config.getString(path).split("_");
        return new Location(Bukkit.getWorld(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]), Float.parseFloat(split[4]), Float.parseFloat(split[5]));
    }
}