package me.minotopia.minoduel.arena;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import li.l1t.common.collections.Couple;
import li.l1t.common.games.util.RunnableTeleportLater;
import li.l1t.common.util.CommandHelper;
import li.l1t.common.util.XyValidate;
import li.l1t.common.util.inventory.InventoryHelper;
import me.minotopia.minoduel.MinoDuelPlugin;
import me.minotopia.minoduel.queue.QueueItem;
import org.apache.commons.lang.Validate;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Represents a MinoDuel arena as loaded from configuration.
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 19.7.14 // 1.0
 */
public class MinoDuelArena extends ConfigurableArena {

    private Couple<PlayerInfo> players = null;
    private MinoDuelArenaTaskManager tickManager = new MinoDuelArenaTaskManager(this);

    public MinoDuelArena(@Nonnull ConfigurationSection storageBackend, ArenaManager arenaManager) {
        super(storageBackend, arenaManager);

    }

    @Override
    public Collection<QueueItem> scheduleGame(@Nonnull QueueItem... items) {
        Validate.isTrue(items.length <= SIZE, "Expected maximum of SIZE queue items, given: ", items.length);

        Collection<QueueItem> fitItems = whichCanFit(items);
        if (!fitItems.isEmpty()) {
            scheduleGame(fitItems.stream()
                            .flatMap(item -> item.getPlayers().stream())
                            .collect(Collectors.toList())
            );
        }

        return fitItems;
    }

    private void scheduleGame(@Nonnull List<Player> players) {
        scheduleGame(players.get(0), players.get(1));
    }

    public void scheduleGame(@Nonnull Player plr1, @Nonnull Player plr2) {
        Validate.isTrue(getState() == ArenaState.READY, "This arena is currently not ready: ", getState());
        Validate.notNull(plr1, "Player one is null");
        Validate.notNull(plr2, "Player two is null");
        Validate.isTrue(!getArenaManager().isInGame(plr1), "Player one is currently in another game!");
        Validate.isTrue(!getArenaManager().isInGame(plr2), "Player two is currently in another game!");

        this.players = new Couple<>(
                new PlayerInfo(plr1, getFirstSpawn()),
                new PlayerInfo(plr2, getSecondSpawn())
        );

        players.forEach(PlayerInfo::notifyTeleport);
        getArenaManager().getPlugin().getServer().getScheduler().runTaskLater(getArenaManager().getPlugin(),
                () -> teleportLater(players.getLeft()), 3L * 20L); //Teleport 3s delayed to allow players to prepare
        getArenaManager().getPlugin().getServer().getScheduler().runTaskLater(getArenaManager().getPlugin(),
                () -> teleportLater(players.getRight()), (3L * 20L) + 1L); //Teleport a tick later to fix players not seeing each other

        state = ArenaState.TELEPORT;
    }

    private void startGame() {
        XyValidate.validateState(isValid(), "This arena is currently not valid");
        XyValidate.validateState(getState() == ArenaState.TELEPORT, "Invalid state: TELEPORT expected, got: %s", getState());
        XyValidate.validateState(players.getLeft().isValid(), "left player is invalid: %s" + players.getLeft().getName());
        XyValidate.validateState(players.getRight().isValid(), "right player is invalid: %s" + players.getRight().getName());

        this.players.forEach(PlayerInfo::prepare);

        state = ArenaState.WAIT;
        tickManager.start();
    }

