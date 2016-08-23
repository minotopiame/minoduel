package me.minotopia.minoduel.arena;

import com.google.common.collect.ImmutableList;
import li.l1t.common.collections.CaseInsensitiveMap;
import li.l1t.common.util.inventory.ItemStackFactory;
import me.minotopia.minoduel.MinoDuelPlugin;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.math.RandomUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Static utility methods for managing MinoDuel arenas.
 *
 * @since 1.0
 */
public class ArenaManager {

    public static final String DEFAULT_REWARDS_PATH = "default-rewards";

    private final File arenaConfigFile;
    private final YamlConfiguration arenaConfig;
    protected final Map<String, MinoDuelArena> arenaCache = new CaseInsensitiveMap<>();
    private final Map<UUID, Arena> playersInGame = new HashMap<>();
    private final MinoDuelPlugin plugin;
    private ArenaMenu arenaMenu;
    private List<ItemStack> defaultRewards;
    private ItemStack anyArenaIcon;

    public ArenaManager(MinoDuelPlugin plugin) {
        this.plugin = plugin;
        arenaConfigFile = new File(plugin.getDataFolder(), "arenas.yml");
        if(!arenaConfigFile.exists()) {
            try {
                Files.createFile(arenaConfigFile.toPath());
            } catch (IOException e) {
                throw new RuntimeException("Couldn't create arena config", e);
            }
        }
        arenaConfig = YamlConfiguration.loadConfiguration(arenaConfigFile);

        if (plugin.getConfig().contains(MinoDuelArena.CONFIG_PATH)){
            arenaConfig.createSection(MinoDuelArena.CONFIG_PATH,
                    plugin.getConfig().getConfigurationSection(MinoDuelArena.CONFIG_PATH).getValues(true));
            plugin.getConfig().set(MinoDuelArena.CONFIG_PATH, null);
            saveArena(null);
            plugin.saveConfig();
        }
    }

    public void initialise() {
        this.arenaMenu = new ArenaMenu(this, plugin);
    }

    public boolean isInGame(@Nonnull Player plr) {
        return getPlayerArena(plr) != null;
    }

    @Nullable
    public Arena getPlayerArena(@Nonnull Player plr) {
        return playersInGame.get(plr.getUniqueId());
    }

    protected void setPlayerArena(@Nonnull Player plr, @Nullable Arena arena) {
        if (arena == null){
            playersInGame.remove(plr.getUniqueId());
        } else {
            playersInGame.put(plr.getUniqueId(), arena);
        }
    }

    @SuppressWarnings("unchecked")
    public List<ItemStack> getDefaultRewards() {
        if (defaultRewards == null){
            defaultRewards = (List<ItemStack>) plugin.getConfig().getList(DEFAULT_REWARDS_PATH);

            if (defaultRewards == null || defaultRewards.isEmpty()){
                defaultRewards = Collections.singletonList(new ItemStackFactory(Material.DIAMOND).displayName("§61vs1-Belohnung!").produce());
                plugin.getConfig().set(DEFAULT_REWARDS_PATH, defaultRewards);
                plugin.saveConfig();
            }
        }

        return defaultRewards;
    }

    public void setDefaultRewards(List<ItemStack> defaultRewards) {
        defaultRewards.removeIf(stack -> stack == null || stack.getType() == Material.AIR);
        Validate.notEmpty(defaultRewards, "Cannot set no rewards!");

        plugin.getConfig().set(DEFAULT_REWARDS_PATH, defaultRewards);
        this.defaultRewards = defaultRewards;
        plugin.saveConfig();
    }

    public void saveLocation(ConfigurationSection section, Location loc) {
        section.set("world", loc.getWorld().getName());
        section.set("x", loc.getX());
        section.set("y", loc.getY());
        section.set("z", loc.getZ());
        section.set("yaw", loc.getYaw());
        section.set("pitch", loc.getPitch());
    }

