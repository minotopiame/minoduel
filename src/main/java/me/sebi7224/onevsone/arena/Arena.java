package me.sebi7224.onevsone.arena;

import com.google.common.base.Objects;
import me.sebi7224.onevsone.MainClass;
import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import io.github.xxyy.common.checklist.Checklist;
import io.github.xxyy.common.checklist.renderer.CommandSenderRenderer;
import io.github.xxyy.common.collections.Couple;
import io.github.xxyy.common.games.util.RunnableTeleportLater;
import io.github.xxyy.common.lib.com.intellij.annotations.NotNull;
import io.github.xxyy.common.lib.com.intellij.annotations.Nullable;
import io.github.xxyy.common.util.inventory.InventoryHelper;
import io.github.xxyy.common.util.task.NonAsyncBukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Represents an 1vs1 arena as loaded from configuration.
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 19.7.14
 */
public class Arena { //TODO: It should be possible to put arenas out of service temporarily
    public static final String CONFIG_PATH = "arenas";

    private static final String FIRST_SPAWN_PATH = "spawn1";
    private static final String SECOND_SPAWN_PATH = "spawn2";
    private static final String ICON_STACK_PATH = "icon";
    private static final String SPECIFIC_REWARD_PATH = "rewards";
    private static final String INVENTORY_KIT_PATH = "items";
    private static final String ARMOR_KIT_PATH = "armor";

    private static final CommandSenderRenderer CHECKLIST_RENDERER = new CommandSenderRenderer.Builder()
            .brackets(true).uncheckedEmpty(false).build();

    private final String name;
    @Nullable
    private ConfigurationSection configSection;
    private Location firstSpawn;
    private Location secondSpawn;
    private ItemStack iconStack;
    private List<ItemStack> specificRewards;
    private ItemStack[] inventoryKit;
    private ItemStack[] armorKit;

    private Couple<PlayerInfo> players = null;
    private RunnableArenaTick tickTask = new RunnableArenaTick();
    private Checklist validityChecklist = new Checklist()
            .append("Arena gelöscht!",() -> configSection != null)
            .append("Spawn 1 gesetzt", () -> firstSpawn != null)
            .append("Spawn 2 gesetzt", () -> secondSpawn != null)
            .append("Kit gesetzt", () -> inventoryKit != null && armorKit != null)
            .append("Icon gesetzt", () -> iconStack != null);

    public Arena(@NotNull ConfigurationSection storageBackend) {
        this.name = storageBackend.getName();
        this.configSection = storageBackend;
    }

    public void scheduleGame(@NotNull Player plr1, @NotNull Player plr2) {
        Validate.isTrue(isReady(), "This arena is currently not ready");
        Validate.notNull(plr1, "Player one is null");
        Validate.notNull(plr2, "Player two is null");

        this.players = new Couple<>(
                new PlayerInfo(plr1, getFirstSpawn()),
                new PlayerInfo(plr2, getSecondSpawn())
        );

        players.forEach(PlayerInfo::sendTeleportMessage);
        players.forEach(this::teleportLater);
    }

