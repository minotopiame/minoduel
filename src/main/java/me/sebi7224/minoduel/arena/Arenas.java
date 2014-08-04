package me.sebi7224.minoduel.arena;

import com.google.common.collect.ImmutableList;
import me.sebi7224.minoduel.MainClass;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.xxyy.common.collections.CaseInsensitiveMap;
import io.github.xxyy.common.lib.com.intellij.annotations.NotNull;
import io.github.xxyy.common.lib.com.intellij.annotations.Nullable;
import io.github.xxyy.common.util.inventory.ItemStackFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Static utility methods for managing MinoDuel arenas.
 * @since 1.0
 */
public final class Arenas {

    static final Map<String, Arena> arenaCache = new CaseInsensitiveMap<>();
    private static List<ItemStack> defaultRewards;
    private static ItemStack anyArenaIcon;
    private static Map<UUID, Arena> playersInGame = new HashMap<>();

    private Arenas() {

    }

    public static boolean isInGame(@NotNull Player plr) {
        return getPlayerArena(plr) != null;
    }

    @Nullable
    public static Arena getPlayerArena(@NotNull Player plr) {
        return playersInGame.get(plr.getUniqueId());
    }

    static void setPlayerArena(@NotNull Player plr, @Nullable Arena arena) {
        if (arena == null) {
            playersInGame.remove(plr.getUniqueId());
        } else {
            playersInGame.put(plr.getUniqueId(), arena);
        }
    }

    @Deprecated
    public static Set<String> getArenas() {
        if (MainClass.instance().getConfig().getConfigurationSection("arenas.") != null) {
            return MainClass.instance().getConfig().getConfigurationSection("arenas.").getKeys(false);
        } else {
            return null;
        }
    }

    @Deprecated
    public static boolean exists(String arena_name) {
        return MainClass.instance().getConfig().contains("arenas." + arena_name);
    }

    @Deprecated
    public static boolean isArenaReady(String arena_name) {
        return MainClass.instance().getConfig().getBoolean("arenas." + arena_name + ".ready");
    }

    public static List<ItemStack> getDefaultRewards() {
        if (defaultRewards == null) {
            //noinspection unchecked
            defaultRewards = (List<ItemStack>) MainClass.instance().getConfig().getList("default-rewards");

            if (defaultRewards == null || defaultRewards.isEmpty()) {
                defaultRewards = Arrays.asList(new ItemStackFactory(Material.DIAMOND).displayName("§61vs1-Belohnung!").produce());
                MainClass.instance().getConfig().set("default-rewards", defaultRewards);
                MainClass.instance().saveConfig();
            }
        }

        return defaultRewards;
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

    /**
     * Gets a semi-random Arena object from the collection of know Arenas.
     *
     * @return Any Arena object
     * @throws IllegalStateException If no arenas are known
     */
    public static Arena any() {
        Validate.isTrue(!arenaCache.isEmpty(), "No arenas known!");
        return arenaCache.values().stream()
                .skip(RandomUtils.nextInt(arenaCache.size()))
                .findFirst().get();
    }

    /**
     * @return An immutable view of all known arenas
     */
    public static Collection<Arena> all() {
        return ImmutableList.copyOf(arenaCache.values());
    }

    /**
     * @return whether any arena if defined
     */
    public static boolean anyExist() {
        return !arenaCache.isEmpty();
    }

    public static boolean existsByName(String name) {
        return arenaCache.containsKey(name);
    }

    /**
     * Gets an Arena from its name.
     *
     * @param name Name of the Arena to get.
     * @return An Arena with the given name or NULL if none exists.
     */
    public static Arena byName(String name) {
        Arena arena = arenaCache.get(name);

        if (arena == null && MainClass.instance().getConfig().contains(Arena.CONFIG_PATH + "." + name)) {
            Arena.reloadArenas(MainClass.getInstance().getConfig());
            arena = arenaCache.get(name);
        }

        return arena;
    }

    /**
     * Creates a new Arena with empty properties. Replaces existing ones.
     *
     * @param name   Name of the new Arena.
     * @param config Configuration backend to use to persist arena information
     * @return the created Arena
     */
    public static Arena createArena(String name, FileConfiguration config) {
        Arena arena = new Arena(config.getConfigurationSection(Arena.CONFIG_PATH).createSection(name));
        arenaCache.put(name, arena);
        return arena;
    }

    /**
     * Reloads arenas from a given {@link org.bukkit.configuration.file.FileConfiguration}. Loads from path {@link Arena#CONFIG_PATH}.
     * Existing objects are modified.
     *
     * @param source Where to get data from
     */
    public static void reloadArenas(FileConfiguration source) {
        Arena.reloadArenas(source);
    }

    /**
     * Gets the "any arena" icon for display in the arena menu.
     * @return the global "any arena" icon
     */
    public static ItemStack getAnyArenaIcon() {
        if (anyArenaIcon == null) {
            anyArenaIcon = MainClass.instance().getConfig().getItemStack("any-arena-icon");

            if (anyArenaIcon == null) {
                anyArenaIcon = new ItemStackFactory(Material.DIRT)
                        .displayName("§eArena egal")
                        .produce();
            }
        }

        return anyArenaIcon;
    }
}
