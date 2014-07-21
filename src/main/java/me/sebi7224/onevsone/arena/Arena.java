package me.sebi7224.onevsone.arena;

import com.google.common.base.Objects;
import io.github.xxyy.common.collections.CaseInsensitiveMap;
import io.github.xxyy.common.collections.Couple;
import io.github.xxyy.common.util.inventory.InventoryHelper;
import io.github.xxyy.common.util.task.NonAsyncBukkitRunnable;
import me.sebi7224.onevsone.MainClass;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

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
    private static final String INVENTORY_KIT_PATH = "items";
    private static final String ARMOR_KIT_PATH = "armor";
    private static final Map<String, Arena> arenaCache = new CaseInsensitiveMap<>();

    private final String name;
    private ConfigurationSection configSection;
    private Location firstSpawn;
    private Location secondSpawn;
    private ItemStack iconStack;
    private List<ItemStack> specificRewards;
    private ItemStack[] inventoryKit;
    private ItemStack[] armorKit;

    private Couple<PlayerInfo> currentPlayers = null;
    private RunnableArenaTick tickTask = new RunnableArenaTick();

    public Arena(ConfigurationSection storageBackend) {
        this.name = storageBackend.getName();
        this.configSection = storageBackend;
    }

    public void startGame(Player plr1, Player plr2) {
        Validate.isTrue(currentPlayers == null);

        this.currentPlayers = new Couple<>(new PlayerInfo(plr1), new PlayerInfo(plr2));

        this.currentPlayers.getLeft().getPlayer().teleport(getFirstSpawn());
        this.currentPlayers.getRight().getPlayer().teleport(getSecondSpawn());

        this.currentPlayers.forEach(PlayerInfo::sendStartMessage);

        this.currentPlayers.forEach(pi -> {
            Player plr = pi.getPlayer();
            plr.setFireTicks(0);
            plr.setHealth(plr.getMaxHealth());
            plr.setFoodLevel(20);
            plr.getInventory().setContents(getInventoryKit());
            plr.getInventory().setArmorContents(getArmorKit());
            plr.setGameMode(GameMode.SURVIVAL);
            //noinspection deprecation
            plr.updateInventory();
            plr.closeInventory();
            plr.setFlying(false);
            plr.playSound(plr.getLocation(), Sound.ENDERMAN_TELEPORT, 1, 0);
        });
    }

    /**
     * Ends this game, gives the winner their reward (if applicable)
     *
     * @param winner The winner of the game or NULL if no winner could be determined.
     */
    public void endGame(PlayerInfo winner) {
        Validate.isTrue(winner == null || winner.getArena().equals(this));
        Validate.isTrue(currentPlayers != null);

        //Clean up players - teleport them back etc
        currentPlayers.forEach(PlayerInfo::invalidate);

        tickTask.reset();

        if (winner != null) { //A winner has been determined
            PlayerInfo loser = currentPlayers.getOther(winner);

            Bukkit.broadcastMessage(MainClass.getPrefix() + "§a" + winner.getPlayer().getName() + " §7hat gegen §c" + loser.getPlayer().getName() + " §7 gewonnen! (§6" + this.getName() + "§7)");
            // ^^^^ TODO: winners and losers could get random (fun) messages like in vanilla

            //Treat winner nicely
            getRewards().stream() //Add reward to inventory TODO: should be more random (Class RewardSet or so)
                    .forEach(winner.getPlayer().getInventory()::addItem);
        } else {
            Bukkit.broadcastMessage(MainClass.getPrefix() + "§7Der Kampf zwischen §a" +
                    currentPlayers.getLeft().getPlayer().getName() +
                    "§7 und §a" + currentPlayers.getRight().getPlayer().getName() +
                    " §7 is unentschieden ausgegangen! (§6" + this.getName() + "§7)");
        }

        currentPlayers = null;
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

    public Couple<PlayerInfo> getCurrentPlayers() {
        return currentPlayers;
    }

    public Location getFirstSpawn() {
        return firstSpawn;
    }

    public Location getSecondSpawn() {
        return secondSpawn;
    }

    public ItemStack getIconStack() {
        return iconStack.clone();
    }

    public ItemStack[] getInventoryKit() {
        return inventoryKit;
    }

    public ItemStack[] getArmorKit() {
        return armorKit;
    }

    /////// SETTERS ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setArmorKit(ItemStack[] armorKit) {
        this.armorKit = armorKit;
    }

    public void setInventoryKit(ItemStack[] inventoryKit) {
        this.inventoryKit = inventoryKit;
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
    @SuppressWarnings("unchecked")
    private void updateFromConfig() {
        this.firstSpawn = ArenaManager.getLocation(configSection.getConfigurationSection("spawn1"));
        this.secondSpawn = ArenaManager.getLocation(configSection.getConfigurationSection("spawn2"));
        this.iconStack = configSection.getItemStack(ICON_STACK_PATH);
        this.specificRewards = (List<ItemStack>) configSection.getList(SPECIFIC_REWARD_PATH, new ArrayList<ItemStack>());

        if(configSection.contains(INVENTORY_KIT_PATH)) {
            List<ItemStack> tempInvKit = (List<ItemStack>) configSection.getList(INVENTORY_KIT_PATH);
            this.inventoryKit = tempInvKit == null ? null : tempInvKit.toArray(new ItemStack[InventoryType.PLAYER.getDefaultSize()]);
        }

        if(configSection.contains(ARMOR_KIT_PATH)) {
            List<ItemStack> tempInvKit = (List<ItemStack>) configSection.getList(ARMOR_KIT_PATH);
            this.inventoryKit = tempInvKit == null ? null : tempInvKit.toArray(new ItemStack[4]);
        }
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
    public static Arena byName(String name) {
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
                    existingArenas.remove(key);
                }
            }
        }

        for (Arena removedArena : existingArenas.values()) {
            removedArena.getCurrentPlayers()
                    .forEach(pi -> pi.getPlayer().sendMessage("§cDeine Arena wurde entfernt. Bitte entschuldige die Unannehmlichkeiten!"));
            removedArena.endGame(null);
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

    ///////////// BEST RUNNABLE ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Runnable which ticks Arenas every 5 seconds.
     */
    private class RunnableArenaTick extends NonAsyncBukkitRunnable {
        private static final int TICKS_IN_A_GAME = 60; //Ticks every 5 seconds
        private int ticksLeft = 0;

        public void start() {
            this.ticksLeft = TICKS_IN_A_GAME;
            this.runTaskTimer(MainClass.instance(), 20L * 5L, 20L * 5L); //This task so far only announces time left and the smallest interval is 5 seconds
        }

        public void reset() {
            this.ticksLeft = 0;
            this.tryCancel();
        }

        @Override
        public void run() {
            ticksLeft--; //Ticks, as in 1vs1 ticks, not to be confused with game ticks

            //Announce full minutes
            if (ticksLeft % 12 == 0) { //every minute
                getCurrentPlayers().stream()
                        .forEach(pi -> pi.getPlayer().sendMessage(MainClass.getPrefix() + "§7Noch §e" + ticksLeft / 12 + " §7Minuten!"));
            } else if (ticksLeft == 6 || ticksLeft < 4) { //30, 15, 10 & 5 seconds before end
                getCurrentPlayers().stream()
                        .forEach(pi -> pi.getPlayer().sendMessage(MainClass.getPrefix() + "§7Noch §e" + ticksLeft * 5 + " §7Sekunden!"));
            } else if (ticksLeft == 0) {
                Arena.this.endGame(null);
            }
        }
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
        private final String name;
        private Player player;
        private boolean valid = true;

        protected PlayerInfo(Player plr) {
            this.player = plr;
            this.name = plr.getName();
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
         *
         * @return the associated Arena or NULL if none.
         */
        public Arena getArena() {
            return isValid() ? Arena.this : null;
        }

        /**
         * Returns the Player object backing this PlayerInfo.
         * This returns NULL if {@link #isValid()} returns FALSE.
         *
         * @return the associated Player object or NULL if none.
         */
        public Player getPlayer() {
            return isValid() ? player : null;
        }

        /**
         * Returns the initial name of the wrapped player. Still works if {@link #isValid()} returns FALSE.
         *
         * @return The name of the wrapped Player.
         */
        public String getName() {
            return name;
        }

        /**
         * @return whether this information is still valid, i.e. the player is still playing in that arena.
         */
        public boolean isValid() {
            return valid;
        }

        protected void sendStartMessage() {
            getPlayer().sendMessage(MainClass.getPrefix() + "§eDu kämpfst jetzt gegen §a" +
                    Arena.this.getCurrentPlayers().getOther(this).getName() + "§6 (" + Arena.this.getName() + ")");
            getPlayer().sendMessage(MainClass.getPrefix() + "§eMögen die Spiele beginnen!");
        }
    }
}