    @SuppressWarnings("deprecation") //updateInventory
    private void startGame() {
        Validate.validState(isValid(), "This arena is currently not valid");
        Validate.validState(isOccupied(), "Cannot start game in empty arena!");
        Validate.validState(players.getLeft().isValid(), "left player is invalid: " + players.getLeft().getName());
        Validate.validState(players.getRight().isValid(), "right player is invalid: " + players.getRight().getName());

        this.players.forEach(PlayerInfo::sendStartMessage);

        this.players.forEach(pi -> {
            Player plr = pi.getPlayer();
            plr.setFireTicks(0);
            plr.setHealth(plr.getMaxHealth());
            plr.setFoodLevel(20);
            plr.getInventory().setContents(getInventoryKit());
            plr.getInventory().setArmorContents(getArmorKit());
            plr.setGameMode(GameMode.SURVIVAL);
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
        endGame(winner, true);
    }

    /**
     * Ends this game, gives the winner their reward (if applicable)
     *
     * @param winner               The winner of the game or NULL if no winner could be determined.
     * @param sendUndecidedMessage whether to send the "no winner could be determined" message if {@code winner} is NULL
     */
    public void endGame(PlayerInfo winner, boolean sendUndecidedMessage) {
        Validate.isTrue(winner == null || winner.getArena().equals(this));
        Validate.isTrue(players != null);

        //Clean up players - teleport them back etc
        players.forEach(PlayerInfo::invalidate);

        tickTask.reset();

        if (winner != null) { //A winner has been determined
            PlayerInfo loser = players.getOther(winner);

            Bukkit.broadcastMessage(MainClass.getPrefix() + "§a" + winner.getPlayer().getName() + " §7hat gegen §c" + loser.getPlayer().getName() + " §7 gewonnen! (§6" + this.getName() + "§7)");
            // ^^^^ TODO: winners and losers could get random (fun) messages like in vanilla

            //Treat winner nicely
            getRewards().stream() //Add reward to inventory TODO: should be more random (Class RewardSet or so)
                    .forEach(winner.getPlayer().getInventory()::addItem);
        } else {
            if (sendUndecidedMessage) {
                Bukkit.broadcastMessage(MainClass.getPrefix() + "§7Der Kampf zwischen §a" +
                        players.getLeft().getPlayer().getName() +
                        "§7 und §a" + players.getRight().getPlayer().getName() +
                        " §7 is unentschieden ausgegangen! (§6" + this.getName() + "§7)");
            }
        }

        players = null;
    }

    /**
     * Removes this arena from cache and storage.
     */
    public void remove() {
        Validate.validState(configSection != null, "Can't remove already removed arena!");

        if (isOccupied()) {
            players.forEach(plr -> plr.getPlayer().sendMessage("§cDie Arena, in der du warst, wurde entfernt. Bitte entschuldige die Unannehmlichkeiten."));
            endGame(null);
        }

        configSection.getParent().set(configSection.getName(), null);
        Arenas.arenaCache.remove(getName());
        configSection = null;
    }

    /**
     * Returns whether this arena is valid and ready to be used.
     *
     * @return the current validity state
     */
    public boolean isValid() { //TODO: Should we use validityChecklist.isDone() ? -> overhead considerable?
        return configSection != null &&
                firstSpawn != null &&
                secondSpawn != null &&
                inventoryKit != null &&
                armorKit != null &&
                iconStack != null;
    }

    /**
     * Sends a checklist to given CommandSender explaining what is missing to make this arena valid, if anything.
     * @param sender the receiver of the checklist
     */
    public void sendChecklist(CommandSender sender) {
        if(isOccupied()) {
            sender.sendMessage("§7(besetzt)");
        }
        if(configSection == null) {
            sender.sendMessage("§c(gelöscht)");
            return;
        }

        CHECKLIST_RENDERER.renderFor(sender, validityChecklist);
    }

    public String getValidityString() {
        if(isOccupied()) {
            return "§7(besetzt)";
        }
        if(isValid()) {
            return "§a(valide)";
        }
        if(configSection == null) {
            return "§c(gelöscht)";
        }

        return "§c(invalide)";
    }

    ///////////// GETTERS //////////////////////////////////////////////////////////////////////////////////////////////

    public List<ItemStack> getRewards() {
        if (specificRewards == null || specificRewards.isEmpty()) {
            return Arenas.getDefaultRewards();
        }

        return specificRewards;
    }

    /**
     * @return whether this arena is ready to accept players.
     */
    public boolean isReady() {
        return isValid() && !isOccupied();
    }

    public boolean isOccupied() {
        return players != null;
    }

    public Couple<PlayerInfo> getPlayers() {
        return players;
    }

    /**
     * @return A string nicely showing this arena's current players.
     */
    public String getPlayerString() {
        if(!isOccupied()) {
            return "§7§oleer";
        }

        return "§c" +
                (players.getLeft() == null ? "???" : players.getLeft().getName()) +
                "§7 vs. §c" +
                (players.getRight() == null ? "???" : players.getRight().getName());
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

    protected ConfigurationSection getConfigSection() {
        return configSection;
    }

    /////// SETTERS ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setArmorKit(ItemStack[] armorKit) {
        this.armorKit = armorKit;
    }

    public void setInventoryKit(ItemStack[] inventoryKit) {
        this.inventoryKit = inventoryKit;
    }

    public void setFirstSpawn(Location firstSpawn) {
        Validate.validState(configSection != null, "This arena has been removed!");

        Arenas.saveLocation(configSection.createSection(FIRST_SPAWN_PATH), firstSpawn);
        this.firstSpawn = firstSpawn;
    }

    public void setSecondSpawn(Location secondSpawn) {
        Validate.validState(configSection != null, "This arena has been removed!");

        Arenas.saveLocation(configSection.createSection(SECOND_SPAWN_PATH), secondSpawn);
        this.secondSpawn = secondSpawn;
    }

    public void setIconStack(ItemStack iconStack) {
        Validate.validState(configSection != null, "This arena has been removed!");

        configSection.set(ICON_STACK_PATH, iconStack);
        this.iconStack = iconStack;
    }

    public void setRewards(List<ItemStack> specificRewards) {
        Validate.validState(configSection != null, "This arena has been removed!");

        this.configSection.set(SPECIFIC_REWARD_PATH, specificRewards);
        this.specificRewards = specificRewards;
    }

    /**
     * Gets the opposite player of the argument.
     *
     * @param plr Player not to target (you get it?!!)
     * @return opposite player or NULL if the argument is not in this arena or no opposite player could be found.
     */
    public PlayerInfo getOther(@NotNull Player plr) {
        Validate.validState(isValid(), "Arena is invalid!");

        if (players.getLeft().getPlayer().equals(plr)) {
            return players.getRight();
        } else if (players.getRight().getPlayer().equals(plr)) {
            return players.getLeft();
        } else {
            return null;
        }
    }

    public String getName() {
        return name;
    }

    //private utility methods
    @SuppressWarnings("unchecked")
    private void updateFromConfig() {
        Validate.validState(this.configSection != null, "This arena has been removed!");

        this.firstSpawn = Arenas.getLocation(configSection.getConfigurationSection("spawn1"));
        this.secondSpawn = Arenas.getLocation(configSection.getConfigurationSection("spawn2"));
        this.iconStack = configSection.getItemStack(ICON_STACK_PATH);
        this.specificRewards = (List<ItemStack>) configSection.getList(SPECIFIC_REWARD_PATH, new ArrayList<ItemStack>());

        if (configSection.contains(INVENTORY_KIT_PATH)) {
            List<ItemStack> tempInvKit = (List<ItemStack>) configSection.getList(INVENTORY_KIT_PATH);
            this.inventoryKit = tempInvKit == null ? null : tempInvKit.toArray(new ItemStack[InventoryType.PLAYER.getDefaultSize()]);
        }

        if (configSection.contains(ARMOR_KIT_PATH)) {
            List<ItemStack> tempInvKit = (List<ItemStack>) configSection.getList(ARMOR_KIT_PATH);
            this.inventoryKit = tempInvKit == null ? null : tempInvKit.toArray(new ItemStack[4]);
        }
    }

    private void teleportLater(@NotNull PlayerInfo playerInfo) {
        Validate.notNull(playerInfo.getPlayer(), "player is null");

        RunnableTeleportLater.TeleportCompleteHandler completeHandler =
                RunnableTeleportLater.MessageTeleportCompleteHandler.getHandler(Locale.GERMAN, (runnableTeleportLater, failureReason, lastTry) -> {
                    if (!playerInfo.isValid()) { //Players might execute /1vs1 leave during the teleport period
                        runnableTeleportLater.tryCancel();
                        return;
                    }

                    if (failureReason == null) {
                        playerInfo.setInArena(true);

                        if (players.getOther(playerInfo).isInArena()) {
                            startGame();
                        } else {
                            playerInfo.getPlayer().sendMessage("§eBitte warte, bis dein Gegner teleportiert wird.");
                        }
                    } else if (lastTry) {
                        playerInfo.getPlayer().sendMessage("§cWir haben es bereits oft genug probiert, die Teleportation wird jetzt abgebrochen.");
                        players.getOther(playerInfo).getPlayer()
                                .sendMessage("§4" + playerInfo.getName() + "§c konnte nicht stillhalten, daher kann das Spiel nicht beginnen. Bitte versuche es erneut.");
                        endGame(null, false);
                    }
                });

        new RunnableTeleportLater(playerInfo.getPlayer(), playerInfo.getSpawnLocation(), 5, completeHandler)
                .runTaskTimer(MainClass.instance(), MainClass.instance().getTeleportDelayTicks(), MainClass.instance().getTeleportDelayTicks());
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

    static void reloadArenas(FileConfiguration source) {
        Map<String, Arena> existingArenas = new HashMap<>(Arenas.arenaCache);
        Arenas.arenaCache.clear();

        if (source.contains(Arena.CONFIG_PATH)) {
            ConfigurationSection arenaSection = source.getConfigurationSection(Arena.CONFIG_PATH);
            for (String key : arenaSection.getKeys(false)) {
                Arena existingArena = existingArenas.get(key);
                ConfigurationSection section = source.getConfigurationSection(Arena.CONFIG_PATH + "." + key);

                if (existingArena == null) {
                    Arenas.arenaCache.put(key, Arena.fromConfigSection(section));
                } else {
                    existingArena.configSection = section;
                    existingArena.updateFromConfig();
                    Arenas.arenaCache.put(key, existingArena);
                    existingArenas.remove(key);
                }
            }
        }

        for (Arena removedArena : existingArenas.values()) {
            removedArena.getPlayers()
                    .forEach(pi -> pi.getPlayer().sendMessage("§cDeine Arena wurde entfernt. Bitte entschuldige die Unannehmlichkeiten!"));
            removedArena.endGame(null, false);
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
                .add("players", players)
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
                getPlayers().stream()
                        .forEach(pi -> pi.getPlayer().sendMessage(MainClass.getPrefix() + "§7Noch §e" + ticksLeft / 12 + " §7Minuten!"));
            } else if (ticksLeft == 6 || ticksLeft < 4) { //30, 15, 10 & 5 seconds before end
                getPlayers().stream()
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
        private final Location spawnLocation;
        private final String name;
        private Player player;
        private boolean valid = true;
        private boolean inArena = false;

        protected PlayerInfo(Player plr, Location spawnLocation) {
            this.spawnLocation = spawnLocation;
            this.player = plr;
            this.name = plr.getName();
            this.previousExperience = plr.getTotalExperience();
            this.previousLocation = plr.getLocation();

            Arenas.setPlayerArena(player, Arena.this);
        }

        /**
         * Invalidates this PlayerInfo, removing the player from the game.
         * This teleports them back to their previous location and resets their experience to the previous value.
         * This also clears their inventory.
         * <b>This doesn't, however, remove them from the player list of the associated {@link me.sebi7224.onevsone.arena.Arena}!</b>
         */
        protected void invalidate() {
            this.valid = false;
            this.inArena = false;
            player.setTotalExperience(previousExperience);
            player.teleport(previousLocation);
            player.setFoodLevel(20);
            player.setHealth(player.getMaxHealth());
            player.setFireTicks(0);
            player.setExhaustion(0.0F);
            player.setSaturation(9.6F); //Notch's Golden apple - balance between prev sat and taking away eventual golden apples consumed during the fight
            InventoryHelper.clearInventory(player);
            Arenas.setPlayerArena(player, null);
            this.player = null; //Don't keep Player ref in case this object is accidentally kept
        }

        /**
         * Sets the new inArena (i.e. whether the player is currently in the arena) value of this player.
         *
         * @param inArena whether the player is currently in the arena
         */
        protected void setInArena(boolean inArena) {
            this.inArena = inArena;
        }

        /**
         * This actually returns whether the player has been teleported to a spawn point but not teleported back yet.
         *
         * @return whether the player is currently in the arena
         */
        public boolean isInArena() {
            return inArena;
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

        public Location getSpawnLocation() {
            return spawnLocation;
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

        protected void sendTeleportMessage() {
            getPlayer().sendMessage(MainClass.getPrefix() + "§eDu wirst jetzt gegen §a" +
                    Arena.this.getPlayers().getOther(this).getName() + "§e kämpfen!");
            getPlayer().sendMessage(MainClass.getPrefix() + "§8Bitte stillhalten, du wirst in die Arena §7" + Arena.this.getName() + "§8 teleportiert!");
        }

        protected void sendStartMessage() {
            getPlayer().sendMessage(MainClass.getPrefix() + "§eMögen die Spiele beginnen!");
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
