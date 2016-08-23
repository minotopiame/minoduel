package me.minotopia.minoduel.queue;

import me.minotopia.minoduel.arena.Arena;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Abstract implementation of a base queue item.
 * Subclasses should implement their own equals() and hashCode() since this class does not take players into account.
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
    public boolean has(Player player) {
        return getPlayers().contains(player);
    }

    @Override
    public boolean hasAny(Collection<Player> players) {
        return players.stream().anyMatch(this::has);
    }

    @Override
    public void onRemoval(Player cause) {
        sendMessage(QueueMessage.REMOVAL_NOTIFICATION, cause);
    }

    @Override
    @SuppressWarnings("RedundantIfStatement")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractQueueItem)) return false;

        AbstractQueueItem that = (AbstractQueueItem) o;

        if (size != that.size) return false;
        if (preferredArena != null ? !preferredArena.equals(that.preferredArena) : that.preferredArena != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = size;
        result = 31 * result + (preferredArena != null ? preferredArena.hashCode() : 0);
        return result;
    }
}
