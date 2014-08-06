package me.sebi7224.minoduel.queue;

import me.sebi7224.minoduel.arena.Arena;
import org.bukkit.entity.Player;

import io.github.xxyy.common.lib.com.intellij.annotations.Nullable;

/**
 * Abstract implementation of a base queue item.
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 6.8.14
 */
public abstract class AbstractQueueItem implements QueueItem {
    private final int size;
    private final Arena preferredArena;

    protected AbstractQueueItem(int size, Arena preferredArena) {
        this.size = size;
        this.preferredArena = preferredArena;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public Player getFirst() {
        return getPlayers().get(0);
    }

    @Override @Nullable
    public Arena getPreferredArena() {
        return preferredArena;
    }

    @Override
    public boolean canFight(@Nullable QueueItem item) {
        return (item == null ? 0 : item.size()) + size() == 2;
    }
}
