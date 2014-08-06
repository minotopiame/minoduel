package me.sebi7224.minoduel.queue;

import me.sebi7224.minoduel.arena.Arena;
import org.bukkit.entity.Player;

import io.github.xxyy.common.annotation.Unused;
import io.github.xxyy.common.lib.com.intellij.annotations.NotNull;
import io.github.xxyy.common.lib.com.intellij.annotations.Nullable;

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
    void sendMessage(@NotNull QueueMessage message, @NotNull Object... args);

    /**
     * @return how many players are represented by this queue item
     */
    int size();

    @Unused
    boolean canFight(@Nullable QueueItem item);

    /**
     * @return an immutable list of all players represented by this queue item.
     */
    @NotNull
    List<Player> getPlayers();

    /**
     * @return the first player represented by this queue item.
     */
    @NotNull
    Player getFirst();

    /**
     * Checks whether a player is included in this item.
     *
     * @param player the player to look for
     * @return whether this player is represented by this queue item
     */
    boolean has(Player player);

    public enum QueueMessage {
        POSITION_NOTIFICATION
    }
}