    @Override
    public void endGame(ArenaPlayerInfo winner, boolean sendUndecidedMessage) {
        Validate.isTrue(winner == null || winner.getArena().equals(this));
        Validate.isTrue(players != null);

        tickManager.stop();
        Player winnerPlayer = null;

        if (winner != null) { //A winner has been determined
            winnerPlayer = winner.getPlayer(); //Need this later
            PlayerInfo loser = players.getOther((PlayerInfo) winner);

            CommandHelper.broadcast(MinoDuelPlugin.PREFIX + "§a" + winner.getName() + " §7hat gegen §c" + loser.getName() + " §7gewonnen! (§6" + this.getName() + "§7)", null);
            // ^^^^ TODO: winners and losers could get random (fun) messages like in vanilla
            //TODO: stats in MySQL w/ fancy leaderboard on website
        } else {
            if (sendUndecidedMessage) {
                CommandHelper.broadcast(MinoDuelPlugin.PREFIX + "§7Der Kampf zwischen §a" +
                        players.getLeft().getPlayer().getName() +
                        "§7 und §a" + players.getRight().getPlayer().getName() +
                        " §7 ist unentschieden ausgegangen! (§6" + this.getName() + "§7)", null);
            }
        }

        //Clean up players - teleport them back etc
        players.forEach(plr -> {
            getArenaManager().getPlugin().getMtcHook().setInGame(false, plr.getPlayer().getUniqueId());
            teleportBackLater(plr.getPlayer());
            plr.invalidate();
        });

        if (winnerPlayer != null) { //We need to do this here since inventories get cleared in invalidate()
            getRewards().stream() //Add reward to inventory TODO: should be more random (Class RewardSet or so)
                    .map(ItemStack::clone)
                    .forEach(winnerPlayer.getInventory()::addItem); //getPlayer() returns null after invalidate()
        }

        players = null;
        state = ArenaState.READY;
    }

    private void teleportBackLater(Player player) {
        if (getArenaManager().getPlugin().isEnabled() && getArenaManager().getPlugin().getLocationSaver().hasLocation(player)) {
            getArenaManager().getPlugin().getServer().getScheduler().runTaskLater(getArenaManager().getPlugin(),
                    () -> getArenaManager().getPlugin().getLocationSaver().loadLocation(player),
                    2L
            );
        }
    }

    @Override
    public String getValidityString() {
        if (isOccupied()) {
            return "(" + getPlayerString() + ")";
        }
        if (isValid()) {
            return "(valide)";
        }
        if (configSection == null) {
            return "(gelöscht)";
        }

        return "(invalide)";
    }

