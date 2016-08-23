package me.minotopia.minoduel.queue;

import me.minotopia.minoduel.arena.Arena;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * Represents an item in the MinoDuel waiting queue.
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 6.8.14
 */
public interface QueueItem {
    /**
     * @return the arena preferred by this item, or null if none
     */
    @Nullable
    Arena getPreferredArena();

    /**
     * Sends a pre-defined status message to this item's players.
     *
     * @param message the type of message to send
     * @param args    the arguments for the message, if required by the type.
     */
    void sendMessage(@Nonnull QueueMessage message, @Nonnull Object... args);

    /**
     * @return how many players are represented by this queue item
     */
    int size();

    /**
     * @return an immutable list of all players represented by this queue item.
     */
    @Nonnull
    List<Player> getPlayers();

    /**
     * @return the first player represented by this queue item.
     */
    @Nonnull
    Player getFirst();

    /**
     * Checks whether a player is included in this item.
     *
     * @param player the player to look for
     * @return whether this player is represented by this queue item
     */
    boolean has(Player player);

    /**
     * Checks whether this item contains any of the passed players.
     * @param players the players to check
     * @return whether any of the passed players is represented by this item
     */
    boolean hasAny(Collection<Player> players);

    /**
     * This is called when this item is removed from the queue.
     * This isn't called when this item is pushed to an arena.
     * @param cause the player who caused the removal or NULL if the item was removed as a whole
     */
    void onRemoval(Player cause);

    public enum QueueMessage {
        /**
         * The item is notified of its position.
         * Arguments: int (position), String (Arena name)
         */
        POSITION_NOTIFICATION,
        /**
         * The item is notified that it is removed because of a server reload.
         * Arguments: none
         */
        RELOAD_REMOVED,
        /**
         * The item is notified of its removal.
         * Argument: Player (player who caused the removal or NULL if the item was removed as a whole)
         */
        REMOVAL_NOTIFICATION
    }
}
