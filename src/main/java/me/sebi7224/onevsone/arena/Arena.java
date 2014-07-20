package me.sebi7224.onevsone.arena;

import com.google.common.base.Objects;
import io.github.xxyy.common.collections.CaseInsensitiveMap;
import io.github.xxyy.common.util.inventory.InventoryHelper;
import me.sebi7224.onevsone.Command1vs1;
import me.sebi7224.onevsone.MainClass;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an 1vs1 arena as loaded from configuration.
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 19.7.14
 */
public class Arena {
    public static final String CONFIG_PATH = "arenas";
    private static final String FIRST_SPAWN_PATH = "spawn1";
    private static final String SECOND_SPAWN_PATH = "spawn2";
    private static final String ICON_STACK_PATH = "icon";
    private static final String SPECIFIC_REWARD_PATH = "rewards";
    private static final Map<String, Arena> arenaCache = new CaseInsensitiveMap<>();

    private final String name;
    private ConfigurationSection configSection;
    private Location firstSpawn;
    private Location secondSpawn;
    private ItemStack iconStack;
    private List<ItemStack> specificRewards;
    private PlayerInfo[] currentPlayers = null;

    public Arena(ConfigurationSection storageBackend) {
        this.name = storageBackend.getName();
        this.configSection = storageBackend;
    }

    public void endGame(PlayerInfo winner) {
        Validate.isTrue(winner.getArena().equals(this));
        //Clean up

        Bukkit.getScheduler().cancelTask(Command1vs1.runningTasks.remove(arena.getName())); //remove() returns the previous value TODO: the Arena class should take care of this

        Bukkit.broadcastMessage(getPrefix() + "§a" + winner.getName() + " §7hat gegen §c" + loser.getName() + " §7 gewonnen! (Arena §6" + arena + "§7)");
        // ^^^^ TODO: winners and losers could get random (fun) messages like in vanilla

        //Treat winner nicely
        arena.getRewards().stream() //Add reward to inventory TODO: should be more random (Class RewardSet or so)
                .forEach(winner.getInventory()::addItem);
    }

    ///////////// GETTERS //////////////////////////////////////////////////////////////////////////////////////////////

    public List<ItemStack> getRewards() {
        if (specificRewards == null || specificRewards.isEmpty()) {
            return ArenaManager.getDefaultRewards();
        }

        return specificRewards;
    }

    public boolean isOccupied() {
        return currentPlayers != null;
    }

    public PlayerInfo[] getCurrentPlayers() {
        return currentPlayers;
    }

    public Location getFirstSpawn() {
        return firstSpawn;
    }

    public Location getSecondSpawn() {
        return secondSpawn;
    }

    public ItemStack getIconStack() {
        return iconStack;
    }

