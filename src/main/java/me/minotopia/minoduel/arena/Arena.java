package me.minotopia.minoduel.arena;

import li.l1t.common.collections.Couple;
import me.minotopia.minoduel.queue.QueueItem;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 6.8.14
 */
public interface Arena {
    int SIZE = 2;
    /**
     * A static string that gets appended to MinoDuel kit items so that they can be
     * identified if somehow removed from the game environment.
     */
    String KIT_LORE_MARKER = "§7Besitz dieses Items außerhalb von 1vs1 ist verboten!";

    /**
     * Tries to schedule a game from two queue items.
     * This first tries to schedule a game with both players. If that is not possible because this arena's size doesn't allow it,
     * each individual item is checked if it can fit in this arena.
     * <b>Only exactly {@link #SIZE} arguments will be accepted here.</b>
     *
     * @param items the items to try (checked in encountering order)
     * @return a collection of all items that are participating in the game. If no game was started, this is empty.
     */
    Collection<QueueItem> scheduleGame(@Nonnull QueueItem... items);

    /**
     * Ends this game and hands the winner their reward (if applicable).
     * This also broadcasts a message stating the game result.
     *
     * @param winner The winner of the game or NULL if no winner could be determined.
     */
    void endGame(@Nullable ArenaPlayerInfo winner);

    /**
     * Ends this game and hands the winner their reward (if applicable).
     * This also broadcasts a message stating the game result.
     *
     * @param winner               The winner of the game or NULL if no winner could be determined.
     * @param sendUndecidedMessage whether to send a message when no winner could be determined (i.e. {@code winner == null})
     */ //teleportBack is a bad design choice. Refactor if anyone has time.
    void endGame(ArenaPlayerInfo winner, boolean sendUndecidedMessage);

    String getValidityString();

    /**
     * @return whether this arena is ready to accept players.
     */
    boolean isReady();

    /**
     * Removes this arena from cache and storage.
     */
    void remove();

    /**
     * Returns whether this arena is valid and ready to be used.
     *
     * @return the current validity state
     */
    boolean isValid();

    /**
     * Sends a checklist to given CommandSender explaining what is missing to make this arena valid, if anything.
     *
     * @param sender the receiver of the checklist
     */
    void sendChecklist(CommandSender sender);

    List<ItemStack> getRewards();

    boolean isOccupied();

    Couple<? extends ArenaPlayerInfo> getPlayers();

    String getPlayerString();

    /**
     * Gets the opposite player of the argument.
     *
     * @param plr Player not to target (you get it?!!)
     * @return opposite player or NULL if the argument is not in this arena or no opposite player could be found.
     */
    ArenaPlayerInfo getOther(@Nonnull Player plr);

    Location getFirstSpawn();

    Location getSecondSpawn();

    ItemStack getIconStack();

    ItemStack[] getInventoryKit();

    ItemStack[] getArmorKit();

    boolean isEnabled();

    void setArmorKit(ItemStack[] armorKit);

    void setInventoryKit(ItemStack[] inventoryKit);

    void setFirstSpawn(Location firstSpawn);

    void setSecondSpawn(Location secondSpawn);

    void setIconStack(ItemStack iconStack);

    void setRewards(List<ItemStack> specificRewards);

    void setDoAllRewards(boolean doAllRewards);

    void setEnabled(boolean enabled);

    String getName();

    ArenaManager getArenaManager();

    ArenaState getState();
}