    public Location getLocation(ConfigurationSection section) {
        if (section == null){
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

    private float getFloat(String floatString) {
        if (floatString == null){
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
    public Arena any() {
        Validate.isTrue(!arenaCache.isEmpty(), "No arenas known!");
        return arenaCache.values().stream()
                .skip(RandomUtils.nextInt(arenaCache.size()))
                .findFirst().get();
    }

    /**
     * Gets the first ready arena.
     *
     * @return a ready arena or NULL if no arenas are ready.
     */
    public Arena firstReady() {
        return arenaCache.values().stream()
                .filter(Arena::isReady)
                .findAny().orElse(null);
    }

    /**
     * @return An immutable view of all known arenas
     */
    public Collection<Arena> all() {
        return ImmutableList.copyOf(arenaCache.values());
    }

    /**
     * @return whether any arena if defined
     */
    public boolean anyExist() {
        return !arenaCache.isEmpty();
    }

    public boolean existsByName(String name) {
        return arenaCache.containsKey(name);
    }

    /**
     * Gets an Arena from its name.
     *
     * @param name Name of the Arena to get.
     * @return An Arena with the given name or NULL if none exists.
     */
    public Arena byName(String name) {
        Arena arena = arenaCache.get(name);

        if (arena == null && arenaConfig.contains(MinoDuelArena.CONFIG_PATH + "." + name)){
            reloadArenas();
            arena = arenaCache.get(name);
        }

        return arena;
    }

    /**
     * Creates a new Arena with empty properties. Replaces existing ones.
     *
     * @param name   the name of the new Arena
     * @return the created Arena
     */
    public Arena createArena(String name) {
        if (!arenaConfig.contains(MinoDuelArena.CONFIG_PATH)){
            arenaConfig.createSection(MinoDuelArena.CONFIG_PATH);
        }

        MinoDuelArena arena = new MinoDuelArena(
                arenaConfig.getConfigurationSection(MinoDuelArena.CONFIG_PATH).createSection(name),
                this
        );

        arenaCache.put(name, arena);
        saveArena(arena);

        return arena;
    }

    /**
     * Reloads arenas from a given {@link org.bukkit.configuration.file.FileConfiguration}. Loads from path {@link MinoDuelArena#CONFIG_PATH}.
     * Existing objects are modified.
     */
    public void reloadArenas() {
        Map<String, MinoDuelArena> existingArenas = new HashMap<>(arenaCache);
        arenaCache.clear();

        if (arenaConfig.contains(MinoDuelArena.CONFIG_PATH)){
            ConfigurationSection arenaSection = arenaConfig.getConfigurationSection(MinoDuelArena.CONFIG_PATH);
            for (String key : arenaSection.getKeys(false)) {
                MinoDuelArena existingArena = existingArenas.get(key);
                ConfigurationSection section = arenaSection.getConfigurationSection(key);

                if (existingArena == null){
                    arenaCache.put(key, MinoDuelArena.fromConfigSection(section, this));
                } else {
                    existingArena.updateFrom(section);
                    arenaCache.put(key, existingArena);
                    existingArenas.remove(key);
                }
            }
        }

        for (Arena removedArena : existingArenas.values()) {
            removedArena.getPlayers()
                    .forEach(pi -> pi.getPlayer().sendMessage("§cDeine Arena wurde entfernt. Bitte entschuldige die Unannehmlichkeiten!"));
            removedArena.endGame(null, false);
        }

        arenaMenu.refresh();
    }

    /**
     * Gets the "any arena" icon for display in the arena menu.
     *
     * @return the global "any arena" icon
     */
    public ItemStack getAnyArenaIcon() {
        if (anyArenaIcon == null){
            anyArenaIcon = plugin.getConfig().getItemStack("any-arena-icon");

            if (anyArenaIcon == null){
                anyArenaIcon = new ItemStackFactory(Material.DIRT)
                        .displayName("§eArena egal")
                        .produce();
            }
        }

        return anyArenaIcon;
    }

    public ArenaMenu getArenaMenu() {
        return arenaMenu;
    }

    public void onReload() {
        arenaCache.values().stream()
                .filter(MinoDuelArena::isOccupied).forEach(arena -> {
            arena.getPlayers().forEach(plr -> plr.getPlayer().sendMessage(plugin.getPrefix() + "Dein 1vs1 wurde aufgrund eines Reloads beendet! Wir bitten, die Unannehmlichkeiten zu entschuldigen."));
            arena.endGame(null, false);
        });
    }

    protected void registerValidityChange(@SuppressWarnings("UnusedParameters") Arena arena) {
        arenaMenu.refresh();
    }

    public void saveArena(@SuppressWarnings("UnusedParameters") @Nullable Arena arena) { //null saves all
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                arenaConfig.save(arenaConfigFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save arena config: ", e);
            }
        });
    }

    public MinoDuelPlugin getPlugin() {
        return plugin;
    }

    public YamlConfiguration getArenaConfig() {
        return arenaConfig;
    }
}