    /////// SETTERS ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setPlayers(Player plr1, Player plr2) {
        this.currentPlayers = new PlayerInfo[]{
                new PlayerInfo(plr1),
                new PlayerInfo(plr2)
        };
    }

    public void setFirstSpawn(Location firstSpawn) {
        ArenaManager.saveLocation(configSection.createSection(FIRST_SPAWN_PATH), firstSpawn);
        this.firstSpawn = firstSpawn;
    }

    public void setSecondSpawn(Location secondSpawn) {
        ArenaManager.saveLocation(configSection.createSection(SECOND_SPAWN_PATH), secondSpawn);
        this.secondSpawn = secondSpawn;
    }

    public void setIconStack(ItemStack iconStack) {
        configSection.set(ICON_STACK_PATH, iconStack);
        this.iconStack = iconStack;
    }

    public void setRewards(List<ItemStack> specificRewards) {
        this.configSection.set(SPECIFIC_REWARD_PATH, specificRewards);
        this.specificRewards = specificRewards;
    }

    public String getName() {
        return name;
    }

    // private utility methods
    private void updateFromConfig() {
        this.firstSpawn = ArenaManager.getLocation(configSection.getConfigurationSection("spawn1"));
        this.secondSpawn = ArenaManager.getLocation(configSection.getConfigurationSection("spawn2"));
        this.iconStack = configSection.getItemStack(ICON_STACK_PATH);
        //noinspection unchecked
        this.specificRewards = (List<ItemStack>) configSection.getList(SPECIFIC_REWARD_PATH, new ArrayList<ItemStack>());
    }

    /////////// STATIC UTIL ////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates an Arena from a config section.
     * The name is taken from {@link ConfigurationSection#getName()}, all other properties are optional.
     * Changes on the returned object are written back to the {@link ConfigurationSection}.
     *
     * @param section Section to get an Arena from.
     * @return An Arena corresponding to {@code section}.
     */
    public static Arena fromConfigSection(ConfigurationSection section) {
        Arena arena = new Arena(section);
        arena.updateFromConfig();
        return arena;
    }

    /**
     * Gets an Arena from its name.
     *
     * @param name Name of the Arena to get.
     * @return An Arena with the given name or NULL if none exists.
     */
    public static Arena fromName(String name) {
        Arena arena = arenaCache.get(name);

        if (arena == null && MainClass.instance().getConfig().contains(CONFIG_PATH + "." + name)) {
            reloadArenas(MainClass.getInstance().getConfig());
            arena = arenaCache.get(name);
        }

        return arena;
    }

    public static boolean existsByName(String name) {
        return arenaCache.containsKey(name);
    }

    /**
     * Creates a new Arena with empty properties. Replaces existing ones.
     *
     * @param name   Name of the new Arena.
     * @param config Configuration backend to use to persist arena information
     * @return the created Arena
     */
    public static Arena createArena(String name, FileConfiguration config) {
        Arena arena = new Arena(config.getConfigurationSection(CONFIG_PATH).createSection(name));
        arenaCache.put(name, arena);
        return arena;
    }

    /**
     * Reloads arenas from a given {@link FileConfiguration}. Loads from path {@link #CONFIG_PATH}.
     * Existing objects are modified.
     *
     * @param source Where to get data from
     */
    public static void reloadArenas(FileConfiguration source) {
        Map<String, Arena> existingArenas = new HashMap<>(arenaCache);
        arenaCache.clear();

        if (source.contains(CONFIG_PATH)) {
            ConfigurationSection arenaSection = source.getConfigurationSection(CONFIG_PATH);
            for (String key : arenaSection.getKeys(false)) {
                Arena existingArena = existingArenas.get(key);
                ConfigurationSection section = source.getConfigurationSection(CONFIG_PATH + "." + key);

                if (existingArena == null) {
                    arenaCache.put(key, Arena.fromConfigSection(section));
                } else {
                    existingArena.configSection = section;
                    existingArena.updateFromConfig();
                    arenaCache.put(key, existingArena);
                }
            }
        }
    }

    //////// OVERRIDDEN OBJECT METHODS /////////////////////////////////////////////////////////////////////////////////
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Arena arena = (Arena) o;

        return name.equals(arena.name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("name", name)
                .add("configSection", configSection)
                .add("firstSpawn", firstSpawn)
                .add("secondSpawn", secondSpawn)
                .add("iconStack", iconStack)
                .add("specificRewards", specificRewards)
                .add("currentPlayers", currentPlayers)
                .toString();
    }

    //////////// LE INNER CLASS ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Stores information about a player playing in 1vs1.
     *
     * @author <a href="http://xxyy.github.io/">xxyy</a>
     * @since 20.7.14
     */
    public class PlayerInfo {
        private final int previousExperience;
        private final Location previousLocation;
        private Player player;
        private boolean valid = true;

        public PlayerInfo(Player plr) {
            this.player = plr;
            this.previousExperience = plr.getTotalExperience();
            this.previousLocation = plr.getLocation();
        }

        /**
         * Invalidates this PlayerInfo, removing the player from the game.
         * This teleports them back to their previous location and resets their experience to the previous value.
         * This also clears their inventory.
         * <b>This doesn't, however, remove them from the player list of the associated {@link me.sebi7224.onevsone.arena.Arena}!</b>
         */
        protected void invalidate() {
            this.valid = false;
            player.setTotalExperience(previousExperience);
            player.teleport(previousLocation);
            InventoryHelper.clearInventory(player);
            MainClass.getPlayersinFight().remove(player);
            this.player = null; //Don't keep Player ref in case this object is accidentally kept
        }

        /**
         * This gets the Arena object associated with this PlayerInfo.
         * This always returns NULL if {@link #isValid()} returns FALSE.
         * @return the associated Arena or NULL if none.
         */
        public Arena getArena() {
            return isValid() ? Arena.this : null;
        }

        /**
         * @return whether this information is still valid, i.e. the player is still playing in that arena.
         */
        public boolean isValid() {
            return valid;
        }
    }
}
