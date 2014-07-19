package me.sebi7224.onevsone;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ArenaManager {

    private ArenaManager() {

    }

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
        saveLocation(MainClass.instance().getConfig().getConfigurationSection(path), loc);
        MainClass.instance().saveConfig();
    }

    public static void saveLocation(ConfigurationSection section, Location loc) {
        section.set("world", loc.getWorld().getName());
        section.set("x", loc.getX());
        section.set("y", loc.getY());
        section.set("z", loc.getZ());
        section.set("yaw", loc.getYaw());
        section.set("pitch", loc.getPitch());
    }

    public static Location getLocation(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        String world = section.getString("world");
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float pitch = getFloat(section.getString("pitch"));
        float yaw = getFloat(section.getString("yaw"));

        return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
    }

    public static Location getLocation(String path) {
        return getLocation(MainClass.instance().getConfig().getConfigurationSection(path));
    }

    private static float getFloat(String floatString) {
        if (floatString == null) {
            return 0F;
        }

        try {
            return Float.parseFloat(floatString);
        } catch (NumberFormatException e) {
            return 0F;
        }
    }
}
