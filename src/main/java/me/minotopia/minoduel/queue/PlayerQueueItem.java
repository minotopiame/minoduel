package me.minotopia.minoduel.queue;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import me.minotopia.minoduel.MinoDuelPlugin;
import me.minotopia.minoduel.arena.Arena;
import org.bukkit.entity.Player;

import io.github.xxyy.lib.intellij_annotations.NotNull;
import io.github.xxyy.lib.intellij_annotations.Nullable;

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

    public PlayerQueueItem(@NotNull Player player, @Nullable Arena preferredArena) {
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

    @Override
    public boolean has(Player other) {
        return player.equals(other);
    }

    private String getMessage(QueueMessage type, Object... args) {
        String result = MinoDuelPlugin.PREFIX;

        switch (type) {
            case POSITION_NOTIFICATION:
                result += String.format(WaitingQueueManager.POSITION_NOTIFICATION_FORMAT, args);
                break;
            case REMOVAL_NOTIFICATION:
                result += String.format("Du hast die Warteschlange der Arena %s verlassen!",
                        (getPreferredArena() == null ? "(egal)" : getPreferredArena().getName()));
                break;
            case RELOAD_REMOVED:
                result += "Du hast die Warteschlange wegen eines Serverreloads verlassen. Wir bitten, die Unannehmlichkeiten zu entschuldigen.";
                break;
            default:
                throw new AssertionError("Unknown message type for " + getClass() + ": "+type.name());
        }

        return result;
    }

    @Override
    @SuppressWarnings("RedundantIfStatement")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PlayerQueueItem that = (PlayerQueueItem) o;

        if (!player.equals(that.player)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + player.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("player", player)
                .add("preferredArena", getPreferredArena())
                .toString();
    }
}