    ///////////// GETTERS //////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean isReady() {
        return isValid() && !isOccupied();
    }

    @Override
    public boolean isOccupied() {
        return players != null;
    }

    @Override
    public Couple<PlayerInfo> getPlayers() {
        return players;
    }

    @Override
    public String getPlayerString() {
        if (!isOccupied()) {
            return "§7§oleer";
        }

        return "§c" +
                (players.getLeft() == null ? "???" : players.getLeft().getName()) +
                "§7 vs. §c" +
                (players.getRight() == null ? "???" : players.getRight().getName());
    }

    @Override
    public ArenaPlayerInfo getOther(@Nonnull Player plr) {
        XyValidate.validateState(isValid(), "Arena is invalid!");

        if (players.getLeft().getPlayer().equals(plr)) {
            return players.getRight();
        } else if (players.getRight().getPlayer().equals(plr)) {
            return players.getLeft();
        } else {
            return null;
        }
    }

    protected void setState(ArenaState newState) {
        state = newState;
    }

    ///////////////////////////// PRIVATE UTIL /////////////////////////////////////////////////////////////////////////

    private void teleportLater(@Nonnull PlayerInfo playerInfo) {
        Validate.notNull(playerInfo.getPlayer(), "player is null");

        RunnableTeleportLater.TeleportCompleteHandler completeHandler =
                RunnableTeleportLater.MessageTeleportCompleteHandler.getHandler(Locale.GERMAN, (runnableTeleportLater, failureReason, lastTry) -> {
                    if (!playerInfo.isValid()) { //Players might execute /1vs1 leave during the teleport period
                        runnableTeleportLater.tryCancel();
                        return;
                    }

                    if (failureReason == null && !playerInfo.isInArena() /* race condition or something */) {
                        playerInfo.onArrival();

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
                .runTaskTimer(getArenaManager().getPlugin(), 20L,
                        getArenaManager().getPlugin().getTeleportDelayTicks());
    }

    private ItemStack[] cloneAndMarkKit(ItemStack[] stacks, String playerName) {
        ItemStack[] cleaned = InventoryHelper.cloneAll(stacks);

        for (ItemStack stack : cleaned) {
            if (stack == null || stack.getType().equals(Material.AIR)) {
                continue;
            }

            ItemMeta meta = stack.getItemMeta();
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add("§71vs1-Kit von §8" + playerName);
            lore.add(KIT_LORE_MARKER);
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }

        return cleaned;
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
    public static MinoDuelArena fromConfigSection(ConfigurationSection section, ArenaManager arenaManager) {
        MinoDuelArena arena = new MinoDuelArena(section, arenaManager);
        arena.updateFromConfig();
        return arena;
    }

    public static Collection<QueueItem> whichCanFit(@Nonnull QueueItem... items) {
        if (items.length > SIZE) {
            throw new UnsupportedOperationException("Cannot fit more than " + SIZE + " items: " + items.length);
        }

        if (items.length == 1) {
            if (items[0].size() == SIZE) {
                return ImmutableList.copyOf(items);
            }
        } else if (!items[0].equals(items[1])) {
            if (items[0].size() + items[1].size() == SIZE) {
                return ImmutableList.copyOf(items);
            } else if (items[0].size() == SIZE) {
                return ImmutableList.of(items[0]);
            } else if (items[1].size() == SIZE) {
                return ImmutableList.of(items[1]);
            }
        }

        return ImmutableList.of();
    }

    //////// OVERRIDDEN OBJECT METHODS /////////////////////////////////////////////////////////////////////////////////
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MinoDuelArena arena = (MinoDuelArena) o;

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

    //////////// LE INNER CLASS ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Stores information about a player playing in this MinoDuel arena.
     *
     * @author <a href="http://xxyy.github.io/">xxyy</a>
     * @since 20.7.14 // 1.0
     */
    public class PlayerInfo implements ArenaPlayerInfo {
        private final int previousExperience;
        private final Location spawnLocation;
        private final String name;
        private final UUID uniqueId;
        private Player player;
        private boolean valid = true;
        private boolean inArena = false;

        protected PlayerInfo(Player plr, Location spawnLocation) {
            this.spawnLocation = spawnLocation;
            this.player = plr;
            this.name = plr.getName();
            this.uniqueId = plr.getUniqueId();
            this.previousExperience = plr.getTotalExperience();
            getArenaManager().getPlugin().getLocationSaver().saveLocation(player);

            getArenaManager().setPlayerArena(player, MinoDuelArena.this);
        }

        /**
         * Invalidates this PlayerInfo, removing the player from the game.
         * This teleports them back to their previous location and resets their experience to the previous value.
         * This also clears their inventory.
         * <b>This doesn't, however, remove them from the player list of the associated {@link MinoDuelArena}!</b>
         */
        protected void invalidate() {
            this.valid = false;
            this.inArena = false;
            player.setTotalExperience(previousExperience);
            player.setFoodLevel(20);
            player.setHealth(player.getMaxHealth());
            player.setFireTicks(0);
            player.setExhaustion(0.0F);
            player.setSaturation(9.6F); //Notch's Golden apple - balance between prev sat and taking away eventual golden apples consumed during the fight
            player.getActivePotionEffects().forEach(eff -> player.removePotionEffect(eff.getType()));
            InventoryHelper.clearInventory(player);
            if (player.getOpenInventory() != null) {
                if (player.getOpenInventory().getType().equals(InventoryType.CRAFTING)) { //yes this is what having your own inventory open is, idk either
                    player.getOpenInventory().getTopInventory().clear();
                }
                player.getOpenInventory().setCursor(new ItemStack(Material.AIR));
                player.closeInventory();
            }
            getArenaManager().setPlayerArena(player, null);

            if (getArenaManager().getPlugin().isEnabled() && getArenaManager().getPlugin().getInventorySaver().hasInventory(player)) {
                getArenaManager().getPlugin().getServer().getScheduler().runTask(getArenaManager().getPlugin(), () -> {
                            if (getArenaManager().getPlugin().getInventorySaver().loadInventory(player)) {
                                player.sendMessage(MinoDuelPlugin.PREFIX + "Du hast dein Inventar von vorhin zurückerhalten!");
                            }
                        }
                );
            }

            this.player = null; //Don't keep Player ref in case this object is accidentally kept
        }

        protected boolean prepare() {
            player.showPlayer(getOther(player).getPlayer());

            if (getArenaManager().getPlugin().getMtcHook().isInOtherGame(player.getUniqueId())) {
                getOther(player).getPlayer().sendMessage("§cDein Gegner hat ein Spiel in einem anderen Plugin (" +
                        getArenaManager().getPlugin().getMtcHook().getBlockingPlugin(player.getUniqueId()) +
                        ") begonnen. 1vs1 kann daher nicht fortfahren.");
                player.getPlayer().sendMessage("§cDu hast ein Spiel in einem anderen Plugin (" +
                        getArenaManager().getPlugin().getMtcHook().getBlockingPlugin(player.getUniqueId()) +
                        ") begonnen. 1vs1 kann daher nicht fortfahren.");
                endGame(null, false);
                return false;
            }

            player.getInventory().setContents(cloneAndMarkKit(getInventoryKit(), name)); //This removes any items that were there before
            player.getInventory().setArmorContents(cloneAndMarkKit(getArmorKit(), name));
            getArenaManager().getPlugin().getEssentialsHook().disableGodMode(player);

            return true;
        }

        protected void onArrival() {
            if (!InventoryHelper.isInventoryEmpty(player)) {
                if (!getArenaManager().getPlugin().getInventorySaver().saveInventory(player)) {
                    getPlayer().sendMessage(MinoDuelPlugin.PREFIX + "§cDein Inventar konnte nicht gespeichert werden! Daher können wir nicht fortfahren :(");
                    getOther(getPlayer()).getPlayer().sendMessage(MinoDuelPlugin.PREFIX + "§cDas Inventar deines Gegners konnte nicht gepeichert werden," +
                            " daher musste das Spiel abgebrochen werden!");
                    endGame(null, false);
                    return;
                }
                getPlayer().sendMessage(MinoDuelPlugin.PREFIX + "§cDein Inventar wurde gespeichert. Du erhältst es nach dem Duell zurück.");
            }

            getArenaManager().getPlugin().getMtcHook().setInGame(true, player.getUniqueId());
            setInArena(true);
            player.setFireTicks(0);
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setGameMode(GameMode.SURVIVAL);
            player.closeInventory();
            player.getInventory().clear();
            player.updateInventory();
            player.setFlying(false);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMEN_TELEPORT, 1, 0);
        }

        /**
         * Sets the new inArena (i.e. whether the player is currently in the arena) value of this player.
         *
         * @param inArena whether the player is currently in the arena
         */
        protected void setInArena(boolean inArena) {
            this.inArena = inArena;

            if (inArena) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 1639 * 20, 128)); //This blocks any movements and additionally zooms in
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 1639 * 20, 128)); //This blocks jumping - Movement is possible w/o this
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 1639 * 20, 127)); //This blocks any damage
            }
        }

        /**
         * This actually returns whether the player has been teleported to a spawn point but not teleported back yet.
         *
         * @return whether the player is currently in the arena
         */
        public boolean isInArena() {
            return inArena;
        }

        @Override
        public Arena getArena() {
            return isValid() ? MinoDuelArena.this : null;
        }

        @Override
        public Location getSpawnLocation() {
            return spawnLocation;
        }

        @Override
        public Player getPlayer() {
            return isValid() ? player : null;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isValid() {
            return valid;
        }

        @Override
        public UUID getUniqueId() {
            return uniqueId;
        }

        protected void notifyTeleport() {
            getPlayer().playSound(getPlayer().getLocation(), Sound.BLOCK_NOTE_PLING, 1, 0.76F); //note 7, C#4
            getPlayer().sendMessage(MinoDuelPlugin.PREFIX + "§eDu wirst in 3 Sekunden gegen §a" +
                    MinoDuelArena.this.getPlayers().getOther(this).getName() + "§e kämpfen!");
            getPlayer().sendMessage(MinoDuelPlugin.PREFIX + "§8Bitte stillhalten, du wirst gleich teleportiert! (§7" + MinoDuelArena.this.getName() + "§8)");
        }

        protected void notifyGameStart() {
            getPlayer().sendMessage(MinoDuelPlugin.PREFIX + "§eMögen die Spiele beginnen!");
            getPlayer().playSound(getPlayer().getLocation(), Sound.BLOCK_NOTE_PLING, 1, 0.94F); //note 11, F4

            getPlayer().getActivePotionEffects().stream()
                    .forEach(eff -> getPlayer().removePotionEffect(eff.getType()));
        }

        protected void notifyWaitTick(int secondsLeft) {
            getPlayer().sendMessage(MinoDuelPlugin.PREFIX + "§7Das Spiel beginnt in §8" + secondsLeft + "§7 Sekunden!");
            getPlayer().playSound(getPlayer().getLocation(), Sound.BLOCK_NOTE_PLING, 1, 0.76F); //note 7, C#4
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
