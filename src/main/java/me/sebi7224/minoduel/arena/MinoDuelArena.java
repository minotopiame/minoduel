package me.sebi7224.minoduel.arena;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import me.sebi7224.minoduel.MinoDuelPlugin;
import me.sebi7224.minoduel.queue.QueueItem;
import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import io.github.xxyy.common.collections.Couple;
import io.github.xxyy.common.games.util.RunnableTeleportLater;
import io.github.xxyy.common.util.inventory.InventoryHelper;
import io.github.xxyy.common.util.task.NonAsyncBukkitRunnable;
import io.github.xxyy.lib.intellij_annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Represents a MinoDuel arena as loaded from configuration.
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 19.7.14 // 1.0
 */
public class MinoDuelArena extends ConfigurableArena { //TODO: It should be possible to put arenas out of service temporarily

    private Couple<PlayerInfo> players = null;
    private RunnableArenaTick tickTask = new RunnableArenaTick();

    public MinoDuelArena(@NotNull ConfigurationSection storageBackend) {
        super(storageBackend);
    }

    @Override
    public Collection<QueueItem> scheduleGame(@NotNull QueueItem... items) {
        Validate.isTrue(items.length == SIZE, "Can only accept SIZE queue items!");

        if(items[0].size() + items[1].size() == SIZE) {
            scheduleGame(items[0].getFirst(), items[1].getFirst());
            return ImmutableList.of(items[0], items[1]);
        } else if(items[0].size() == SIZE) {
            scheduleGame(items[0].getPlayers());
            return ImmutableList.of(items[0]);
        } else if(items[1].size() == SIZE) {
            scheduleGame(items[1].getPlayers());
            return ImmutableList.of(items[1]);
        } else { //No possible method to match any of these into this arena
            return ImmutableList.of();
        }
    }

    private void scheduleGame(@NotNull List<Player> players) {
        scheduleGame(players.get(0), players.get(1));
    }

    public void scheduleGame(@NotNull Player plr1, @NotNull Player plr2) {
        Validate.isTrue(isReady(), "This arena is currently not ready");
        Validate.notNull(plr1, "Player one is null");
        Validate.notNull(plr2, "Player two is null");
        Validate.isTrue(!Arenas.isInGame(plr1), "Player one is currently in another game!");
        Validate.isTrue(!Arenas.isInGame(plr2), "Player two is currently in another game!");

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

    @Override
    public void endGame(PlayerInfo winner, boolean sendUndecidedMessage) {
        Validate.isTrue(winner == null || winner.getArena().equals(this));
        Validate.isTrue(players != null);

        //Clean up players - teleport them back etc
        players.forEach(PlayerInfo::invalidate);

        tickTask.reset();

        if (winner != null) { //A winner has been determined
            PlayerInfo loser = players.getOther(winner);

            Bukkit.broadcastMessage(MinoDuelPlugin.PREFIX + "§a" + winner.getPlayer().getName() + " §7hat gegen §c" + loser.getPlayer().getName() + " §7 gewonnen! (§6" + this.getName() + "§7)");
            // ^^^^ TODO: winners and losers could get random (fun) messages like in vanilla

            //Treat winner nicely
            getRewards().stream() //Add reward to inventory TODO: should be more random (Class RewardSet or so)
                    .forEach(winner.getPlayer().getInventory()::addItem);
        } else {
            if (sendUndecidedMessage) {
                Bukkit.broadcastMessage(MinoDuelPlugin.PREFIX + "§7Der Kampf zwischen §a" +
                        players.getLeft().getPlayer().getName() +
                        "§7 und §a" + players.getRight().getPlayer().getName() +
                        " §7 is unentschieden ausgegangen! (§6" + this.getName() + "§7)");
            }
        }

        players = null;
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

    /////// SETTERS ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
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
                .runTaskTimer(MinoDuelPlugin.inst(), MinoDuelPlugin.inst().getTeleportDelayTicks(), MinoDuelPlugin.inst().getTeleportDelayTicks());
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
    public static MinoDuelArena fromConfigSection(ConfigurationSection section) {
        MinoDuelArena arena = new MinoDuelArena(section);
        arena.updateFromConfig();
        return arena;
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

    ///////////// BEST RUNNABLE ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Runnable which ticks Arenas every 5 seconds.
     */
    private class RunnableArenaTick extends NonAsyncBukkitRunnable {
        private static final int TICKS_IN_A_GAME = 60; //Ticks every 5 seconds
        private int ticksLeft = 0;

        public void start() {
            this.ticksLeft = TICKS_IN_A_GAME;
            this.runTaskTimer(MinoDuelPlugin.inst(), 20L * 5L, 20L * 5L); //This task so far only announces time left and the smallest interval is 5 seconds
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
                        .forEach(pi -> pi.getPlayer().sendMessage(MinoDuelPlugin.PREFIX + "§7Noch §e" + ticksLeft / 12 + " §7Minuten!"));
            } else if (ticksLeft == 6 || ticksLeft < 4) { //30, 15, 10 & 5 seconds before end
                getPlayers().stream()
                        .forEach(pi -> pi.getPlayer().sendMessage(MinoDuelPlugin.PREFIX + "§7Noch §e" + ticksLeft * 5 + " §7Sekunden!"));
            } else if (ticksLeft == 0) {
                MinoDuelArena.this.endGame(null);
            }
        }
    }

    //////////// LE INNER CLASS ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Stores information about a player playing in this MinoDuel arena.
     *
     * @author <a href="http://xxyy.github.io/">xxyy</a>
     * @since 20.7.14 // 1.0
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

            Arenas.setPlayerArena(player, MinoDuelArena.this);
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
            return isValid() ? MinoDuelArena.this : null;
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
            getPlayer().sendMessage(MinoDuelPlugin.PREFIX + "§eDu wirst jetzt gegen §a" +
                    MinoDuelArena.this.getPlayers().getOther(this).getName() + "§e kämpfen!");
            getPlayer().sendMessage(MinoDuelPlugin.PREFIX + "§8Bitte stillhalten, du wirst in die Arena §7" + MinoDuelArena.this.getName() + "§8 teleportiert!");
        }

        protected void sendStartMessage() {
            getPlayer().sendMessage(MinoDuelPlugin.PREFIX + "§eMögen die Spiele beginnen!");
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
