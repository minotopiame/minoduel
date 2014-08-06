package me.sebi7224.minoduel.queue;

import com.google.common.collect.ImmutableList;
import me.sebi7224.minoduel.MinoDuelPlugin;
import me.sebi7224.minoduel.arena.Arena;
import org.bukkit.entity.Player;

import io.github.xxyy.common.lib.com.intellij.annotations.NotNull;

import java.util.List;

/**
 * Represents a single player queueing for MinoDuel.
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 6.8.14
 */
public class PlayerQueueItem extends AbstractQueueItem {
    public static final int SIZE = 1;
    private final Player player;

    protected PlayerQueueItem(Player player, Arena preferredArena) {
        super(SIZE, preferredArena);
        this.player = player;
    }

    @Override
    public void sendMessage(@NotNull QueueMessage message, Object... args) {
        player.sendMessage(getMessage(message, args));
    }

    @Override
    public List<Player> getPlayers() {
        return ImmutableList.of(player);
    }

    @Override
    public Player getFirst() {
        return player;
    }

    private String getMessage(QueueMessage type, Object... args) {
        String result = MinoDuelPlugin.getPrefix();

        switch (type) {
            case POSITION_NOTIFICATION:
                result += String.format("Du bist §e%d.§6 in der Warteschlange der Arena §e%s§6!", args);
                break;
            default:
                throw new AssertionError("Unknown message type for " + getClass() + "!");
        }

        return result;
    }
}
